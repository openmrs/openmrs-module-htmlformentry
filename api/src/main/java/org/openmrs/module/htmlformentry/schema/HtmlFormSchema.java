package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the schema of an HTML Form
 */
public class HtmlFormSchema {

	private String name;
	private List<HtmlFormSection> sections = new ArrayList<HtmlFormSection>();
	private List<HtmlFormField> fields = new ArrayList<HtmlFormField>();
    
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
	 * Gets all the sections in the schema; don't use these to modify the schema, however, use addNewSection, addFieldToActiveSection, and endSection
	 * 
	 * @return the sections
	 */
	public List<HtmlFormSection> getSections() {
		return sections;
	}

    public List<HtmlFormField> getFields() {
        return fields;
    }


    // TODO fix getAllFields and getAllSections so that they return Lists, and the Lists are in the order the elements appear on the form?

    /**
     * Returns the entire flattened set of fields
     */
    public Set<HtmlFormField> getAllFields() {
        return getAllFieldsRecursiveSectionHelper(new HashSet<HtmlFormField>(), null);
    }

    private Set<HtmlFormField> getAllFieldsRecursiveSectionHelper(Set<HtmlFormField> fields, HtmlFormSection section) {
        getAllFieldsRecursiveFieldHelper(fields, section == null ? this.getFields() : section.getFields());
        for (HtmlFormSection s : section == null ? this.getSections() : section.getSections()) {
            getAllFieldsRecursiveSectionHelper(fields, s);
        }
        return fields;
    }

    private Set<HtmlFormField> getAllFieldsRecursiveFieldHelper(Set<HtmlFormField> fields, List<HtmlFormField> fieldsToAdd) {
        if (fieldsToAdd != null) {
            for (HtmlFormField f : fieldsToAdd) {
                fields.add(f);
                if (f instanceof ObsGroup) {
                    getAllFieldsRecursiveFieldHelper(fields, ((ObsGroup) f).getChildren());
                }
            }
        }
        return fields;
    }

    /**
     * Returns the entire flattened set of sections
     */
    public Set<HtmlFormSection> getAllSections() {
        return getAllSectionsRecursiveHelper(new HashSet<HtmlFormSection>(), null);
    }

    public Set<HtmlFormSection> getAllSectionsRecursiveHelper(Set<HtmlFormSection> sections, HtmlFormSection section) {
        if (sections != null) {
            for (HtmlFormSection s : section == null ? this.getSections() : section.getSections()) {
                sections.add(s);
                getAllSectionsRecursiveHelper(sections, s);
            }
        }
        return sections;
    }
	
}
