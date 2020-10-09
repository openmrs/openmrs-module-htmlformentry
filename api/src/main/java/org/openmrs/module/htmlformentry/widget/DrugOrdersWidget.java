package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.DrugOrder;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

public class DrugOrdersWidget implements Widget {
	
	private Map<String, String> parameters;
	
	private DrugOrderField field;
	
	private List<DrugOrder> initialValue;
	
	public DrugOrdersWidget(Map<String, String> parameters, DrugOrderField field) {
		this.parameters = parameters;
		this.field = field;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (List<DrugOrder>) initialValue;
	}
	
	public Map<String, Object> buildJsonConfiguration(FormEntryContext context) {
		Map ret = new LinkedHashMap();
		
		Map<String, Object> contextMap = new LinkedHashMap<>();
		contextMap.put("mode", context.getMode().name());
		contextMap.put("fieldName", context.getFieldName(this));
		ret.put("context", contextMap);
		
		Map<String, Object> configMap = new LinkedHashMap<>();
		for (String name : parameters.keySet()) {
			String value = parameters.get(name);
			Object convertedValue = value;
			if (value != null) {
				String[] split = value.split(",");
				if (split.length > 1) {
					List<String> l = new ArrayList<>();
					for (String listVal : split) {
						l.add(listVal);
					}
					convertedValue = l;
				}
			}
			configMap.put(name, convertedValue);
		}
		ret.put("config", configMap);
		
		List<Map<String, Object>> valMap = toJsonObject(initialValue);
		ret.put("initialValue", valMap);
		
		return ret;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a span with id = "fieldName_section"
		sb.append("<span id=\"").append(fieldName).append("_section\">");
		
		// TODO: Consider rendering all drugs in the system, possibly in an autocomplete
		if (field.getDrugOrderAnswers().size() == 0) {
			throw new IllegalStateException("No drugs configured on drugOrder tag");
		} else {
			// Drug selector has id = "fieldName_drug"
			String selectId = fieldName + "_drug";
			sb.append("<select id=\"").append(selectId).append("\" name=\"").append(selectId).append("\">");
			
			// If there are > 1 drug configured, then show an empty element
			if (field.getDrugOrderAnswers().size() > 1) {
				sb.append("<option value=\"\">").append(translate("DrugOrder.drug") + "...").append("</option>");
			}
			for (DrugOrderAnswer a : field.getDrugOrderAnswers()) {
				Integer id = a.getDrug().getId();
				sb.append("<option value=\"").append(id).append("\"").append(">");
				sb.append(a.getDisplayName());
				sb.append("</option>");
			}
			sb.append("</select>");
		}
		
		// Add a hidden input for the field
		sb.append("<input type=\"hidden\" id=\"").append(fieldName).append("\" name=\"").append(fieldName).append("\"/>");
		
		// Wrap the area to display the order details in a span with id="fieldName_details"
		sb.append("<span id=\"").append(fieldName).append("_details\"></span>");
		
		sb.append("</span>");
		sb.append("<script type=\"text/javascript\">");
		sb.append("jQuery(function() { htmlForm.initializeDrugOrderWidget(");
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonConfig = mapper.writeValueAsString(buildJsonConfiguration(context));
			sb.append(jsonConfig);
		}
		catch (Exception e) {
			throw new IllegalStateException("Error building json configuration for drug order widget", e);
		}
		sb.append(")});");
		sb.append("</script>");
		
		return sb.toString();
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String value = request.getParameter(context.getFieldName(this));
		return fromJsonObject(value);
	}
	
	public static List<Map<String, Object>> toJsonObject(List<DrugOrder> drugOrders) {
		List<Map<String, Object>> l = new ArrayList<>();
		for (DrugOrder drugOrder : drugOrders) {
			Map<String, Object> m = new LinkedHashMap<>();
			addJsonProperty(m, "orderId", drugOrder.getId());
			addJsonProperty(m, "uuid", drugOrder.getUuid());
			addJsonProperty(m, "patient", drugOrder.getPatient());
			addJsonProperty(m, "encounter", drugOrder.getEncounter());
			addJsonProperty(m, "orderReason", drugOrder.getOrderReason());
			addJsonProperty(m, "action", drugOrder.getAction().name());
			addJsonProperty(m, "previousOrder", drugOrder.getPreviousOrder());
			addJsonProperty(m, "drug", drugOrder.getDrug());
			addJsonProperty(m, "dosingType", drugOrder.getDosingType().getSimpleName());
			addJsonProperty(m, "careSetting", drugOrder.getCareSetting());
			addJsonProperty(m, "orderType", drugOrder.getOrderType());
			addJsonProperty(m, "orderer", drugOrder.getOrderer());
			addJsonProperty(m, "dosingInstructions", drugOrder.getDosingInstructions());
			addJsonProperty(m, "dose", drugOrder.getDose());
			addJsonProperty(m, "doseUnits", drugOrder.getDoseUnits());
			addJsonProperty(m, "route", drugOrder.getRoute());
			addJsonProperty(m, "frequency", drugOrder.getFrequency());
			addJsonProperty(m, "asNeeded", drugOrder.getAsNeeded());
			addJsonProperty(m, "asNeededCondition", drugOrder.getAsNeededCondition());
			addJsonProperty(m, "dateActivated", drugOrder.getDateActivated());
			addJsonProperty(m, "urgency", drugOrder.getUrgency().name());
			addJsonProperty(m, "scheduledDate", drugOrder.getScheduledDate());
			addJsonProperty(m, "effectiveStartDate", drugOrder.getEffectiveStartDate());
			addJsonProperty(m, "autoExpireDate", drugOrder.getAutoExpireDate());
			addJsonProperty(m, "effectiveStopDate", drugOrder.getEffectiveStopDate());
			addJsonProperty(m, "dateStopped", drugOrder.getDateStopped());
			addJsonProperty(m, "duration", drugOrder.getDuration());
			addJsonProperty(m, "durationUnits", drugOrder.getDurationUnits());
			addJsonProperty(m, "quantity", drugOrder.getQuantity());
			addJsonProperty(m, "quantityUnits", drugOrder.getQuantityUnits());
			addJsonProperty(m, "instructions", drugOrder.getInstructions());
			addJsonProperty(m, "numRefills", drugOrder.getNumRefills());
			l.add(m);
		}
		return l;
	}
	
	protected static void addJsonProperty(Map<String, Object> m, String property, Object value) {
		if (value != null) {
			if (value instanceof OpenmrsObject) {
				value = ((OpenmrsObject) value).getId();
			} else if (value instanceof Date) {
				value = (new SimpleDateFormat("yyyy-MM-dd").format((Date) value));
			}
		}
		m.put(property, value);
	}
	
	protected static DrugOrder fromJsonObject(String json) {
		DrugOrder drugOrder = null;
		// TODO: Implement this
		
		return drugOrder;
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
}
