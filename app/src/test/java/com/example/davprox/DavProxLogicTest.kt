package com.example.davprox

import com.example.davprox.data.TrafficRepository
import com.example.davprox.model.TrafficRecord
import org.junit.Assert.assertEquals
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
        assertTrue(curl.contains("-H \"Authorization: Bearer 12345\""))
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
