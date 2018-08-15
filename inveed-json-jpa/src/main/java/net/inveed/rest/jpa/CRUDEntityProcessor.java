package net.inveed.rest.jpa;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.inveed.rest.jpa.typeutils.EntityTypeExt;
import net.inveed.commons.reflection.BeanTypeDesc;

public class CRUDEntityProcessor {

	public <T> List<T> list(EntityManager em, Class<T> type, int pageSize, int page) {
		if (page <= 0) {
			page = 1;
		}
		if (pageSize <= 0) {
			pageSize = 50;
		}
		int startPos = pageSize * (page - 1);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(type);
		Root<T> root = q.from(type);
		q = q.select(root);
		TypedQuery<T> query = em.createQuery(q);
		return query.setFirstResult(startPos).setMaxResults(pageSize).getResultList();
	}

	public <T> T get(EntityManager em, BeanTypeDesc<T> entityType, String id)
			throws JsonGenerationException, JsonMappingException, IOException {

		if (!em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(entityType.getType()))
			return null;

		@SuppressWarnings("unchecked")
		EntityTypeExt<T> ete = entityType.getExtension(EntityTypeExt.class);
		return ete.get(em, id);
	}
}
