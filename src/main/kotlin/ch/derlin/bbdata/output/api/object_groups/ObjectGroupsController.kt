package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository) {

    @Protected
    @GetMapping("")
    fun getAll(@UserId userId: Int,
               @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
               @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean): MappingJacksonValue {
        val ogrpList =
                if (writable) objectGroupsRepository.findAllWritable(userId)
                else objectGroupsRepository.findAll(userId)

        return ObjectGroup.asJacksonMapping(ogrpList, withObjects)

    }

    @Protected
    @GetMapping("/{id}")
    fun getOneById(@UserId userId: Int,
                   @PathVariable(value = "id") id: Long,
                   @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean,
                   @RequestParam("withObjects", required = false, defaultValue = "false") withObjects: Boolean): MappingJacksonValue {
        val obj =
                if (writable) objectGroupsRepository.findOneWritable(userId, id)
                else objectGroupsRepository.findOne(userId, id)

        return ObjectGroup.asJacksonMapping(obj.orElseThrow { ItemNotFoundException("object group") }, withObjects)
    }
}