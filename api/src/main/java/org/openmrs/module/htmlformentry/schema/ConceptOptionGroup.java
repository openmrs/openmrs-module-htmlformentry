package org.openmrs.module.htmlformentry.schema;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

/**
 * Represents a configuration for a group of Concepts that are used to configure a form or form
 * field
 */
public class ConceptOptionGroup {
	
	private Concept concept; // Specifies a concept whose set members or concept answers define the concepts to include
	
	private String groupLabel; // A label or message code that should be used to visually group these options in the UI
	
	private String groupClass; // A CSS class for the UI to apply to the grouping element
	
	private String labelMessagePrefix; // Message code prefix to prepend to concept uuid for each answer
	
	private String labelNameType; // Concept name type to use for each member
	
	private String labelNameTag; // Concept name tag to use for each member
	
	private String optionClass; // Optional CSS class to apply to all options
	
	private String retiredOptionClass; // Optional CSS class to apply to retired options
	
	private Boolean sortAlphabetically; // If true re-sorts the options in the set alphabetically
	
	public static ConceptOptionGroup newInstance(Map<String, String> attributes) throws BadFormDesignException {
		ConceptOptionGroup g = new ConceptOptionGroup();
		if (attributes != null) {
			for (String att : attributes.keySet()) {
				String val = attributes.get(att);
				if (StringUtils.isNotBlank(val)) {
					if (att.equalsIgnoreCase("concept")) {
						Concept concept = HtmlFormEntryUtil.getConcept(val);
						if (concept == null) {
							throw new BadFormDesignException("Unable to find concept: " + val);
						}
						g.setConcept(concept);
					} else if (att.equalsIgnoreCase("labelMessagePrefix")) {
						g.setLabelMessagePrefix(val);
					} else if (att.equalsIgnoreCase("labelNameType")) {
						g.setLabelNameType(val);
					} else if (att.equalsIgnoreCase("labelNameTag")) {
						g.setLabelNameTag(val);
					} else if (att.equalsIgnoreCase("optionClass")) {
						g.setOptionClass(val);
					} else if (att.equalsIgnoreCase("retiredOptionClass")) {
						g.setRetiredOptionClass(val);
					} else if (att.equalsIgnoreCase("sortAlphabetically")) {
						g.setSortAlphabetically(Boolean.parseBoolean(val));
					} else if (att.equalsIgnoreCase("groupLabel")) {
						g.setGroupLabel(val);
					} else if (att.equalsIgnoreCase("groupClass")) {
						g.setGroupClass(val);
					} else {
						throw new BadFormDesignException("Unknown attribute " + att + " for concept option set tag");
					}
				}
			}
		}
		return g;
	}
	
	public ConceptOptionGroup() {
	}
	
	public Concept getConcept() {
		return concept;
	}
	
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	public String getLabelMessagePrefix() {
		return labelMessagePrefix;
	}
	
	public void setLabelMessagePrefix(String labelMessagePrefix) {
		this.labelMessagePrefix = labelMessagePrefix;
	}
	
	public String getLabelNameType() {
		return labelNameType;
	}
	
	public void setLabelNameType(String labelNameType) {
		this.labelNameType = labelNameType;
	}
	
	public String getLabelNameTag() {
		return labelNameTag;
	}
	
	public void setLabelNameTag(String labelNameTag) {
		this.labelNameTag = labelNameTag;
	}
	
	public String getOptionClass() {
		return optionClass;
	}
	
	public void setOptionClass(String optionClass) {
		this.optionClass = optionClass;
	}
	
	public String getRetiredOptionClass() {
		return retiredOptionClass;
	}
	
	public void setRetiredOptionClass(String retiredOptionClass) {
		this.retiredOptionClass = retiredOptionClass;
	}
	
	public Boolean getSortAlphabetically() {
		return sortAlphabetically;
	}
	
	public void setSortAlphabetically(Boolean sortAlphabetically) {
		this.sortAlphabetically = sortAlphabetically;
	}
	
	public String getGroupLabel() {
		return groupLabel;
	}
	
	public void setGroupLabel(String groupLabel) {
		this.groupLabel = groupLabel;
	}
	
	public String getGroupClass() {
		return groupClass;
	}
	
	public void setGroupClass(String groupClass) {
		this.groupClass = groupClass;
	}
}
