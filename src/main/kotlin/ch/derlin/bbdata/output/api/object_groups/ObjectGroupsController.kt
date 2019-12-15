package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.exceptions.AppException
import ch.derlin.bbdata.output.security.UserId
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository) {

    @GetMapping("")
    fun getAll(@UserId userId: Int,
               @RequestParam("writable", required = false) writable: Boolean,
               @RequestParam("withObjects", required = false) withObjects: Boolean): MappingJacksonValue {
        val ogrpList =
                if (writable) objectGroupsRepository.findAllWritable(userId)
                else objectGroupsRepository.findAll(userId)

        return ObjectGroup.asJacksonMapping(ogrpList, withObjects)

    }

    @GetMapping("/{id}")
    fun getOneById(@UserId userId: Int,
                   @PathVariable(value = "id") id: Long,
                   @RequestParam("writable", required = false) writable: Boolean,
                   @RequestParam("withObjects", required = false) withObjects: Boolean): MappingJacksonValue {
        val obj =
                if (writable) objectGroupsRepository.findOneWritable(userId, id)
                else objectGroupsRepository.findOne(userId, id)

        return ObjectGroup.asJacksonMapping(obj.orElseThrow {
            AppException.itemNotFound()
        }, withObjects)
    }
}