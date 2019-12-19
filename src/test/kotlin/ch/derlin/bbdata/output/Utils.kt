package ch.derlin.bbdata.output

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.web.client.RestClientException

/**
 * date: 19.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

object JsonEntity {
    val headers = HttpHeaders()
    val EMPTY: HttpEntity<Unit>

    init {
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        EMPTY = HttpEntity(Unit, headers)

    }

    fun <T> create(body: T) = HttpEntity(body, headers)
}

@Throws(RestClientException::class)
fun <T> TestRestTemplate.putForEntity(url: String, request: T, responseType: Class<T>, vararg uriVariables: Any?): ResponseEntity<T> {
    return this.exchange(url, HttpMethod.PUT, JsonEntity.create(request), responseType, uriVariables)
}

@Throws(RestClientException::class)
fun TestRestTemplate.getQueryJson(url: String, vararg uriVariables: Any?): Pair<HttpStatus, DocumentContext> {
    val response = this.getQueryString(url, *uriVariables)
    return response.statusCode to JsonPath.parse(response.body)
}

@Throws(RestClientException::class)
fun TestRestTemplate.getQueryString(url: String, vararg uriVariables: Any?): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.GET, JsonEntity.EMPTY, String::class.java, uriVariables)
}

@Throws(RestClientException::class)
fun TestRestTemplate.putQueryString(url: String, vararg uriVariables: Any?): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.PUT, JsonEntity.EMPTY, String::class.java, uriVariables)
}

@Throws(RestClientException::class)
fun TestRestTemplate.deleteQueryString(url: String, vararg uriVariables: Any?): ResponseEntity<String> {
    return this.exchange(url, HttpMethod.DELETE, JsonEntity.EMPTY, String::class.java, uriVariables)
}