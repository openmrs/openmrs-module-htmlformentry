package org.openmrs.module.htmlformentry.widget;

import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.FORMAT_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.ON_SELECT;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

public class DrugOrdersWidget implements Widget {

	private DrugOrderField drugOrderField;

	private DrugOrderWidgetConfig widgetConfig;

	private List<Map<String, String>> drugOrderOptions;
	
	private List<DrugOrderWidget> drugOrderWidgets;
	
	private List<DrugOrder> initialValue;
	
	public DrugOrdersWidget() {
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (List<DrugOrder>) initialValue;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		startTag(writer, "div", "drugOrdersSection", fieldName, null);
		writer.println();
		
		// Add a hidden input for the field name to submit the json data back to the server to represent the changes
		String inputId = fieldName;
		writer.println("<input type=\"hidden\" id=\"" + inputId + "\" name=\"" + inputId + "\"/>");
		writer.println();
		
		// If the format is onselect, hide all of the drug sections (unless already in the encounter) and show select widget
		boolean onSelect = ON_SELECT.equals(widgetConfig.getDrugOrderAttributes().getOrDefault(FORMAT_ATTRIBUTE, ""));
		startTag(writer, "span", "drugSelectorSection", fieldName, (onSelect ? "" : "display:none"));
		writer.println();
		
		// Add a drug selector to the section.  This will only be visible if the section is visible
		startTag(writer, "select", "drugSelector", fieldName, null);
		writer.println();
		for (DrugOrderAnswer a : drugOrderField.getDrugOrderAnswers()) {
			Integer id = a.getDrug().getId();
			writer.print("<option value=\"" + id + "\"" + ">");
			writer.print(a.getDisplayName());
			writer.println("</option>");
		}
		writer.println("</select>");
		
		writer.println("</span>");
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		
		for (DrugOrderAnswer a : drugOrderField.getDrugOrderAnswers()) {
			DrugOrder initialValueForDrug = getInitialValueForDrug(a.getDrug());
			
			// All elements for a given drug will have an id prefix like "fieldName_drugId"
			String idPrefix = fieldName + "_" + a.getDrug().getId();
			String sectionStyle = (onSelect && initialValueForDrug == null ? "display:none" : "");
			
			startTag(writer, "div", "drugOrderSection", idPrefix, sectionStyle);
			writer.println();
			
			DrugOrderWidget drugOrderWidget = new DrugOrderWidget(context, widgetConfig);
			if (initialValueForDrug != null) {
				drugOrderWidget.setInitialValue(initialValueForDrug);
			}
			getDrugOrderWidgets().add(drugOrderWidget);
			writer.print(drugOrderWidget.generateHtml(context));
			
			writer.println();
			writer.println("</div>");
			writer.println("<script type=\"text/javascript\">");
			writer.println("jQuery(function() { htmlForm.initializeDrugOrderWidget('" + idPrefix + "')});");
			writer.println("</script>");
		}
		
		writer.println("</div>");
		
		return writer.getContent();
	}
	
	protected void startTag(CapturingPrintWriter w, String tagName, String classId, String elementPrefix, String cssStyle) {
		w.print("<" + tagName);
		w.print(" id=\"" + elementPrefix + classId + "\"");
		w.print(" class=\"" + classId + "\"");
		if (StringUtils.isNotBlank(cssStyle)) {
			w.print(" style=\"" + cssStyle + "\"");
		}
		w.print(">");
	}
	
	public DrugOrder getInitialValueForDrug(Drug drug) {
		if (initialValue != null) {
			for (DrugOrder drugOrder : initialValue) {
				if (drugOrder.getDrug().equals(drug)) {
					return drugOrder;
				}
			}
		}
		return null;
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		List<DrugOrder> drugOrders = new ArrayList<>();
		for (DrugOrderWidget widget : getDrugOrderWidgets()) {
			DrugOrder drugOrder = (DrugOrder) widget.getValue(context, request);
			if (drugOrder != null) {
				drugOrders.add(drugOrder);
			}
		}
		return drugOrders;
	}

	public DrugOrderField getDrugOrderField() {
		return drugOrderField;
	}

	public void setDrugOrderField(DrugOrderField drugOrderField) {
		this.drugOrderField = drugOrderField;
	}

	public DrugOrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}

	public void setWidgetConfig(DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
	}

	public List<Map<String, String>> getDrugOrderOptions() {
		if (drugOrderOptions == null) {
			drugOrderOptions = new ArrayList<>();
		}
		return drugOrderOptions;
	}
	
	public void addDrugOrderOption(Map<String, String> drugOrderOption) {
		getDrugOrderOptions().add(drugOrderOption);
	}
	
	public List<DrugOrderWidget> getDrugOrderWidgets() {
		if (drugOrderWidgets == null) {
			drugOrderWidgets = new ArrayList<>();
		}
		return drugOrderWidgets;
	}
}
