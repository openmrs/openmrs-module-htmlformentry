
package org.openmrs.module.htmlformentry.handler;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.OpenmrsObject;

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
    Class<? extends OpenmrsObject> clazz;

    //Helper attributes for the designer
    boolean required;	// is this required or optional?
    String description;	// short description of how to use this attribute
    String uiWidget;	// the uiWidget to choose. See AttributeTag from HTML Form Entry Designer
    String label;		// label of the attribute
    Map<String, String> args; // arguments for the uiWidget 
	
	/** 
	 * Constructor
	 * 
	 * @param attribute
	 * @param clazz
	 */
	public AttributeDescriptor(String name, Class<? extends OpenmrsObject> clazz) {
	    this.name = name;
	    this.clazz = clazz;
	    this.required = false;
	    this.description = "";
	    this.uiWidget = "text";
	    this.label = name;
	    this.args = new HashMap<String, String>();
    }
	
	public AttributeDescriptor(String name, Class<? extends OpenmrsObject> clazz, String label, boolean required, String description, String uiWidget) {
		this.name = name;
		this.label = label;
		this.required = required;
		this.description = description;
		this.uiWidget = uiWidget;
		this.clazz = clazz;
	    this.args = new HashMap<String, String>();
	}
	
	public AttributeDescriptor(String name, String label, boolean required, String description, String uiWidget) {
		this.name = name;
		this.label = label;
		this.required = required;
		this.description = description;
		this.uiWidget = uiWidget;
		this.clazz = null;
	    this.args = new HashMap<String, String>();
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

	/**
	 * @return the required
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the uiWidget
	 */
	public String getUiWidget() {
		return uiWidget;
	}

	/**
	 * @param uiWidget the uiWidget to set
	 */
	public void setUiWidget(String uiWidget) {
		this.uiWidget = uiWidget;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void addArgument(String key, String value) {
		args.put(key, value);
	}
	
	public String getArgument(String key) {
		return args.get(key);
	}
	
}
