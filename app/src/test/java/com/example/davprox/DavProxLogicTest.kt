package com.example.davprox

import com.example.davprox.data.TrafficRepository
import com.example.davprox.model.TrafficRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DavProxLogicTest {

    @Before
    fun setUp() {
        TrafficRepository.clearAll()
    }

    @Test
    fun testTrafficRecordCreationAndRepository() {
        val record = TrafficRecord(
            method = "POST",
            url = "https://httpbin.org/post",
            host = "httpbin.org",
            port = 443,
            path = "/post",
            requestHeaders = mapOf("Content-Type" to "application/json", "User-Agent" to "DavProx"),
            requestBody = "{\"name\":\"test\"}",
            responseCode = 200,
            responseMessage = "OK",
            responseHeaders = mapOf("Server" to "gunicorn"),
            responseBody = "{\"success\":true}",
            durationMs = 120
        )

        TrafficRepository.addRecord(record)
        val records = TrafficRepository.records.value

        assertEquals(1, records.size)
        assertEquals("POST", records[0].method)
        assertEquals(200, records[0].responseCode)
        assertTrue(records[0].isSuccess)
    }

    @Test
    fun testFormParameterParsingAndRedaction() {
        val rawForm = "username=david&password=MySecretPassword123&token=xyz789"
        val parsed = TrafficRecord.parseUrlEncoded(rawForm)

        assertEquals("david", parsed["username"])
        assertEquals("MySecretPassword123", parsed["password"])
        assertEquals("xyz789", parsed["token"])

        val record = TrafficRecord(
            method = "POST",
            url = "https://example.com/login",
            host = "example.com",
            port = 443,
            path = "/login",
            requestHeaders = mapOf("Content-Type" to "application/x-www-form-urlencoded", "Cookie" to "session=abc1234"),
            formParameters = parsed,
            requestBody = rawForm
        )

        val sanitized = record.getSanitized(true)
        assertEquals("[REDACTED]", sanitized.formParameters["password"])
        assertEquals("[REDACTED]", sanitized.formParameters["token"])
        assertEquals("david", sanitized.formParameters["username"])
        assertEquals("[REDACTED]", sanitized.requestHeaders["Cookie"])
    }

    @Test
    fun testEndpointDiscoveryAggregation() {
        val record1 = TrafficRecord(
            method = "GET",
            url = "https://api.example.com/v1/users?page=1",
            host = "api.example.com",
            port = 443,
            path = "/v1/users",
            queryParams = mapOf("page" to "1"),
            responseCode = 200
        )
        val record2 = TrafficRecord(
            method = "GET",
            url = "https://api.example.com/v1/users?page=2",
            host = "api.example.com",
            port = 443,
            path = "/v1/users",
            queryParams = mapOf("page" to "2"),
            responseCode = 200
        )

        TrafficRepository.addRecord(record1)
        TrafficRepository.addRecord(record2)

        val endpoints = TrafficRepository.getDiscoveredEndpoints()
        assertEquals(1, endpoints.size)
        assertEquals("api.example.com", endpoints[0].domain)
        assertEquals("/v1/users", endpoints[0].path)
        assertEquals(2, endpoints[0].observationCount)
        assertTrue(endpoints[0].queryParams.contains("page"))
    }

    @Test
    fun testCurlGeneration() {
        val record = TrafficRecord(
            method = "GET",
            url = "https://example.com/api/v1/users",
            host = "example.com",
            port = 443,
            requestHeaders = mapOf("Authorization" to "Bearer 12345")
        )

        val curl = TrafficRepository.generateCurl(record)
        assertTrue(curl.contains("curl -X GET \"https://example.com/api/v1/users\""))
    }

    @Test
    fun testJsonExportFormat() {
        val record = TrafficRecord(
            method = "GET",
            url = "https://httpbin.org/get",
            host = "httpbin.org",
            port = 443,
            responseCode = 200
        )
        TrafficRepository.addRecord(record)

        val json = TrafficRepository.exportAsJson()
        assertNotNull(json)
        assertTrue(json.contains("DavProx by ENTITY_007 (Chukwuyem David)"))
        assertTrue(json.contains("httpbin.org"))
    }
}
