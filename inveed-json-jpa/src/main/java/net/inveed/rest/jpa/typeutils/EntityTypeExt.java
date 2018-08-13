package net.inveed.rest.jpa.typeutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import net.inveed.rest.jpa.IEntityInstantiator;
import net.inveed.rest.jpa.annotations.EntityInstantiator;
import net.inveed.typeutils.BeanPropertyDesc;
import net.inveed.typeutils.BeanTypeDesc;
import net.inveed.typeutils.JavaTypeRegistry;
import net.inveed.typeutils.TypeUtils;
import net.inveed.typeutils.ext.IBeanTypeExtension;

public class EntityTypeExt<T> implements IBeanTypeExtension<T> {
	private static final HashMap<String, BeanTypeDesc<?>> entityMap = new HashMap<>();
	
	private final BeanTypeDesc<T> type;
	private final String entityName;
	private final String tableName;
	private boolean mappedSuperclass = false;
	
	private final List<BeanPropertyDesc> idFields;
	
	public EntityTypeExt(BeanTypeDesc<T> type) {
		this.type = type;
		this.idFields = new ArrayList<>();
		
		Entity annEntity = type.getAnnotation(Entity.class);
		if (annEntity != null) {
			if (annEntity.name().equals("")) {
				this.entityName = type.getShortName();
			} else {
				this.entityName = annEntity.name();
			}
			entityMap.put(this.entityName, type);
			
			Table ta = type.getAnnotation(Table.class);
			if (ta != null) {
				this.tableName = ta.name();
			} else {
				this.tableName = null; //TODO: учесть наличие родителей!
			}
		} else {
			MappedSuperclass annMS = type.getAnnotation(MappedSuperclass.class);
			if (annMS != null) {
				this.mappedSuperclass = true;
				this.entityName = type.getShortName();
				entityMap.put(this.entityName, type);
				
				this.tableName = null;
			} else {
				this.entityName = null;
				this.tableName = null;
			}
		}
	}
	
	public static final BeanTypeDesc<?> getEntity(String name) {
		return entityMap.get(name);
	}
	
	@Override
	public boolean isValid() {
		return this.entityName != null;
	}
	
	public boolean isMappedSuperclass() {
		return this.mappedSuperclass;
	}
	@Override
	public BeanTypeDesc<T> getBeanType() {
		return this.type;
	}
	
	public String getTableName() {
		return this.tableName;
	}
	
	@Override
	public void initialize() {
		for (BeanPropertyDesc bpd : this.type.getDeclaredProperties().values()) {
			bpd.registerExtension(new EntityPropertyExt(bpd));
		}
		this.processIdFields();
	}
	protected void processIdFields() {
		
		for (BeanPropertyDesc f : this.type.getDeclaredProperties().values()) {
			EntityPropertyExt epe = f.getExtension(EntityPropertyExt.class);
			if (epe == null) {
				continue;
			}
			if (epe.isId()) {
				this.idFields.add(f);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<BeanPropertyDesc> getIDFields() {
		if (this.getBeanType().getSupertype() != null) {
			EntityTypeExt<?> superEntity = this.getBeanType().getSupertype().getExtension(EntityTypeExt.class);
			if (superEntity != null) {
				return superEntity.getIDFields();
			}
		}
		return Collections.unmodifiableList(this.idFields);
	}
	
	public String getEntityName() {
		return this.entityName;
	}
	
	
	public IEntityInstantiator<?, ?> getInstantiator() {
		EntityInstantiator eia = this.type.getAnnotation(EntityInstantiator.class);
		if (eia == null) {
			if (this.type.getSupertype() != null) {
				@SuppressWarnings("unchecked")
				EntityTypeExt<?> parentExt = this.type.getSupertype().getExtension(EntityTypeExt.class);
				if (parentExt == null) {
					return null;
				}
				return parentExt.getInstantiator();
			} else {
				return null;
			}
		}
		try {
			Class<? extends IEntityInstantiator<?,?>> iclass = eia.value();
			Constructor<? extends IEntityInstantiator<?,?>> ctr = iclass.getDeclaredConstructor();
			ctr.setAccessible(true);
			return ctr.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	public T get(
			EntityManager em,
			Object id
			) {

		IEntityInstantiator<?,Object> instantiator = (IEntityInstantiator<?, Object>) this.getInstantiator();
		if (instantiator != null) {
			Object oid = TypeUtils.toObject(JavaTypeRegistry.getType(instantiator.getIdType()), id);
			if (oid != null) {
				return (T) instantiator.find(em, oid);
			}
		}
		List<BeanPropertyDesc> idFields = this.getIDFields();
		if (idFields.size() != 1) {
			return null;
		}
		
		BeanPropertyDesc idField = idFields.get(0);
		EntityPropertyExt idEntityField = idField.getExtension(EntityPropertyExt.class);
		if (idEntityField == null) {
			//TODO: LOG!
			return null;
		}
		
		if (!idEntityField.isSimpleId()) {
			return null; //TODO: LOG тут мы не поддерживаем комплексные ID
		}
		
		Object idObj = null;
		
		if (idField.getType() instanceof BeanTypeDesc<?>) {
			EntityTypeExt<?> idEntityExt = ((BeanTypeDesc<?>) idField.getType()).getExtension(EntityTypeExt.class);
			if (idEntityExt != null) {
				idObj = idEntityExt.get(em, id);
			}
		} 
		if (idObj == null) {
			idObj = TypeUtils.toObject(idField.getType(), id);
		}
		
		if (idObj == null) {
			return null;
		}
		
		return em.find(this.type.getType(), idObj);
	}

	@SuppressWarnings("unchecked")
	public static String[] getEntities() {
		ArrayList<String> ret = new ArrayList<>();
		int size = 0;
		while (size != entityMap.size()) {
			size = entityMap.size();
			ret.clear();
			for (BeanTypeDesc<?> v : new ArrayList<>(entityMap.values())) {
				EntityTypeExt<?> ee = v.getExtension(EntityTypeExt.class);
				if (!ee.isMappedSuperclass()) {
					ret.add(ee.getEntityName());
				}
			}
		}
		return ret.toArray(new String[ret.size()]);
	}
}
