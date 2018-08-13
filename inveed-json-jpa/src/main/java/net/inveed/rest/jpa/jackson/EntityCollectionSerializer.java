package net.inveed.rest.jpa.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;

public class EntityCollectionSerializer extends JsonSerializer<Collection<?>> {
	private final CollectionSerializer master;
	public EntityCollectionSerializer(CollectionSerializer master) {
		this.master = master;
	}
	@Override
	public void serialize(Collection<?> value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		if (value != null) {
			ArrayList<Object> l = new ArrayList<>();
			for (Object v : value) {
				if (v instanceof HibernateProxy) {
					v = ((HibernateProxy) v).getHibernateLazyInitializer().getImplementation();
				}
				l.add(v);
			}
			value = l;
		}
		master.serialize(value, jgen, provider);
	}


}
