package ch.derlin.bbdata.output

import ch.derlin.bbdata.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * date: 08.02.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.OUTPUT_ONLY)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestSecurityScopes {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping

    companion object {
        val RO_BODY = JsonEntity.empty(HU to REGULAR_USER_ID, HA to APIKEY(REGULAR_USER_ID, ro = true)) // read-only
        val BAD_APIKEY_BODY = JsonEntity.empty(HU to REGULAR_USER_ID, HA to "notanapikey") // wrong
        val PUBLIC_ENDPOINTS = setOf("/units", "/types", "/login")

        // extract the HttpMethod and URL of all registered endpoints, excluding doc
        private fun RequestMappingHandlerMapping.getEndpoints(): Sequence<Pair<HttpMethod?, String>> =
                this.handlerMethods.keys.asSequence().map {
                    Pair(
                            it.methodsCondition.methods.firstOrNull()?.let { HttpMethod.resolve(it.name) },
                            it.patternsCondition.patterns.first().toString().replace("\\{[a-zA-Z]+\\}".toRegex(), "1"))
                }.filterNot { it.first == null || it.second.startsWith("/doc") }

        // create one single string from the list of failed tests
        private fun List<Triple<HttpMethod, String, ResponseEntity<String>>>.toErrorMessage(): String =
                this.map { (method, url, resp) ->
                    "[${method}] ${url} -> ${resp.statusCode} ${resp.body?.let { it.substring(0, Math.min(30, it.length)) }}"
                }.joinToString("\n")
    }

    @Test
    fun `1-0 all non GET methods should have scope WRITE`() {
        // test all non GET methods and ensure it raises a FORBIDDEN exception because the apikey is read-only
        val problems = requestMappingHandlerMapping.getEndpoints().filterNot { (method, url) ->
            method == HttpMethod.GET || url.startsWith("/log")
        }.map { (method, url) ->
            Triple(method!!, url, restTemplate.exchange(url, HttpMethod.resolve(method.name), RO_BODY, String::class.java))
        }.filterNot {
            it.third.statusCode == HttpStatus.FORBIDDEN && it.third.body!!.contains("read-only")
        }.toList()

        Assertions.assertTrue(problems.isEmpty(), problems.toErrorMessage())
    }

    @Test
    fun `1-1 most GET methods should have at least scope READ`() {

        // test all (non-public) GET methods and ensure it raises a BAD APIKEY exception because the apikey is invalid
        val problems = requestMappingHandlerMapping.getEndpoints().filter { (method, url) ->
            method == HttpMethod.GET && !PUBLIC_ENDPOINTS.contains(url)
        }.map { (method, url) ->
            Triple(method!!, url, restTemplate.exchange(url, HttpMethod.resolve(method.name), BAD_APIKEY_BODY, String::class.java))
        }.filterNot {
            it.third.statusCode == HttpStatus.FORBIDDEN && it.third.body!!.contains("bad apikey")
        }.toList()

        Assertions.assertTrue(problems.isEmpty(), problems.toErrorMessage())
    }

}