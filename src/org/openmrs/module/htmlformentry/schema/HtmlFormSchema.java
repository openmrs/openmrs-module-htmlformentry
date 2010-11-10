package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Represents the schema of an HTML Form
 */
public class HtmlFormSchema {

	private String name;
	private List<HtmlFormSection> sections = new ArrayList<HtmlFormSection>();
	//for embedded sections
	private Stack<HtmlFormSection> sectionsStack = new Stack<HtmlFormSection>();
	//this captures the schema elements in the same order as the FormSubmissionAction list
	private List<HtmlFormField> allFields = new ArrayList<HtmlFormField>();
    
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
		if (sectionsStack.size() > 0)
		    sectionsStack.peek().addChildSection(section);
		sectionsStack.push(section);
		allFields.add(section);
	}
	
	/**
	 * Adds a new empty section to the end of the schema
	 */
	public void startNewSection() {
		addSection(new HtmlFormSection());
	}
	
	/**
	 * Returns the last section from the schema
	 * 
	 * @return the last section of the schema
	 */
	public HtmlFormSection getCurrentSection() {
	    if (sectionsStack.size() == 0)
	        startNewSection();
		return sectionsStack.peek();
	}
	
	/**
	 * Adds an HTML Form Field to the schema
	 * 
	 * @param field the field to add
	 */
	public void addField(HtmlFormField field) {
		getCurrentSection().addField(field);
		allFields.add(field);
	}
	
	/**
	 * 
	 * Sets the current section to length - 2 of sections array, if it exists.
	 *
	 */
	public void endSection(){
	    sectionsStack.pop();
	}

	/**
	 * 
	 * returns the list of HtmlFormField in the same order as the FormSubmissionActions in the Session
	 * 
	 * @return allFields 
	 */
    public List<HtmlFormField> getAllFields() {
        return allFields;
    }
	
	
	
	
	
}
