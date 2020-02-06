package ch.derlin.bbdata.common.cassandra

import ch.derlin.bbdata.common.dates.JodaUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import org.joda.time.DateTime
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.*
import java.io.Serializable

/**
 * date: 27.01.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@PrimaryKeyClass
data class RawValuePK(
        @PrimaryKeyColumn(name = "object_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val objectId: Int? = null,

        @PrimaryKeyColumn(name = "month", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        @JsonIgnore
        val month: String? = null,

        @PrimaryKeyColumn(name = "timestamp", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        val timestamp: DateTime? = null
) : Serializable

@Table("raw_values")
data class RawValue(
        @PrimaryKey
        @field:JsonUnwrapped
        private val key: RawValuePK,

        @Column
        val value: String = "",

        @Column
        val comment: String? = null
) : StreamableCsv {
    override fun csvValues(vararg args: Any): List<Any?> =
            listOf(key.objectId, key.timestamp?.let { JodaUtils.format(it) }, value, comment)

    companion object {
        const val csvHeadersString = "object_id,timestamp,value,comment"
        val csvHeaders: List<String> = csvHeadersString.split(",")
    }

}