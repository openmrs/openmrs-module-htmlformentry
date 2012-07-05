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
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"60.0\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultNumericValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"2\" defaultValue=\"invalidNumber\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultTextValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"8\" defaultValue=\"sometext\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"sometext\""));
	}
	
	@Test
	public void shouldSetDefaultCodedValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"1001\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"invalidValue\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsNotAllowedAnswer() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"2\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToTrue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"true\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"true\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToFalse() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"false\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"false\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToNone() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultBooleanValueIsInvalid() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"yes\" style=\"no_yes_dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultDateValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test
	public void shouldSetDefaultDatetimeValue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultDateAndDefaultDatetimeSet() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
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
	public void shouldDisplayCommentDetailsIfShowCommentFieldIsTrue() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\" showCommentField=\"true\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		System.out.println(session.getHtmlToDisplay());
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("htmlformentry.comment") > -1);
	}
	
	@Test
	public void shouldNotDisplayCommentDetailsIfShowCommentFieldIsSetToFalse() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\" showCommentField=\"false\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("htmlformentry.comment") < 0);
	}
	
	@Test
	public void shouldNotDisplayCommentDetailsIfShowCommentFieldIsMissing() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("htmlformentry.comment") < 0);
	}
	
	@Test
	public void testSettingComment() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsFormWithComment";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "100");
				request.addParameter("w9", "test comment");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				Assert.assertEquals(1, results.getObsCreatedCount());
				Assert.assertEquals("test comment", results.getEncounterCreated().getObs().iterator().next().getComment());
			}
		}.run();
	}
}
