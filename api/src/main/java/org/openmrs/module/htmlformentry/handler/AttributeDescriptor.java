
package org.openmrs.module.htmlformentry.handler;


/**
 * Attribute descriptors are used to define the attributes that a specific tag handler supports
 * 
 * Name is the name of the attribute, and clazz is the class (if any) that the attribute values reference.
 * For example, the "conceptId" attribute on the "obs" tag would have name = "conceptId" and clazz = "Concept.class"
 *
 * Attribute descriptors are defined within tag handlers using the createAttributeDescriptors method
 */
public class AttributeDescriptor {

	// the name of the attribute
	String name;
	
	// the clazz that the attribute values references (may be null if the attribute does not reference any OpenMRS objects)
    Class<?> clazz;

	
	/** 
	 * Constructor
	 * 
	 * @param attribute
	 * @param clazz
	 */
	public AttributeDescriptor(String name, Class<?> clazz) {
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
	
    public Class<?> getClazz() {
    	return clazz;
    }
	
    public void setClazz(Class<?> clazz) {
    	this.clazz = clazz;
    }
	
}
