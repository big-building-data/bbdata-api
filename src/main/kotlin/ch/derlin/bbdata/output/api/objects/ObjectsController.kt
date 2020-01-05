package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.HiddenParam
import ch.derlin.bbdata.output.PageableAsQueryParam
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.types.UnitRepository
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@RestController
@RequestMapping("/objects")
@Tag(name = "Objects", description = "Manage objects")
class ObjectController(private val objectRepository: ObjectRepository) {

    @Protected
    @PageableAsQueryParam
    @GetMapping("")
    fun getObjects(
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
    fun getObject(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "writable", required = false, defaultValue = "false") writable: Boolean = false
    ): Objects? = objectRepository.findById(id, userId, writable).orElseThrow { ItemNotFoundException("object") }

    // ======== TAGS

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @PutMapping("{id}/tags")
    fun addTags(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "tags", required = true) tags: List<String>): ResponseEntity<String> =
            addOrDeleteTags(userId, id, tags, add = true)

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @DeleteMapping("{id}/tags")
    fun removeTags(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long,
            @RequestParam(name = "tags", required = true) tags: List<String>): ResponseEntity<String> =
            addOrDeleteTags(userId, id, tags, delete = true)

    private fun addOrDeleteTags(userId: Int, id: Long, tags: List<String>,
                                add: Boolean = false, delete: Boolean = false): ResponseEntity<String> {
        assert(add || delete)
        val obj = objectRepository.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }

        val statuses =
                if (delete) tags.map { obj.removeTag(it) }
                else tags.map { obj.addTag(it) }

        if (statuses.any { x -> x }) {
            objectRepository.save(obj)
            return CommonResponses.ok()
        }

        return CommonResponses.notModifed()
    }

    // ======== ENABLE/DISABLE

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @PostMapping("{id}/enable")
    fun enableObject(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = false)

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @PostMapping("{id}/disable")
    fun disableObject(
            @UserId userId: Int,
            @PathVariable(value = "id") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = true)

    private fun enableDisable(userId: Int, id: Long, disabled: Boolean): ResponseEntity<String> {
        val obj = objectRepository.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }
        if (obj.disabled != disabled) {
            obj.disabled = disabled
            objectRepository.save(obj)
            return CommonResponses.ok()
        }
        return CommonResponses.notModifed()
    }

}

@RestController
@RequestMapping("/objects")
@Tag(name = "Objects", description = "Manage objects")
class NewObjectController(private val objectRepository: ObjectRepository,
                          private val userGroupRepository: UserGroupRepository,
                          private val unitRepository: UnitRepository) {

    class NewObject {
        @NotNull
        @Size(min = Objects.NAME_MIN, max = Objects.NAME_MAX)
        val name: String? = null

        @Size(max = Beans.DESCRIPTION_MAX)
        val description: String? = null

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