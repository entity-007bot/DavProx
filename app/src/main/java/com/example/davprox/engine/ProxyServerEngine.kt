package com.example.davprox.engine

import android.util.Log
import com.example.davprox.data.TrafficRepository
import com.example.davprox.model.TrafficClassification
import com.example.davprox.model.TrafficRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID

object ProxyServerEngine {
    private const val TAG = "DavProxEngine"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentPort = MutableStateFlow(8080)
    val currentPort: StateFlow<Int> = _currentPort.asStateFlow()

    private val _activeConnections = MutableStateFlow(0)
    val activeConnections: StateFlow<Int> = _activeConnections.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving IP address", e)
        }
        return "127.0.0.1"
    }

    @Synchronized
    fun startProxy(port: Int = _currentPort.value) {
        if (_isRunning.value) {
            stopProxy()
        }

        _lastError.value = null
        _currentPort.value = port

        serverJob = scope.launch {
            try {
                // Bind to 0.0.0.0 so all interfaces (loopback 127.0.0.1 and Wi-Fi LAN IP) can route traffic
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(port))
                serverSocket = socket
                _isRunning.value = true
                Log.i(TAG, "DavProx Proxy Server started on 0.0.0.0:$port")

                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        clientSocket.soTimeout = 20000 // 20s socket timeout
                        _activeConnections.value += 1
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.e(TAG, "Socket accept error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start proxy on port $port", e)
                _lastError.value = "Failed to bind port $port: ${e.localizedMessage}"
                _isRunning.value = false
            }
        }
    }

    @Synchronized
    fun stopProxy() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        } finally {
            _isRunning.value = false
            _activeConnections.value = 0
            Log.i(TAG, "DavProx Proxy Server stopped")
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "127.0.0.1"
        try {
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            // Keep connection open for HTTP Keep-Alive persistent sessions from Chrome
            var keepAlive = true
            while (keepAlive && clientSocket.isConnected && !clientSocket.isClosed && scope.isActive) {
                // Read HTTP request line
                val requestLine = try {
                    readLine(clientIn)
                } catch (_: SocketTimeoutException) {
                    break
                } ?: break

                val trimmed = requestLine.trim()
                if (trimmed.isEmpty()) continue

                val parts = trimmed.split(" ")
                if (parts.size < 2) break

                val method = parts[0].uppercase()
                val rawTarget = parts[1]
                val protocol = if (parts.size >= 3) parts[2] else "HTTP/1.1"

                // Read request headers
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine(clientIn) ?: break
                    if (line.isEmpty()) break
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1) {
                        val key = line.substring(0, colonIdx).trim()
                        val value = line.substring(colonIdx + 1).trim()
                        headers[key] = value
                    }
                }

                val connHeader = headers["Connection"] ?: headers["Proxy-Connection"] ?: ""
                if (connHeader.equals("close", ignoreCase = true) || protocol == "HTTP/1.0") {
                    keepAlive = false
                }

                if (method == "CONNECT") {
                    handleHttpsConnect(clientSocket, clientIn, clientOut, rawTarget, protocol, headers, clientIp)
                    keepAlive = false // CONNECT tunnels take over the socket completely
                } else {
                    handleHttpRequest(clientIn, clientOut, method, rawTarget, protocol, headers, clientIp)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling client connection: ${e.message}")
        } finally {
            _activeConnections.value = maxOf(0, _activeConnections.value - 1)
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun handleHttpsConnect(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        target: String,
        protocol: String,
        headers: Map<String, String>,
        clientIp: String
    ) {
        val startTime = System.currentTimeMillis()
        val hostPort = target.split(":")
        val host = hostPort[0]
        val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 443 else 443

        val recordId = UUID.randomUUID().toString()
        val initialRecord = TrafficRecord(
            id = recordId,
            timestamp = startTime,
            method = "CONNECT",
            scheme = "https",
            url = "https://$target",
            host = host,
            port = port,
            path = "/",
            protocol = protocol,
            requestHeaders = headers,
            requestBody = null,
            responseCode = 0,
            responseMessage = "TLS Tunnel Handshake",
            durationMs = 0,
            isTlsTunnel = true,
            classification = TrafficClassification.NETWORK_TUNNEL,
            clientIp = clientIp
        )
        TrafficRepository.addRecord(initialRecord)

        var remoteSocket: Socket? = null
        try {
            remoteSocket = Socket(host, port)
            remoteSocket.soTimeout = 45000
            val destIp = remoteSocket.inetAddress?.hostAddress

            // Respond 200 OK to establish tunnel with Chrome/browser
            val okResponse = "HTTP/1.1 200 Connection Established\r\nProxy-Agent: DavProx/1.0\r\n\r\n"
            clientOut.write(okResponse.toByteArray(Charsets.US_ASCII))
            clientOut.flush()

            val tunnelConnected = System.currentTimeMillis()
            TrafficRepository.updateRecord(
                initialRecord.copy(
                    destIp = destIp,
                    responseCode = 200,
                    responseMessage = "Connection Established",
                    durationMs = tunnelConnected - startTime
                )
            )

            // Bi-directional relay between browser client and remote server
            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()

            val clientToRemote = scope.launch(Dispatchers.IO) {
                pipeStreams(clientIn, remoteOut)
            }
            val remoteToClient = scope.launch(Dispatchers.IO) {
                pipeStreams(remoteIn, clientOut)
            }

            scope.launch(Dispatchers.IO) {
                clientToRemote.join()
                remoteToClient.join()
                val totalDuration = System.currentTimeMillis() - startTime
                TrafficRepository.updateRecord(
                    initialRecord.copy(
                        destIp = destIp,
                        responseCode = 200,
                        responseMessage = "Tunnel Closed",
                        durationMs = totalDuration
                    )
                )
            }
        } catch (e: Exception) {
            val failDuration = System.currentTimeMillis() - startTime
            try {
                val failResponse = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: text/plain\r\n\r\nDavProx: ${e.message}"
                clientOut.write(failResponse.toByteArray())
                clientOut.flush()
            } catch (_: Exception) {}

            TrafficRepository.updateRecord(
                initialRecord.copy(
                    responseCode = 502,
                    responseMessage = "Bad Gateway",
                    error = e.localizedMessage,
                    durationMs = failDuration
                )
            )
        }
    }

    private fun handleHttpRequest(
        clientIn: InputStream,
        clientOut: OutputStream,
        method: String,
        rawTarget: String,
        protocol: String,
        headers: Map<String, String>,
        clientIp: String
    ) {
        val startTime = System.currentTimeMillis()
        val recordId = UUID.randomUUID().toString()

        // Read request body if present (e.g. form POST, JSON payload)
        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        val requestBodyBytes = if (contentLength > 0) {
            readExact(clientIn, contentLength)
        } else {
            ByteArray(0)
        }
        val requestBodyStr = if (requestBodyBytes.isNotEmpty()) {
            String(requestBodyBytes, Charsets.UTF_8)
        } else null

        // Parse target URL, host, path, query parameters
        val urlObj = try {
            if (rawTarget.startsWith("http://") || rawTarget.startsWith("https://")) {
                URL(rawTarget)
            } else {
                val hostHeader = headers["Host"] ?: "localhost"
                URL("http://$hostHeader$rawTarget")
            }
        } catch (e: Exception) {
            null
        }

        val scheme = urlObj?.protocol ?: "http"
        val host = urlObj?.host ?: headers["Host"] ?: "unknown"
        val port = if (urlObj != null && urlObj.port != -1) urlObj.port else if (scheme == "https") 443 else 80
        val fullUrl = urlObj?.toString() ?: rawTarget
        val requestPath = if (urlObj != null) {
            val p = urlObj.path.ifEmpty { "/" }
            if (urlObj.query != null) "$p?${urlObj.query}" else p
        } else {
            rawTarget
        }

        // Parse query parameters
        val queryParams = mutableMapOf<String, String>()
        urlObj?.query?.let { q ->
            queryParams.putAll(TrafficRecord.parseUrlEncoded(q))
        }

        // Parse form parameters if application/x-www-form-urlencoded or application/json
        val reqContentType = headers["Content-Type"]
        val formParams = mutableMapOf<String, String>()
        if (reqContentType?.contains("application/x-www-form-urlencoded", ignoreCase = true) == true) {
            formParams.putAll(TrafficRecord.parseUrlEncoded(requestBodyStr))
        } else if (reqContentType?.contains("application/json", ignoreCase = true) == true && requestBodyStr != null) {
            try {
                val json = JSONObject(requestBodyStr)
                json.keys().forEach { k ->
                    formParams[k] = json.optString(k, "")
                }
            } catch (_: Exception) {}
        }

        val initialRecord = TrafficRecord(
            id = recordId,
            timestamp = startTime,
            method = method,
            scheme = scheme,
            url = fullUrl,
            host = host,
            port = port,
            path = requestPath.substringBefore("?"),
            queryParams = queryParams,
            protocol = protocol,
            requestHeaders = headers,
            requestContentType = reqContentType,
            requestBody = requestBodyStr,
            formParameters = formParams,
            requestSize = (requestLineBytes(method, requestPath, protocol) + headersSize(headers) + requestBodyBytes.size).toLong(),
            classification = TrafficClassification.PARSED_HTTP,
            clientIp = clientIp
        )
        TrafficRepository.addRecord(initialRecord)

        var remoteSocket: Socket? = null
        try {
            remoteSocket = Socket(host, port)
            remoteSocket.soTimeout = 15000
            val destIp = remoteSocket.inetAddress?.hostAddress

            val remoteOut = remoteSocket.getOutputStream()
            val remoteIn = remoteSocket.getInputStream()

            // Forward the modified request line to upstream server
            val outboundReqLine = "$method $requestPath $protocol\r\n"
            remoteOut.write(outboundReqLine.toByteArray(Charsets.US_ASCII))

            // Forward headers (strip proxy specific ones and set close for clear upstream boundary)
            for ((key, value) in headers) {
                if (!key.equals("Proxy-Connection", ignoreCase = true)) {
                    remoteOut.write("$key: $value\r\n".toByteArray(Charsets.US_ASCII))
                }
            }
            remoteOut.write("Connection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))

            // Forward request body if present
            if (requestBodyBytes.isNotEmpty()) {
                remoteOut.write(requestBodyBytes)
            }
            remoteOut.flush()

            // Read the upstream response
            val responseStatusLine = readLine(remoteIn) ?: throw Exception("Empty response from upstream")
            val statusParts = responseStatusLine.split(" ", limit = 3)
            val statusCode = if (statusParts.size >= 2) statusParts[1].toIntOrNull() ?: 200 else 200
            val statusMsg = if (statusParts.size >= 3) statusParts[2] else ""

            val responseHeaders = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(remoteIn) ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = line.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()
                    responseHeaders[key] = value
                }
            }

            val respContentType = responseHeaders["Content-Type"]
            val respTransferEncoding = responseHeaders["Transfer-Encoding"]
            val respContentLen = responseHeaders["Content-Length"]?.toIntOrNull()

            val responseBodyBytes = ByteArrayOutputStream()

            // Read response body based on content length or chunked encoding
            if (respTransferEncoding?.contains("chunked", ignoreCase = true) == true) {
                readChunkedBody(remoteIn, responseBodyBytes)
            } else if (respContentLen != null && respContentLen > 0) {
                val body = readExact(remoteIn, respContentLen)
                responseBodyBytes.write(body)
            } else {
                // Read until EOF or socket close from remote server
                val buffer = ByteArray(4096)
                var read: Int
                try {
                    while (remoteIn.read(buffer).also { read = it } != -1) {
                        responseBodyBytes.write(buffer, 0, read)
                    }
                } catch (_: SocketTimeoutException) {}
            }

            val finalBodyBytes = responseBodyBytes.toByteArray()
            val finalDuration = System.currentTimeMillis() - startTime

            // Write full response back to browser client
            clientOut.write("$responseStatusLine\r\n".toByteArray(Charsets.US_ASCII))
            for ((k, v) in responseHeaders) {
                clientOut.write("$k: $v\r\n".toByteArray(Charsets.US_ASCII))
            }
            clientOut.write("\r\n".toByteArray(Charsets.US_ASCII))
            if (finalBodyBytes.isNotEmpty()) {
                clientOut.write(finalBodyBytes)
            }
            clientOut.flush()

            val bodyString = try {
                String(finalBodyBytes, Charsets.UTF_8)
            } catch (_: Exception) {
                "[Binary payload: ${finalBodyBytes.size} bytes]"
            }

            TrafficRepository.updateRecord(
                initialRecord.copy(
                    destIp = destIp,
                    responseCode = statusCode,
                    responseMessage = statusMsg,
                    responseHeaders = responseHeaders,
                    responseContentType = respContentType,
                    responseBody = bodyString,
                    durationMs = finalDuration,
                    responseSize = finalBodyBytes.size.toLong()
                )
            )
        } catch (e: Exception) {
            val failDuration = System.currentTimeMillis() - startTime
            val failMsg = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nDavProx: ${e.message}"
            try {
                clientOut.write(failMsg.toByteArray())
                clientOut.flush()
            } catch (_: Exception) {}

            TrafficRepository.updateRecord(
                initialRecord.copy(
                    responseCode = 502,
                    responseMessage = "Bad Gateway",
                    error = e.localizedMessage,
                    durationMs = failDuration
                )
            )
        } finally {
            try { remoteSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun readChunkedBody(input: InputStream, output: ByteArrayOutputStream) {
        while (true) {
            val sizeLine = readLine(input) ?: break
            val chunkSizeStr = sizeLine.trim().split(";")[0]
            val chunkSize = chunkSizeStr.toIntOrNull(16) ?: break
            if (chunkSize == 0) {
                // Read trailing CRLF
                readLine(input)
                break
            }
            val chunk = readExact(input, chunkSize)
            output.write(chunk)
            // Read trailing CRLF after chunk
            readLine(input)
        }
    }

    private fun readLine(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        var b: Int
        var prevWasCr = false
        while (input.read().also { b = it } != -1) {
            if (b == '\n'.code) {
                val bytes = out.toByteArray()
                val len = if (prevWasCr && bytes.isNotEmpty()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, Charsets.US_ASCII)
            }
            prevWasCr = (b == '\r'.code)
            out.write(b)
        }
        if (out.size() == 0) return null
        return out.toString(Charsets.US_ASCII.name())
    }

    private fun readExact(input: InputStream, length: Int): ByteArray {
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(data, offset, length - offset)
            if (count == -1) break
            offset += count
        }
        return if (offset == length) data else data.copyOf(offset)
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var read: Int
        try {
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: Exception) {}
    }

    private fun requestLineBytes(m: String, p: String, proto: String): Int =
        (m.length + p.length + proto.length + 4)

    private fun headersSize(headers: Map<String, String>): Int =
        headers.entries.sumOf { it.key.length + it.value.length + 4 }
}
