package net.inveed.rest.jpa.jackson;

import java.io.IOException;
import java.util.Set;

import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;

import net.inveed.rest.jpa.annotations.TypeDescriminator;
import net.inveed.rest.jpa.annotations.TypeDescriminatorField;
import net.inveed.rest.jpa.jackson.JsonConfiguration.SerializationTheadLocal;
import net.inveed.rest.jpa.typeutils.EntityTypeExt;
import net.inveed.typeutils.BeanTypeDesc;
import net.inveed.typeutils.JavaTypeDesc;
import net.inveed.typeutils.JavaTypeRegistry;

public class EntitySerializer extends BeanSerializerBase {
	private static final long serialVersionUID = -9035067749825063337L;


	EntitySerializer(BeanSerializerBase source) {
		super(source);
	}

	EntitySerializer(EntitySerializer source, ObjectIdWriter objectIdWriter) {
		super(source, objectIdWriter);
	}

	EntitySerializer(EntitySerializer source, Set<String> toIgnore) {
		super(source, toIgnore);
	}

	public EntitySerializer(EntitySerializer source, ObjectIdWriter objectIdWriter, Object filterId) {
		super(source, objectIdWriter, filterId);
	}

	@Override
	public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
		return new EntitySerializer(this, objectIdWriter);
	}

	@Override
	protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
		return new EntitySerializer(this, toIgnore);
	}

	@Override
	protected BeanSerializerBase asArraySerializer() {
		return new BeanAsArraySerializer(this);
	}

	@Override
	public BeanSerializerBase withFilterId(Object filterId) {
		return new EntitySerializer(this, this._objectIdWriter, filterId);
	}

	private String getTypeDescriminatorFieldName(BeanTypeDesc<?> btd) {
		BeanTypeDesc<?> root = btd;
		while (btd != null) {
			@SuppressWarnings("unchecked")
			EntityTypeExt<?> ete = btd.getExtension(EntityTypeExt.class);
			if (ete == null) {
				break;
			} else {
				root = btd;
				btd = btd.getSupertype();
			}
		}
		TypeDescriminatorField a = root.getDeclaredAnnotation(TypeDescriminatorField.class);
		if (a == null) {
			return "#type";
		} else {
			return a.value();
		}
	}

	private String getTypeDescriminator(BeanTypeDesc<?> btd, EntityTypeExt<?> ete) {
		TypeDescriminator a = btd.getDeclaredAnnotation(TypeDescriminator.class);
		if (a == null) {
			return ete.getEntityName();
		} else {
			return a.value();
		}
	}

	
	@Override
	public void serialize(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
		
		SerializationTheadLocal ctx = null; 
		try {
			if (_objectIdWriter != null) {
				gen.setCurrentValue(bean);
				_serializeWithObjectId(bean, gen, provider, true);
				return;
			}
			
			if (bean == null) {
				gen.writeNull();
				return;
			}
			
			Class<?> entityType = bean.getClass();
			if (bean instanceof HibernateProxy) {
				entityType = ((HibernateProxy) bean).getHibernateLazyInitializer().getPersistentClass();
				bean = ((HibernateProxy) bean).getHibernateLazyInitializer().getImplementation();
			}
			String descField = null;
			String desc = null;
			
			JavaTypeDesc<?> et = JavaTypeRegistry.getType(entityType);
			if (et != null) {
				if (et instanceof BeanTypeDesc<?>) {
	
					BeanTypeDesc<?> btd = (BeanTypeDesc<?>) et;
	
					@SuppressWarnings("unchecked")
					EntityTypeExt<?> ete = btd.getExtension(EntityTypeExt.class);
					if (ete != null) {
						// в уровнях учитываем только сущности, которые могут быть сериализованы как ID.
						ctx = JsonConfiguration.getSerializationContext();
						ctx.currentLevel++;
						
						descField = this.getTypeDescriminatorFieldName(btd);
						desc = this.getTypeDescriminator(btd, ete);
					}
				} 
			}
			
			gen.writeStartObject(bean);
			if (_propertyFilterId != null) {
				serializeFieldsFiltered(bean, gen, provider);
			} else {
				serializeFields(bean, gen, provider);
			}
			
			if (desc != null) {
				gen.writeStringField(descField, desc);
			}
			gen.writeEndObject();
		} finally {
			if (ctx != null)
				ctx.currentLevel--;
		}
	}
}
