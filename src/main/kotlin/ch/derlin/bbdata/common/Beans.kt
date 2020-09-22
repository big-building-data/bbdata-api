package ch.derlin.bbdata.common

import javax.validation.constraints.Size

/**
 * date: 07.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

/*
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
@ConstraintComposition
@Constraint(validatedBy = [])
@Size(min = 3, max = 60)
annotation class NameSize(
        val message: String = "Invalid name.",
        val groups: Array<KClass<*>> = [],
        val payload: Array<KClass<*>> = [])
*/

object Beans {

    const val DESCRIPTION_MAX = 255

    open class Description {

        @Size(max = DESCRIPTION_MAX)
        val description: String? = null
    }

}


fun String?.truncate(maxLength: Int = 30, ellipsis: String = "[${Typography.ellipsis}]"): String? =
        if (this != null && this.length > maxLength) this.take(maxLength).plus(ellipsis) else this