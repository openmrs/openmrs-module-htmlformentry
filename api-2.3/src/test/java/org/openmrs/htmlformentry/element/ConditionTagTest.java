package org.openmrs.htmlformentry.element;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.30.xml");
	}
	
	@Test
	public void shouldRecordAndEditCondition() throws Exception {
		new RegressionTestHelper() {
			
			protected String add(String widget, int offset) {
				return "w" + (Integer.parseInt(widget.substring(1)) + offset);
			}
			
			protected String getStatusWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 2);
			}
			
			protected String getOnsetDateWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 4);
			}
			
			protected String getEndDateWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 5);
			}
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Optional Coded Condition:",
				        "Required Coded Condition:", "Optional Non-coded Condition:", "Required Non-coded Condition:",
				        "Preset Condition:" };
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// setup for Optional Coded Condition
				request.addParameter(widgets.get("Optional Coded Condition:"), "Epilepsy");
				request.addParameter(widgets.get("Optional Coded Condition:") + "_hid", "3476");
				request.addParameter(getStatusWidget(widgets.get("Optional Coded Condition:")), "active");
				request.addParameter(getOnsetDateWidget(widgets.get("Optional Coded Condition:")), "2014-02-11");
				
				// setup for required Coded Condition
				request.addParameter(widgets.get("Required Coded Condition:"), "Epilepsy");
				request.addParameter(widgets.get("Required Coded Condition:") + "_hid", "3476");
				request.addParameter(getStatusWidget(widgets.get("Required Coded Condition:")), "active");
				request.addParameter(getOnsetDateWidget(widgets.get("Required Coded Condition:")), "2014-02-11");
				
				// setup for Optional Non-coded Condition
				request.addParameter(widgets.get("Optional Non-coded Condition:"), "Anemia (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Optional Non-coded Condition:")), "inactive");
				request.addParameter(getOnsetDateWidget(widgets.get("Optional Non-coded Condition:")), "2013-02-11");
				request.setParameter(getEndDateWidget(widgets.get("Optional Non-coded Condition:")), "2019-04-11");
				
				// setup for required Non-coded Condition
				request.addParameter(widgets.get("Required Non-coded Condition:"), "Anemia (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Required Non-coded Condition:")), "inactive");
				request.addParameter(getOnsetDateWidget(widgets.get("Required Non-coded Condition:")), "2013-02-11");
				request.setParameter(getEndDateWidget(widgets.get("Required Non-coded Condition:")), "2019-04-11");
				
				// setup for preset condition7
				request.addParameter(getStatusWidget(widgets.get("Preset Condition:")), "inactive");
				request.addParameter(getOnsetDateWidget(widgets.get("Preset Condition:")), "2014-02-11");
				request.setParameter(getEndDateWidget(widgets.get("Preset Condition:")), "2020-04-11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[3]);
				
				results.assertNoErrors();
				assertThat(conditions.length, is(5));
				
				Condition actualCondition;
				// Optional Coded Condition
				{
					actualCondition = conditions[0];
					Assert.assertEquals(ConditionClinicalStatus.ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Epilepsy"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				// Required coded condition
				{
					actualCondition = conditions[1];
					Assert.assertEquals(ConditionClinicalStatus.ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Epilepsy"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				// Optional Non-coded Condition
				{
					actualCondition = conditions[2];
					Assert.assertEquals(ConditionClinicalStatus.INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertEquals("2013-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2019-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				// Required non-coded condition
				{
					actualCondition = conditions[3];
					Assert.assertEquals(ConditionClinicalStatus.INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertEquals("2013-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2019-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
				// Optional preset condition
				{
					actualCondition = conditions[4];
					Assert.assertEquals(ConditionClinicalStatus.INACTIVE, actualCondition.getClinicalStatus());
					assertThat(actualCondition.getCondition().getCoded().getId(), is(22));
					Assert.assertEquals("2014-02-11", dateAsString(actualCondition.getOnsetDate()));
					Assert.assertEquals("2020-04-11", dateAsString(actualCondition.getEndDate()));
					Assert.assertNotNull(actualCondition.getId());
				}
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(getOnsetDateWidget(widgets.get("Required Coded Condition:")), "2020-02-11");
				request.removeParameter(getStatusWidget(widgets.get("Preset Condition:")));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				// setup
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);
				
				results.assertNoErrors();
				Assert.assertEquals(4, conditions.length);
				
				Condition currentCondition = conditions[1];
				Assert.assertEquals("2020-02-11", dateAsString(currentCondition.getOnsetDate()));
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
				// Verify for onset date
				assertTrue(html.contains("Onset Date: <span class=\"value\">12/01/2017</span>"));
				// Verify for end date
				assertTrue(html.contains("End Date: <span class=\"value\">15/01/2019</span>"));
				
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
				    "<input type=\"text\"  id=\"w28\" name=\"w28\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
				// Verify the condition status - 'Inactive'
				assertTrue(html.contains(
				    "<input type=\"radio\" id=\"w30_1\" name=\"w30\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
				// Verify the onset date - '2017-01-12'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w32-display', '#w32', '2017-01-12')</script>"));
				// Verify the end date - '2019-01-15'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w33-display', '#w33', '2019-01-15')</script>"));
				
			}
			
		}.run();
	}
	
}
