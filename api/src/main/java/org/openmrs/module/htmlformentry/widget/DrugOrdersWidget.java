package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.util.JsonObject;

public class DrugOrdersWidget implements Widget {
	
	private static final Log log = LogFactory.getLog(DrugOrdersWidget.class);
	
	private DrugOrderWidgetConfig widgetConfig;
	
	private final DrugOrderWidget drugOrderWidget;
	
	private Map<Drug, List<DrugOrder>> initialValue;
	
	public DrugOrdersWidget(FormEntryContext context, DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
		this.drugOrderWidget = new DrugOrderWidget(context, widgetConfig);
	}
	
	@Override
	public void setInitialValue(Object v) {
		initialValue = (Map<Drug, List<DrugOrder>>) v;
	}
	
	public List<DrugOrder> getInitialValueForDrug(Drug drug) {
		return initialValue == null ? new ArrayList<>() : initialValue.getOrDefault(drug, new ArrayList<>());
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrdersWidget.generateHtml");
		
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		startTag(writer, "div", fieldName, "drugorders-element", null);
		writer.println();
		
		// Add a section to the orders section
		writer.println("<div id=\"" + fieldName + "_header\" class=\"drugorders-header-section\"></div>");
		
		// Add a section that will contain the selected drug orders
		writer.println("<div id=\"" + fieldName + "_orders\" class=\"drugorders-order-section\"></div>");
		
		// Add a section that contains the order form template to use for entering orders
		writer.println("<div id=\"" + fieldName + "_template\" class=\"drugorders-order-form\" style=\"display:none;\">");
		writer.println(drugOrderWidget.generateHtml(context));
		writer.println("</div>");
		
		// Establish a json config that initializes the javascript-based widget
		Integer patId = context.getExistingPatient().getPatientId();
		Integer encId = context.getExistingEncounter() == null ? null : context.getExistingEncounter().getEncounterId();
		
		JsonObject jsonConfig = new JsonObject();
		jsonConfig.addString("fieldName", fieldName);
		jsonConfig.addString("today", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		jsonConfig.addString("patientId", patId.toString());
		jsonConfig.addString("encounterId", encId == null ? "" : encId.toString());
		jsonConfig.addString("mode", context.getMode().name());
		if (widgetConfig.getAttributes() != null) {
			for (String att : widgetConfig.getAttributes().keySet()) {
				jsonConfig.addString(att, widgetConfig.getAttributes().get(att));
			}
		}
		
		// In order to re-configure date widgets, add some additional configuration
		DateWidget w = (DateWidget) drugOrderWidget.getDateActivatedWidget();
		JsonObject dateConfig = jsonConfig.addObject("dateWidgetConfig");
		dateConfig.addString("dateFormat", w.jsDateFormat());
		dateConfig.addString("yearsRange", w.getYearsRange());
		dateConfig.addString("locale", w.getLocaleForJquery());
		
		JsonObject jsonDrugWidgets = jsonConfig.addObject("widgets");
		for (String key : drugOrderWidget.getWidgetReplacements().keySet()) {
			jsonDrugWidgets.addString(key, context.getFieldName(drugOrderWidget.getWidgetReplacements().get(key)));
		}
		
		// Add any translations needed by the default views
		String prefix = "htmlformentry.drugOrder.";
		JsonObject translations = jsonConfig.addObject("translations");
		translations.addTranslation(prefix, "asNeeded");
		translations.addTranslation(prefix, "previousOrder");
		translations.addTranslation(prefix, "present");
		translations.addTranslation(prefix, "noOrders");
		translations.addTranslation(prefix, "chooseDrug");
		
		DrugOrderField field = widgetConfig.getDrugOrderField();
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		for (DrugOrderAnswer doa : field.getDrugOrderAnswers()) {
			Drug drug = doa.getDrug();
			String drugLabel = doa.getDisplayName();
			
			// For each rendered drugOrderWidget, add configuration of that widget into json for javascript
			JsonObject jsonDrug = jsonConfig.addObjectToArray("drugs");
			jsonDrug.addString("drugId", drug.getId().toString());
			jsonDrug.addString("drugLabel", drugLabel);
			
			for (DrugOrder d : getInitialValueForDrug(drug)) {
				Order pd = d.getPreviousOrder();
				JsonObject jho = jsonDrug.addObjectToArray("history");
				jho.addString("orderId", d.getOrderId().toString());
				jho.addString("encounterId", d.getEncounter().getEncounterId().toString());
				jho.addString("previousOrderId", pd == null ? "" : pd.getOrderId().toString());
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
				jho.addIdAndLabel("effectiveStartDate", "value", "display", d.getEffectiveStartDate());
				jho.addIdAndLabel("duration", "value", "display", d.getDuration());
				jho.addIdAndLabel("durationUnits", "value", "display", d.getDurationUnits());
				jho.addIdAndLabel("autoExpireDate", "value", "display", d.getAutoExpireDate());
				jho.addIdAndLabel("dateStopped", "value", "display", d.getDateStopped());
				jho.addIdAndLabel("effectiveStopDate", "value", "display", d.getEffectiveStopDate());
				jho.addIdAndLabel("quantity", "value", "display", d.getQuantity());
				jho.addIdAndLabel("quantityUnits", "value", "display", d.getQuantityUnits());
				jho.addIdAndLabel("numRefills", "value", "display", d.getNumRefills());
				jho.addIdAndLabel("orderReason", "value", "display", d.getOrderReason());
			}
		}
		
		// Add javascript function to initialize widget as appropriate
		String defaultLoadFn = "htmlForm.initializeDrugOrdersWidgets";
		String onLoadFn = widgetConfig.getAttributes().getOrDefault("onLoadFunction", defaultLoadFn);
		writer.println("<script type=\"text/javascript\">");
		writer.println("jQuery(function() { " + onLoadFn + "(");
		writer.println(jsonConfig.toJson());
		writer.println(")});");
		writer.println("</script>");
		
		writer.println("</div>");
		
		sw.stop();
		log.trace("DrugOrdersWidget.generateHtml: " + sw.toString());
		
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
		// TODO: Figure out how to retrieve the drug oders from the request
		return ret;
	}
	
	public DrugOrderField getDrugOrderField() {
		return widgetConfig.getDrugOrderField();
	}
	
	public DrugOrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}
	
	public void setWidgetConfig(DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
	}
}
