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
import org.openmrs.Encounter;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.util.JsonObject;

public class DrugOrdersWidget implements Widget {
	
	private DrugOrderField drugOrderField;
	
	private DrugOrderWidgetConfig widgetConfig;
	
	private Map<Drug, DrugOrderWidget> drugOrderWidgets;
	
	private Map<Drug, List<DrugOrder>> initialValue;
	
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
		this.initialValue = (Map<Drug, List<DrugOrder>>) v;
	}
	
	public List<DrugOrder> getInitialValueForDrug(Drug drug) {
		return initialValue == null ? new ArrayList<>() : initialValue.getOrDefault(drug, new ArrayList<>());
	}
	
	public DrugOrder getInitialValueForDrugInEncounter(Encounter encounter, Drug drug) {
		DrugOrder ret = null;
		if (encounter != null && drug != null) {
			// These are ordered by effectiveStartDate asc, so return the latest drug order in the list
			for (DrugOrder drugOrder : getInitialValueForDrug(drug)) {
				if (drugOrder.getEncounter().equals(encounter)) {
					ret = drugOrder;
				}
			}
		}
		return ret;
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
		JsonObject jsonConfig = new JsonObject();
		jsonConfig.addString("fieldName", fieldName);
		jsonConfig.addString("mode", context.getMode().name());
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		for (Drug drug : getDrugOrderWidgets().keySet()) {
			DrugOrderWidget drugOrderWidget = getDrugOrderWidgets().get(drug);
			String drugLabel = drugOrderWidget.getDrugOrderAnswer().getDisplayName();
			
			// All elements for a given drug will have an id prefix like "fieldName_drugId"
			String drugOrderSectionId = fieldName + "_" + drug.getId();
			String sectionStyle = (onSelect && drugOrderWidget.getInitialValue() == null ? "display:none" : "");
			startTag(writer, "div", drugOrderSectionId, "drugOrderSection", sectionStyle);
			
			writer.append("<span class=\"drugOrdersDrugName\">").append(drugLabel).append("</span>");
			String entryId = drugOrderSectionId + "_entry";
			startTag(writer, "span", entryId, "drugOrderEntry", "display:none;");
			writer.print(drugOrderWidget.generateHtml(context));
			writer.println();
			writer.println("</span>");
			
			writer.println("</div>");
			
			// For each rendered drugOrderWidget, add configuration of that widget into json for javascript
			JsonObject jsonDrug = jsonConfig.addObjectToArray("drugs");
			jsonDrug.addString("drugId", drug.getId().toString());
			jsonDrug.addString("drugLabel", drugLabel);
			jsonDrug.addString("sectionId", drugOrderSectionId);
			
			JsonObject jsonDrugWidgets = jsonDrug.addObject("widgets");
			for (String key : drugOrderWidget.getWidgetReplacements().keySet()) {
				jsonDrugWidgets.addString(key, context.getFieldName(drugOrderWidget.getWidgetReplacements().get(key)));
			}
			
			for (DrugOrder d : getInitialValueForDrug(drug)) {
				JsonObject jho = jsonDrug.addObjectToArray("history");
				jho.addIdAndLabel("orderId", "value", "display", d);
				jho.addIdAndLabel("previousOrderId", "value", "display", d.getPreviousOrder());
				jho.addIdAndLabel("action", "value", "display", d.getAction());
				jho.addIdAndLabel("drug", "value", "display", d.getDrug());
				jho.addIdAndLabel("careSetting", "value", "display", d.getCareSetting());
				jho.addIdAndLabel("dosingType", "value", "display", d.getDosingType());
				jho.addIdAndLabel("orderType", "value", "display", d.getOrderType());
				jho.addIdAndLabel("dosingInstructions", "value", "display", d.getDosingInstructions());
				jho.addIdAndLabel("dose", "value", "display", d.getDose());
				jho.addIdAndLabel("doseUnits", "value", "display", d.getDoseUnits());
				jho.addIdAndLabel("route", "value", "display", d.getRoute());
				jho.addIdAndLabel("frequency", "value", "display", d.getFrequency());
				jho.addIdAndLabel("asNeeded", "value", "display", d.getAsNeeded());
				jho.addIdAndLabel("instructions", "value", "display", d.getInstructions());
				jho.addIdAndLabel("urgency", "value", "display", d.getUrgency());
				jho.addIdAndLabel("dateActivated", "value", "display", d.getDateActivated());
				jho.addIdAndLabel("scheduledDate", "value", "display", d.getScheduledDate());
				jho.addIdAndLabel("duration", "value", "display", d.getDuration());
				jho.addIdAndLabel("durationUnits", "value", "display", d.getDurationUnits());
				jho.addIdAndLabel("quantity", "value", "display", d.getQuantity());
				jho.addIdAndLabel("quantityUnits", "value", "display", d.getQuantityUnits());
				jho.addIdAndLabel("numRefills", "value", "display", d.getNumRefills());
				jho.addIdAndLabel("orderReason", "value", "display", d.getOrderReason());
			}
		}
		
		// Add javascript function to initialize widget as appropriate
		String onLoadFn = widgetConfig.getAttributes().get("onLoadFunction");
		if (StringUtils.isNotBlank(onLoadFn)) {
			writer.println("<script type=\"text/javascript\">");
			writer.println("jQuery(function() { " + onLoadFn + "(");
			writer.println(jsonConfig.toJson());
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
