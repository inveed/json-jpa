package net.inveed.rest.jpa.jackson;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import net.inveed.rest.jpa.IComplexIdEntity;
import net.inveed.rest.jpa.annotations.JsonEmbeddedEntity;
import net.inveed.rest.jpa.jackson.JsonConfiguration.SerializationTheadLocal;
import net.inveed.rest.jpa.typeutils.EntityTypeExt;
import net.inveed.rest.jpa.typeutils.JsonTypeExt;
import net.inveed.commons.reflection.BeanPropertyDesc;
import net.inveed.commons.reflection.BeanTypeDesc;
import net.inveed.commons.reflection.JavaTypeDesc;
import net.inveed.commons.reflection.JavaTypeRegistry;

public class EntitySerializationPropertyFilter extends SimpleBeanPropertyFilter {
	private static final Logger LOG = LoggerFactory.getLogger(EntitySerializationPropertyFilter.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
			throws Exception {
		
		JavaTypeDesc<?> pojoType = JavaTypeRegistry.getType(pojo.getClass());
		if (!(pojoType instanceof BeanTypeDesc<?>)) {
			super.serializeAsField(pojo, jgen, provider, writer);
			return;
		}
		
		BeanTypeDesc<?> beanType = (BeanTypeDesc<?>) pojoType;
		
		EntityTypeExt<?> entityType = beanType.getExtension(EntityTypeExt.class);
		if (entityType == null) {
			super.serializeAsField(pojo, jgen, provider, writer);
			return;
		}

		JsonTypeExt<?> beanJsonExt = beanType.getExtension(JsonTypeExt.class);
		BeanPropertyDesc fld;
		if (beanJsonExt == null) {
			fld = beanType.getProperty(writer.getName());
		} else {
			fld = beanJsonExt.getJSONProperty(writer.getName());
		}
		
		if (fld == null) {
			LOG.warn("Cannot find property with name '{}' in type '{}'", writer.getName(), beanType.getFullName());
			super.serializeAsField(pojo, jgen, provider, writer);
			return;
		}
		
		if (fld.getType().getExtension(EntityTypeExt.class) == null) {
			try {
				super.serializeAsField(pojo, jgen, provider, writer);
			} catch (Exception e) {
				throw e;
			}
			return;
		}
		
		Object value = fld.getValue(pojo);
		if (value == null) {
			super.serializeAsField(pojo, jgen, provider, writer);
			return;
		}
		
		// в уровнях учитываем только сущности, которые должны быть сериализованы как ID.
		SerializationTheadLocal ctx = JsonConfiguration.getSerializationContext();
		
		// Нужно понять, сериализовать ли сущность как ID или как обычный объект
		int deep = ctx.deep;
		
		JsonEmbeddedEntity jeea = fld.getAnnotation(JsonEmbeddedEntity.class);
		if (jeea != null) {
			// Возможно, конкретное свойство надо сериализовать как обычный объект.
			deep = Math.max(jeea.deep(), deep);
			LOG.debug("Found JsonEmbeddedEntity annotation for property '{}' with deep {}. Effective deep: {}", fld.getName(), jeea.deep(), deep);
		}
		//ctx.currentLevel++;
		try {
			if (ctx.currentLevel <= deep) {
				LOG.debug("Serializing entity '{}' in '{}' property '{}' as POJO because of requested nesting level {} (current level is {})", fld.getType().getType().getName(), beanType.getFullName(), fld.getName(), deep, ctx.currentLevel - 1);
			
				super.serializeAsField(pojo, jgen, provider, writer);
				return;
			}
			this.serializeEntityId(pojo, value, entityType, fld.getType().getExtension(EntityTypeExt.class), jgen, provider, writer);
		} finally {
			//ctx.currentLevel --;
		}
	}

	protected void serializeEntityId(Object pojo, Object value, EntityTypeExt<?> entityType, EntityTypeExt<?> fieldType, JsonGenerator gen, SerializerProvider provider, PropertyWriter writer) throws Exception {
		if (value instanceof IComplexIdEntity) {
			LOG.debug("Serializing as ComplexID value");
			IComplexIdEntity cie = (IComplexIdEntity) value;
			gen.writeFieldName(writer.getName());
			gen.writeObject(cie.getComplexId());
			return;
		}
		
		List<BeanPropertyDesc> idFields = fieldType.getIDFields();
		if (idFields == null || idFields.size() < 1) {
			LOG.warn("Cannot find ID fields for entity {}", entityType.getEntityName());
			super.serializeAsField(pojo, gen, provider, writer);
			return;
		}
		
		if (idFields.size() == 1) {
			gen.writeFieldName(writer.getName());
			gen.writeObject(idFields.get(0).getValue(value));
			return;
		} else {
			// Идентификатор-объект
			gen.writeFieldName(writer.getName());
			gen.writeStartObject();
			for (BeanPropertyDesc bpd : idFields) {
				gen.writeFieldName(bpd.getName());
				gen.writeObject(bpd.getValue(value));
			}
			gen.writeEndObject();
		}
		
		LOG.warn("Multiple IDs are not supported for entity {}. Serializing as POJO.", entityType.getEntityName());
		super.serializeAsField(pojo, gen, provider, writer);		
	}
}
