package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

public class HtmlFormSection {

	private String name;
	private List<HtmlFormField> fields = new ArrayList<HtmlFormField>();
    
    public HtmlFormSection() { }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the elements
	 */
	public List<HtmlFormField> getFields() {
		return fields;
	}
	
	/**
	 * @param element
	 */
	public void addField(HtmlFormField field) {
		fields.add(field);
	}

	/**
	 * @param elements the elements to set
	 */
	public void setFields(List<HtmlFormField> fields) {
		this.fields = fields;
	}
}
