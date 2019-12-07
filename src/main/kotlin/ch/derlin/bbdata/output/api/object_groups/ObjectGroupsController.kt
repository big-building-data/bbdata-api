package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Constants
import ch.derlin.bbdata.output.exceptions.AppException
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository) {

    @GetMapping("")
    fun getAll(@RequestHeader(Constants.HEADER_USER) userId: Int,
               @RequestParam("writable", required = false) writable: Boolean,
               @RequestParam("withObjects", required = false) withObjects: Boolean): MappingJacksonValue {
        val ogrpList =
                if (writable) objectGroupsRepository.findAllWritable(userId)
                else objectGroupsRepository.findAll(userId)

        return ObjectGroup.asJacksonMapping(ogrpList, withObjects)

    }

    @GetMapping("/{id}")
    fun getOneById(@PathVariable(value = "id") id: Long,
                   @RequestHeader(Constants.HEADER_USER) userId: Int,
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