package ch.derlin.bbdata.common.exceptions

/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

open class AppException(val details: Any) : Throwable() {
    val name: String get() = this.javaClass.name.split('.').last()
}


class ItemNotFoundException(itemName: String = "resource", msg: String? = null) :
        AppException(msg ?: "The $itemName was not found or can't be accessed with this apikey.")

class UnauthorizedException(msg: String = "This resource is protected.") :
        AppException(msg)

class ForbiddenException(msg: String = "You don't have the right to access this resource.") :
        AppException(msg)

class BadApikeyException(msg: String = "") :
        AppException(msg)

class WrongParamsException(msg: String = "") :
        AppException(msg)