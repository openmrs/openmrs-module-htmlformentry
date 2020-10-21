package org.openmrs.module.htmlformentry.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.schema.DrugOrderField;

/**
 * Holds the configuration for a DrugOrderWidget
 */
public class DrugOrderWidgetConfig {
	
	private DrugOrderField drugOrderField;
	
	private Map<String, String> attributes;
	
	private Map<String, String> templateAttributes;
	
	private String templateContent;
	
	private Map<String, Map<String, String>> templateWidgets;
	
	private Map<String, String> drugOrderAttributes;
	
	private Map<String, String> discontinueReasonAttributes;
	
	private List<Map<String, String>> discontinueReasonOptions;
	
	public DrugOrderWidgetConfig() {
	}
	
	// INSTANCE METHODS
	
	public Map<String, String> getTemplateConfig(String property) {
		return templateWidgets.getOrDefault(property, new HashMap<>());
	}
	
	// PROPERTY ACCESSORS
	
	public DrugOrderField getDrugOrderField() {
		return drugOrderField;
	}
	
	public void setDrugOrderField(DrugOrderField drugOrderField) {
		this.drugOrderField = drugOrderField;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	public Map<String, String> getTemplateAttributes() {
		return templateAttributes;
	}
	
	public void setTemplateAttributes(Map<String, String> templateAttributes) {
		this.templateAttributes = templateAttributes;
	}
	
	public String getTemplateContent() {
		return templateContent;
	}
	
	public void setTemplateContent(String templateContent) {
		this.templateContent = templateContent;
	}
	
	public Map<String, Map<String, String>> getTemplateWidgets() {
		if (templateWidgets == null) {
			templateWidgets = new LinkedHashMap<>();
		}
		return templateWidgets;
	}
	
	public void addTemplateWidget(String key, Map<String, String> attributes) {
		getTemplateWidgets().put(key, attributes);
	}
	
	public Map<String, String> getDrugOrderAttributes() {
		if (drugOrderAttributes == null) {
			drugOrderAttributes = new LinkedHashMap<>();
		}
		return drugOrderAttributes;
	}
	
	public void setDrugOrderAttributes(Map<String, String> drugOrderAttributes) {
		this.drugOrderAttributes = drugOrderAttributes;
	}
	
	public void addDiscontinueReasonOption(Map<String, String> discontinueReasonOption) {
		getDiscontinueReasonOptions().add(discontinueReasonOption);
	}
	
	public Map<String, String> getDiscontinueReasonAttributes() {
		return discontinueReasonAttributes;
	}
	
	public void setDiscontinueReasonAttributes(Map<String, String> discontinueReasonAttributes) {
		this.discontinueReasonAttributes = discontinueReasonAttributes;
	}
	
	public List<Map<String, String>> getDiscontinueReasonOptions() {
		if (discontinueReasonOptions == null) {
			discontinueReasonOptions = new ArrayList<>();
		}
		return discontinueReasonOptions;
	}
}
