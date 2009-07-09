package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

public class HtmlFormSchema {

	private String name;
	private List<HtmlFormSection> sections = new ArrayList<HtmlFormSection>();
    
    public HtmlFormSchema() { }

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
	 * @return the sections
	 */
	public List<HtmlFormSection> getSections() {
		return sections;
	}
	
	/**
	 * @param section
	 */
	public void addSection(HtmlFormSection section) {
		sections.add(section);
	}

	/**
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
	 * @return
	 */
	public HtmlFormSection getLastSection() {
		if (sections.isEmpty()) {
			startNewSection();
		}
		return sections.get(sections.size()-1);
	}
	
	/**
	 * @param element
	 */
	public void addField(HtmlFormField field) {
		getLastSection().addField(field);
	}
}
