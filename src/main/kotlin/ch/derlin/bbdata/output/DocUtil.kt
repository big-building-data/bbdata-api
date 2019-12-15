package ch.derlin.bbdata.output

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema


/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// ------- Pageable

// see https://github.com/springdoc/springdoc-openapi/issues/251
// The solution form the FAQ (https://springdoc.github.io/springdoc-openapi-demos/faq.html)
// that adds springdoc-openapi-data-rest dependency doesn't seem to work as expected...
// Usage: annotate the method with @PageableAsQueryParam and the Pageable attribute with @HiddenParam

@kotlin.annotation.Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Parameter(hidden = true)
annotation class HiddenParam


@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Parameters(
        Parameter(
                `in` = ParameterIn.QUERY,
                description = "Page you want to retrieve (0..N)",
                name = "page",
                schema = Schema(type = "integer", defaultValue = "0")),
        Parameter(
                `in` = ParameterIn.QUERY,
                description = "Number of records per page.",
                name = "size",
                schema = Schema(type = "integer", defaultValue = "20")),
        Parameter(
                `in` = ParameterIn.QUERY,
                description = "Sorting criteria in the format: property(,asc|desc). "
                        + "Default sort order is ascending. " + "Multiple sort criteria are supported.",
                name = "sort",
                array = ArraySchema(schema = Schema(type = "string")))
)
annotation class PageableAsQueryParam