package net.inveed.rest.jpa.typeutils;

import java.util.HashMap;

import net.inveed.commons.reflection.BeanPropertyDesc;
import net.inveed.commons.reflection.BeanTypeDesc;
import net.inveed.commons.reflection.ext.IBeanTypeExtension;

public class JsonTypeExt<T> implements IBeanTypeExtension<T> {
	private BeanTypeDesc<T> type;
	private final Object _lock = new Object();
	
	private HashMap<String, BeanPropertyDesc> __jsonProperties;
	public JsonTypeExt(BeanTypeDesc<T> type) {
		this.type = type;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public BeanTypeDesc<T> getBeanType() {
		return this.type;
	}

	@Override
	public void initialize() {
		if (this.__jsonProperties != null) {
			return;
		} 
		synchronized (this._lock) {
			if (this.__jsonProperties != null) {
				return;
			} 
			this.findProperties();
		}
	}

	private void findProperties() {
		HashMap<String, BeanPropertyDesc> jps = new HashMap<>();
		for (BeanPropertyDesc bpd : this.getBeanType().getDeclaredProperties().values()) {
			JsonPropertyExt jpe = new JsonPropertyExt(bpd, this);
			bpd.registerExtension(jpe);
			jpe.initialize();
		}
		
		for (BeanPropertyDesc bpd : this.getBeanType().getDeclaredProperties().values()) {
			JsonPropertyExt jpe = bpd.getExtension(JsonPropertyExt.class);
			if (jpe == null) {
				continue;
			}
			jps.put(jpe.getJSONName(), bpd);
		}
		this.__jsonProperties = jps;
	}
	
	public BeanPropertyDesc getJSONProperty(String name) {
		this.initialize();
		BeanPropertyDesc ret = this.__jsonProperties.get(name);
		if (ret != null) {
			return ret;
		}
		if (this.getBeanType().getSupertype() != null) {
			@SuppressWarnings("unchecked")
			JsonTypeExt<?> ste = this.getBeanType().getSupertype().getExtension(JsonTypeExt.class);
			if (ste == null) {
				return null;
			}
			return ste.getJSONProperty(name);
		}
		return null;
	}
}
