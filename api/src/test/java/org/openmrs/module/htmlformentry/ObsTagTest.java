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

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests the obs tag.
 */
public class ObsTagTest extends BaseModuleContextSensitiveTest {
	
	private Patient patient;
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
		LogicUtil.registerDefaultRules();
	}
	
	@Test
	public void shouldSetDefaultNumericValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"2\" defaultValue=\"60\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"60.0\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultNumericValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"2\" defaultValue=\"invalidNumber\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
	}
	
	@Test
	public void shouldSetDefaultTextValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"8\" defaultValue=\"sometext\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"sometext\""));
	}
	
	@Test
	public void shouldSetDefaultCodedValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"1001\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"invalidValue\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsNotAllowedAnswer() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"2\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToTrue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"true\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"true\" selected=\"true\">"));
	}

    @Test
    public void shouldSetSelectSize() throws Exception {
        String htmlform = "<htmlform><obs conceptId=\"1\" required=\"true\" size=\"3\" id=\"paymentAmount\" answerLabels=\"50,Exempt\" answers=\"50,0\" defaultValue=\"50\" style=\"dropdown\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        String htmlToDisplay = session.getHtmlToDisplay();
        Assert.assertTrue("Result: " + htmlToDisplay,
                htmlToDisplay.matches(".*<select.*size=3.*"));
    }

	@Test
	public void shouldSetDefaultBooleanValueToFalse() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"false\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"false\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToNone() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultBooleanValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"yes\" style=\"no_yes_dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
	}
	
	@Test
	public void shouldSetDefaultDateValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test
	public void shouldSetDefaultDatetimeValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultDateAndDefaultDatetimeSet() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
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
				Assert.assertTrue("Should contain default text: " + html, html.contains("default text"));
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
				Assert.assertTrue("Should contain default date: " + html, html.contains("2011-06-10"));
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
				Assert.assertTrue("Should contain default boolean: " + html, html.contains("<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"true\" checked=\"true\"/>"));
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
				Assert.assertTrue("View should not contain default boolean: " + html, html.contains("Boolean: <span class=\"emptyValue\">"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should not contain default boolean: " + html, html.contains("<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"true\"/>"));
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
				Assert.assertTrue("Should contain default coded value: " + html, html.contains("value=\"1002\" checked=\"true\""));
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
				Assert.assertTrue("View should not contain default coded value: " + html, html.contains("Coded: <span class=\"emptyValue\">"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertFalse("Edit should not contain default coded value: " + html, html.contains("checked=\"true\""));
			}
			
			
		}.run();
	}

    @Test
	public void shouldSupportCheckboxForNumericObs() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"2\" answer=\"8\" answerLabel=\"Eight\" style=\"checkbox\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		   session.getHtmlToDisplay().contains("<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"8.0\"/>"));
	}

    @Test(expected = NumberFormatException.class)
	public void shouldThrowExceptionWithCheckboxIfAnswerIsNotNumeric() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"2\" answer=\"eight\" answerLabel=\"Eight\" style=\"checkbox\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
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
                results.assertObsCreated(1, "8.0");
			}

            public boolean doViewEncounter() {
				return true;
			}

            public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain checked numeric value: " + html, html.contains("NumericValue: <span class=\"value\">[X]&#160;"));
			}
		}.run();
	}


    /**
     * verifies whether the previous obs is correctly voided when a new obs created, with changing the numeric value
     * of checkbox, tests the changing of numeric value too.
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
                results.assertObsCreated(1,"8.0");
			}

            @Override
            public boolean doEditEncounter() {
		            return true;
	        }

            @Override
			public String[] widgetLabelsForEdit() {
				return new String[] {"Date:", "NumericValue:" };
			}
            @Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("NumericValue:"), "4");
			}

            @Override
            public void testEditedResults(SubmissionResults results){
                results.assertNoErrors();
                results.assertObsCreatedCount(1);
                results.assertObsVoided(1,"8.0");
                results.assertObsCreated(1,"4.0");

            }
		}.run();
	}

    @Test
	public void shouldDisplayDefaultOrUserDefinedCommentFieldLabelIfRequested() throws Exception {
		// If there is no comment label text defined, show the default comment text
        String htmlform = "<htmlform><obs conceptId=\"1\" labelText=\"CD4 count\" showCommentField=\"true\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        Assert.assertTrue(session.getHtmlToDisplay().contains("htmlformentry.comment: <input type=\"text\" name=\"w3\" id=\"w3\"/>"));

        // else show the user entered comment field text
        String htmlform2 = "<htmlform><obs conceptId=\"1\" labelText=\"CD4 count\" commentFieldLabel=\"Add comment\"/></htmlform>";
		FormEntrySession session2 = new FormEntrySession(patient, htmlform2, null);
		Assert.assertTrue(session2.getHtmlToDisplay().contains("Add comment <input type=\"text\" name=\"w3\" id=\"w3\"/>"));

        String htmlform3 = "<htmlform><obs conceptId=\"1\" labelText=\"CD4 count\" showCommentField=\"true\" commentFieldLabel=\"Add comment\"/></htmlform>";
        FormEntrySession session3 = new FormEntrySession(patient, htmlform3, null);
        Assert.assertTrue(session3.getHtmlToDisplay().contains("Add comment <input type=\"text\" name=\"w3\" id=\"w3\"/>"));

	}

    @Test
    public void shouldAddCustomIdToSpanAroundObs() throws Exception {
        String htmlform = "<htmlform><obs id=\"obs-id\" conceptId=\"1\" labelText=\"CD4 count\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        String htmlToDisplay = session.getHtmlToDisplay();
        Assert.assertTrue(htmlToDisplay.contains("<span id=\"obs-id\" class=\"obs-field\">"));
    }

    @Test
    public void shouldAddCustomClassToSpanAroundObs() throws Exception {
        String htmlform = "<htmlform><obs class=\"custom-class\" conceptId=\"1\" labelText=\"CD4 count\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        String htmlToDisplay = session.getHtmlToDisplay();
        Assert.assertTrue(htmlToDisplay.contains("<span class=\"obs-field custom-class\">"));
    }

    @Test
    public void shouldAddCustomIDAndClassToSpanAroundObs() throws Exception {
        String htmlform = "<htmlform><obs id=\"obs-id\" class=\"custom-class\" conceptId=\"1\" labelText=\"CD4 count\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        String htmlToDisplay = session.getHtmlToDisplay();
        Assert.assertTrue(htmlToDisplay.contains("<span id=\"obs-id\" class=\"obs-field custom-class\">"));
    }
}
