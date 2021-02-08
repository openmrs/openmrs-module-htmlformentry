package org.openmrs.module.htmlformentry.tester;

import org.openmrs.Drug;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;

public class DrugOrderFieldTester {
	
	private static int nextFieldNum = 0;
	
	private final String suffix;
	
	private final FormSessionTester formSessionTester;
	
	private DrugOrderFieldTester(String suffix, FormSessionTester formSessionTester) {
		this.suffix = suffix;
		this.formSessionTester = formSessionTester;
	}

	public static DrugOrderFieldTester forDrug(Integer conceptId, Integer drugId, String drugNonCoded, FormSessionTester formSessionTester) {
		String nextSuffix = "_" + conceptId + "_" + nextFieldNum++;
		DrugOrderFieldTester tester = new DrugOrderFieldTester(nextSuffix, formSessionTester);
		tester.setField("concept", conceptId.toString());
		tester.setField("drug", drugId == null ? "" : drugId.toString());
		tester.setField("drugNonCoded", drugNonCoded);
		return tester;
	}
	
	public static DrugOrderFieldTester forDrug(Integer drugId, String suffix, FormSessionTester formSessionTester) {
		Drug drug = Context.getConceptService().getDrug(drugId);
		DrugOrderFieldTester tester = new DrugOrderFieldTester(suffix, formSessionTester);
		tester.setField("concept", drug.getConcept().getConceptId().toString());
		tester.setField("drug", drugId.toString());
		return tester;
	}
	
	public static DrugOrderFieldTester forDrug(Integer drugId, FormSessionTester formSessionTester) {
		Drug drug = Context.getConceptService().getDrug(drugId);
		String nextSuffix = "_" + drug.getConcept().getConceptId() + "_" + nextFieldNum++;
		DrugOrderFieldTester tester = new DrugOrderFieldTester(nextSuffix, formSessionTester);
		tester.setField("concept", drug.getConcept().getConceptId().toString());
		tester.setField("drug", drugId.toString());
		return tester;
	}
	
	public DrugOrderFieldTester setField(String property, String value) {
		String formField = formSessionTester.getFormField("order-field-label order-" + property) + suffix;
		formSessionTester.setFormField(formField, value);
		return this;
	}
	
	public String getSuffix() {
		return suffix;
	}
	
	public DrugOrderFieldTester previousOrder(String val) {
		return setField("previousOrder", val);
	}
	
	public DrugOrderFieldTester orderAction(String val) {
		return setField("action", val);
	}
	
	public DrugOrderFieldTester careSetting(String val) {
		return setField("careSetting", val);
	}
	
	public DrugOrderFieldTester orderType(String val) {
		return setField("orderType", val);
	}
	
	public DrugOrderFieldTester freeTextDosing(String val) {
		setField("dosingType", FreeTextDosingInstructions.class.getName());
		return dosingInstructions(val);
	}
	
	public DrugOrderFieldTester simpleDosing(String dose, String doseUnits, String frequency, String route) {
		setField("dosingType", SimpleDosingInstructions.class.getName());
		return dose(dose).doseUnits(doseUnits).frequency(frequency).route(route);
	}
	
	public DrugOrderFieldTester dosingInstructions(String val) {
		return setField("dosingInstructions", val);
	}
	
	public DrugOrderFieldTester dose(String val) {
		return setField("dose", val);
	}
	
	public DrugOrderFieldTester doseUnits(String val) {
		return setField("doseUnits", val);
	}
	
	public DrugOrderFieldTester route(String val) {
		return setField("route", val);
	}
	
	public DrugOrderFieldTester frequency(String val) {
		return setField("frequency", val);
	}
	
	public DrugOrderFieldTester asNeeded(String val) {
		return setField("asNeeded", val);
	}
	
	public DrugOrderFieldTester instructions(String val) {
		return setField("instructions", val);
	}
	
	public DrugOrderFieldTester urgency(String val) {
		return setField("urgency", val);
	}
	
	public DrugOrderFieldTester dateActivated(String val) {
		return setField("dateActivated", val);
	}
	
	public DrugOrderFieldTester scheduledDate(String val) {
		return setField("scheduledDate", val);
	}
	
	public DrugOrderFieldTester duration(String val) {
		return setField("duration", val);
	}
	
	public DrugOrderFieldTester durationUnits(String val) {
		return setField("durationUnits", val);
	}
	
	public DrugOrderFieldTester quantity(String val) {
		return setField("quantity", val);
	}
	
	public DrugOrderFieldTester quantityUnits(String val) {
		return setField("quantityUnits", val);
	}
	
	public DrugOrderFieldTester numRefills(String val) {
		return setField("numRefills", val);
	}
	
	public DrugOrderFieldTester voided(String val) {
		return setField("voided", val);
	}
	
	public DrugOrderFieldTester discontinueReason(String val) {
		return setField("discontinueReason", val);
	}
}
