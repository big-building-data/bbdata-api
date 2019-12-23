package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.HiddenParam
import ch.derlin.bbdata.output.PageableAsQueryParam
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.types.Unit
import ch.derlin.bbdata.output.api.types.UnitRepository
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Objects", description = "Manage objects")
class ObjectController(private val objectRepository: ObjectRepository) {

    @Protected
    @PageableAsQueryParam
    @GetMapping("")
    fun getAll(
            @UserId userId: Int,
            @RequestParam(name = "writable", required = false, defaultValue = "false") writable: Boolean,
            @RequestParam(name = "tags", required = false, defaultValue = "") unparsedTags: String,
            @RequestParam(name = "search", required = false, defaultValue = "") search: String,
            @HiddenParam pageable: Pageable
    ): List<Objects> {
        val tags = unparsedTags.split(",").map { s -> s.trim() }.filter { s -> s.length > 0 }
        return if (tags.size > 0)
            objectRepository.findAllByTag(tags, userId, writable, search, pageable)
        else
            objectRepository.findAll(userId, writable, search, pageable)
    }

    @Protected
    @GetMapping("/{id}")
    fun getById(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "writable", required = false, defaultValue = "false") writable: Boolean = false
    ): Objects? = objectRepository.findById(id, userId, writable).orElseThrow { ItemNotFoundException("object") }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @RequestMapping("{id}/tags", method = arrayOf(RequestMethod.PUT, RequestMethod.DELETE))
    fun addOrDeleteTags(
            request: HttpServletRequest,
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "tags", required = true) rawTags: String = ""): ResponseEntity<Unit> {
        val obj = objectRepository.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }

        val tags = rawTags.split(",").map { t -> t.trim() }
        // TODO: modified not working !!!
        val statuses =
                if (request.method == RequestMethod.DELETE.name) tags.map { obj.removeTag(it) }
                else tags.map { obj.addTag(it) }
        objectRepository.save(obj)

        return ResponseEntity.status(
                if (statuses.any { x -> x }) HttpStatus.OK else HttpStatus.NOT_MODIFIED
        ).build()
    }

}

@RestController
@RequestMapping("/objects")
@Tag(name = "Objects", description = "Manage objects")
class NewObjectController(private val objectRepository: ObjectRepository,
                          private val userGroupRepository: UserGroupRepository,
                          private val unitRepository: UnitRepository) {

    class NewObject : Beans.NameDescription() {
        @NotNull
        val owner: Int? = null

        @NotEmpty
        val unitSymbol: String = ""
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("")
    fun newObject(@UserId userId: Int,
                  @RequestBody @Valid newObject: NewObject
    ): Objects {

        val userGroup = userGroupRepository.findMine(userId, newObject.owner!!, admin = true).orElseThrow {
            ItemNotFoundException("UserGroup ('${newObject.owner}', writable=true)")
        }

        val unit = unitRepository.findById(newObject.unitSymbol).orElseThrow {
            WrongParamsException("Unit '${newObject.unitSymbol}' does not exist.")
        }

        return objectRepository.saveAndFlush(Objects(
                name = newObject.name,
                description = newObject.description,
                unit = unit,
                owner = userGroup
        ))
    }
}