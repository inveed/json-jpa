package net.inveed.rest.jpa.typeutils;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.inveed.typeutils.BeanPropertyDesc;
import net.inveed.typeutils.ext.IBeanPropertyExtension;

public class JsonPropertyExt implements IBeanPropertyExtension {
	private final BeanPropertyDesc beanProperty;
	private final JsonTypeExt<?> type;
	private int order = 1000;
	private String jsonName;

	public JsonPropertyExt(BeanPropertyDesc bpd, JsonTypeExt<?> type) {
		this.beanProperty = bpd;
		this.type = type;
	}
	
	public String getJSONName() {
		return this.jsonName;
	}

	public int getOrder() {
		return this.order;
	}
	
	public JsonTypeExt<?> getType() {
		return this.type;
	}
	
	public void initialize() {
		JsonProperty jp = this.beanProperty.getAnnotation(JsonProperty.class);
		if (jp != null) {
			this.order = jp.index();
			this.jsonName = jp.value();
		} else {
			this.order = 1000;
			this.jsonName = this.beanProperty.getName();
		}
	}

	@Override
	public boolean canGet() {
		return true;
	}

	@Override
	public boolean canSet() {
		return true;
	}
}
