package net.inveed.rest.jpa.typeutils;


import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import net.inveed.typeutils.BeanPropertyDesc;
import net.inveed.typeutils.ext.IBeanPropertyExtension;


public class EntityPropertyExt implements IBeanPropertyExtension {

	private final BeanPropertyDesc property;
	private Boolean isId;
	private Boolean isSimpleId;
	
	protected EntityPropertyExt(BeanPropertyDesc property) {
		this.property = property;
	}
	
	private void initializeEntityPropertyDesc() {
		Id idAnnotation = this.property.getAnnotation(Id.class);
		EmbeddedId eidAnnotation = this.property.getAnnotation(EmbeddedId.class);
		
		this.isId = idAnnotation != null || eidAnnotation != null;
		this.isSimpleId = idAnnotation != null && eidAnnotation == null;
	}
	public boolean isId() {
		if (this.isId == null || this.isSimpleId == null) {
			this.initializeEntityPropertyDesc();
		}
		return isId;
	}

	public boolean isSimpleId() {
		if (this.isId == null || this.isSimpleId == null) {
			this.initializeEntityPropertyDesc();
		}
		return isSimpleId;
	}

	@Override
	public boolean canGet() {
		return true;
	}

	@Override
	public boolean canSet() {
		if (this.property.getAnnotation(Id.class) != null) {
			return false;
		}
		
		Column ca = this.property.getAnnotation(Column.class);
		if (ca != null && !ca.updatable()) {
			return false;
		}
		ManyToOne ma = this.property.getAnnotation(ManyToOne.class);
		if (ma != null) {
			JoinColumn ja = this.property.getAnnotation(JoinColumn.class);
			if (ja != null) {
				return ja.updatable();
			} else {
				JoinColumns jcsa = this.property.getAnnotation(JoinColumns.class);
				if (jcsa == null) {
					return false; // маппинг на чтение
				}
				for (JoinColumn jc : jcsa.value()) {
					if (!jc.updatable()) {
						return false;
					}
				}
			}
		} 
		return true;
	}
}
