package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.common.Beans
import ch.derlin.bbdata.common.HiddenParam
import ch.derlin.bbdata.common.PageableAsQueryParam
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.types.UnitRepository
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.object_groups.ObjectGroupsController
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
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
    @Operation(description = "Get the list of objects.<br>" +
            "Use `writable=true` to get only objects you own (hence have right to edit). " +
            "Use `tags=tag1[,tagN]` to filter the list by tags. " +
            "Use `search=substring` to look for `substring` in the object names.<br>" +
            "Note: you can also paginate and sort the results as described below. Don't specify `size` for no pagination (default).")
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
    @Operation(description = "Get an object details.")
    @GetMapping("/{objectId}")
    fun getObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long
    ): Objects? = objectRepository.findById(id, userId, writable = false).orElseThrow { ItemNotFoundException("object") }

    @Protected
    @Operation(description = "Get all object groups an object belongs to.")
    @GetMapping("/{objectId}/objectGroups")
    fun getGroupsOfObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long
    ): List<ObjectGroup>? = objectRepository.findById(id, userId, writable = false).orElseThrow { ItemNotFoundException("object") }.getObjectGroups()

    // ======== TAGS

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Add tag(s) to an object you own. Use `,` to specify multiple tags, e.g. `tags=one,two`. " +
            "If all tags are already present, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @PutMapping("{objectId}/tags")
    fun addTags(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long,
            @RequestParam(name = "tags", required = true) tags: List<String>): ResponseEntity<String> =
            addOrDeleteTags(userId, id, tags, add = true)

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Remove tag(s) from an object you own. Use `,` to specify multiple tags, e.g. `tags=one,two`. " +
            "If no tags is already present, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @DeleteMapping("{objectId}/tags")
    fun removeTags(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long,
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
    @Operation(description = "Enable an object you own. If the object is already enabled, it simply returns NOT MODIFIED.")
    @SimpleModificationStatusResponse
    @PostMapping("{objectId}/enable")
    fun enableObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = false)

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Disable an object you own. If the object is already disabled, it simply returns NOT MODIFIED. " +
            "When an object gets disabled, all its tokens are deleted and it cannot receive new values (no new measures).")
    @SimpleModificationStatusResponse
    @PostMapping("{objectId}/disable")
    fun disableObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = true)


    private fun enableDisable(userId: Int, id: Long, disabled: Boolean): ResponseEntity<String> {
        // note: the deletion of tokens when the object is disabled is done in a MySQL trigger, objects_AUPD
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

    class EditableFields {
        @Size(min = Objects.NAME_MIN, max = Objects.NAME_MAX)
        val name: String? = null

        @Size(max = Beans.DESCRIPTION_MAX)
        val description: String? = null
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Create a new object.<br>" +
            "_NOTE_: since users can be _admins_ of multiple user groups, " +
            "you need to explicitly pass the ID of the user group that will own the object via `owner`.")
    @PutMapping("")
    fun newObject(@UserId userId: Int,
                  @RequestBody @Valid newObject: NewObject
    ): Objects {

        val userGroup = userGroupRepository.findMine(userId, newObject.owner!!, admin = true).orElseThrow {
            ItemNotFoundException("UserGroup ('${newObject.owner}', admin=true)")
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

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Edit an object you own, such as its name and description.")
    @PostMapping("/{objectId}")
    fun editObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long,
            @Valid @NotNull @RequestBody editableFields: EditableFields
    ): Objects? {
        val obj = objectRepository.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }
        editableFields.name?.let { obj.name = it }
        editableFields.description?.let { obj.description = it }
        return objectRepository.saveAndFlush(obj)
    }

}