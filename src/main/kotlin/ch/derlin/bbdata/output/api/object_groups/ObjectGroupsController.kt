package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.common.Beans.DESCRIPTION_MAX
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.user_groups.UserGroupAccessManager
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsController(private val objectGroupAccessManager: ObjectGroupAccessManager,
                             private val userGroupAccessManager: UserGroupAccessManager) {

    class NewObjectGroup {
        @NotNull
        @Size(min = ObjectGroup.NAME_MIN, max = ObjectGroup.NAME_MAX)
        val name: String? = null

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null

        @NotNull
        val owner: Int? = null
    }

    class EditableOgroupFields {
        @Size(min = ObjectGroup.NAME_MIN, max = ObjectGroup.NAME_MAX)
        val name: String? = null

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null
    }

    @Protected
    @GetMapping("")
    @Operation(description = "Get all object groups. You can use the parameter `writable` to only return " +
            "object groups you own (hence have the right to edit), and `withObjects` to also get the list of objects in each.")
    @ApiResponse(
            responseCode = "200",
            description = "default response. Note: if *withObject* is false, the `objects` array will be missing.",
            content = arrayOf(Content(
                    array = ArraySchema(schema = Schema(implementation = ObjectGroup::class))
            )))
    fun getObjectGroups(@UserId userId: Int,
                        @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
                        @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean)
            : List<ObjectGroup> {
        val ogrpList = objectGroupAccessManager.findAll(userId, writable)

        if (!withObjects) return ogrpList
        return ogrpList.map { it.withObjects() }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Create a new, empty object group.<br>" +
            "_NOTE_: since users can be _admins_ of multiple user groups, " +
            "you need to explicitly pass the ID of the user group that will own the new group via `owner`.")
    @PutMapping("")
    fun createObjectGroup(
            @UserId userId: Int,
            @Valid @NotNull @RequestBody newOgrp: NewObjectGroup): ObjectGroup {

        val owner = userGroupAccessManager.getAccessibleGroup(userId, newOgrp.owner!!, admin = true).orElseThrow {
            ItemNotFoundException("userGroup (${newOgrp.owner})")
        }

        return objectGroupAccessManager.objectGroupsRepository.saveAndFlush(
                ObjectGroup(
                        name = newOgrp.name,
                        description = newOgrp.description,
                        owner = owner
                )
        )
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Edit an object group you own, such as its name or description.")
    @PostMapping("/{objectGroupId}")
    fun editObjectGroup(@UserId userId: Int,
                        @PathVariable("objectGroupId") id: Long,
                        @Valid @NotNull @RequestBody editableFields: EditableOgroupFields): ObjectGroup {

        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        editableFields.name?.let { ogrp.name = it }
        editableFields.description?.let { ogrp.description = it }
        return objectGroupAccessManager.objectGroupsRepository.saveAndFlush(ogrp)
    }

    @Protected
    @GetMapping("/{objectGroupId}")
    @Operation(description = "Get the details of an object group (name, description). " +
            "To also get the objects it contains, use the URL parameter `withObjects=true` (default: false).")
    @ApiResponse(
            responseCode = "200",
            description = "default response. Note: if *withObject* is false, the `objects` array will be missing.",
            content = arrayOf(Content(schema = Schema(implementation = ObjectGroup::class))))
    fun getObjectGroup(@UserId userId: Int,
                       @PathVariable(value = "objectGroupId") id: Long,
                       @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean): ObjectGroup {
        val ogrp = objectGroupAccessManager.findOne(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }
        return if (withObjects) ogrp.withObjects() else ogrp
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Delete an object group you own.")
    @DeleteMapping("/{objectGroupId}")
    @SimpleModificationStatusResponse
    fun deleteObjectGroup(@UserId userId: Int, @PathVariable(value = "objectGroupId") id: Long): ResponseEntity<String> {
        if (!objectGroupAccessManager.findOne(userId, id).isPresent) {
            return CommonResponses.notModifed()
        }
        val ogrp = objectGroupAccessManager.findOne(userId, id, writable = true).orElseThrow {
            ForbiddenException("You must have admin rights on this object group to delete it.")
        }
        objectGroupAccessManager.objectGroupsRepository.delete(ogrp)
        return CommonResponses.ok()
    }
}