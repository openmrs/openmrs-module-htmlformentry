package org.openmrs.module.htmlformentry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The goal of this class is to extend RegressionTestHelper in a manner that is geared towards
 * testing the DrugOrder tag and to make it easy to test multiple variations of this tag without
 * having to maintain lots of duplicate code and test htmlform resources.
 */
public abstract class DrugOrderRegressionTestHelper extends RegressionTestHelper {
	
	private static Log log = LogFactory.getLog(DrugOrderRegressionTestHelper.class);
	
	@Override
	public Patient getPatient() {
		return Context.getPatientService().getPatient(8);
	}
	
	public Date getEncounterDate() {
		return HtmlFormEntryUtil.startOfDay(new Date());
	}
	
	protected Encounter encounter;
	
	protected DrugOrder initialOrder;
	
	/*
	 * The values to submit in entry
	 */
	public List<DrugOrderRequestParams> getDrugOrderEntryRequestParams() {
		return new ArrayList<>();
	}
	
	/*
	 * The values to submit in edit
	 */
	public List<DrugOrderRequestParams> getDrugOrderEditRequestParams() {
		return new ArrayList<>();
	}
	
	@Override
	public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
		request.setParameter(widgets.get("Date:"), dateAsString(getEncounterDate()));
		request.setParameter(widgets.get("Location:"), "2");
		request.setParameter(widgets.get("Provider:"), "502");
		for (DrugOrderRequestParams params : getDrugOrderEntryRequestParams()) {
			params.applyToRequest(request, widgets);
		}
	}
	
	@Override
	public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
		log.trace("Applying to request: " + widgets);
		request.setParameter(widgets.get("Date:"), dateAsString(getEncounterDate()));
		request.setParameter(widgets.get("Location:"), "2");
		request.setParameter(widgets.get("Provider:"), "502");
		for (DrugOrderRequestParams params : getDrugOrderEditRequestParams()) {
			params.applyToRequest(request, widgets);
		}
	}
	
	@Override
	public boolean doEditEncounter() {
		return !getDrugOrderEditRequestParams().isEmpty();
	}
	
	@Override
	public Encounter getEncounterToEdit() {
		return doEditEncounter() ? encounter : null;
	}
	
	@Override
	public String getFormName() {
		return "drugOrderTestForm";
	}
	
	@Override
	public String[] widgetLabels() {
		List<String> l = new ArrayList<>();
		l.add("Date:");
		l.add("Location:");
		l.add("Provider:");
		for (int i = 0; i < 3; i++) {
			l.add(">Drug<##" + i);
			l.add(">Order Action<##" + i);
			l.add(">Previous Order<##" + i);
			l.add(">Care Setting<##" + i);
			l.add(">Dosing Type<##" + i);
			l.add(">Order Type<##" + i);
			l.add(">Dosing Instructions<##" + i);
			l.add(">Dose<##" + i);
			l.add(">Dose Units<##" + i);
			l.add(">Route<##" + i);
			l.add(">Frequency<##" + i);
			l.add(">As Needed<##" + i);
			l.add(">Instructions<##" + i);
			l.add(">Urgency<##" + i);
			l.add(">Order Date<##" + i);
			l.add(">Scheduled Date<##" + i);
			l.add(">Duration<##" + i);
			l.add(">Duration Units<##" + i);
			l.add(">Quantity<##" + i);
			l.add(">Quantity Units<##" + i);
			l.add(">Number of Refills<##" + i);
			l.add(">Voided<##" + i);
			l.add(">Discontinue Reason<##" + i);
		}
		return l.toArray(new String[] {});
	}
	
	@Override
	public String[] widgetLabelsForEdit() {
		return doEditEncounter() ? widgetLabels() : super.widgetLabelsForEdit();
	}
	
	@Override
	public void testBlankFormHtml(String html) {
		for (String widgetLabel : widgetLabels()) {
			String[] split = widgetLabel.split("##");
			assertThat(html, containsString(split[0]));
		}
	}
	
	public static class DrugOrderRequestParams {
		
		private int fieldNum;
		
		private String drug;
		
		private String previousOrder;
		
		private String action;
		
		private String careSetting;
		
		private String dosingType;
		
		private String orderType;
		
		private String dosingInstructions;
		
		private String dose;
		
		private String doseUnits;
		
		private String route;
		
		private String frequency;
		
		private String asNeeded;
		
		private String instructions;
		
		private String urgency;
		
		private String dateActivated;
		
		private String scheduledDate;
		
		private String duration;
		
		private String durationUnits;
		
		private String quantity;
		
		private String quantityUnits;
		
		private String numRefills;
		
		private String voided;
		
		private String discontinueReason;
		
		public DrugOrderRequestParams(int fieldNum) {
			this.fieldNum = fieldNum;
		}
		
		public void applyToRequest(MockHttpServletRequest request, Map<String, String> widgets) {
			applyIfNotNull(request, widgets, ">Drug<", fieldNum, drug);
			applyIfNotNull(request, widgets, ">Previous Order<", fieldNum, previousOrder);
			applyIfNotNull(request, widgets, ">Order Action<", fieldNum, action);
			applyIfNotNull(request, widgets, ">Care Setting<", fieldNum, careSetting);
			applyIfNotNull(request, widgets, ">Dosing Type<", fieldNum, dosingType);
			applyIfNotNull(request, widgets, ">Order Type<", fieldNum, orderType);
			applyIfNotNull(request, widgets, ">Dosing Instructions<", fieldNum, dosingInstructions);
			applyIfNotNull(request, widgets, ">Dose<", fieldNum, dose);
			applyIfNotNull(request, widgets, ">Dose Units<", fieldNum, doseUnits);
			applyIfNotNull(request, widgets, ">Route<", fieldNum, route);
			applyIfNotNull(request, widgets, ">Frequency<", fieldNum, frequency);
			applyIfNotNull(request, widgets, ">As Needed<", fieldNum, asNeeded);
			applyIfNotNull(request, widgets, ">Instructions<", fieldNum, instructions);
			applyIfNotNull(request, widgets, ">Urgency<", fieldNum, urgency);
			applyIfNotNull(request, widgets, ">Order Date<", fieldNum, dateActivated);
			applyIfNotNull(request, widgets, ">Scheduled Date<", fieldNum, scheduledDate);
			applyIfNotNull(request, widgets, ">Duration<", fieldNum, duration);
			applyIfNotNull(request, widgets, ">Duration Units<", fieldNum, durationUnits);
			applyIfNotNull(request, widgets, ">Quantity<", fieldNum, quantity);
			applyIfNotNull(request, widgets, ">Quantity Units<", fieldNum, quantityUnits);
			applyIfNotNull(request, widgets, ">Number of Refills<", fieldNum, numRefills);
			applyIfNotNull(request, widgets, ">Voided<", fieldNum, voided);
			applyIfNotNull(request, widgets, ">Discontinue Reason<", fieldNum, discontinueReason);
		}
		
		protected void applyIfNotNull(MockHttpServletRequest request, Map<String, String> widgets, String label,
		        int fieldNum, String value) {
			if (value != null) {
				request.setParameter(widgets.get(label + "##" + fieldNum) + "_" + drug, value);
			}
		}
		
		public int getFieldNum() {
			return fieldNum;
		}
		
		public void setFieldNum(int fieldNum) {
			this.fieldNum = fieldNum;
		}
		
		public String getDrug() {
			return drug;
		}
		
		public void setDrug(String drug) {
			this.drug = drug;
		}
		
		public String getPreviousOrder() {
			return previousOrder;
		}
		
		public void setPreviousOrder(String previousOrder) {
			this.previousOrder = previousOrder;
		}
		
		public String getAction() {
			return action;
		}
		
		public void setAction(String action) {
			this.action = action;
		}
		
		public String getCareSetting() {
			return careSetting;
		}
		
		public void setCareSetting(String careSetting) {
			this.careSetting = careSetting;
		}
		
		public String getDosingType() {
			return dosingType;
		}
		
		public void setDosingType(String dosingType) {
			this.dosingType = dosingType;
		}
		
		public String getOrderType() {
			return orderType;
		}
		
		public void setOrderType(String orderType) {
			this.orderType = orderType;
		}
		
		public String getDosingInstructions() {
			return dosingInstructions;
		}
		
		public void setDosingInstructions(String dosingInstructions) {
			this.dosingInstructions = dosingInstructions;
		}
		
		public String getDose() {
			return dose;
		}
		
		public void setDose(String dose) {
			this.dose = dose;
		}
		
		public String getDoseUnits() {
			return doseUnits;
		}
		
		public void setDoseUnits(String doseUnits) {
			this.doseUnits = doseUnits;
		}
		
		public String getRoute() {
			return route;
		}
		
		public void setRoute(String route) {
			this.route = route;
		}
		
		public String getFrequency() {
			return frequency;
		}
		
		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}
		
		public String getAsNeeded() {
			return asNeeded;
		}
		
		public void setAsNeeded(String asNeeded) {
			this.asNeeded = asNeeded;
		}
		
		public String getInstructions() {
			return instructions;
		}
		
		public void setInstructions(String instructions) {
			this.instructions = instructions;
		}
		
		public String getUrgency() {
			return urgency;
		}
		
		public void setUrgency(String urgency) {
			this.urgency = urgency;
		}
		
		public String getDateActivated() {
			return dateActivated;
		}
		
		public void setDateActivated(String dateActivated) {
			this.dateActivated = dateActivated;
		}
		
		public String getScheduledDate() {
			return scheduledDate;
		}
		
		public void setScheduledDate(String scheduledDate) {
			this.scheduledDate = scheduledDate;
		}
		
		public String getDuration() {
			return duration;
		}
		
		public void setDuration(String duration) {
			this.duration = duration;
		}
		
		public String getDurationUnits() {
			return durationUnits;
		}
		
		public void setDurationUnits(String durationUnits) {
			this.durationUnits = durationUnits;
		}
		
		public String getQuantity() {
			return quantity;
		}
		
		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}
		
		public String getQuantityUnits() {
			return quantityUnits;
		}
		
		public void setQuantityUnits(String quantityUnits) {
			this.quantityUnits = quantityUnits;
		}
		
		public String getNumRefills() {
			return numRefills;
		}
		
		public void setNumRefills(String numRefills) {
			this.numRefills = numRefills;
		}
		
		public String getVoided() {
			return voided;
		}
		
		public void setVoided(String voided) {
			this.voided = voided;
		}
		
		public String getDiscontinueReason() {
			return discontinueReason;
		}
		
		public void setDiscontinueReason(String discontinueReason) {
			this.discontinueReason = discontinueReason;
		}
	}
	
	protected Map<String, String> toMap(String... entries) {
		Map<String, String> ret = new LinkedHashMap<>();
		for (int i = 0; i < entries.length; i += 2) {
			ret.put(entries[i], entries[i + 1]);
		}
		return ret;
	}
	
	protected Date daysAfterEncounterDate(int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(getEncounterDate());
		calendar.add(Calendar.DATE, days);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
	
	protected Date adjustMillis(Date d, int millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(d);
		calendar.add(Calendar.MILLISECOND, millis);
		return calendar.getTime();
	}
}
