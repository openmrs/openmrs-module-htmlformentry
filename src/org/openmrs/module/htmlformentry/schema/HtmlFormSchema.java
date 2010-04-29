package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the schema of an HTML Form
 */
public class HtmlFormSchema {

	private String name;
	private List<HtmlFormSection> sections = new ArrayList<HtmlFormSection>();
    
    public HtmlFormSchema() { }

	/**
	 * Gets the name of the schema
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the schema
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets all the sections in the schema
	 * 
	 * @return the sections
	 */
	public List<HtmlFormSection> getSections() {
		return sections;
	}
	
	/**
	 * Adds a section to the schema
	 * 
	 * @param section the section to add
	 */
	public void addSection(HtmlFormSection section) {
		sections.add(section);
	}

	/**
	 * Sets the sections of the schema
	 * 
	 * @param sections the sections to set
	 */
	public void setSections(List<HtmlFormSection> sections) {
		this.sections = sections;
	}
	
	/**
	 * Adds a new empty section to the end of the schema
	 */
	public void startNewSection() {
		sections.add(new HtmlFormSection());
	}
	
	/**
	 * Returns the last section from the schema
	 * 
	 * @return the last section of the schema
	 */
	public HtmlFormSection getLastSection() {
		if (sections.isEmpty()) {
			startNewSection();
		}
		return sections.get(sections.size()-1);
	}
	
	/**
	 * Adds an HTML Form Field to the schema
	 * 
	 * @param field the field to add
	 */
	public void addField(HtmlFormField field) {
		getLastSection().addField(field);
	}
}
