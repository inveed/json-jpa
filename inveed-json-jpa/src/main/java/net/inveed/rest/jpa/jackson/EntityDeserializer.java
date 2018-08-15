package net.inveed.rest.jpa.jackson;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;

import net.inveed.rest.jpa.IEntityInstantiator;
import net.inveed.rest.jpa.JsonTypeUtils;
import net.inveed.rest.jpa.typeutils.EntityTypeExt;
import net.inveed.commons.reflection.BeanPropertyDesc;
import net.inveed.commons.reflection.BeanTypeDesc;
import net.inveed.commons.reflection.JavaTypeDesc;
import net.inveed.commons.reflection.JavaTypeRegistry;


public class EntityDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer, ResolvableDeserializer{
	private static final Logger LOG = LoggerFactory.getLogger(EntityDeserializer.class);
	
	private BeanProperty property;
	private JsonDeserializer<?> parent;
	
	public EntityDeserializer(JsonDeserializer<?> parent, DeserializationConfig config, BeanDescription beanDesc) {
		this.parent = parent;
	}
	
	public EntityDeserializer(BeanProperty property, JsonDeserializer<?> parent) {
		this.property = property;
		this.parent = parent;
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		if (this.property == null || this.property.getType() == null) {
			LOG.warn("Something went wrong. Trying to deserialize without context.");
			return this.parent.deserialize(p, ctxt);
		} else {
			JavaTypeDesc<?> et = JavaTypeRegistry.getType(this.property.getType().getRawClass());
			if (et == null) {
				LOG.warn("Something went wrong. Cannot find type for property type {}.", this.property.getType().getRawClass());
				return this.parent.deserialize(p, ctxt);
			}
			if (et instanceof BeanTypeDesc<?>) {
				
				BeanTypeDesc<?> btd = (BeanTypeDesc<?>) et;
				
				@SuppressWarnings("unchecked")
				EntityTypeExt<?> ete = btd.getExtension(EntityTypeExt.class);
				if (ete == null) {
					LOG.debug("Deserializing bean type {}", et.getType());
					return this.parent.deserialize(p, ctxt);
				} else {
					LOG.debug("Deserializing entity type {}", et.getType());
					return this.deserialize(p, ctxt, ete);
				}
			} else {
				LOG.debug("Deserializing simple type {}", et.getType());
				return this.parent.deserialize(p, ctxt);
			}
		}
	}

	public Object deserialize(JsonParser p, DeserializationContext ctxt, EntityTypeExt<?> type) throws IOException, JsonProcessingException {
		if (p.getCurrentToken() == JsonToken.START_OBJECT) {
			LOG.warn("Complex ID types not implemented");
			return this.parent.deserialize(p, ctxt);
		}
		
		@SuppressWarnings("unchecked")
		IEntityInstantiator<Object, Object> ei=  (IEntityInstantiator<Object, Object>) type.getInstantiator();
		if (ei != null) {
			LOG.debug("Trying to use instantiator {}", ei.getClass());
			Class<Object> idType = ei.getIdType();
			JavaTypeDesc<?> fieldType = JavaTypeRegistry.getType(idType);
			
			Object idObject = JsonTypeUtils.getPrimitive(fieldType, p);
			if (idObject == null) {
				LOG.warn("Cannot read complex ID object with type ", fieldType.getType());
				throw new JsonEntityNotFoundException(ei.getClass(), idObject);
			}
			
			LOG.debug("Instantiating entity {} with ID '{}'", type.getEntityName(), idObject);
			try {
				Object ret = ei.find(JsonConfiguration.getContextEntityManager(), idObject);
				if (ret == null) {
					LOG.warn("Cannot find entity with instantiator. Complex ID is {}", idObject);
					throw new JsonEntityNotFoundException(ei.getClass(), idObject);
				}
				return ret;
			} catch (EntityNotFoundException e) {
				LOG.warn("Cannot find entity with instantiator (exception handled). Complex ID is {}", idObject);
				throw new JsonEntityNotFoundException(ei.getClass(), idObject);
			}
			
		}
		
		List<BeanPropertyDesc> idFields = type.getIDFields();
		if (idFields == null || idFields.size() < 1) {
			LOG.warn("Cannot determine ID fields for type ", type.getBeanType().getType());
			throw new JsonEntityNotFoundException(type.getBeanType().getType(), null);
		}
		
		if (idFields.size() > 1) {
			LOG.warn("Multiple fields in ID not implemented");
			throw new JsonEntityNotFoundException(type.getBeanType().getType(), null);
		} else {
			JavaTypeDesc<?> fieldType = idFields.get(0).getType();
			Object idObject = JsonTypeUtils.getPrimitive(fieldType, p);
			if (idObject == null) {
				LOG.warn("Cannot read ID object with type ", fieldType.getType());
				throw new JsonEntityNotFoundException(type.getBeanType().getType(), null);
			}
			LOG.debug("Searching for entity {} with ID '{}'", type.getEntityName(), idObject);
			try {
				Object ret = JsonConfiguration.getContextEntityManager().find(this.property.getType().getRawClass(), idObject);
				if (ret == null) {
					throw new JsonEntityNotFoundException(type.getBeanType().getType(), idObject);
				}
				return ret;
			} catch (EntityNotFoundException e) {
				throw new JsonEntityNotFoundException(type.getBeanType().getType(), idObject);
			}
		}
	}


	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
			throws JsonMappingException {
		if (property == null) {
			 return this.parent;
		}
		if (this.property != null && isPropEqual(this.property,property)) {
			return this;
		}
		return new EntityDeserializer(property, this.parent);
	}
	
	private static final boolean isPropEqual(BeanProperty bp1, BeanProperty bp2){
		if (!bp1.getName().equals(bp2.getName())) {
			return false;
		}
		
		if (!bp1.getType().equals(bp2.getType())) {
			return false;
		}
		return true;
	}

	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		if (this.parent instanceof ResolvableDeserializer) {
			((ResolvableDeserializer) this.parent).resolve(ctxt);
		}
	}

}
