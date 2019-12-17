package ch.derlin.bbdata.output.exceptions

/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

open class AppException(val details: Any) : Throwable() {
    val name: String get() = this.javaClass.name.split('.').last()
}

class ItemNotFoundException(itemName: String = "resource") :
        AppException("The ${itemName} was not found or can't be accessed with this apikey.")

class ForbiddenException(msg: String = "This resource is protected.") :
        AppException(msg)

class BadApikeyException(msg: String = "") :
        AppException(msg)

class WrongParamsException(msg: String = ""):
        AppException(msg)