package org.openmrs.module.htmlformentry.web.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DrugOrderSearchController {
	
	@RequestMapping("/module/htmlformentry/activeDrugOrders")
	@ResponseBody
	public Object getDrugOrders(@RequestParam(value = "patient") Patient patient,
	        @RequestParam(value = "asOfDate", required = false) String asOfDateParam) throws Exception {
		
		List<Map<String, Map<String, String>>> drugOrders = new ArrayList<>();
		Date asOfDate = null;
		if (StringUtils.isNotBlank(asOfDateParam)) {
			asOfDate = new SimpleDateFormat("yyyy-MM-dd").parse(asOfDateParam);
		}
		List<Order> orders = Context.getOrderService().getActiveOrders(patient, null, null, asOfDate);
		for (Order order : orders) {
			order = HibernateUtil.getRealObjectFromProxy(order);
			if (order instanceof DrugOrder) {
				DrugOrder d = (DrugOrder) order;
				Map<String, Map<String, String>> m = new LinkedHashMap<>();
				addProperties(m, "drug", d.getDrug());
				addProperties(m, "action", d.getAction());
				addProperties(m, "careSetting", d.getCareSetting());
				addProperties(m, "dosingType", d.getDosingType());
				addProperties(m, "orderType", d.getOrderType());
				addProperties(m, "dosingInstructions", d.getDosingInstructions());
				addProperties(m, "dose", d.getDose());
				addProperties(m, "doseUnits", d.getDoseUnits());
				addProperties(m, "route", d.getRoute());
				addProperties(m, "frequency", d.getFrequency());
				addProperties(m, "asNeeded", d.getAsNeeded());
				addProperties(m, "instructions", d.getInstructions());
				addProperties(m, "urgency", d.getUrgency());
				addProperties(m, "dateActivated", d.getDateActivated());
				addProperties(m, "scheduledDate", d.getScheduledDate());
				addProperties(m, "duration", d.getDuration());
				addProperties(m, "durationUnits", d.getDurationUnits());
				addProperties(m, "quantity", d.getQuantity());
				addProperties(m, "quantityUnits", d.getQuantityUnits());
				addProperties(m, "numRefills", d.getNumRefills());
				addProperties(m, "orderReason", d.getOrderReason());
				drugOrders.add(m);
			}
		}
		return drugOrders;
	}
	
	protected void addProperties(Map<String, Map<String, String>> m, String property, Object val) throws Exception {
		Map<String, String> valueMap = new LinkedHashMap<>();
		String value = "";
		String display = "";
		if (val != null) {
			if (val instanceof OpenmrsMetadata) {
				OpenmrsMetadata metadata = (OpenmrsMetadata) val;
				value = metadata.getId().toString();
				display = metadata.getName();
			} else if (val instanceof Concept) {
				Concept conceptVal = (Concept) val;
				value = conceptVal.getId().toString();
				display = conceptVal.getDisplayString();
			} else if (val instanceof Date) {
				Date dateVal = (Date) val;
				value = new SimpleDateFormat("yyyy-MM-dd").format(dateVal);
				display = Context.getDateFormat().format(dateVal);
			} else if (val instanceof Class) {
				Class classValue = (Class) val;
				value = classValue.getName();
				display = classValue.getSimpleName();
			} else if (val instanceof Enum) {
				Enum enumVal = (Enum) val;
				value = enumVal.name();
				display = enumVal.name();
			} else {
				value = val.toString();
				display = val.toString();
			}
		}
		valueMap.put("value", value);
		valueMap.put("display", display);
		m.put(property, valueMap);
	}
}
