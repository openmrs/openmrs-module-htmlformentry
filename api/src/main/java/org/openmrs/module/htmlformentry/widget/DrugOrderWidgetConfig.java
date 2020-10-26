package org.openmrs.module.htmlformentry.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.Order;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

/**
 * Holds the configuration for a DrugOrderWidget
 */
public class DrugOrderWidgetConfig {
	
	private DrugOrderField drugOrderField;
	
	private Map<String, String> attributes;
	
	private Map<String, String> templateAttributes;
	
	private String templateContent;
	
	private Map<String, Map<String, String>> orderPropertyAttributes;
	
	private Map<String, List<Option>> orderPropertyOptions;
	
	private Map<String, String> drugOrderAttributes;
	
	public DrugOrderWidgetConfig() {
	}
	
	// INSTANCE METHODS
	
	public Map<String, String> getAttributes(String orderProperty) {
		return orderPropertyAttributes.getOrDefault(orderProperty, new HashMap<>());
	}
	
	public void configureEnumPropertyOptions(String property, Enum[] vals) {
		boolean add = getOrderPropertyOptions(property).isEmpty();
		for (Enum e : vals) {
			String label = HtmlFormEntryUtil.translate("htmlformentry.drugOrder." + property + "." + e.name().toLowerCase());
			addIfOptionsEmptyOtherwisePopulateLabelIfEmpty(property, add, e.name(), label);
		}
	}
	
	public void configureMetadataPropertyOptions(String property, List<? extends OpenmrsMetadata> vals) {
		boolean add = getOrderPropertyOptions(property).isEmpty();
		for (OpenmrsMetadata m : vals) {
			String label = m.getName();
			addIfOptionsEmptyOtherwisePopulateLabelIfEmpty(property, add, m.getId().toString(), label);
		}
	}
	
	public void configureConceptPropertyOptions(String property, List<Concept> vals) {
		boolean add = getOrderPropertyOptions(property).isEmpty();
		for (Concept c : vals) {
			String label = c.getDisplayString();
			addIfOptionsEmptyOtherwisePopulateLabelIfEmpty(property, add, c.getId().toString(), label);
		}
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
	
	/**
	 * If this option list is empty, add a new option. Otherwise, update label if it is empty
	 */
	private void addIfOptionsEmptyOtherwisePopulateLabelIfEmpty(String property, boolean add, String value, String label) {
		if (add) {
			addOrderPropertyOption(property, value, label);
		} else {
			for (Option o : getOrderPropertyOptions(property)) {
				if (o.getValue().equalsIgnoreCase(value) && StringUtils.isBlank(o.getLabel())) {
					o.setLabel(label);
				}
			}
		}
	}
	
	public void toggleDefaultOptions() {
		for (String property : getOrderPropertyOptions().keySet()) {
			String valToSet = "";
			String val = getOrderPropertyAttributes().getOrDefault(property, new HashMap<>()).get("value");
			if (StringUtils.isNotBlank(val)) {
				if ("careSetting".equals(property)) {
					valToSet = HtmlFormEntryUtil.getCareSetting(val).getId().toString();
				} else if ("orderType".equals(property)) {
					valToSet = HtmlFormEntryUtil.getOrderType(val).getId().toString();
				} else if ("doseUnits".equals(property) || "route".equals(property) || "durationUnits".equals(property)
				        || "quantityUnits".equals(property) || "discontinueReason".equals(property)) {
					valToSet = HtmlFormEntryUtil.getConcept(val).getId().toString();
				} else if ("frequency".equals(property)) {
					valToSet = HtmlFormEntryUtil.getOrderFrequency(val).getId().toString();
				} else {
					valToSet = val;
				}
			}
			if (StringUtils.isBlank(valToSet)) {
				if ("orderType".equals(property)) {
					valToSet = HtmlFormEntryUtil.getDrugOrderType().getId().toString();
				}
				if ("urgency".equals(property)) {
					valToSet = Order.Urgency.ROUTINE.name();
				}
			}
			for (Option o : getOrderPropertyOptions().get(property)) {
				if (o.getValue().equals(valToSet)) {
					o.setSelected(true);
				}
			}
		}
	}
	
	public void addOrderPropertyOption(String orderProperty, String value, String label) {
		Map<String, List<Option>> m = getOrderPropertyOptions();
		List<Option> l = m.get(orderProperty);
		if (l == null) {
			l = new ArrayList<>();
			m.put(orderProperty, l);
		}
		l.add(new Option(label, value, false));
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
}
