package net.inveed.rest.jpa;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;

import net.inveed.rest.jpa.typeutils.EntityTypeExt;
import net.inveed.typeutils.JavaTypeDesc;
import net.inveed.typeutils.JavaTypeRegistry;
import net.inveed.typeutils.NativeTypeDesc;

public class JsonTypeUtils {
	public static Object getPrimitive(JavaTypeDesc<?> t, JsonParser p) throws IOException {
		if (t instanceof NativeTypeDesc) {
			NativeTypeDesc<?> type = (NativeTypeDesc<?>) t;
			
			if (type.isInt()) return p.getValueAsInt();
			else if (type.isByte()) return (byte) p.getValueAsInt();
			else if (type.isShort()) return (short) p.getValueAsInt();
			else if (type.isLong()) return p.getValueAsLong();
			else if (type.isFloat()) return (float) p.getValueAsDouble();
			else if (type.isDouble()) return p.getValueAsDouble();
			else if (type.isBoolean()) return p.getValueAsBoolean();
			else if (type.isChar()) return p.getTextCharacters()[0];
			else if (type.isString()) return p.getValueAsString();
			else if (type.isUUID()) return UUID.fromString(p.getValueAsString());
			else if (type.isDate()) return Date.parse(p.getValueAsString());
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static final String getEntityName(Class<?> entityType) {
		if (entityType == null) {
			return null;
		}
		JavaTypeDesc<?> type = JavaTypeRegistry.getType(entityType);
		if (type == null) {
			 return entityType.getSimpleName();
		}
		EntityTypeExt<?> ete = type.getExtension(EntityTypeExt.class);
		if (ete == null) {
			 return entityType.getSimpleName();
		}
		return ete.getEntityName();
	}
}
