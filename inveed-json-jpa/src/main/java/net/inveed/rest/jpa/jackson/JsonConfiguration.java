package net.inveed.rest.jpa.jackson;

import java.io.IOException;
import java.io.StringWriter;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JsonConfiguration {
	
	private static class Ctx {
		EntityManager em;
	}
	public static class SerializationTheadLocal {
		public int deep;
		public int currentLevel;
	}
	
	@JsonFilter("serFilter")
	class PropertyFilterMixIn {}
	
	private ObjectMapper objectMapper;
	
	private static final ThreadLocal<Ctx> ctxLocal = new ThreadLocal<>();
	private static final ThreadLocal<SerializationTheadLocal> slocal = new ThreadLocal<>();

	public ObjectMapper getMapper() {
		if (this.objectMapper != null) {
			return this.objectMapper;
		}
		synchronized (this) {
			if (this.objectMapper != null) {
				return this.objectMapper;
			}
		
			this.objectMapper = new ObjectMapper();
			this.objectMapper.addMixIn(Object.class, PropertyFilterMixIn.class);


			this.objectMapper.registerModule(new Module(){

				@Override
				public String getModuleName() {
					return "inveed-jpa-rest";
				}

				@Override
				public Version version() {
					return Version.unknownVersion();
				}

				@Override
				public void setupModule(SetupContext context) {
					context.addBeanSerializerModifier(new EntityBeanSerializerModifier());
					context.addBeanDeserializerModifier(new EntityBeadDeserializerModifier());
				} });

			EntitySerializationPropertyFilter serFilter = new EntitySerializationPropertyFilter();
			FilterProvider filters = new SimpleFilterProvider().addFilter("serFilter", serFilter);
			this.objectMapper.setFilterProvider(filters);
		}
		
		
		return this.objectMapper;
	}
	
	public static final EntityManager getContextEntityManager() {
		Ctx ctx = ctxLocal.get();
		if (ctx == null) {
			return null;
		}
		return ctx.em;
	}
	
	public static final SerializationTheadLocal getSerializationContext() {
		SerializationTheadLocal ret = slocal.get();
		if (ret == null) {
			ret = new SerializationTheadLocal();
			slocal.set(ret);
		}
		return ret;
	}
	
	public String serialize(Object value, int deep) throws JsonGenerationException, JsonMappingException, IOException {
		SerializationTheadLocal tl = new SerializationTheadLocal();
		tl.deep = deep;
		tl.currentLevel = 0;
		slocal.set(tl);
		try {
			StringWriter w = new StringWriter();
			this.getMapper().writer().writeValue(w, value);
			return w.toString();
		} finally {
			slocal.remove();
		}
		
	}
	
	public JsonNode serializeToNode(Object value, int deep) {
		SerializationTheadLocal tl = new SerializationTheadLocal();
		tl.deep = deep;
		tl.currentLevel = 0;
		slocal.set(tl);
		try {
			return this.getMapper().valueToTree(value);
		} finally {
			slocal.remove();
		}
	}
	
	public <T> T deserialize(EntityManager em, String json, Class<T> type) throws JsonParseException, JsonMappingException, IOException {
		Ctx ctx = new Ctx();
		ctx.em = em;
		ctxLocal.set(ctx);
		try {
			return this.getMapper().readValue(json, type);
		} finally {
			ctxLocal.remove();
		}
	}
	
	public <T> T deserialize(EntityManager em, String json, T target) throws JsonParseException, JsonMappingException, IOException {
		Ctx ctx = new Ctx();
		ctx.em = em;
		ctxLocal.set(ctx);
		try {
			return this.getMapper().readerForUpdating(target).readValue(json);
		} finally {
			ctxLocal.remove();
		}
	}

	public <T> T deserialize(EntityManager em, JsonParser jsonParser, Class<T> type) throws JsonParseException, JsonMappingException, IOException {
		Ctx ctx = new Ctx();
		ctx.em = em;
		ctxLocal.set(ctx);
		try {
			return this.getMapper().readValue(jsonParser, type);
		} finally {
			ctxLocal.remove();
		}
	}
	
	public <T> T deserialize(EntityManager em, JsonParser jsonParser, T target) throws JsonParseException, JsonMappingException, IOException {
		Ctx ctx = new Ctx();
		ctx.em = em;
		ctxLocal.set(ctx);
		try {
			return this.getMapper().readerForUpdating(target).readValue(jsonParser);
		} finally {
			ctxLocal.remove();
		}
	}
}
