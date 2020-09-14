package ch.derlin.bbdata.output.api.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.Constants
import ch.derlin.bbdata.common.*
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.types.UnitRepository
import ch.derlin.bbdata.output.api.user_groups.UserGroupAccessManager
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@RestController
@RequestMapping("/objects")
@Tag(name = "Objects", description = "Manage objects")
class ObjectsController(private val objectsAccessManager: ObjectsAccessManager,
                        private val userGroupAccessManager: UserGroupAccessManager,
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

        val tags: List<String>? = null
    }

    class EditableObjectFields {
        @Size(min = Objects.NAME_MIN, max = Objects.NAME_MAX)
        val name: String? = null

        @Size(max = Beans.DESCRIPTION_MAX)
        val description: String? = null
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Transactional
    @Operation(description = "Create a new object.<br>" +
            "_NOTE_: since users can be _admins_ of multiple user groups, " +
            "you need to explicitly pass the ID of the user group that will own the object via `owner`.")
    @PutMapping("")
    fun newObject(@UserId userId: Int,
                  @RequestBody @Valid newObject: NewObject
    ): Objects = newObjectBulk(userId, listOf(newObject))[0]


    @Protected(SecurityConstants.SCOPE_WRITE)
    @Transactional
    @Operation(description = "Create new objects in bulk.<br>" +
            "This is similar to PUT /objects, but accepts an array of objects to create. " +
            "**RESTRICTION**: all objects MUST have the SAME OWNER.")
    @PutMapping("/bulk")
    fun newObjectBulk(@UserId userId: Int,
                      @RequestBody @Valid newObjects: List<NewObject>
    ): List<Objects> {

        if (newObjects.size == 0)
            throw WrongParamsException("object array is empty.")
        if (newObjects.any { it.owner != newObjects[0].owner })
            throw WrongParamsException("cannot create objects in bulk with different owners")

        val userGroup = userGroupAccessManager.getAccessibleGroup(userId, newObjects[0].owner!!, admin = true).orElseThrow {
            ItemNotFoundException("UserGroup ('${newObjects[0].owner}', admin=true)")
        }

        val objects = objectsAccessManager.objectRepository.saveAll(
                newObjects.map {
                    Objects(
                            name = it.name,
                            description = it.description,
                            unit = unitRepository.findById(it.unitSymbol).orElseThrow {
                                WrongParamsException("Object '${it.name}': unit '${it.unitSymbol}' does not exist.")
                            },
                            owner = userGroup
                    )
                })


        if (newObjects.any { it.tags != null }) {
            newObjects.zip(objects).forEach { (newObj, obj) ->
                newObj.tags?.forEach { obj.addTag(it) }
            }
            return objectsAccessManager.objectRepository.saveAll(objects)

        } else {
            return objects
        }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Edit an object you own, such as its name and description.")
    @PostMapping("/{objectId}")
    fun editObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long,
            @Valid @NotNull @RequestBody editableFields: EditableObjectFields
    ): Objects? {
        val obj = objectsAccessManager.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }
        editableFields.name?.let { obj.name = it }
        editableFields.description?.let { obj.description = it }
        return objectsAccessManager.objectRepository.saveAndFlush(obj)
    }

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
            objectsAccessManager.findAllByTag(tags, userId, writable, search, pageable)
        else
            objectsAccessManager.findAll(userId, writable, search, pageable)
    }

    @Protected
    @Operation(description = "Get an object details.")
    @GetMapping("/{objectId}")
    fun getObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long
    ): Objects? = objectsAccessManager.findById(id, userId, writable = false).orElseThrow { ItemNotFoundException("object") }

    @Protected
    @Operation(description = "Get all object groups an object belongs to.")
    @GetMapping("/{objectId}/objectGroups")
    fun getGroupsOfObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long
    ): List<ObjectGroup>? = objectsAccessManager.findById(id, userId, writable = false).orElseThrow { ItemNotFoundException("object") }.getObjectGroups()

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
        val obj = objectsAccessManager.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }

        val statuses =
                if (delete) tags.map { obj.removeTag(it) }
                else tags.map { obj.addTag(it) }

        if (statuses.any { x -> x }) {
            objectsAccessManager.objectRepository.save(obj)
            return CommonResponses.ok()
        }

        return CommonResponses.notModifed()
    }

    // ======== ENABLE/DISABLE

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Enable an object you own. If the object is already enabled, it simply returns NOT MODIFIED.")
    @CacheEvict(Constants.META_CACHE, allEntries = true)
    @SimpleModificationStatusResponse
    @PostMapping("{objectId}/enable")
    fun enableObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = false)

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Disable an object you own. If the object is already disabled, it simply returns NOT MODIFIED. " +
            "When an object gets disabled, all its tokens are deleted and it cannot receive new values (no new measures).")
    @CacheEvict(Constants.META_CACHE, allEntries = true)
    @SimpleModificationStatusResponse
    @PostMapping("{objectId}/disable")
    fun disableObject(
            @UserId userId: Int,
            @PathVariable(value = "objectId") id: Long): ResponseEntity<String> =
            enableDisable(userId, id, disabled = true)

    private fun enableDisable(userId: Int, id: Long, disabled: Boolean): ResponseEntity<String> {
        // note: the deletion of tokens when the object is disabled is done in a MySQL trigger, objects_AUPD
        val obj = objectsAccessManager.findById(id, userId, writable = true).orElseThrow { ItemNotFoundException("object") }
        if (obj.disabled != disabled) {
            obj.disabled = disabled
            objectsAccessManager.objectRepository.save(obj)
            return CommonResponses.ok()
        }
        return CommonResponses.notModifed()
    }

}