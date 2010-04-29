package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a section in the HTML Form schema
 */
public class HtmlFormSection {

	private String name;
	private List<HtmlFormField> fields = new ArrayList<HtmlFormField>();
    
    public HtmlFormSection() { }

	/**
	 * Gets the name of the section
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the section
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the fields in the section
	 * 
	 * @return the fields
	 */
	public List<HtmlFormField> getFields() {
		return fields;
	}
	
	/**
	 * Adds a field to the section
	 * 
	 * @param field
	 */
	public void addField(HtmlFormField field) {
		fields.add(field);
	}

	/**
	 * Sets the fields in the section
	 * 
	 * @param fieldss the fields to set
	 */
	public void setFields(List<HtmlFormField> fields) {
		this.fields = fields;
	}
}
