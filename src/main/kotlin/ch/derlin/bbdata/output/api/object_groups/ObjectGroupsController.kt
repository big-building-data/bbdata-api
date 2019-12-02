package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Constants
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/objectGroups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository) {

    @GetMapping("")
    fun getAll(@RequestHeader(Constants.HEADER_USER) userId: Int,
               @RequestParam("writable", required = false) writable: Boolean): List<ObjectGroup> {
        return if (writable) objectGroupsRepository.findAllWritable(userId)
        else objectGroupsRepository.findAll(userId)
    }

    @GetMapping("/{id}")
    fun getOneById(@PathVariable(value = "id") id: Long,
                   @RequestHeader(Constants.HEADER_USER) userId: Int,
                   @RequestParam("writable", required = false) writable: Boolean): ObjectGroup {
        return if (writable) objectGroupsRepository.findOne(userId, id)
        else objectGroupsRepository.findOneWritable(userId, id)
    }
}