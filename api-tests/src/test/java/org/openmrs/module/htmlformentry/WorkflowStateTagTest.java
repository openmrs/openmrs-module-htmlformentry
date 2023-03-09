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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 *
 */
public class WorkflowStateTagTest extends BaseHtmlFormEntryTest {
	
	public static final String XML_FORM_NAME = "workflowStateForm";
	
	public static final String XML_CHECKBOX_FORM_NAME = "workflowStateCheckboxForm";
	
	public static final String XML_HIDDEN_FORM_NAME = "workflowStateHiddenForm";
	
	public static final String TEST_PROGRAM = "67bad8f4-5140-11e1-a3e3-00248140a5eb";
	
	public static final String START_STATE = "89d1a292-5140-11e1-a3e3-00248140a5eb";
	
	public static final String MIDDLE_STATE = "99f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String END_STATE = "8ef66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String TEST_STATE7 = "67337cdc-53ad-11e1-8cb6-00248140a5eb";
	
	public static final String DIFFERENT_PROGRAM_WORKFLOW_STATE = "67337cdc-53ad-11e1-8cb6-00248140a5eb";
	
	public static final String MAPPED_STATE = "67337cdc-53ad-11a1-8cb6-00248140a5eb";
	
	public static final Date TODAY = HtmlFormEntryUtil.clearTimeComponent(new Date());
	
	public static final Date ONE_YEAR_AGO = DateUtils.addYears(TODAY, -1);
	
	public static final Date TWO_YEARS_AGO = DateUtils.addYears(TODAY, -2);
	
	public static final Date THREE_YEARS_AGO = DateUtils.addYears(TODAY, -3);
	
	public static final String RETIRED_STATE = "91f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	private Patient patient;
	
	@Autowired
	ProgramWorkflowService programWorkflowService;
	
	@Autowired
	HtmlFormEntryService htmlFormEntryService;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		patient = Context.getPatientService().getPatient(2);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfWorkflowIdMissing() throws Exception {
		String htmlform = "<htmlform><workflowState/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayOnlyInitialStateIfNotEnrolled() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		assertNotPresent(session, MIDDLE_STATE);
		assertNotPresent(session, END_STATE);
	}
	
