package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.api.types.Unit
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.ApikeyWrite
import ch.derlin.bbdata.output.security.UserId
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/objects")
class ObjectController(private val objectRepository: ObjectRepository) {

    @GetMapping("")
    fun getAll(
            @UserId userId: Int,
            @RequestParam(name = "writable", required = false) writable: Boolean,
            @RequestParam(name = "tags", required = false, defaultValue = "") unparsedTags: String,
            @RequestParam(name = "search", required = false, defaultValue = "") search: String,
            pageable: Pageable
    ): List<Objects> {
        val tags = unparsedTags.split(",").map { s -> s.trim() }.filter { s -> s.length > 0 }
        return if (tags.size > 0)
            objectRepository.findAllByTag(tags, userId, writable, search, pageable)
        else
            objectRepository.findAll(userId, writable, search, pageable)
    }

    @GetMapping("/{id}")
    fun getById(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "writable", required = false) writable: Boolean = false
    ): Objects? = objectRepository.findById(id, userId, writable).orElseThrow { AppException.itemNotFound() }

    @RequestMapping("{id}/tags", method = arrayOf(RequestMethod.PUT, RequestMethod.DELETE))
    @ApikeyWrite
    fun addOrDeleteTags(
            request: HttpServletRequest,
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "tags", required = true) rawTags: String = ""): ResponseEntity<Unit> {
        val obj = objectRepository.findById(id, userId, writable = true).orElseThrow { AppException.itemNotFound() }

        val tags = rawTags.split(",").map { t -> t.trim() }
        val modified =
                if (request.method == RequestMethod.DELETE.name) tags.any { obj.removeTag(it) }
                else tags.any { obj.addTag(it) }
        objectRepository.save(obj)

        return ResponseEntity.status(
                if (modified) HttpStatus.OK else HttpStatus.NOT_MODIFIED
        ).build()
    }

}

@RestController
@RequestMapping("/objects")
class NewObjectController(private val objectRepository: ObjectRepository,
                          private val userGroupRepository: UserGroupRepository) {

    class NewObject : Beans.NameDescription() {
        @NotNull
        val owner: Int? = null

        @NotEmpty
        val unitSymbol: String = ""
    }

    @PutMapping("")
    @ApikeyWrite
    fun newObject(@UserId userId: Int,
                  @RequestBody @Valid newObject: NewObject
                  ): Objects {

        val userGroup = userGroupRepository.findMine(userId, newObject.owner!!, admin = true).orElseThrow {
            AppException.forbidden("UserGroup '${newObject.owner}' does not exist or is not writable.")
        }

        return objectRepository.saveAndFlush(Objects(
                name = newObject.name,
                description = newObject.description,
                unit = Unit(symbol = newObject.unitSymbol),
                owner = userGroup
        ))
    }
}