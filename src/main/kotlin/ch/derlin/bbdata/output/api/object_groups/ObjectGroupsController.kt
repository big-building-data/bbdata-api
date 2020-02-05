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
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
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
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository,
                             private val userGroupRepository: UserGroupRepository) {

    class NewObjectGroup {
        @NotNull
        @Size(min = ObjectGroup.NAME_MIN, max = ObjectGroup.NAME_MAX)
        val name: String? = null

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null

        @NotNull
        val owner: Int? = null
    }

    class EditableFields {
        @Size(min = ObjectGroup.NAME_MIN, max = ObjectGroup.NAME_MAX)
        val name: String? = null

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null
    }

    @Protected
    @GetMapping("")
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

        val ogrpList =
                if (writable) objectGroupsRepository.findAllWritable(userId)
                else objectGroupsRepository.findAll(userId)

        if (!withObjects) return ogrpList
        return ogrpList.map { it.withObjects() }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("")
    fun createObjectGroup(@UserId userId: Int,
                  @Valid @NotNull @RequestBody newOgrp: NewObjectGroup): ObjectGroup {

        val owner = userGroupRepository.findMine(userId, newOgrp.owner!!).orElseThrow {
            ItemNotFoundException("userGroup (${newOgrp.owner})")
        }

        return objectGroupsRepository.saveAndFlush(
                ObjectGroup(
                        name = newOgrp.name,
                        description = newOgrp.description,
                        owner = owner
                )
        )
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @PostMapping("/{objectGroupId}")
    fun editObjectGroup(@UserId userId: Int,
                @PathVariable("objectGroupId") id: Long,
                @Valid @NotNull @RequestBody editableFields: EditableFields): ObjectGroup {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("objectGroup (${id})")
        }
        editableFields.name?.let { ogrp.name = it }
        editableFields.description?.let { ogrp.description = it }
        return objectGroupsRepository.saveAndFlush(ogrp)
    }

    @Protected
    @GetMapping("/{objectGroupId}")
    @ApiResponse(
            responseCode = "200",
            description = "default response. Note: if *withObject* is false, the `objects` array will be missing.",
            content = arrayOf(Content(schema = Schema(implementation = ObjectGroup::class))))
    fun getObjectGroup(@UserId userId: Int,
                   @PathVariable(value = "objectGroupId") id: Long,
                   @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
                   @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean): ObjectGroup {
        val opt =
                if (writable) objectGroupsRepository.findOneWritable(userId, id)
                else objectGroupsRepository.findOne(userId, id)

        val ogrp = opt.orElseThrow { ItemNotFoundException("object group ($id)") }
        return if (withObjects) ogrp.withObjects() else ogrp
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @DeleteMapping("/{objectGroupId}")
    @SimpleModificationStatusResponse
    fun deleteObjectGroup(@UserId userId: Int, @PathVariable(value = "objectGroupId") id: Long): ResponseEntity<String> {
        if (!objectGroupsRepository.findOne(userId, id).isPresent) {
            return CommonResponses.notModifed()
        }
        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ForbiddenException("You must have admin rights on this object group to delete it.")
        }
        objectGroupsRepository.delete(ogrp)
        return CommonResponses.ok()
    }
}