
package org.openmrs.module.htmlformentry.handler;

import org.openmrs.OpenmrsObject;

public class AttributeDescriptor {

	String name;
	
    Class<? extends OpenmrsObject> clazz;

	
	/** 
	 * Constructor
	 * 
	 * @param attribute
	 * @param clazz
	 */
	public AttributeDescriptor(String name, Class<? extends OpenmrsObject> clazz) {
	    this.name = name;
	    this.clazz = clazz;
    }

	/**
	 * Getters and Setters
	 */
    public String getName() {
    	return name;
    }

    public void setName(String name) {
    	this.name = name;
    }
	
    public Class<? extends OpenmrsObject> getClazz() {
    	return clazz;
    }
	
    public void setClazz(Class<? extends OpenmrsObject> clazz) {
    	this.clazz = clazz;
    }
	
}
