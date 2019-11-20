package ch.derlin.bbdata.api.output.entities.objects

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.api.output.entities.object_groups.ObjectGroup
import ch.derlin.bbdata.api.output.entities.object_groups.ObjectGroupsRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/objects")
class ObjectController(private val repo: ObjectGroupsRepository) {
    @GetMapping("")
    fun getAll(): List<ObjectGroup> = repo.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable(value = "id") id: Long): ResponseEntity<ObjectGroup> {
        return repo.findById(id).map { o ->
            ResponseEntity.ok(o)
        }.orElse(ResponseEntity.notFound().build())
    }
}