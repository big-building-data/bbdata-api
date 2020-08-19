package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.common.exceptions.WrongParamsException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
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
class ObjectsCommentController(private val objectsAccessManager: ObjectsAccessManager,
                               private val commentRepository: CommentRepository) {

    // TODO: who can create comment ? Currently: anyone having access
    // TODO: add a userId column to the comment table !!!!!

    class NewComment {
        @NotNull
        val from: DateTime? = null

        @NotNull
        val to: DateTime? = null

        @NotBlank
        val comment: String? = null
    }

    @Protected
    @Operation(description = "Get all comments attached to an object. " +
            "If `forDate` is specified, only return comments covering a given date. " +
            "For example: if comment C1 covers 2050-01-01 to 2050-02-01, it will be returned for date 2050-01-12, but " +
            "not for date 2050-03-01.")
    @GetMapping("/{objectId}/comments")
    fun getComments(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @RequestParam("forDate", required = false) forDate: DateTime?): List<Comment> {

        val obj = objectsAccessManager.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        if(forDate != null) return commentRepository.findForDate(obj.id!!, forDate)
        else return obj.comments
    }


    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Attach a new comment to an object. " +
            "A comment is a text that covers/applies to a specific period.")
    @PutMapping("/{objectId}/comments")
    fun createComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @Valid @NotNull @RequestBody newComment: NewComment): Comment {

        val obj = objectsAccessManager.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        if (newComment.from!! > newComment.to!!) {
            throw WrongParamsException("from ('${newComment.from}') should be <= to ('${newComment.to}')")
        }

        return commentRepository.saveAndFlush(Comment(
                objectId = obj.id!!,
                from = newComment.from,
                to = newComment.to,
                comment = newComment.comment!!
        ))
    }

    @Protected
    @Operation(description = "Get a comment by ID.")
    @GetMapping("/{objectId}/comments/{commentId}")
    fun getComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @PathVariable("commentId") id: Int): Comment {

        val obj = objectsAccessManager.findById(objectId, userId, writable = false).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }
        return commentRepository.findByIdAndObjectId(id, obj.id!!).orElseThrow {
            ItemNotFoundException("comment ($id)")
        }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Delete a comment.")
    @SimpleModificationStatusResponse
    @DeleteMapping("/{objectId}/comments/{commentId}")
    fun deleteComment(
            @UserId userId: Int,
            @PathVariable("objectId") objectId: Long,
            @PathVariable("commentId") id: Int): ResponseEntity<String> {

        val obj = objectsAccessManager.findById(objectId, userId, writable = false).orElseThrow {
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