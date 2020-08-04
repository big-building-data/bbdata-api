## Tricks

**Jackson DateTime support**

```kotlin
@JsonComponent
class JodaDateTimeSerializer : JsonSerializer<DateTime>() {

    override fun serialize(value: DateTime, gen: JsonGenerator, serializers: SerializerProvider) =
            gen.writeString(JodaUtils.format(value))

}

@JsonComponent
class JodaDateTimeDeserializer : JsonDeserializer<DateTime>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): DateTime =
            JodaUtils.parse(p!!.valueAsString)

}
```

**hateoas**


**XML support**

Simply add this dependency to `build.gradle.kts`:
```kotlin
implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
```

**Dynamic exclusion of property in JSON serialization**

Goal: in `ObjectGroup`, choose dynamically whether to return the list of objects or not. Possibilities:

1. As in the first implementation, use `ObjectGroup` and `ObjectGroupExtended`. Problem: we need different 
    queries in the repository, hence duplicating a lot of code
2. DTOs: requires a conversion + a lot of repetition in the definition/instantation of them
3. `@JsonView`. This annotation let us define *views*, such as `Simple` and `Detailed`, and then configure
    on the go the jackson mapping to use one or the other. Nice, however every nested object need also to 
    to be annotated. Here, it would mean annotate `UserGroup`, `Unit`, etc. Overkill since it only concerns one endpoint
4. The cleaner in my opinion: use a `JsonFilter`. For that, one has to specify on the entity that a filter will be used
    and then of course create a filter that will exclude the `objects` properties upon some condition.
5. Annotate the `objects` field with `@field:JsonInclude(JsonInclude.Include.NON_NULL)` and set it to null 
   when `withObjects` is false
    
Implementation of 3), `@JsonView`:
```python
interface Views {
    interface Simple
    interface Detailed: Simple
}

// on the Entity (note the @JsonView can be set on a whole class as well)
data class XX (
    @JsonView(Views.Simple::class)
    prop: Int,

    @JsonView(Views.Details::class)
    objects: List<String>
)

// in the controller
val mapping = MappingJacksonValue(xx)
mapping.serializationView = if (withObjects) Views.Detailed::class.java else Views.Simple::class.java
return mapping
```

Implementation of 4):

```python

// annoate the entity / DTO
@JsonFilter("noObjectsFilter")
data class ObjectGroup(...)

// define a helper function which applies the filter
fun asJacksonMapping(obj: Any, withObjects: Boolean = false): MappingJacksonValue {
   val mapping = MappingJacksonValue(obj)
   // the secret: to NOT ignore any properties, just pass a non-existing property name (_x_)
   mapping.filters = SimpleFilterProvider().addFilter(
           "noObjectsFilter",
           SimpleBeanPropertyFilter.serializeAllExcept(if (withObjects) "_x_" else "objects"))
   return mapping
}

// use it
val ogrpList = objectGroupRepository.findAll()
return asJacksonMapping(ogrpList)
```

**documentation**

* do not use the same name for different inner classes representing parameters (e.g. `EditableFields`), as openapi swagger will get confused

## Resources

* repository custom implementations with EntityManager: 
    https://dzone.com/articles/add-custom-functionality-to-a-spring-data-reposito
* native queries in Kotlin:
    https://blog.trifork.com/2018/08/28/spring-data-native-queries-and-projections-in-kotlin/
* advices on how to use hibernate with Spring/Kotlin (to read!!!):
    https://kotlinexpertise.com/hibernate-with-kotlin-spring-boot/
    
## TODO

* review endpoints
* provide custom constructors for POJO
* remove Optionals ? in repos
* `@RequestAttribute` vs `@RequestHeader` + rethink the auth mechanism
* profiles ?
* use the spring data rest fully (HATEOAS, functions in repository become endpoints) ?
* in values, check the date range in controller + use object creation date to avoid scanning too many month ?
* directly save into cassandra from the input API ?
* add salt to passwords in MySQL !!
* we can delete usergroups, but not users. Users that are part of no groups are useless... ??

## Permissions, gosh

* userGroups: 
    - any can see the userGroup basic infos /userGroups, /userGroups/{id}
    - users part of the group can see other users /userGroups/{id}/users
    - admins only can modify group (add users, etc.)
