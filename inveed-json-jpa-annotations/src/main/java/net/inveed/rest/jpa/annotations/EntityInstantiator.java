package net.inveed.rest.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.inveed.rest.jpa.IEntityInstantiator;

/**
 * Defines class used for creating new instance of marked entity.
 * References class should implement {@link net.inveed.rest.jpa.IEntityInstantiator}
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntityInstantiator {
	Class<? extends IEntityInstantiator<?,?>> value();
}
