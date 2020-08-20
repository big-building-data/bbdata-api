package ch.derlin.bbdata.output.api

import ch.derlin.bbdata.output.security.Protected
import org.springframework.core.annotation.AliasFor
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus


/**
 * date: 05.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.MustBeDocumented
@RequestMapping(
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE])
@ResponseStatus(HttpStatus.OK)
@Protected
annotation class PGet(
        @get:AliasFor(annotation = RequestMapping::class, attribute = "value")
        val value: String = "")


@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.MustBeDocumented
@RequestMapping(
        method = [RequestMethod.PUT],
        produces = [MediaType.APPLICATION_JSON_VALUE])
@ResponseStatus(HttpStatus.OK)
@Protected
annotation class PPutCreate(
        @get:AliasFor(annotation = RequestMapping::class, attribute = "value")
        val value: String = "",
        @get:AliasFor(annotation = RequestMapping::class, attribute = "consumes")
        val consumes: Array<String> = [])

/*
// get security annotation and scope (recursive)
val securityAnnotation =
        handler.method.getAnnotation(Protected::class.java) ?: //
        handler.method.annotations
                .flatMap { it.annotationClass.annotations }
                .filter { it is Protected }
                .firstOrNull() as Protected?
 */

