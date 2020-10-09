package org.openmrs.module.htmlformentry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The goal of this class is to extend RegressionTestHelper in a manner that is geared towards
 * testing the DrugOrder tag and to make it easy to test multiple variations of this tag without
 * having to maintain lots of duplicate code and test htmlform resources.
 */
public abstract class DrugOrdersRegressionTestHelper extends RegressionTestHelper {
	
	@Override
	public Patient getPatient() {
		return Context.getPatientService().getPatient(8);
	}
	
	public Date getEncounterDate() {
		return HtmlFormEntryUtil.startOfDay(new Date());
	}
	
	protected Encounter encounter;
	
	/*
	 * The tag(s) to test on this particular test case
	 */
	public abstract List<Map<String, String>> getDrugOrderTags();
	
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
		int i = 0;
		for (DrugOrderRequestParams params : getDrugOrderEntryRequestParams()) {
			params.applyToRequest(request, widgets, i++);
		}
	}
	
	@Override
	public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
		request.setParameter(widgets.get("Date:"), dateAsString(getEncounterDate()));
		request.setParameter(widgets.get("Location:"), "2");
		request.setParameter(widgets.get("Provider:"), "502");
		int i = 0;
		for (DrugOrderRequestParams params : getDrugOrderEditRequestParams()) {
			params.applyToRequest(request, widgets, i++);
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
		return null;
	}
	
	@Override
	public String getFormXml() {
		StringBuilder sb = new StringBuilder();
		String newLine = System.getProperty("line.separator");
		sb.append("<htmlform>").append(newLine);
		sb.append("     Date: <encounterDate/>").append(newLine);
		sb.append("     Location: <encounterLocation/>").append(newLine);
		sb.append("     Provider: <encounterProvider role=\"Provider\"/>").append(newLine);
		sb.append("     <section headerLabel=\"Drug Section\">").append(newLine);
		for (Map<String, String> tag : getDrugOrderTags()) {
			sb.append("         <drugOrder").append(newLine);
			for (String attr : tag.keySet()) {
				sb.append(" ").append(attr).append("=\"").append(tag.get(attr)).append("\"");
			}
			sb.append(" />").append(newLine);
		}
		sb.append("     </section>").append(newLine);
		sb.append("     <submit/>").append(newLine);
		sb.append("</htmlform>").append(newLine);
		return sb.toString();
	}
	
	@Override
	public String[] widgetLabels() {
		List<String> l = new ArrayList<>();
		l.add("Date:");
		l.add("Location:");
		l.add("Provider:");
		for (int i = 0; i < getDrugOrderTags().size(); i++) {
			l.add("action field!!" + i);
			l.add("drug field!!" + i);
			l.add("dose field!!" + i);
			l.add("doseUnits field!!" + i);
			l.add("route field!!" + i);
			l.add("frequency field!!" + i);
			l.add("dosingInstructions field!!" + i);
			l.add("asNeeded field!!" + i);
			l.add("startDate field!!" + i);
			l.add("duration field!!" + i);
			l.add("durationUnits field!!" + i);
			l.add("quantity field!!" + i);
			l.add("quantityUnits field!!" + i);
			l.add("instructions field!!" + i);
			l.add("numRefills field!!" + i);
			l.add("discontinuedReason field!!" + i);
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
			String[] split = widgetLabel.split("!!");
			assertThat(html, containsString(split[0]));
		}
	}
	
	public static class DrugOrderRequestParams {
		
		private String action;
		
		private String dosingInstructions;
		
		private String dose;
		
		private String doseUnits;
		
		private String route;
		
		private String frequency;
		
		private String asNeeded;
		
		private String startDate;
		
		private String duration;
		
		private String durationUnits;
		
		private String quantity;
		
		private String quantityUnits;
		
		private String instructions;
		
		private String numRefills;
		
		private String discontinuedReason;
		
		public DrugOrderRequestParams() {
		}
		
		public void applyToRequest(MockHttpServletRequest request, Map<String, String> widgets, int fieldNum) {
			request.setParameter(widgets.get("action field!!" + fieldNum), action);
			request.setParameter(widgets.get("dose field!!" + fieldNum), dose);
			request.setParameter(widgets.get("doseUnits field!!" + fieldNum), doseUnits);
			request.setParameter(widgets.get("route field!!" + fieldNum), route);
			request.setParameter(widgets.get("frequency field!!" + fieldNum), frequency);
			request.setParameter(widgets.get("asNeeded field!!" + fieldNum), asNeeded);
			request.setParameter(widgets.get("dosingInstructions field!!" + fieldNum), dosingInstructions);
			request.setParameter(widgets.get("startDate field!!" + fieldNum), startDate);
			request.setParameter(widgets.get("duration field!!" + fieldNum), duration);
			request.setParameter(widgets.get("durationUnits field!!" + fieldNum), durationUnits);
			request.setParameter(widgets.get("quantity field!!" + fieldNum), quantity);
			request.setParameter(widgets.get("quantityUnits field!!" + fieldNum), quantityUnits);
			request.setParameter(widgets.get("instructions field!!" + fieldNum), instructions);
			request.setParameter(widgets.get("numRefills field!!" + fieldNum), numRefills);
			request.setParameter(widgets.get("discontinuedReason field!!" + fieldNum), discontinuedReason);
		}
		
		public String getAction() {
			return action;
		}
		
		public void setAction(String action) {
			this.action = action;
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
		
		public String getStartDate() {
			return startDate;
		}
		
		public void setStartDate(String startDate) {
			this.startDate = startDate;
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
		
		public String getInstructions() {
			return instructions;
		}
		
		public void setInstructions(String instructions) {
			this.instructions = instructions;
		}
		
		public String getNumRefills() {
			return numRefills;
		}
		
		public void setNumRefills(String numRefills) {
			this.numRefills = numRefills;
		}
		
		public String getDiscontinuedReason() {
			return discontinuedReason;
		}
		
		public void setDiscontinuedReason(String discontinuedReason) {
			this.discontinuedReason = discontinuedReason;
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
}
