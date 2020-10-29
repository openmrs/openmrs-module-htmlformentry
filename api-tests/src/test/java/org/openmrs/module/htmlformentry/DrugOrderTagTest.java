package org.openmrs.module.htmlformentry;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

public class DrugOrderTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/drugOrderElement.xml");
	}
	
	@Test
	public void testDrugOrderTag_shouldCreateAndDiscontinueDrugOrder() throws Exception {
		final RegressionTestHelper createAndEditEncounterTest = new RegressionTestHelper() {
			
			final Date date = new Date();
			
			private Encounter encounter;
			
			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(8);
			}
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "1");
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(encounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders, containsInAnyOrder(allOf(hasProperty("drug", is(drug)),
				    hasProperty("dose", is(1.0)), hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))))));
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return encounter;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "2");
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
				//discontinue date
				request.setParameter("w15", dateAsString(date));
			}
			
			public void testEditedResults(SubmissionResults results) {
				Encounter editedEncounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(editedEncounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders,
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(2.0)),
				            hasProperty("action", is(Order.Action.REVISE))),
				        allOf(hasProperty("drug", is(drug)), hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))),
				            hasProperty("action", is(Order.Action.DISCONTINUE)))));
			}
		};
		createAndEditEncounterTest.run();
		
		//Test viewing edited drug order
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			public Encounter getEncounterToView() {
				return createAndEditEncounterTest.getEncounterToEdit();
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				MatcherAssert.assertThat(html, containsString("DrugOrder.dose <span class=\"value\">2</span>"));
			}
		}.run();
	}
	
	@Test
	public void testDrugOrderTag_shouldCreateAndEditDrugOrder() throws Exception {
		final RegressionTestHelper createAndEditEncounterTest = new RegressionTestHelper() {
			
			final Date date = new Date();
			
			private Encounter encounter;
			
			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(8);
			}
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "1");
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(encounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders, contains(allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)),
				    hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))))));
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return encounter;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "2");
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
			}
			
			public void testEditedResults(SubmissionResults results) {
				Encounter editedEncounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(editedEncounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders,
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(2.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))),
				            hasProperty("action", is(Order.Action.REVISE))),
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))))));
			}
		};
		createAndEditEncounterTest.run();
		
		//Test viewing edited drug order
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			public Encounter getEncounterToView() {
				return createAndEditEncounterTest.getEncounterToEdit();
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				MatcherAssert.assertThat(html, containsString("DrugOrder.dose <span class=\"value\">2</span>"));
			}
		}.run();
	}
	
	@Test
	public void testDrugOrderTag_shouldEditDiscontinueDrugOrder() throws Exception {
		final RegressionTestHelper createAndEditEncounterTest = new RegressionTestHelper() {
			
			final Date date = DateUtils.addDays(new Date(), -4);
			
			final Date discontinueDate = DateUtils.addDays(new Date(), -2);
			
			final Date newDiscontinueDate = DateUtils.addDays(new Date(), -3);
			
			private Encounter encounter;
			
			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(8);
			}
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "1");
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
				
				//discontinue date
				request.setParameter("w15", dateAsString(discontinueDate));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(encounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders,
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug)), hasProperty("action", is(Order.Action.DISCONTINUE)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(discontinueDate)))))));
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return encounter;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//drug
				request.setParameter("w7", "2");
				//start date
				request.setParameter("w9", dateAsString(date));
				//dosing type
				request.setParameter("w17", "org.openmrs.SimpleDosingInstructions");
				//dose
				request.setParameter("w11", "2"); //changes to dose should not be persisted
				request.setParameter("w18", "51");
				//care setting
				request.setParameter("w26", "2");
				//route
				request.setParameter("w25", "22");
				//frequency
				request.setParameter("w13", "1");
				
				//discontinue date
				request.setParameter("w15", dateAsString(newDiscontinueDate));
			}
			
			public void testEditedResults(SubmissionResults results) {
				Encounter editedEncounter = results.getEncounterCreated();
				
				List<Order> orders = getOrderList(editedEncounter);
				
				Drug drug = Context.getConceptService().getDrug(2);
				MatcherAssert.assertThat(orders,
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug)), hasProperty("voided", is(true)),
				            hasProperty("action", is(Order.Action.DISCONTINUE)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(discontinueDate))))),
				        allOf(hasProperty("drug", is(drug)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(newDiscontinueDate)))),
				            hasProperty("action", is(Order.Action.DISCONTINUE)))));
			}
		};
		createAndEditEncounterTest.run();
		
		//Test viewing edited drug order
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			public Encounter getEncounterToView() {
				return createAndEditEncounterTest.getEncounterToEdit();
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				MatcherAssert.assertThat(html, containsString("DrugOrder.dose <span class=\"value\">1</span>"));
			}
		}.run();
	}
	
	private List<Order> getOrderList(Encounter encounter) {
		return new ArrayList<>(encounter.getOrders());
	}
}
