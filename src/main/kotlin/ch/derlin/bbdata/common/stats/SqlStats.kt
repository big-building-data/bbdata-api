package ch.derlin.bbdata.common.stats

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.input.NewValue
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import kotlin.math.abs

/**
 * date: 25.05.20
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@Profile(Profiles.CASSANDRA_STATS)
@Entity
@Table(name = "stats")
data class SqlStats(

        @Id
        @Column(name = "objectId")
        var objectId: Long? = null,

        @Column(name = "n_reads")
        var nReads: Long = 0,

        @Column(name = "n_writes")
        var nWrites: Long = 0,

        @Column(name = "last_ts")
        var lastTs: DateTime? = null,

        @Column(name = "avg_sample_period")
        var avgSamplePeriod: Double = .0
) {
    /** THIS IS NOT USED ANYMORE see SqlStatsRepository for more details
    fun updateWithNewValue(v: NewValue) {
        if (nWrites > 0L) {
            val deltaMs = abs(v.timestamp!!.millis - lastTs!!.millis)
            avgSamplePeriod = (avgSamplePeriod * (nWrites - 1) + deltaMs) / nWrites
        }

        if (lastTs == null || lastTs!! < v.timestamp)
            lastTs = v.timestamp

        nWrites += 1
    }
    */

}