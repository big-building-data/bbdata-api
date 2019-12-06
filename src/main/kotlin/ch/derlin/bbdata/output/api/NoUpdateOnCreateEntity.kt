package ch.derlin.bbdata.output.api

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.PostLoad
import javax.persistence.PrePersist
import org.springframework.data.domain.Persistable
import javax.persistence.MappedSuperclass


/**
 * date: 05.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

@MappedSuperclass
abstract class NoUpdateOnCreateEntity<ID> : Persistable<ID> {
    /*
     * see https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-persistence.saving-entites.strategies
     */
    @Transient
    private var isNew = true

    @JsonIgnore
    override fun isNew(): Boolean {
        return isNew
    }

    @PrePersist
    @PostLoad
    internal fun markNotNew() {
        this.isNew = false
    }
}