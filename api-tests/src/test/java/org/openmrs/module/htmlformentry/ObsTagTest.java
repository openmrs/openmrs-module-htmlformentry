/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * Tests the obs tag.
 */
public class ObsTagTest extends BaseHtmlFormEntryTest {
	
	private Patient patient;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		patient = Context.getPatientService().getPatient(2);
	}
	
	@Test
	public void shouldSetDefaultNumericValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"5089\" defaultValue=\"60\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"60\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultNumericValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"5089\" defaultValue=\"invalidNumber\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldSetDefaultTextValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"80000\" defaultValue=\"sometext\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"sometext\""));
	}
	
	@Test
	public void shouldSetDefaultCodedValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"1001\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"invalidValue\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsNotAllowedAnswer() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"2\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToTrue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"18\" defaultValue=\"true\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"true\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetSelectSize() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"5497\" required=\"true\" size=\"3\" id=\"paymentAmount\" answerLabels=\"50,Exempt\" answers=\"50,0\" defaultValue=\"50\" style=\"dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		assertTrue("Result: " + htmlToDisplay, htmlToDisplay.matches(".*<select.*size=\"3\".*"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToFalse() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"18\" defaultValue=\"false\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"false\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToNone() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"18\" defaultValue=\"\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultBooleanValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"18\" defaultValue=\"yes\" style=\"no_yes_dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldSetDefaultDateValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test
	public void shouldSetDefaultDatetimeValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultDateAndDefaultDatetimeSet() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayPlaceholderInTextObs() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithTextPlaceholder";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				assertTrue(
				    html.contains("Text: <input type=\"text\" name=\"w8\" id=\"w8\" placeholder=\"type something\"/>"));
			}
		}.run();
	}
	
	@Test
	public void shouldNotDisplayDefaultValueForEmptyTextValueInViewAndEdit() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDefaultTextValue";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Text:" };
			}
			
			public void testBlankFormHtml(String html) {
				assertTrue("Should contain default text: " + html, html.contains("default text"));
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Text:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(0);
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertFalse("Should not contain default text: " + html, html.contains("default text"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertFalse("Should not contain default text: " + html, html.contains("default text"));
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotDisplayDefaultValueForEmptyDateValueInViewAndEdit() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDefaultDateValue";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			public void testBlankFormHtml(String html) {
				assertTrue("Should contain default date: " + html, html.contains("2011-06-10"));
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(0);
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertFalse("Should not contain default date: " + html, html.contains("2011-06-10"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertFalse("Should not contain default date: " + html, html.contains("2011-06-10"));
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotDisplayDefaultValueForEmptyBooleanValueInViewAndEdit() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDefaultBooleanValue";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Boolean:" };
			}
			
			public void testBlankFormHtml(String html) {
				assertTrue("Should contain default boolean: " + html,
				    html.contains("<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"true\" checked=\"true\"/>"));
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Boolean:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(0);
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				assertTrue("View should not contain default boolean: " + html,
				    html.contains("Boolean: <span class=\"emptyValue\">"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				assertTrue("Edit should not contain default boolean: " + html,
				    html.contains("<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"true\"/>"));
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotDisplayDefaultValueForEmptyCodedValueInViewAndEdit() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDefaultCodedValue";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:" };
			}
			
			public void testBlankFormHtml(String html) {
				assertTrue("Should contain default coded value: " + html, html.contains("value=\"1002\" checked=\"true\""));
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(0);
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				assertTrue("View should not contain default coded value: " + html,
				    html.contains("Coded: <span class=\"emptyValue\">"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertFalse("Edit should not contain default coded value: " + html,
				    html.contains("checked=\"true\""));
			}
			
		}.run();
	}
	
	@Test
	public void shouldSupportCheckboxForNumericObs() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"5089\" answer=\"8\" answerLabel=\"Eight\" style=\"checkbox\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"8.0\"/>"));
	}
	
	@Test(expected = NumberFormatException.class)
	public void shouldThrowExceptionWithCheckboxIfAnswerIsNotNumeric() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"5089\" answer=\"eight\" answerLabel=\"Eight\" style=\"checkbox\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldSubmitObsWithNumericValueCheckbox() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "SingleObsFormWithNumericCheckbox";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "NumericValue:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("NumericValue:"), "8");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(5497, "8");
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				assertTrue("View should contain checked numeric value: " + html,
				    html.contains("NumericValue: <span class=\"value\">[X]&#160;"));
			}
		}.run();
	}
	
	/**
	 * verifies whether the previous obs is correctly voided when a new obs created, with changing the
	 * numeric value of checkbox, tests the changing of numeric value too.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldVoidPreviousObsWhenEditNumericValueCheckbox() throws Exception {
		new RegressionTestHelper() {
			
			Date date = new Date();
			
			@Override
			public String getFormName() {
				return "SingleObsFormWithNumericCheckbox";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "NumericValue:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("NumericValue:"), "8");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(5497, "8");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "NumericValue:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("NumericValue:"), "4");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertObsCreatedCount(1);
				results.assertObsVoided(5497, "8");
				results.assertObsCreated(5497, "4");
				
			}
		}.run();
	}
	
	@Test
	public void shouldDisplayDefaultOrUserDefinedCommentFieldLabelIfRequested() throws Exception {
		// If there is no comment label text defined, show the default comment text
		String htmlform = "<htmlform><obs conceptId=\"5497\" labelText=\"CD4 count\" showCommentField=\"true\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertTrue(session.getHtmlToDisplay().contains("Comment: <input type=\"text\" name=\"w3\" id=\"w3\"/>"));
		
		// else show the user entered comment field text
		String htmlform2 = "<htmlform><obs conceptId=\"5497\" labelText=\"CD4 count\" commentFieldLabel=\"Add comment\"/></htmlform>";
		FormEntrySession session2 = new FormEntrySession(patient, htmlform2, null);
		assertTrue(session2.getHtmlToDisplay().contains("Add comment <input type=\"text\" name=\"w3\" id=\"w3\"/>"));
		
		String htmlform3 = "<htmlform><obs conceptId=\"5497\" labelText=\"CD4 count\" showCommentField=\"true\" commentFieldLabel=\"Add comment\"/></htmlform>";
		FormEntrySession session3 = new FormEntrySession(patient, htmlform3, null);
		assertTrue(session3.getHtmlToDisplay().contains("Add comment <input type=\"text\" name=\"w3\" id=\"w3\"/>"));
		
	}
	
	@Test
	public void shouldDisplayDefaultOrUserDefinedCommentFieldCodeIfRequested() throws Exception {
		
		String htmlform1 = "<htmlform><obs conceptId=\"5497\" labelText=\"CD4 count\" commentFieldCode=\"some.message.code\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform1, null);
		String html = session.getHtmlToDisplay();
		assertTrue(session.getHtmlToDisplay().contains("some.message.code <input type=\"text\" name=\"w3\" id=\"w3\"/>"));
		
		String htmlform2 = "<htmlform><obs conceptId=\"5497\" labelText=\"CD4 count\" showCommentField=\"true\" commentFieldCode=\"some.message.code\"/></htmlform>";
		FormEntrySession session2 = new FormEntrySession(patient, htmlform2, null);
		String html2 = session2.getHtmlToDisplay();
		assertTrue(session2.getHtmlToDisplay().contains("some.message.code <input type=\"text\" name=\"w3\" id=\"w3\"/>"));
		
	}
	
	@Test
	public void shouldAddCustomIdToSpanAroundObs() throws Exception {
		String htmlform = "<htmlform><obs id=\"obs-id\" conceptId=\"5497\" labelText=\"CD4 count\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		assertTrue(htmlToDisplay.contains("<span id=\"obs-id\" class=\"obs-field\">"));
	}
	
	@Test
	public void shouldAddCustomClassToSpanAroundObs() throws Exception {
		String htmlform = "<htmlform><obs class=\"custom-class\" conceptId=\"5497\" labelText=\"CD4 count\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		assertTrue(htmlToDisplay.contains("<span class=\"obs-field custom-class\">"));
	}
	
	@Test
	public void shouldAddConceptAnswersDefinedInConceptSet() throws Exception {
		String htmlform = "<htmlform><obs id=\"obs-id\" conceptId=\"1000\"  answerConceptSetIds=\"1004\" labelText=\"Allergy\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		HtmlFormSchema schema = session.getContext().getSchema();
		Assert.assertEquals(1, schema.getFields().size());
		ObsField field = (ObsField) schema.getFields().get(0);
		
		// Expected
		Concept set = Context.getConceptService().getConcept(1004);
		List<Concept> setMembers = Context.getConceptService().getConceptsByConceptSet(set);
		
		Assert.assertEquals(setMembers.size(), field.getAnswers().size());
		
		for (ObsFieldAnswer answer : field.getAnswers()) {
			Assert.assertTrue(setMembers.contains(answer.getConcept()));
		}
	}
	
	@Test
	public void shouldAddCustomIDAndClassToSpanAroundObs() throws Exception {
		String htmlform = "<htmlform><obs id=\"obs-id\" class=\"custom-class\" conceptId=\"5497\" labelText=\"CD4 count\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		assertTrue(htmlToDisplay.contains("<span id=\"obs-id\" class=\"obs-field custom-class\">"));
	}
	
	@Test
	public void shouldSubmitObsWithAutocomplete() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:!!1" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:!!1"), "1001");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(1000, "PENICILLIN");
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				assertTrue("View should contain coded value: " + html,
				    html.contains("Coded: <span class=\"value\">PENICILLIN</span>"));
			}
		}.run();
	}
	
	@Test
	public void shouldEditObsWithAutocomplete() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:!!1" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:!!1"), "1001");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Coded:!!1" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Coded:!!1"), "1002");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertObsCreatedCount(1);
				results.assertObsVoided(1000, "PENICILLIN");
				results.assertObsCreated(1000, "CATS");
			}
			
		}.run();
	}
	
	@Test
	public void dynamicAutocomplete_shouldSubmitObs() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithMultiAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:"), "2"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.addParameter("w8span_0_hid", "1001");
				request.addParameter("w8span_1_hid", "1002");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(2);
				results.assertObsCreated(1000, "PENICILLIN");
				results.assertObsCreated(1000, "CATS");
			}
			
		}.run();
	}
	
	@Test
	public void dynamicAutocomplete_shouldEditExistingObs() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithMultiAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:"), "2"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.addParameter("w8span_0_hid", "1001");
				request.addParameter("w8span_1_hid", "1002");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Coded:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Coded:"), "2"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.setParameter("w8span_0_hid", "1002");
				request.setParameter("w8span_1_hid", "1003");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				
				assertThat(encounter.getAllObs(false).size(), is(2)); // should be two non-voided obs of value 1002 & 1003
				assertThat(encounter.getAllObs(true).size(), is(3)); // should be three obs included the voided obs for 1001
				
				Set<Integer> valueCoded = new HashSet<Integer>();
				
				for (Obs obs : encounter.getAllObs(true)) {
					if (obs.isVoided()) {
						assertThat(obs.getValueCoded().getId(), is(1001));
					} else {
						valueCoded.add(obs.getValueCoded().getId());
					}
				}
				
				assertTrue(valueCoded.contains(1002));
				assertTrue(valueCoded.contains(1003));
			}
			
		}.run();
	}
	
	@Test
	public void dynamicAutocomplete_shouldEditExistingObsWhenSomeObsAreRemoved() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithMultiAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:"), "2"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.addParameter("w8span_0_hid", "1001");
				request.addParameter("w8span_1_hid", "1002");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Coded:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Coded:"), "1"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.setParameter("w8span_0_hid", "1003");
				request.removeParameter("w8span_1_hid");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				
				assertThat(encounter.getAllObs(false).size(), is(1)); // should be one non-voided obs of value 1003
				assertThat(encounter.getAllObs(true).size(), is(3)); // should be three obs included the voided obs for 1001 and 1002
				
				Set<Integer> valueCoded = new HashSet<Integer>();
				
				for (Obs obs : encounter.getAllObs(true)) {
					if (!obs.isVoided()) {
						assertThat(obs.getValueCoded().getId(), is(1003));
					} else {
						valueCoded.add(obs.getValueCoded().getId());
					}
				}
				
				assertTrue(valueCoded.contains(1002));
				assertTrue(valueCoded.contains(1001));
			}
			
		}.run();
	}
	
	@Test
	public void dynamicAutocomplete_shouldVoidAllExistingObsIfEmpty() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithMultiAutocomplete";
			}
			
			public Patient getPatient() {
				return patient;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Coded:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Coded:"), "2"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.addParameter("w8span_0_hid", "1001");
				request.addParameter("w8span_1_hid", "1002");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Coded:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Coded:"), "0"); // in the dynamic autocomplete, the widget value is just the count of the number of entries
				request.removeParameter("w8span_0_hid");
				request.removeParameter("w8span_1_hid");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				
				assertThat(encounter.getAllObs(false).size(), is(0)); // no none-voided obs
				assertThat(encounter.getAllObs(true).size(), is(2)); // the existing obs should have been voided
			}
			
		}.run();
	}
	
	@Test
	public void testObsWithControlsSections() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsWithControlsSections";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				assertTrue(html.contains(
				    "htmlForm.setupWhenThen('allergy', {\"1001\":\"#penicillin-followup\",\"1002\":\"#cats-followup\"}, {\"1002\":\"window.alert('Cats!')\"}, {\"1002\":\"window.alert('Not Cats!')\"});"));
				assertTrue(html.contains(
				    "htmlForm.setupWhenThen('previous-admission', {\"true\":\"#previous-admission-true\",\"false\":\"#previous-admission-false\"}, {\"true\":\"window.alert('Admited Before')\"}, {\"true\":\"window.alert('Not Admited Before')\"});"));
			}
			
		}.run();
	}
	
	@Test
	public void testObsDrugsWithAutoComplete() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsDrugFormWithAutocomplete";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				assertTrue(html.contains("autocomplete"));
				assertTrue(html.contains("source: '/openmrs/module/htmlformentry/drugSearch.form?includeRetired=true'"));
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergic to drug:", "NYQUIL as checkbox:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergic to drug:"), "Drug:3");
				request.addParameter(widgets.get("NYQUIL as checkbox:"), "Drug:11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(3);
				results.assertObsCreated(1000, Context.getConceptService().getDrug(3));
				results.assertObsCreated(8119, Context.getConceptService().getDrug(11));
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				assertThat(html, containsString("Allergic to drug: Aspirin"));
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;NyQuil</span>", html);
			}
			
			@Override
			public void testEditFormHtml(String html) {
				assertTrue(html.contains("<input id=\"w8-value\" type=\"hidden\" name=\"w8\" value=\"Drug:3\"/>"));
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergic to drug:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Allergic to drug:"), "Drug:2");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertObsCreated(1000, Context.getConceptService().getDrug(2));
			}
		}.run();
	}
	
	@Test
	public void testObsDrugsWithDropdown() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsDrugFormWithDropdown";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				assertTrue(
				    html.contains("<option value=\"Drug:2\">Triomune-30</option><option value=\"Drug:3\">Aspirin</option>"));
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergic to drug:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergic to drug:"), "Drug:3");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(1000, Context.getConceptService().getDrug(3));
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				assertThat(html, containsString(" <span class=\"value\">Aspirin</span>"));
			}
			
			@Override
			public void testEditFormHtml(String html) {
				assertTrue(html.contains("<option value=\"Drug:3\" selected=\"true\">Aspirin</option>"));
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergic to drug:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Allergic to drug:"), "Drug:2");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertObsCreated(1000, Context.getConceptService().getDrug(2));
			}
		}.run();
	}
	
	@Test
	public void shouldDisplayObsPropertyAccessors() throws Exception {
		String htmlform = "<htmlform><obs id=\"myObs\" conceptId=\"1000\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myObs.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myObs.error'\\]", session.getFieldAccessorJavascript());
	}
	
	@Test
	public void shouldDisplayObsPropertyAccessorsForDateAndTimeForObsOfTypeDateTime() throws Exception {
		String htmlform = "<htmlform><obs id=\"myObs\" conceptId=\"1007\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myObs.value.date'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myObs.value.time'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myObs.error'\\]", session.getFieldAccessorJavascript());
	}
}