	@Test
	public void shouldDisplayAllStatesIfInInitialState() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		assertPresent(session, MIDDLE_STATE);
		assertPresent(session, END_STATE);
	}
	
	@Test
	public void shouldNotDisplayRetiredStates() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertNotPresent(session, RETIRED_STATE);
	}
	
	@Test
	public void shouldDisplayDropdownIfNoStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("option"));
	}
	
	@Test
	public void shouldDisplayDropdownIfDropdownStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" type=\"dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("option"));
	}
	
	@Test
	public void shouldDisplayRadioIfRadioStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" type=\"radio\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("type=\"radio\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStateIdAndCheckboxStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" type=\"checkbox\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStateIdAndHiddenStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" type=\"hidden\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfInvalidStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" type=\"invalid\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStates() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		assertPresent(session, END_STATE);
		assertNotPresent(session, MIDDLE_STATE);
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStatesGivenIds() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"200,201\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		assertPresent(session, END_STATE);
		assertNotPresent(session, MIDDLE_STATE);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedInvalidStates() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + ",some invalid state,"
		        + END_STATE + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedDifferentProgramStates() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + ","
		        + DIFFERENT_PROGRAM_WORKFLOW_STATE + "," + END_STATE + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStateAndDefaultToCheckbox() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		assertNotPresent(session, MIDDLE_STATE);
		assertNotPresent(session, END_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("checkbox"));
	}
	
	@Test
	public void shouldDisplayStateSpecifiedByMapping() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		String htmlform = "<htmlform><workflowState workflowId=\"107\" stateId=\"XYZ:Test Code\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, MAPPED_STATE);
	}
	
	@Test
	public void shouldDisplayIfSpecifiedStateAndCheckboxStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" type=\"checkbox\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("checkbox"));
	}
	
	@Test
	public void shouldDisplayIfSpecifiedStateAndHiddenStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" type=\"hidden\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, START_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("hidden"));
	}
	
	@Test
	public void shouldDisplaySpecifiedStateWithWhitespaces() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"    " + START_STATE
		        + "      \"/></htmlform>";
		new FormEntrySession(patient, htmlform, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedStateAndDropdownStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" type=\"dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedStateAndRadioStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" type=\"radio\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedInvalidState() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"invalid\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedDifferentProgramState() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + DIFFERENT_PROGRAM_WORKFLOW_STATE
		        + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String endState = "Transition to end state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + ", " + endState + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, startState);
		assertPresent(session, endState);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNotEnoughStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfTooManyStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + ",someState,someOtherState\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, startState);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateIdAndStateLabels() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabels=\""
		        + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateIdsAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStatesAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateLabel=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStatesAndStateLabels() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateLabelsAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void shouldDisplayLabel() throws Exception {
		String label = "Some label text";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" labelText=\"" + label + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		assertPresent(session, label);
	}
	
	@Test
	public void shouldEnrollInProgramAndTransitionToState() throws Exception {
		//Given: Patient has no workflow state
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				//When: Html form is entered in which workflow state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state Y is created with a start date of the encounter date
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, null);
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateBeforeAnotherState() throws Exception {
		//Given: A patient has a program that started 2 years ago, with state X, then a workflow state of Z from today to current
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(END_STATE, TODAY);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				//When: Html form is being back entered with an encounter date of one year ago in which state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: State X end date is changed from today to one year ago, state Y is added from one year ago until today, and state Z remains the same
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, TODAY);
				assertState(pp, END_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
		}.run();
		
	}
	
	@Test
	public void shouldTransitionToStateAfterAnotherState() throws Exception {
		//Given: Patient has a workflow state of X starting one year ago
		transitionToState(START_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is being entered with an encounter date of today, in which workflow state Y is selected
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state X is stopped with a stop date of Jan 2012, Workflow state Y is created with a start date of Jan 2012 and is still current
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, TODAY);
				assertState(pp, MIDDLE_STATE, TODAY, null);
			}
		}.run();
	}
	
	@Test
	public void shouldNotTransitionToSameState() throws Exception {
		//Given: Patient has a workflow state of X starting one year ago and still active
		transitionToState(START_STATE, ONE_YEAR_AGO);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which workflow state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: No change to workflow state
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateAfterCurrent() throws Exception {
		//Given: Patient has an enrollment with state X starting two years ago and still active
		transitionToState(START_STATE, TWO_YEARS_AGO);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of one year ago in which state Z is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: This should end state X one year ago and start state Z one year ago with no end date
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				assertState(pp, END_STATE, ONE_YEAR_AGO, null);
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateAfterCurrentOnTheSameDay() throws Exception {
		//Given: Patient has no workflow state
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: This should enroll patient today with state X starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">START STATE"));
			}
		}.run();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: A second Html form / encounter is entered with an encounter date of today in which state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state X is stopped with a stop date of today, Workflow state Y is created with a start date of today and is still current
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, TODAY);
				assertState(pp, MIDDLE_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">MIDDLE STATE"));
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateInBetweenStates() throws Exception {
		//Given: Patient has a workflow state of X from two years ago until today, then a workflow state of Z from today to current
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(END_STATE, TODAY);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of one year ago in which workflow state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow X is changed to a stop date of one year ago, Workflow Y is created with a start date of one year ago and a stop date of today, and workflow Z remains unchanged
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, TODAY);
				assertState(pp, END_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.removeAllParameters(); // This prevents resubmission, while allowing us to test html in edit mode
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">MIDDLE STATE"));
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotTransitionIfNotAnswered() throws Exception {
		//Given: Patient has a workflow state of X
		transitionToState(START_STATE, ONE_YEAR_AGO);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Question on Html form is not answered
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: No action
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">START STATE"));
			}
		}.run();
	}
	
	@Test
	public void shouldAllowToEditStateWithSameDate() throws Exception {
		//Given: Patient has no workflow state
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: This should enroll patient today with state X starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited with and state Y is selected
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, MIDDLE_STATE, TODAY, null);
				//Then: This should remove state X and add state Y starting today (replace state X with Y with same date)
				assertNoState(pp, START_STATE);
			}
		}.run();
	}
	
	@Test
	public void shouldMoveProgramEnrollmentAndProgramStateStartEarlier() throws Exception {
		//Given: Patient has no workflow state
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: This should enroll patient today with state X starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and encounter date is changed to one year ago and state Y is selected
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				//Then: This should change the enrollment date to one year ago, remove state X, and add state Y starting one year ago
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, null);
				assertNoState(pp, START_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldMoveProgramStateStartLater() throws Exception {
		//Given: Patient has no workflow state
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of one year ago in which state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: This should enroll patient with state X starting one year ago
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited with and state Y is selected starting today
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: This should keep the enrollment date as one year ago, remove state X, and add state Y starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, MIDDLE_STATE, TODAY, null);
				assertNoState(pp, START_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldTransitionFromNoStateToSelectedStateOnEdit() throws Exception {
		//Given: Patient has an enrollment starting today, with no state
		enrollInProgram(START_STATE, TODAY);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of one year ago in which no state is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), ""); // set no state
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: The enrollment date has NOT been changed (since no state was set)
				List<PatientProgram> patientPrograms = patientPrograms(patient, TEST_PROGRAM);
				Assert.assertEquals(1, patientPrograms.size());
				PatientProgram patientProgram = patientPrograms.get(0);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(TODAY), dateAsString(patientProgram.getDateEnrolled()));
				Assert.assertEquals(0, patientProgram.getStates().size());
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and encounter date is changed to today and state X is selected
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: the existing patient program still exists and the enrollment date has not been changed and there is one state X starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				Assert.assertEquals(1, nonVoidedStates(pp));
				assertState(pp, START_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldCreateNewProgramIfEncounterDateNotDuringProgramEnrollmentOnEdit() throws Exception {
		//Given: Patient has an enrollment with no state, starting today, and still active
		enrollInProgram(START_STATE, TODAY);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which no state is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), ""); // set no state
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and encounter date is changed to one year ago and state X is selected
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				//Then: we should now have two program enrollments, one from one year ago until today and one starting today
				List<PatientProgram> pps = patientPrograms(results.getPatient(), TEST_PROGRAM);
				Assert.assertEquals(2, pps.size());
				PatientProgram pp1 = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, TODAY);
				Assert.assertEquals(1, nonVoidedStates(pp1));
				assertState(pp1, START_STATE, ONE_YEAR_AGO, TODAY);
				PatientProgram pp2 = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				Assert.assertNotNull(pp2);
				Assert.assertNotEquals(pp1, pp2);
			}
			
		}.run();
	}
	
	@Test
	public void shouldEndExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		//Given: Patient has an enrollment in state X starting two years ago, and still active
		transitionToState(START_STATE, TWO_YEARS_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state Z is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: one enrollment starting two years ago, with state X from two years ago until today, and state Z starting today
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				assertState(pp, END_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and encounter date is changed to one year ago and state y is selected
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				//Then: one enrollment starting two years ago, with state X from two years ago until one year ago, state Y starting one year ago, and no state Z
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				Assert.assertEquals(2, nonVoidedStates(pp));
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldRemoveExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		//Given: Patient has an enrollment in state X from two years ago until one year ago, state Y from one year ago, and still active
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(MIDDLE_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state Z is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and encounter date is changed to one year ago and state Z is selected
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: one enrollment starting two years ago, with state X from two years ago until one year ago, state Z starting one year ago and still active, and no state Y
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				Assert.assertEquals(2, nonVoidedStates(pp));
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				assertState(pp, END_STATE, ONE_YEAR_AGO, null);
				assertNoState(pp, MIDDLE_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldShiftExistingStateEndDateForwardAsNeededWhenShiftingStateLater() throws Exception {
		//Given: Patient has an enrollment in state X from two years ago nd still active
		transitionToState(START_STATE, TWO_YEARS_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of one year ago in which state Z is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited with an encounter date of today in which state Z is selected
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: one enrollment starting two years ago, with state X from two years ago until today and state Z starting today and still active
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				Assert.assertEquals(2, nonVoidedStates(pp));
				assertState(pp, START_STATE, TWO_YEARS_AGO, TODAY);
				assertState(pp, END_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test(expected = FormEntryException.class)
	public void shouldFailIfAttemptingToShiftStateStartDatePastEndDate() throws Exception {
		//Given: Patient has an enrollment in state X from two years ago until one year ago, state z from one year ago, and still active
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(END_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of two years ago in which no state is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TWO_YEARS_AGO));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited with an encounter date of today in which state X is selected
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
		}.run();
	}
	
	// See HTML-321
	@Test
	public void shouldNotMoveStateIfDateDiffersFromOriginalEncounterDateOnEdit() throws Exception {
		//Given: Patient has an enrollment with state X starting one year ago
		transitionToState(START_STATE, ONE_YEAR_AGO);
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today in which state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: the existing enrollment with state X remains the same, starting one year ago
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and submitted without changing anything (encounter date today, with state X)
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: the existing enrollment with state X remains the same, starting one year ago
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotRemovePreviousStateOnEditIfEncounterDateDoesNotMatchStateStartDate() throws Exception {
		
		//Given: Patient has an enrollment starting 3 years ago, with start state from 3 years ago until 2 years ago, and from 1 year ago still active
		transitionToState(START_STATE, THREE_YEARS_AGO, TWO_YEARS_AGO);
		transitionToState(START_STATE, ONE_YEAR_AGO);
		
		PatientProgram pp = assertProgram(patient, TEST_PROGRAM, THREE_YEARS_AGO, null);
		assertState(pp, START_STATE, THREE_YEARS_AGO, TWO_YEARS_AGO);
		assertState(pp, START_STATE, ONE_YEAR_AGO, null);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//When: Html form is entered with an encounter date of today with no state selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				//Then: nothing should have changed, since no state was entered.
				PatientProgram pp = assertProgram(patient, TEST_PROGRAM, THREE_YEARS_AGO, null);
				assertState(pp, START_STATE, THREE_YEARS_AGO, TWO_YEARS_AGO);
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				//When: Html form is edited and submitted with the same encounter date (today), but with state Y selected
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				//Then: the existing enrollment and states should remain, with the most recent state X ending today, and state Y starting today with no end date
				PatientProgram pp = assertProgram(patient, TEST_PROGRAM, THREE_YEARS_AGO, null);
				assertState(pp, START_STATE, THREE_YEARS_AGO, TWO_YEARS_AGO);
				assertState(pp, START_STATE, ONE_YEAR_AGO, TODAY);
				assertState(pp, MIDDLE_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test
	public void checkboxShouldAppearCheckedIfCurrentlyInSpecifiedState() throws Exception {
		
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(MIDDLE_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				Pattern p = Pattern.compile(".*checked=\"true\"/><label for=\".*\">MIDDLE STATE.*",
				    Pattern.MULTILINE | Pattern.DOTALL);
				Assert.assertTrue("Checkbox should be checked: " + html, p.matcher(html).matches());
			}
			
		}.run();
	}
	
	@Test
	public void checkboxShouldNotAppearCheckedIfNotCurrentlyInSpecifiedState() throws Exception {
		
		transitionToState(START_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				Assert.assertTrue("Checkbox should not be checked: " + html,
				    !html.contains("checked=\"true\"/>MIDDLE STATE"));
			}
			
		}.run();
	}
	
	@Test
	public void checkboxShouldSetPatientInState() throws Exception {
		
		transitionToState(START_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient should now be in the middle state
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, MIDDLE_STATE, TODAY, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("Checkbox should be checked: " + html, html.contains("[X]&#160;MIDDLE STATE"));
			}
			
		}.run();
	}
	
	@Test
	// note that in the future we may want to add the code so that unchecking a checkbox takes a patient out of a state
	// this test case can be modified when we add this functionality
	public void checkboxShouldNotRemovePatientInState() throws Exception {
		
		transitionToState(START_STATE, TWO_YEARS_AGO);
		transitionToState(MIDDLE_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient still be in the middle state
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, null);
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("Checkbox should be checked: " + html, html.contains("[X]&#160;MIDDLE STATE"));
			}
			
		}.run();
	}
	
	@Test
	public void hiddenTagShouldSetPatientInState() throws Exception {
		
		transitionToState(START_STATE, ONE_YEAR_AGO);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_HIDDEN_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// hidden tag, so nothing gets submitted
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
			}
			
			@Override
			// note that we have to use an edit encounter to test, as currently the Regression Test helper does not properly mock hidden inputs on initial submittal
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// hidden tag, so nothing gets submitted
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient should now be in the middle state
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, MIDDLE_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldNotEnrollInProgramTwiceIfTwoWorkflowStateSelectorsOnForm() throws Exception {
		//Given: Patient has no workflow state and is not enrolled in the program
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "workflowStateFormWithTwoWorkflows";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:", "AnotherState:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				//When: Html form is entered in which workflow state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
				request.addParameter(widgets.get("AnotherState:"), TEST_STATE7); // workflow state 207 in regressionTestData
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				
				Assert.assertEquals(1, patientPrograms(patient, TEST_PROGRAM).size());
				
				// double check that states have been set
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, START_STATE, TODAY, null);
				assertState(pp, TEST_STATE7, TODAY, null);
			}
		}.run();
	}
	
	@Test
	public void shouldNotEnrollInProgramAgainIfPatientAlreadyEnrolledInProgram() throws Exception {
		
		// create a program enrollment for test patient
		PatientProgram patientProgram = new PatientProgram();
		patientProgram.setPatient(patient);
		patientProgram.setProgram(Context.getProgramWorkflowService().getProgram(10));
		patientProgram.setDateEnrolled(ONE_YEAR_AGO);
		Context.getProgramWorkflowService().savePatientProgram(patientProgram);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				//When: Html form is entered in which workflow state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				
				Assert.assertEquals(1, patientPrograms(patient, TEST_PROGRAM).size());
				
				// double check that state has been set
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, TODAY, null);
			}
		}.run();
	}
	
	@Test
	public void shouldNotEnrollInProgramOnEditIfPatientAlreadyEnrolledInProgram() throws Exception {
		
		// create a program enrollment for test patient
		PatientProgram patientProgram = new PatientProgram();
		patientProgram.setPatient(patient);
		patientProgram.setProgram(Context.getProgramWorkflowService().getProgram(10));
		patientProgram.setDateEnrolled(ONE_YEAR_AGO);
		Context.getProgramWorkflowService().savePatientProgram(patientProgram);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// on first submit, don't submit any state
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				
				// sanity check
				Assert.assertEquals(1, patientPrograms(patient, TEST_PROGRAM).size());
				
				// double check that state has been set
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertNoState(pp, START_STATE);
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				Assert.assertEquals(1, patientPrograms(patient, TEST_PROGRAM).size());
				
				// double check that state has been set
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, TODAY, null);
			}
		}.run();
	}
	
	private void assertNotPresent(FormEntrySession session, String state) throws Exception {
		Assert.assertFalse("No " + state + " in result:" + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains(state));
	}
	
	private void assertPresent(FormEntrySession session, String state) throws Exception {
		Assert.assertTrue(state + " in result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains(state));
	}
	
	// enroll the patient in the program associated with the specified state (but do NOT put the patient in that state)
	private void enrollInProgram(String state, Date date) {
		ProgramWorkflowState workflowState = Context.getProgramWorkflowService().getStateByUuid(state);
		
		PatientProgram patientProgram = getPatientProgramByWorkflow(patient, workflowState.getProgramWorkflow(), date);
		if (patientProgram == null) {
			patientProgram = new PatientProgram();
			patientProgram.setPatient(patient);
			patientProgram.setProgram(workflowState.getProgramWorkflow().getProgram());
			patientProgram.setDateEnrolled(date);
		}
		
		Context.getProgramWorkflowService().savePatientProgram(patientProgram);
	}
	
	private void transitionToState(String state) {
		transitionToState(state, new Date());
	}
	
	private void transitionToState(String state, Date date) {
		transitionToState(state, date, null);
	}
	
	private void transitionToState(String state, Date startDate, Date endDate) {
		ProgramWorkflowState workflowState = Context.getProgramWorkflowService().getStateByUuid(state);
		ProgramWorkflow workflow = workflowState.getProgramWorkflow();
		Program program = workflow.getProgram();
		List<PatientProgram> pps = programWorkflowService.getPatientPrograms(patient, program, null, null, null, null,
		    false);
		PatientProgram patientProgram = null;
		if (pps.size() == 1) {
			patientProgram = pps.get(0);
		} else if (pps.size() > 1) {
			throw new IllegalArgumentException("Multiple patient programs found for patient");
		}
		if (pps.isEmpty()) {
			patientProgram = new PatientProgram();
			patientProgram.setPatient(patient);
			patientProgram.setProgram(workflowState.getProgramWorkflow().getProgram());
			patientProgram.setDateEnrolled(startDate);
		}
		patientProgram.transitionToState(workflowState, startDate);
		if (endDate != null) {
			patientProgram.getCurrentState(workflowState.getProgramWorkflow()).setEndDate(endDate);
		}
		Context.getProgramWorkflowService().savePatientProgram(patientProgram);
	}
	
	private PatientProgram getPatientProgramByWorkflow(Patient patient, ProgramWorkflow workflow, Date activeDate) {
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient,
		    workflow.getProgram(), null, null, null, null, false);
		for (PatientProgram patientProgram : patientPrograms) {
			for (PatientState patientState : patientProgram.getStates()) {
				if (patientState.getState().getProgramWorkflow().equals(workflow)) {
					if (activeDate != null) {
						if (patientState.getActive(activeDate)) {
							return patientProgram;
						}
					} else {
						return patientProgram;
					}
				}
			}
		}
		return null;
	}
	
	protected List<PatientProgram> patientPrograms(Patient patient, String program) {
		Program p = programWorkflowService.getProgramByUuid(program);
		return programWorkflowService.getPatientPrograms(patient, p, null, null, null, null, false);
	}
	
	protected PatientProgram patientProgram(Patient patient, String program, Date programStart, Date programEnd) {
		for (PatientProgram pp : patientPrograms(patient, program)) {
			boolean programStartMatches = OpenmrsUtil.nullSafeEquals(programStart, pp.getDateEnrolled());
			boolean programEndMatches = OpenmrsUtil.nullSafeEquals(programEnd, pp.getDateCompleted());
			if (programStartMatches && programEndMatches) {
				return pp;
			}
		}
		return null;
	}
	
	protected PatientProgram assertProgram(Patient patient, String program, Date programStart, Date programEnd) {
		PatientProgram pp = patientProgram(patient, program, programStart, programEnd);
		Assert.assertNotNull(pp);
		return pp;
	}
	
	protected PatientState assertState(PatientProgram pp, String state, Date stateStart, Date stateEnd) {
		PatientState ret = null;
		for (PatientState ps : pp.getStates()) {
			boolean stateMatches = ps.getState().getUuid().equals(state);
			boolean startMatches = OpenmrsUtil.nullSafeEquals(stateStart, ps.getStartDate());
			boolean endMatches = OpenmrsUtil.nullSafeEquals(stateEnd, ps.getEndDate());
			if (stateMatches && startMatches && endMatches) {
				ret = ps;
			}
		}
		Assert.assertNotNull(ret);
		return ret;
	}
	
	protected void assertNoState(PatientProgram pp, String state) {
		for (PatientState ps : pp.getStates()) {
			if (ps.getState().getUuid().equals(state) && (ps.getVoided() == null || !ps.getVoided())) {
				Assert.fail("Did not expect a state but found one from " + ps.getStartDate() + " to " + ps.getEndDate());
			}
		}
	}
	
	protected int nonVoidedStates(PatientProgram pp) {
		int ret = 0;
		for (PatientState ps : pp.getStates()) {
			if (ps.getVoided() == null || !ps.getVoided()) {
				ret++;
			}
		}
		return ret;
	}
}
