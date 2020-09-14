package ch.derlin.bbdata.common

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression

/**
 * This annotation can be used to only enable a component or configuration is the
 * spring.cache.type property is set (i.e. not none).
 * example:
 *
 * ```
 * @Configuration
 * @CachingEnabled
 * @EnableCaching
 * class CacheConfig {
 *      /** enable caching only if spring.cache.type specified */
 * }
 * ```
 *
 * date: 14.09.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ConditionalOnExpression("'\${spring.cache.type:none}' != 'none'")
annotation class OnCacheEnabled