package ch.derlin.bbdata.output.security

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.core.annotation.AliasFor

/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.ANNOTATION_CLASS)
@SecurityRequirement(name = "auth")
annotation class Protected (
        @get:AliasFor(annotation = SecurityRequirement::class, attribute = "scopes")
        vararg val value: String)