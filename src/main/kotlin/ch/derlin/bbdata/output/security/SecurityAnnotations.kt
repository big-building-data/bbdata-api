package ch.derlin.bbdata.output.security

/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ApikeyWrite

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class NoHeaderRequired