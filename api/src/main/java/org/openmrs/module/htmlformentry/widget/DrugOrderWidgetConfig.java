package org.openmrs.module.htmlformentry.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.module.htmlformentry.schema.DrugOrderField;

/**
 * Holds the configuration for a DrugOrderWidget This mainly serves as an object into which the
 * DrugOrdersTagHandler can store the configuration following parsing, validating, and processing
 * all of the xml tag configuration in the htmlform, and enables passing this configuration to the
 * various widgets that are used to appropriately render the controls
 */
public class DrugOrderWidgetConfig {
	
	private DrugOrderField drugOrderField;
	
	private Map<String, String> attributes;
	
	private Map<String, String> templateAttributes;
	
	private String templateContent;
	
	private Map<String, Map<String, String>> orderPropertyAttributes;
	
	private Map<String, List<Option>> orderPropertyOptions;
	
	public DrugOrderWidgetConfig() {
	}
	
	// INSTANCE METHODS
	
	public Map<String, String> getAttributes(String orderProperty) {
		return orderPropertyAttributes.getOrDefault(orderProperty, new HashMap<>());
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
	
	public Map<String, Map<String, String>> getOrderPropertyAttributes() {
		if (orderPropertyAttributes == null) {
			orderPropertyAttributes = new LinkedHashMap<>();
		}
		return orderPropertyAttributes;
	}
	
	public void setOrderPropertyAttributes(String key, Map<String, String> attributes) {
		getOrderPropertyAttributes().put(key, attributes);
	}
	
	public Map<String, List<Option>> getOrderPropertyOptions() {
		if (orderPropertyOptions == null) {
			orderPropertyOptions = new HashMap<>();
		}
		return orderPropertyOptions;
	}
	
	public List<Option> getOrderPropertyOptions(String property) {
		return getOrderPropertyOptions().getOrDefault(property, new ArrayList<>());
	}
	
	public void addOrderPropertyOptions(String property, List<Option> options) {
		getOrderPropertyOptions().put(property, options);
	}
}
