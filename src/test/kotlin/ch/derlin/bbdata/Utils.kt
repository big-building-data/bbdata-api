package ch.derlin.bbdata

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.*
import org.springframework.web.client.RestClientException

/**
 * date: 19.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object JsonEntity {

    fun jsonHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        return headers
    }

    fun empty(vararg additionalHeaders: Pair<String, Any?>): HttpEntity<Unit> {
        val headers = jsonHeaders()
        additionalHeaders.map { headers.add(it.first, it.second.toString()) }
        return HttpEntity(Unit, headers)
    }

    fun <T> create(body: T, vararg additionalHeaders: Pair<String, Any?>): HttpEntity<T> {
        val headers = jsonHeaders()
        additionalHeaders.map { headers.add(it.first, it.second.toString()) }
        return HttpEntity(body, headers)
    }
}


// === GET

@Throws(RestClientException::class)
fun TestRestTemplate.getQueryJson(url: String, vararg additionalHeaders: Pair<String, Any?>): Pair<HttpStatus, DocumentContext> {
    val response = this.getQueryString(url, *additionalHeaders)
    return response.statusCode to JsonPath.parse(response.body)
}

@Throws(RestClientException::class)
fun TestRestTemplate.getQueryString(url: String, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.GET, JsonEntity.empty(*additionalHeaders), String::class.java)
}

// === PUT

@Throws(RestClientException::class)
fun TestRestTemplate.putQueryString(url: String, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.PUT, JsonEntity.empty(*additionalHeaders), String::class.java)
}

@Throws(RestClientException::class)
fun <T> TestRestTemplate.putWithBody(url: String, request: T, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.PUT, JsonEntity.create(request, *additionalHeaders), String::class)
}

// === POST

@Throws(RestClientException::class)
fun <T> TestRestTemplate.postWithBody(url: String, request: T, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.POST, JsonEntity.create(request, *additionalHeaders), String::class)
}

@Throws(RestClientException::class)
fun TestRestTemplate.postQueryString(url: String, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.POST, JsonEntity.empty(*additionalHeaders), String::class.java)
}

// === DELETE

@Throws(RestClientException::class)
fun TestRestTemplate.deleteQueryString(url: String, vararg additionalHeaders: Pair<String, Any?>): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.DELETE, JsonEntity.empty(*additionalHeaders), String::class.java)
}