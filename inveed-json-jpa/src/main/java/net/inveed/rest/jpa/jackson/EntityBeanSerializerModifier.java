package net.inveed.rest.jpa.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;

public class EntityBeanSerializerModifier extends BeanSerializerModifier {
	@Override
	public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
			JsonSerializer<?> serializer) {
		if (serializer instanceof CollectionSerializer) {
			return new EntityCollectionSerializer((CollectionSerializer) serializer);
		} else if (serializer instanceof BeanSerializerBase) {
			return new EntitySerializer((BeanSerializerBase) serializer);
		} else {
			return serializer;
		}
	}
	
	@Override
	public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
			BeanDescription beanDesc, JsonSerializer<?> serializer) {
		if (serializer instanceof CollectionSerializer) {
			return new EntityCollectionSerializer((CollectionSerializer) serializer);
		} else {
			return super.modifyCollectionSerializer(config, valueType, beanDesc, serializer);
		}
	}
}
