package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/objectGroups")
class ObjectGroupsController(private val repo: ObjectGroupsRepository) {
    @GetMapping("")
    fun getAllObjects(): List<ObjectGroup> = repo.findAll()

    @GetMapping("/{id}")
    fun getObjectById(@PathVariable(value = "id") id: Long): ResponseEntity<ObjectGroup> {
        return repo.findById(id).map { o ->
            ResponseEntity.ok(o)
        }.orElse(ResponseEntity.notFound().build())
    }
}