package org.openmrs.htmlformentry.element;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseModuleContextSensitiveTest {

	// field names
	private String searchWidgetIdForCurrentCondition = "w7";

	private String additionalDetailsForCurrentCondition = "w9";

	private String statusWidgetIdForCurrentCondition = "w10";

	private String searchWidgetIdForPastCondition = "w12";

	private String statusWidgetIdForPastCondition = "w14";

	private String searchWidgetIdForPresetCondition = "w16";

	private String statusWidgetIdForPresetCondition = "w18";

	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.30.xml");
	}

	@Test
	public void shouldRecordAndEditCondition() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "conditionForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");

				// setup for current condition
				request.addParameter(searchWidgetIdForCurrentCondition, "Epilepsy");
				request.addParameter(searchWidgetIdForCurrentCondition + "_hid", "3476");
				request.addParameter(statusWidgetIdForCurrentCondition, "active");
				request.addParameter(additionalDetailsForCurrentCondition, "Additional details");

				// setup for past condition
				request.addParameter(searchWidgetIdForPastCondition, "Some past condition");
				request.addParameter(statusWidgetIdForPastCondition, "inactive");

				// setup for preset condition
				request.addParameter(searchWidgetIdForPresetCondition, "Some preset condition");
				request.addParameter(statusWidgetIdForPresetCondition, "inactive");

			}

			@Override
			public void testResults(SubmissionResults results) {
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);
				Concept expectedCondition = Context.getConceptService().getConceptByName("Epilepsy");

				results.assertNoErrors();
				Assert.assertEquals(3, conditions.length);

				Condition currentCondition = conditions[0];
				Assert.assertEquals(ConditionClinicalStatus.ACTIVE, currentCondition.getClinicalStatus());
				Assert.assertEquals(expectedCondition, currentCondition.getCondition().getCoded());
				Assert.assertEquals("Additional details", currentCondition.getAdditionalDetail());
				Assert.assertNotNull(currentCondition.getId());

				Condition pastCondition = conditions[1];
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, pastCondition.getClinicalStatus());
				Assert.assertEquals("Some past condition", pastCondition.getCondition().getNonCoded());
				Assert.assertNotNull(pastCondition.getId());

				Condition presetCondition = conditions[2];
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, presetCondition.getClinicalStatus());
				Assert.assertEquals("Some preset condition", presetCondition.getCondition().getNonCoded());
				Assert.assertNotNull(presetCondition.getId());
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// edit onset date for the current condition
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				// setup
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);

				results.assertNoErrors();
				Assert.assertEquals(3, conditions.length);

				Condition currentCondition = conditions[0];
				Assert.assertEquals("2020-02-11", dateAsString(currentCondition.getOnsetDate()));
			}

		}.run();
	}

	@Test
	public void shouldRecordAndEditConditionWithoutStatus() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "conditionForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");

				// setup for current condition
				request.addParameter(searchWidgetIdForCurrentCondition, "Epilepsy");
				request.addParameter(searchWidgetIdForCurrentCondition + "_hid", "3476");
				request.addParameter(statusWidgetIdForCurrentCondition, "active");

				// setup for past condition
				request.addParameter(searchWidgetIdForPastCondition, "Some past condition");
				request.addParameter(statusWidgetIdForPastCondition, "inactive");

				// setup for preset condition
				request.addParameter(searchWidgetIdForPresetCondition, "Some preset condition without status");
			}

			@Override
			public void testResults(SubmissionResults results) {
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);
				Concept expectedCondition = Context.getConceptService().getConceptByName("Epilepsy");

				results.assertNoErrors();
				Assert.assertEquals(2, conditions.length);

				Condition currentCondition = conditions[0];
				Assert.assertEquals(ConditionClinicalStatus.ACTIVE, currentCondition.getClinicalStatus());
				Assert.assertEquals(expectedCondition, currentCondition.getCondition().getCoded());
				Assert.assertNotNull(currentCondition.getId());

				Condition pastCondition = conditions[1];
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, pastCondition.getClinicalStatus());
				Assert.assertEquals("Some past condition", pastCondition.getCondition().getNonCoded());
				Assert.assertNotNull(pastCondition.getId());
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// edit onset date for the current condition
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				// setup
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);

				results.assertNoErrors();
				Assert.assertEquals(3, conditions.length);

			}

		}.run();
	}
	
	@Test
	public void shouldInitializeDefaultValues() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// Verify for condition
				assertTrue(html.contains("Condition: <span class=\"value\">Edema</span>"));
				// Verify for condition status
				assertTrue(html.contains("Status: <span class=\"value\">inactive</span>"));
				
			}
			
			@Override
			public Patient getPatientToEdit() {
				return getPatient();
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// Verify the condition default value - 'Edema'
				assertTrue(html.contains(
				    "<input type=\"text\"  id=\"w7\" name=\"w7\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
				// Verify the condition Additional detail value - 'Some additional details'
				assertTrue(html.contains("<input type=\"text\" name=\"w9\" id=\"w9\" value=\"Some additional details\"/>"));
				// Verify the condition status - 'Inactive'
				assertTrue(html.contains(
				    "<input type=\"radio\" id=\"w10_1\" name=\"w10\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
				// Verify the onset date - '2017-01-12'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w12-display', '#w12', '2017-01-12')</script>"));
				// Verify the end date - '2019-01-15'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w13-display', '#w13', '2019-01-15')</script>"));
				
			}

		}.run();
	}

}
