package org.openmrs.module.htmlformentry.widget;

import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.FORMAT_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.ON_SELECT;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

public class DrugOrdersWidget implements Widget {
	
	private DrugOrderField drugOrderField;
	
	private DrugOrderWidgetConfig widgetConfig;
	
	private Map<Drug, DrugOrderWidget> drugOrderWidgets;
	
	private Map<Drug, DrugOrder> initialValue;
	
	public DrugOrdersWidget(FormEntryContext context, DrugOrderField drugOrderField, DrugOrderWidgetConfig widgetConfig) {
		this.drugOrderField = drugOrderField;
		this.widgetConfig = widgetConfig;
		for (DrugOrderAnswer answer : drugOrderField.getDrugOrderAnswers()) {
			DrugOrderWidget drugOrderWidget = new DrugOrderWidget(context, answer, widgetConfig);
			getDrugOrderWidgets().put(answer.getDrug(), drugOrderWidget);
		}
	}
	
	@Override
	public void setInitialValue(Object v) {
		this.initialValue = (Map<Drug, DrugOrder>) v;
		if (initialValue != null) {
			for (Drug d : initialValue.keySet()) {
				DrugOrderWidget w = drugOrderWidgets.get(d);
				if (w != null) {
					w.setInitialValue(initialValue.get(d));
				}
			}
		}
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		boolean viewMode = (context.getMode() == FormEntryContext.Mode.VIEW);
		boolean onSelect = ON_SELECT.equals(widgetConfig.getDrugOrderAttributes().getOrDefault(FORMAT_ATTRIBUTE, ""));
		
		// Wrap the entire widget in a div
		startTag(writer, "div", fieldName, "drugOrdersSection", null);
		writer.println();
		
		// Render a widget to choose a drug, if mode is not VIEW, and if configured to do so
		if (!viewMode) {
			String sectionId = fieldName + "DrugSelectorSection";
			startTag(writer, "span", sectionId, "drugSelectorSection", (onSelect ? "" : "display:none"));
			writer.println();
			
			// Add a drug selector to the section.  This will only be visible if the section is visible
			String selectorId = fieldName + "DrugSelector";
			startTag(writer, "select", selectorId, "drugSelector", null);
			writer.println();
			for (DrugOrderAnswer a : drugOrderField.getDrugOrderAnswers()) {
				Integer id = a.getDrug().getId();
				writer.print("<option value=\"" + id + "\"" + ">");
				writer.print(a.getDisplayName());
				writer.println("</option>");
			}
			writer.println("</select>");
			
			writer.println("</span>");
		}
		
		// Establish a json config that can be used to initialize a Javascript function
		Map<String, Object> jsonConfig = new LinkedHashMap<>();
		jsonConfig.put("fieldName", fieldName);
		List<Map<String, Object>> jsonDrugs = new ArrayList<>();
		jsonConfig.put("drugs", jsonDrugs);
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		for (Drug drug : getDrugOrderWidgets().keySet()) {
			DrugOrderWidget drugOrderWidget = getDrugOrderWidgets().get(drug);
			// All elements for a given drug will have an id prefix like "fieldName_drugId"
			String drugOrderSectionId = fieldName + "_" + drug.getId();
			String sectionStyle = (onSelect && drugOrderWidget.getInitialValue() == null ? "display:none" : "");
			
			startTag(writer, "div", drugOrderSectionId, "drugOrderSection", sectionStyle);
			writer.println();
			
			// First we render the drugOrder widget with any existing or active drug order in VIEW mode
			
			writer.print(drugOrderWidget.generateHtml(context));
			writer.println();
			writer.println("</div>");
			
			// For each rendered drugOrderWidget, add configuration of that widget into json for javascript
			Map<String, Object> jsonDrug = new LinkedHashMap<>();
			jsonDrugs.add(jsonDrug);
			jsonDrug.put("drugId", drug.getId());
			jsonDrug.put("drugLabel", drugOrderWidget.getDrugOrderAnswer().getDisplayName());
			jsonDrug.put("sectionId", drugOrderSectionId);
			
			Map<String, String> widgetIds = new LinkedHashMap<>();
			jsonDrug.put("widgetIds", widgetIds);
			
			for (String key : drugOrderWidget.getWidgetReplacements().keySet()) {
				widgetIds.put(key, context.getFieldName(drugOrderWidget.getWidgetReplacements().get(key)));
			}
		}
		
		// Add javascript function to initialize widget as appropriate
		String onLoadFn = widgetConfig.getAttributes().get("onLoadFunction");
		if (StringUtils.isNotBlank(onLoadFn)) {
			writer.println("<script type=\"text/javascript\">");
			writer.println("jQuery(function() { " + onLoadFn + "(");
			writer.println(HtmlFormEntryUtil.serializeToJson(jsonConfig));
			writer.println(")});");
			writer.println("</script>");
		}
		
		writer.println("</div>");
		
		return writer.getContent();
	}
	
	protected void startTag(CapturingPrintWriter w, String tagName, String elementId, String className, String cssStyle) {
		w.print("<" + tagName);
		w.print(" id=\"" + elementId + "\"");
		w.print(" class=\"" + className + "\"");
		if (StringUtils.isNotBlank(cssStyle)) {
			w.print(" style=\"" + cssStyle + "\"");
		}
		w.print(">");
	}
	
	@Override
	public List<DrugOrderWidgetValue> getValue(FormEntryContext context, HttpServletRequest request) {
		List<DrugOrderWidgetValue> ret = new ArrayList<>();
		for (DrugOrderWidget widget : getDrugOrderWidgets().values()) {
			DrugOrderWidgetValue drugOrder = widget.getValue(context, request);
			ret.add(drugOrder);
		}
		return ret;
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
	
	public Map<Drug, DrugOrderWidget> getDrugOrderWidgets() {
		if (drugOrderWidgets == null) {
			drugOrderWidgets = new LinkedHashMap<>();
		}
		return drugOrderWidgets;
	}
}
