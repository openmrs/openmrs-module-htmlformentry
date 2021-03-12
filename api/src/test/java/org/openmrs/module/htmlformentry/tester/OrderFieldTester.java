package org.openmrs.module.htmlformentry.tester;

import org.openmrs.Drug;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;

public class OrderFieldTester {
	
	private static int nextFieldNum = 0;
	
	private final String suffix;
	
	private final FormSessionTester formSessionTester;
	
	private OrderFieldTester(String suffix, FormSessionTester formSessionTester) {
		this.suffix = suffix;
		this.formSessionTester = formSessionTester;
	}
	
	public static OrderFieldTester forConcept(Integer conceptId, FormSessionTester formSessionTester) {
		String nextSuffix = "_" + conceptId + "_" + nextFieldNum++;
		OrderFieldTester tester = new OrderFieldTester(nextSuffix, formSessionTester);
		tester.setField("concept", conceptId.toString());
		return tester;
	}
	
	public static OrderFieldTester forDrug(Integer conceptId, Integer drugId, String drugNonCoded, FormSessionTester t) {
		OrderFieldTester tester = forConcept(conceptId, t);
		tester.setField("drug", drugId == null ? "" : drugId.toString());
		tester.setField("drugNonCoded", drugNonCoded);
		return tester;
	}
	
	public static OrderFieldTester forDrug(Integer drugId, String suffix, FormSessionTester formSessionTester) {
		Drug drug = Context.getConceptService().getDrug(drugId);
		OrderFieldTester tester = new OrderFieldTester(suffix, formSessionTester);
		tester.setField("concept", drug.getConcept().getConceptId().toString());
		tester.setField("drug", drugId.toString());
		return tester;
	}
	
	public static OrderFieldTester forDrug(Integer drugId, FormSessionTester formSessionTester) {
		Drug drug = Context.getConceptService().getDrug(drugId);
		return forDrug(drug.getConcept().getConceptId(), drug.getDrugId(), "", formSessionTester);
	}
	
	public OrderFieldTester setField(String property, String value) {
		String formField = formSessionTester.getFormField("order-field-label order-" + property) + suffix;
		formSessionTester.setFormField(formField, value);
		return this;
	}
	
	public String getSuffix() {
		return suffix;
	}
	
	public OrderFieldTester previousOrder(String val) {
		return setField("previousOrder", val);
	}
	
	public OrderFieldTester orderAction(String val) {
		return setField("action", val);
	}
	
	public OrderFieldTester careSetting(String val) {
		return setField("careSetting", val);
	}
	
	public OrderFieldTester orderType(String val) {
		return setField("orderType", val);
	}
	
	public OrderFieldTester freeTextDosing(String val) {
		setField("dosingType", FreeTextDosingInstructions.class.getName());
		return dosingInstructions(val);
	}
	
	public OrderFieldTester simpleDosing(String dose, String doseUnits, String frequency, String route) {
		setField("dosingType", SimpleDosingInstructions.class.getName());
		return dose(dose).doseUnits(doseUnits).frequency(frequency).route(route);
	}
	
	public OrderFieldTester dosingInstructions(String val) {
		return setField("dosingInstructions", val);
	}
	
	public OrderFieldTester dose(String val) {
		return setField("dose", val);
	}
	
	public OrderFieldTester doseUnits(String val) {
		return setField("doseUnits", val);
	}
	
	public OrderFieldTester route(String val) {
		return setField("route", val);
	}
	
	public OrderFieldTester frequency(String val) {
		return setField("frequency", val);
	}
	
	public OrderFieldTester asNeeded(String val) {
		return setField("asNeeded", val);
	}
	
	public OrderFieldTester instructions(String val) {
		return setField("instructions", val);
	}
	
	public OrderFieldTester urgency(String val) {
		return setField("urgency", val);
	}
	
	public OrderFieldTester dateActivated(String val) {
		return setField("dateActivated", val);
	}
	
	public OrderFieldTester scheduledDate(String val) {
		return setField("scheduledDate", val);
	}
	
	public OrderFieldTester duration(String val) {
		return setField("duration", val);
	}
	
	public OrderFieldTester durationUnits(String val) {
		return setField("durationUnits", val);
	}
	
	public OrderFieldTester quantity(String val) {
		return setField("quantity", val);
	}
	
	public OrderFieldTester quantityUnits(String val) {
		return setField("quantityUnits", val);
	}
	
	public OrderFieldTester numRefills(String val) {
		return setField("numRefills", val);
	}
	
	public OrderFieldTester voided(String val) {
		return setField("voided", val);
	}
	
	public OrderFieldTester discontinueReason(String val) {
		return setField("discontinueReason", val);
	}
}
