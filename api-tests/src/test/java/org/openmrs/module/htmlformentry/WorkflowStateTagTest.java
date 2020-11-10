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
	
	public static final Date ONE_YEAR_IN_FUTURE = DateUtils.addYears(TODAY, 1);
	
	public static final String RETIRED_STATE = "91f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	private Patient patient;
	
	@Autowired
	ProgramWorkflowService programWorkflowService;
	
	@Autowired
	HtmlFormEntryService htmlFormEntryService;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
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
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
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
		//Given: Patient has a workflow state of X starting in Jan 2012
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
				
				//When: Html form is being back entered with an encounter date of June 2011, in which the workflow state selected is Y
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state Y is created with a start date of June 2011 and a stop date of Jan 2012.
				// Workflow state X stays as is.
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
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
		//Given: Patient has a workflow state of X starting in June 2011
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
				//When: Html form is being entered with an encounter date of Jan 2012, in which workflow state Y is selected
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
		//Given: Patient has a workflow state of X starting in June 2011 (still current)
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
				//When: Html form is entered with an encounter date of Jan 2012 in which workflow state X is selected
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
		//Given: Patient has a workflow state of X starting in Jan 2012 (still current)
		transitionToState(START_STATE, TODAY);
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
				//When: Html form is entered with an encounter date of June 2011 in which workflow state X is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: A new workflow state X is created with a Start date of June 2011 and a stop date of Jan 2012
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, START_STATE, ONE_YEAR_AGO, TODAY);
				assertState(pp, START_STATE, TODAY, null);
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateAfterCurrentOnTheSameDay() throws Exception {
		//Given: Patient has a workflow state of X starting in Jan 2012 (active) (still current)
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
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
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
				//When: Html form is entered with an encounter date of Jan 2012 in which workflow state Y is selected
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state X is stopped with a stop date of Jan 2012, Workflow state Y is created with a start date of Jan 2012 and is still current
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
		//Given: Patient has a workflow state of X from June 2011 to Jan 2012, then a workflow state of Y from Jan 2012 to current
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
				//When: Html form is entered with an encounter date of Sept 2011 in which workflow state Z is selected
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow X is changed to a stop date of Sept 2011, Workflow Z is created with a start date of Sept 2011 and a stop date of Jan 2012
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
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
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
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				assertState(pp, MIDDLE_STATE, TODAY, null);
				// assert that the other state no longer exists
				assertNoState(pp, START_STATE);
			}
		}.run();
	}
	
	@Test
	public void shouldMoveProgramEnrollmentAndProgramStateStartEarlier() throws Exception {
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
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
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
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				// assert that the start dates of the program and state have been moved
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, null);
				// assert that the other state no longer exists
				assertNoState(pp, START_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldMoveProgramStateStartLater() throws Exception {
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
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
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
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				// assert that the start date of the program is the same
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				// assert that the start date of the state has moved
				assertState(pp, MIDDLE_STATE, TODAY, null);
				// assert that the other state no longer exists
				assertNoState(pp, START_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldTransitionFromNoStateToSelectedStateOnEdit() throws Exception {
		// first enroll the patient in the program
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
				request.addParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.addParameter(widgets.get("State:"), ""); // set no state
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// make sure the enrollment date has NOT been changed (since no state was set)
				List<PatientProgram> patientPrograms = patientPrograms(patient, TEST_PROGRAM);
				Assert.assertEquals(1, patientPrograms.size());
				PatientProgram patientProgram = patientPrograms.get(0);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(TODAY), dateAsString(patientProgram.getDateEnrolled()));
				
				// assert that no states have been associated
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
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				// first verify that the existing patient program still exists and that the enrollment date has not been changed
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TODAY, null);
				
				// assert that the program has only one state
				Assert.assertEquals(1, nonVoidedStates(pp));
				
				// assert that the start state of the state is correct
				assertState(pp, START_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldCreateNewProgramIfEncounterDateNotDuringProgramEnrollmentOnEdit() throws Exception {
		// first enroll the patient in the program
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
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				// we should now have two program enrollments, one on PAST_DATE and one on DATE
				List<PatientProgram> pps = patientPrograms(results.getPatient(), TEST_PROGRAM);
				Assert.assertEquals(2, pps.size());
				
				// now verify that new state is correct
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, ONE_YEAR_AGO, null);
				
				// assert that the program has only one state
				Assert.assertEquals(1, nonVoidedStates(pp));
				
				// assert that the start state of the state is correct
				assertState(pp, START_STATE, ONE_YEAR_AGO, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldEndExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.addParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// do a sanity check here
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
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, nonVoidedStates(pp));
				
				// verify that the start state now ends on PAST_DATE
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				
				// verify that the middle state starts on PAST_DATE and has no current end date
				assertState(pp, MIDDLE_STATE, ONE_YEAR_AGO, null);
			}
			
		}.run();
	}
	
	@Test
	public void shouldRemoveExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		
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
				request.setParameter(widgets.get("Date:"), dateAsString(ONE_YEAR_AGO));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				//Since moving into terminal state, this should end the program
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, nonVoidedStates(pp));
				
				// verify that the start state
				assertState(pp, START_STATE, TWO_YEARS_AGO, ONE_YEAR_AGO);
				
				// verify that the end state starts on PAST_DATE
				assertState(pp, END_STATE, ONE_YEAR_AGO, null);
				
				// verify that the middle state no longer exists
				assertNoState(pp, MIDDLE_STATE);
			}
			
		}.run();
	}
	
	@Test
	public void shouldShiftExistingStateEndDateForwardAsNeededWhenShiftingStateLater() throws Exception {
		
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
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				PatientProgram pp = assertProgram(results.getPatient(), TEST_PROGRAM, TWO_YEARS_AGO, null);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, nonVoidedStates(pp));
				
				// verify that the start state
				assertState(pp, START_STATE, TWO_YEARS_AGO, TODAY);
				
				// verify that the end state starts on DATE
				assertState(pp, END_STATE, TODAY, null);
			}
			
		}.run();
	}
	
	@Test(expected = FormEntryException.class)
	public void shouldFailIfAttemptingToShiftStateStartDatePastEndDate() throws Exception {
		
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
				request.setParameter(widgets.get("Date:"), dateAsString(TODAY));
				request.setParameter(widgets.get("State:"), START_STATE);
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
		ProgramWorkflowState workflowState = Context.getProgramWorkflowService().getStateByUuid(state);
		
		PatientProgram patientProgram = getPatientProgramByWorkflow(patient, workflowState.getProgramWorkflow(), date);
		if (patientProgram == null) {
			patientProgram = new PatientProgram();
			patientProgram.setPatient(patient);
			patientProgram.setProgram(workflowState.getProgramWorkflow().getProgram());
			patientProgram.setDateEnrolled(date);
		}
		
		patientProgram.transitionToState(workflowState, date);
		
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
