package net.inveed.rest.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Entity of marked property will be serialized as JSON object (instead of entity ID)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface JsonEmbeddedEntity {
	int deep() default Integer.MAX_VALUE;
}
