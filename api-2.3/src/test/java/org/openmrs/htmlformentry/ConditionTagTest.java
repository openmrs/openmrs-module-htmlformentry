package org.openmrs.htmlformentry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openmrs.ConditionClinicalStatus.ACTIVE;
import static org.openmrs.ConditionClinicalStatus.HISTORY_OF;
import static org.openmrs.ConditionClinicalStatus.INACTIVE;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Condition;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil2_3;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void setup() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		executeDataSet("org/openmrs/module/htmlformentry/data/conditions-data.xml");
	}
	
	@Test
	public void shouldRecordAndEditCondition() throws Exception {
		new
		
		RegressionTestHelper() {
			
			protected String add(String widget, int offset) {
				return "w" + (Integer.parseInt(widget.substring(1)) + offset);
			}
			
			protected String getAdditionalDetailWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 2);
			}
			
			protected String getStatusWidget(String conceptSearchWidget) {
				return add(conceptSearchWidget, 3);
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
				
				// filling the optional coded condition tag
				request.addParameter(widgets.get("Optional Coded Condition:"), "Epilepsy");
				request.addParameter(widgets.get("Optional Coded Condition:") + "_hid", "3476");
				request.addParameter(getAdditionalDetailWidget(widgets.get("Optional Coded Condition:")),
				    "Additional detail on this epilepsy");
				request.addParameter(getStatusWidget(widgets.get("Optional Coded Condition:")), "active");
				
				// filling the required coded condition tag
				request.addParameter(widgets.get("Required Coded Condition:"), "Indigestion");
				request.addParameter(widgets.get("Required Coded Condition:") + "_hid", "3475");
				request.addParameter(getAdditionalDetailWidget(widgets.get("Required Coded Condition:")),
				    "Some note on this indigestion");
				request.addParameter(getStatusWidget(widgets.get("Required Coded Condition:")), "active");
				
				// filling the required non-coded condition tag
				request.addParameter(widgets.get("Required Non-coded Condition:"), "Anemia (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Required Non-coded Condition:")), "inactive");
				
				// filling up the preset condition tag
				request.addParameter(getStatusWidget(widgets.get("Preset Condition:")), "history-of");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				final Map<String, Condition> conditions = results.getEncounterCreated().getConditions().stream()
				        .collect(Collectors.toMap(c -> HtmlFormEntryUtil2_3.getControlId(c), c -> c));
				
				results.assertNoErrors();
				assertThat(conditions.size(), is(4));
				
				Condition actualCondition;
				{
					actualCondition = conditions.get("optional_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Epilepsy"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("Additional detail on this epilepsy", actualCondition.getAdditionalDetail());
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Indigestion"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("Some note on this indigestion", actualCondition.getAdditionalDetail());
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("optional_preset_condition");
					Assert.assertEquals(HISTORY_OF, actualCondition.getClinicalStatus());
					assertThat(actualCondition.getCondition().getCoded().getId(), is(22));
					Assert.assertNotNull(actualCondition.getId());
				}
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// removing the concept of the optional coded condition tag
				request.removeParameter(widgets.get("Optional Coded Condition:"));
				request.removeParameter(widgets.get("Optional Coded Condition:") + "_hid");
				
				// filling for the first time in EDIT mode the optional non-coded condition tag 
				request.addParameter(widgets.get("Optional Non-coded Condition:"), "Sneezy cold (non-coded)");
				request.addParameter(getStatusWidget(widgets.get("Optional Non-coded Condition:")), "inactive");
				
				// editing the required coded condition additional details
				request.setParameter(getAdditionalDetailWidget(widgets.get("Required Coded Condition:")),
				    "Updated note on this indigestion");
				
				// removing the status of the preset condition tag
				request.removeParameter(getStatusWidget(widgets.get("Preset Condition:")));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				final Map<String, Condition> conditions = results.getEncounterCreated().getConditions().stream()
				        .collect(Collectors.toMap(c -> HtmlFormEntryUtil2_3.getControlId(c), c -> c));
				
				results.assertNoErrors();
				assertThat(conditions.size(), is(3));
				
				Condition actualCondition;
				{
					actualCondition = conditions.get("required_coded_condition");
					Assert.assertEquals(ACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals(Context.getConceptService().getConceptByName("Indigestion"),
					    actualCondition.getCondition().getCoded());
					Assert.assertEquals("Updated note on this indigestion", actualCondition.getAdditionalDetail());
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("optional_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Sneezy cold (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertNotNull(actualCondition.getId());
				}
				{
					actualCondition = conditions.get("required_noncoded_condition");
					Assert.assertEquals(INACTIVE, actualCondition.getClinicalStatus());
					Assert.assertEquals("Anemia (non-coded)", actualCondition.getCondition().getNonCoded());
					Assert.assertNotNull(actualCondition.getId());
				}
			}
			
		}.run();
	}
	
	@Test
	public void shouldInitializeValuesFromEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public Encounter getEncounterToView() {
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
				return getEncounterToView();
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// Verify the condition default value - 'Edema'
				assertTrue(html.contains(
				    "<input type=\"text\"  id=\"w17\" name=\"w17\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
				// Verify the condition status - 'Inactive'
				assertTrue(html.contains(
				    "<input type=\"radio\" id=\"w20_1\" name=\"w20\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
				
			}
			
		}.run();
	}
	
}
