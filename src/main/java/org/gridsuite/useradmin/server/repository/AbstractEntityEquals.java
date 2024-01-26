package org.gridsuite.useradmin.server.repository;

import jakarta.persistence.Entity;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

/**
 * Recommended implementation of {@link #equals(Object)} and {@link #hashCode()} for JPA {@link Entity}
 */
abstract class AbstractEntityEquals<E extends AbstractEntityEquals<E, ID>, ID extends Serializable> {
    protected abstract ID getId();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null) {
            return false;
        } else {
            final Class<?> oEffectiveClass = other instanceof HibernateProxy otherProxy
                    ? otherProxy.getHibernateLazyInitializer().getPersistentClass()
                    : other.getClass();
            final Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy
                    ? proxy.getHibernateLazyInitializer().getPersistentClass()
                    : this.getClass();
            if (thisEffectiveClass != oEffectiveClass) {
                return false;
            } else {
                //noinspection unchecked (because last if test that other is of the same type as this instance)
                return getId() != null && Objects.equals(getId(), ((E) other).getId());
            }
        }
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
