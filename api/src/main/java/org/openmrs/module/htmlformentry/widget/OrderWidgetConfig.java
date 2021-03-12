package org.openmrs.module.htmlformentry.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.htmlformentry.schema.OrderField;

/**
 * Holds the configuration for an OrderWidget This mainly serves as an object into which the
 * OrderTagHandler can store the configuration following parsing, validating, and processing all of
 * the xml tag configuration in the htmlform, and enables passing this configuration to the various
 * widgets that are used to appropriately render the controls
 */
public class OrderWidgetConfig {
	
	private OrderField orderField;
	
	private Map<String, String> attributes;
	
	private Map<String, String> templateAttributes;
	
	private String templateContent;
	
	private Map<String, Map<String, String>> orderPropertyAttributes;
	
	private Map<String, List<Option>> orderPropertyOptions;
	
	// Serves to provide access to the orderables configured
	
	private Map<Concept, List<Drug>> conceptsAndDrugsConfigured = new HashMap<>();
	
	public OrderWidgetConfig() {
	}
	
	// INSTANCE METHODS
	
	public Map<String, String> getAttributes(String orderProperty) {
		return getOrderPropertyAttributes().getOrDefault(orderProperty, new HashMap<>());
	}
	
	// PROPERTY ACCESSORS
	
	public OrderField getOrderField() {
		return orderField;
	}
	
	public void setOrderField(OrderField orderField) {
		this.orderField = orderField;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getAttribute(String attribute) {
		return attributes == null ? null : attributes.get(attribute);
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
		List<Option> l = getOrderPropertyOptions().get(property);
		if (l == null) {
			l = new ArrayList<>();
			getOrderPropertyOptions().put(property, l);
		}
		return l;
	}
	
	public Option getOption(String property, String value) {
		for (Option o : getOrderPropertyOptions(property)) {
			if (o.getValue().equalsIgnoreCase(value)) {
				return o;
			}
		}
		return null;
	}
	
	public void setOrderPropertyOptions(String property, List<Option> options) {
		getOrderPropertyOptions().put(property, options);
	}
	
	public void addOrderPropertyOption(String property, Option option) {
		getOrderPropertyOptions(property).add(option);
	}
	
	public Map<Concept, List<Drug>> getConceptsAndDrugsConfigured() {
		if (conceptsAndDrugsConfigured == null) {
			conceptsAndDrugsConfigured = new LinkedHashMap<>();
		}
		return conceptsAndDrugsConfigured;
	}
	
	public void setConceptsAndDrugsConfigured(Map<Concept, List<Drug>> conceptsAndDrugsConfigured) {
		this.conceptsAndDrugsConfigured = conceptsAndDrugsConfigured;
	}
}
