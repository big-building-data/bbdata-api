package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.ForbiddenException
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
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

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository,
                             private val userGroupRepository: UserGroupRepository) {

    class NewObjectGroup : Beans.NameDescription() {
        @NotNull
        val owner: Int? = null
    }

    @Protected
    @GetMapping("")
    @ApiResponse(
            responseCode = "200",
            description = "default response. Note: if *withObject* is false, the `objects` array will be missing.",
            content = arrayOf(Content(
                    array = ArraySchema(schema = Schema(implementation = ObjectGroup::class))
            )))
    fun getAll(@UserId userId: Int,
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
    fun createOne(@UserId userId: Int,
                  @Valid @RequestBody newOgrp: NewObjectGroup): ObjectGroup {

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

    @Protected
    @GetMapping("/{id}")
    @ApiResponse(
            responseCode = "200",
            description = "default response. Note: if *withObject* is false, the `objects` array will be missing.",
            content = arrayOf(Content(schema = Schema(implementation = ObjectGroup::class))))
    fun getOneById(@UserId userId: Int,
                   @PathVariable(value = "id") id: Long,
                   @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
                   @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean): ObjectGroup {
        val opt =
                if (writable) objectGroupsRepository.findOneWritable(userId, id)
                else objectGroupsRepository.findOne(userId, id)

        val ogrp = opt.orElseThrow { ItemNotFoundException("object group ($id)") }
        return if (withObjects) ogrp.withObjects() else ogrp
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @DeleteMapping("/{id}")
    @SimpleModificationStatusResponse
    fun deleteOneById(@UserId userId: Int, @PathVariable(value = "id") id: Long): ResponseEntity<String> {
        if(!objectGroupsRepository.findOne(userId, id).isPresent){
            return CommonResponses.notModifed()
        }
        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ForbiddenException("You must have admin rights on this object group to delete it.")
        }
        objectGroupsRepository.delete(ogrp)
        return CommonResponses.ok()
    }
}