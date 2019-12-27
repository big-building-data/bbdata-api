package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.time.DateTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * date: 24.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@RequestMapping("/objects")
@Tag(name = "Objects Comments", description = "Manage object comments")
class ObjectsCommentController(private val objectRepository: ObjectRepository,
                               private val commentRepository: CommentRepository) {

    class NewComment {
        @NotNull
        val from: DateTime? = null

        @NotNull
        val to: DateTime? = null

        @NotBlank
        val comment: String? = null
    }


    @Protected
    @GetMapping("/{objectId}/comments")
    fun getAllComments(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long): List<Comment> {

        val obj = objectRepository.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return obj.comments
    }

    @Protected
    @GetMapping("/{objectId}/comments", params = arrayOf("forDate"))
    fun getAllCommentsForDate(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @RequestParam("forDate") forDate: DateTime): List<Comment> {

        val obj = objectRepository.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return commentRepository.findForDate(obj.id!!, forDate)
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/{objectId}/comments")
    fun createComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @Valid @NotNull @RequestBody newComment: NewComment): Comment {

        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        if (newComment.from!! > newComment.to!!) {
            throw WrongParamsException("from ('${newComment.from}') should be <= to ('${newComment.to}')")
        }

        return commentRepository.saveAndFlush(Comment(
                objectId = obj.id!!,
                from = newComment.from!!,
                to = newComment.to!!,
                comment = newComment.comment!!
        ))
    }

    @Protected
    @GetMapping("/{objectId}/comments/{id}")
    fun getComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @PathVariable("id") id: Int): Comment {

        val obj = objectRepository.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return commentRepository.findByIdAndObjectId(id, obj.id!!).orElseThrow {
            ItemNotFoundException("comment ($id)")
        }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @SimpleModificationStatusResponse
    @DeleteMapping("/{objectId}/comments/{id}")
    fun deleteComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @PathVariable("id") id: Int): ResponseEntity<String> {

        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        val comment = commentRepository.findByIdAndObjectId(id, obj.id!!)
        if (comment.isPresent) {
            commentRepository.delete(comment.get())
            return CommonResponses.ok()
        }
        return CommonResponses.notModifed()
    }
}