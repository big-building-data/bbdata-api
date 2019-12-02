package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.ApikeyWrite
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException

@RestController
@RequestMapping("/objects")
class ObjectController(private val repo: ObjectRepository) {
    @GetMapping("")
    fun getAll(
            @RequestHeader(value = Constants.HEADER_USER) userId: Int,
            @RequestParam(name = "writable", required = false) writable: Boolean,
            @RequestParam(name = "tags", required = false, defaultValue = "") unparsedTags: String,
            @RequestParam(name = "search", required = false, defaultValue = "") search: String,
            pageable: Pageable
    ): List<Objects> {
        val tags = unparsedTags.split(",").map { s -> s.trim() }.filter { s -> s.length > 0 }
        return if (tags.size > 0)
            repo.findAllByTag(tags, userId, writable, search, pageable)
        else
            repo.findAll(userId, writable, search, pageable)
    }

    @GetMapping("/{id}")
    fun getById(
            @PathVariable(value = "id") id: Long,
            @RequestHeader(value = Constants.HEADER_USER) userId: Int,
            @RequestParam(name = "writable", required = false) writable: Boolean = false
    ): Objects? = repo.findById(id, userId, writable) ?: throw AppException.itemNotFound()


    @GetMapping("/exc/{id}")
    fun exc(@PathVariable(value = "id") id: Long): String {
        when (id) {
            1L -> throw AppException.badApiKey()
            2L -> throw AppException.create(HttpStatus.BAD_REQUEST, "BadX", mapOf("x" to 1, "y" to mapOf("z" to "sf")))
            3L -> throw RuntimeException("argg!")
            5L -> throw AppException.fromThrowable(ClassNotFoundException("alésdkfj"))
            6L -> throw AppException.forbidden("nope.")
            else -> return "OK"
        }
    }

    @PutMapping("")
    fun newObject(@RequestBody objects: Objects, @RequestHeader(value = Constants.HEADER_USER) userId: Int) {
        // TODO: get user group + add unitSymbol etc to the object entity
        repo.save(objects)
    }
}