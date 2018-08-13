package net.inveed.rest.jpa.jackson;

public class JsonEntityNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8525194045693689312L;
	
	public JsonEntityNotFoundException(Class<?> entityType, Object id) {
		super("Entity with type '" + entityType +"' and id '" + id + "' not found");
	}
}
