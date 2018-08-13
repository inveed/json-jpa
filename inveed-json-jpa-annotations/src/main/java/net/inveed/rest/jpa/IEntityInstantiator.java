package net.inveed.rest.jpa;

import javax.persistence.EntityManager;

/**
 * Class implemening this interface can be used to create a new instance of specific entity.
 *
 * @param <T> Entity type
 * @param <I> Entity identity type
 */
public interface IEntityInstantiator<T, I> {

	T find(EntityManager em, I id);
	Class<I> getIdType();
}
