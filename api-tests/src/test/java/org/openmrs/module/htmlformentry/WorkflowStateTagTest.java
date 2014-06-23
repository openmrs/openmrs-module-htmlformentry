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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 *
 */
public class WorkflowStateTagTest extends BaseModuleContextSensitiveTest {
	
	public static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	public static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	public static final String XML_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	public static final String XML_FORM_NAME = "workflowStateForm";
	
	public static final String XML_CHECKBOX_FORM_NAME = "workflowStateCheckboxForm";
	
	public static final String XML_HIDDEN_FORM_NAME = "workflowStateHiddenForm";
	
	public static final String START_STATE = "89d1a292-5140-11e1-a3e3-00248140a5eb";
	
	public static final String MIDDLE_STATE = "99f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String END_STATE = "8ef66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String DIFFERENT_PROGRAM_WORKFLOW_STATE = "67337cdc-53ad-11e1-8cb6-00248140a5eb";
	
	public static final String MAPPED_STATE = "6de7ed10-53ad-11e1-8cb6-00248140a5eb";
	
	public static final Date DATE = HtmlFormEntryUtil.clearTimeComponent(new Date());
	
	public static final Date PAST_DATE = HtmlFormEntryUtil.clearTimeComponent(new Date(DATE.getTime() - 31536000000L));
	
	public static final Date FURTHER_PAST_DATE = HtmlFormEntryUtil.clearTimeComponent(new Date(PAST_DATE.getTime() - 31536000000L));
	
	public static final Date FUTURE_DATE = HtmlFormEntryUtil.clearTimeComponent(new Date(DATE.getTime() + 31536000000L));
	
	private static final String RETIRED_STATE = "91f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	private Patient patient;
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
		LogicUtil.registerDefaultRules();
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
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_TEST_DATASET));	
		String htmlform = "<htmlform><workflowState workflowId=\"108\" stateId=\"SNOMED CT:Test Code\"/></htmlform>";
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state Y is created with a start date of the encounter date
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientProgram.getDateEnrolled()));
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateBeforeAnotherState() throws Exception {
		//Given: Patient has a workflow state of X starting in Jan 2012
		transitionToState(END_STATE, DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state Y is created with a start date of June 2011 and a stop date of Jan 2012. Workflow state X stays as is.
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				state = Context.getProgramWorkflowService().getStateByUuid(END_STATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
		}.run();
		
	}
	
	@Test
	public void shouldTransitionToStateAfterAnotherState() throws Exception {
		//Given: Patient has a workflow state of X starting in June 2011
		transitionToState(START_STATE, PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
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
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				patientState = getPatientState(patientProgram, state, DATE);
				
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
		}.run();
	}
	
	@Test
	public void shouldNotTransitionToSameState() throws Exception {
		//Given: Patient has a workflow state of X starting in June 2011 (still current)
		transitionToState(START_STATE, PAST_DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: No change to workflow state
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
			}
		}.run();
	}
	
	@Test
	public void shouldTransitionToStateAfterCurrent() throws Exception {
		//Given: Patient has a workflow state of X starting in Jan 2012 (still current)
		transitionToState(START_STATE, DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: A new workflow state X is created with a Start date of June 2011 and a stop date of Jan 2012
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, null);
				PatientState patientState = getPatientState(patientProgram, state, null);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow state X is stopped with a stop date of Jan 2012, Workflow state Y is created with a start date of Jan 2012 and is still current
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, null);
				PatientState patientState = getPatientState(patientProgram, state, null);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				patientProgram = getPatientProgramByState(results.getPatient(), state, null);
				patientState = getPatientState(patientProgram, state, null);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">MIDDLE STATE"));
			}
		}.run();
	}
	
	@Test
	public void shouldTransitonToStateInBetweenStates() throws Exception {
		//Given: Patient has a workflow state of X from June 2011 to Jan 2012, then a workflow state of Y from Jan 2012 to current
		transitionToState(START_STATE, PAST_DATE);
		transitionToState(END_STATE, FUTURE_DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: Workflow X is changed to a stop date of Sept 2011, Workflow Z is created with a start date of Sept 2011 and a stop date of Jan 2012
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(FUTURE_DATE), dateAsString(patientState.getEndDate()));
				
				state = Context.getProgramWorkflowService().getStateByUuid(END_STATE);
				patientProgram = getPatientProgramByState(results.getPatient(), state, FUTURE_DATE);
				patientState = getPatientState(patientProgram, state, FUTURE_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(FUTURE_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("MIDDLE STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public void testEditFormHtml(String html) {
				Assert.assertTrue("Edit should contain current state: " + html,
				    html.contains("selected=\"true\">MIDDLE STATE"));
			}
		}.run();
	}
	
	@Test
	public void shouldNotTransitionIfNotAnswered() throws Exception {
		//Given: Patient has a workflow state of X
		transitionToState(START_STATE, PAST_DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), "");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				//Then: No action
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(DATE));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
				
				// assert that the other state no longer exists
				state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNull(patientState);
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			public void testEditedResults(SubmissionResults results) {
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				
				// assert that the start dates of the program and state have been moved
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientProgram.getDateEnrolled()));
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
				
				// assert that the other state no longer exists
				state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNull(patientState);
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
				request.addParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.addParameter(widgets.get("State:"), START_STATE);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(DATE));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				
				// assert that the start date of the program is the same
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientProgram.getDateEnrolled()));
				
				// assert that the start date of the state has moved
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
				
				// assert that the other state no longer exists
				state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNull(patientState);
			}
			
		}.run();
	}
	
	@Test
	public void shouldTransitionFromNoStateToSelectedStateOnEdit() throws Exception {
		// first enroll the patient in the program
		enrollInProgram(START_STATE, DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.addParameter(widgets.get("State:"), "");  // set no state
			}
			
			@SuppressWarnings("deprecation")
            @Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// make sure the enrollment date has NOT been changed (since no state was set)
				ProgramWorkflow workflow = Context.getProgramWorkflowService().getWorkflow(100);
				Assert.assertEquals(1, Context.getProgramWorkflowService().getPatientPrograms(patient, workflow.getProgram(), null, null, null, null, false).size());
				PatientProgram patientProgram = Context.getProgramWorkflowService().getPatientPrograms(patient, workflow.getProgram(), null, null, null, null, false).get(0);
				Assert.assertNotNull(patientProgram);	
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientProgram.getDateEnrolled()));
				
				// assert that no states have been associated
				Assert.assertEquals(0, patientProgram.getStates().size());
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(DATE));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				
				// first verify that the existing patient program still exists and that the enrollment date has not been changed
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientProgram.getDateEnrolled()));
				
				// assert that the program has only one state
				Assert.assertEquals(1, patientProgram.getStates().size());
				
				// assert that the start state of the state is correct
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
		}.run();
	}
	
	
	@Test
	public void shouldCreateNewProgramIfEncounterDateNotDuringProgramEnrollmentOnEdit() throws Exception {
		// first enroll the patient in the program
		enrollInProgram(START_STATE, DATE);
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), "");  // set no state
			}
		
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
			
			@SuppressWarnings("deprecation")
            public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				
				// we should now have two program enrollments, one on PAST_DATE and one on DATE
				ProgramWorkflow workflow = Context.getProgramWorkflowService().getWorkflow(100);
				Assert.assertEquals(2, Context.getProgramWorkflowService().getPatientPrograms(patient, workflow.getProgram(), null, null, null, null, false).size());
				
				// now verify that new state is correct
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientProgram.getDateEnrolled()));
				
				// assert that the program has only one state
				Assert.assertEquals(1, patientProgram.getStates().size());
				
				// assert that the start state of the state is correct
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
		}.run();
	}
	
	@Test
	public void shouldEndExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), END_STATE);   
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// do a sanity check here
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(END_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.setParameter(widgets.get("State:"), MIDDLE_STATE);
			}
			
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflowState startState = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				ProgramWorkflowState middleState = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), startState, FURTHER_PAST_DATE);
				Assert.assertNotNull(patientProgram);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, patientProgram.getStates().size());
				
				// verify that the start state now ends on PAST_DATE
				PatientState patientState = getPatientState(patientProgram, startState, FURTHER_PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(FURTHER_PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getEndDate()));
				
				// verify that the middle state starts on PAST_DATE and has no current end date
				patientState = getPatientState(patientProgram, middleState, PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
		}.run();
	}
	
	
	@Test
	public void shouldRemoveExistingStatesAsNeededWhenShiftingStateEarlier() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		transitionToState(MIDDLE_STATE, PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), END_STATE);   
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@SuppressWarnings("deprecation")
            public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflow workflow = Context.getProgramWorkflowService().getWorkflow(100);
				
				ProgramWorkflowState startState = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				ProgramWorkflowState middleState = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				ProgramWorkflowState endState = Context.getProgramWorkflowService().getStateByUuid(END_STATE);
				
				PatientProgram patientProgram = Context.getProgramWorkflowService().getPatientPrograms(patient, workflow.getProgram(), null, null, null, null, false).get(0);
				Assert.assertNotNull(patientProgram);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, patientProgram.statesInWorkflow(workflow, false).size());
				
				// verify that the start state
				PatientState patientState = getPatientState(patientProgram, startState, FURTHER_PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(FURTHER_PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getEndDate()));
				
				// verify that the end state starts on PAST_DATE and has no current end date
				patientState = getPatientState(patientProgram, endState, PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
				
				// verify that the middle state no longer exists
				patientState = getPatientState(patientProgram, middleState, PAST_DATE);
				Assert.assertNull(patientState);
			}
			
		}.run();
	}
	
	@Test
	public void shouldShiftExistingStateEndDateFowardAsNeededWhenShiftingStateLater() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(PAST_DATE));
				request.addParameter(widgets.get("State:"), END_STATE);   
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("END STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(DATE));
				request.setParameter(widgets.get("State:"), END_STATE);
			}
			
			@SuppressWarnings("deprecation")
            public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				ProgramWorkflow workflow = Context.getProgramWorkflowService().getWorkflow(100);
				
				ProgramWorkflowState startState = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
				ProgramWorkflowState endState = Context.getProgramWorkflowService().getStateByUuid(END_STATE);
				
				PatientProgram patientProgram = Context.getProgramWorkflowService().getPatientPrograms(patient, workflow.getProgram(), null, null, null, null, false).get(0);
				Assert.assertNotNull(patientProgram);
				
				// assert that the patient program only has two states
				Assert.assertEquals(2, patientProgram.statesInWorkflow(workflow, false).size());
				
				// verify that the start state
				PatientState patientState = getPatientState(patientProgram, startState, FURTHER_PAST_DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(FURTHER_PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getEndDate()));
				
				// verify that the end state starts on DATE and has no current end date
				patientState = getPatientState(patientProgram, endState, DATE);
				Assert.assertNotNull(patientState);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
		}.run();
	}
	
	@Test(expected = FormEntryException.class)
	public void shouldFailIfAttemptingToShiftStateStartDatePastEndDate() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		transitionToState(END_STATE, PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(FURTHER_PAST_DATE));
				request.addParameter(widgets.get("State:"), "");   
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("View should contain current state: " + html, html.contains("START STATE"));
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:", "State:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Date:"), dateAsString(DATE));
				request.setParameter(widgets.get("State:"), START_STATE);
			}
		}.run();
	}
	
	@Test
	public void checkboxShouldAppearCheckedIfCurrentlyInSpecifiedState() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		transitionToState(MIDDLE_STATE, PAST_DATE);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public void testBlankFormHtml(String html) {				
				Pattern p = Pattern.compile(".*checked=\"true\"/><label for=\".*\">MIDDLE STATE.*", Pattern.MULTILINE | Pattern.DOTALL);
				Assert.assertTrue("Checkbox should be checked: " + html, p.matcher(html).matches());
			}
			
		}.run();
	}
	
	@Test
	public void checkboxShouldNotAppearCheckedIfNotCurrentlyInSpecifiedState() throws Exception {
		 
		transitionToState(START_STATE, PAST_DATE);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_CHECKBOX_FORM_NAME;
			}
			
			@Override
			public void testBlankFormHtml(String html) {		
				Assert.assertTrue("Checkbox should not be checked: " + html, !html.contains("checked=\"true\"/>MIDDLE STATE"));
			}
			
		}.run();
	}	

	@Test
	public void checkboxShouldSetPatientInState() throws Exception {
		 
		transitionToState(START_STATE, PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), MIDDLE_STATE);   
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient should now be in the middle state
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("Checkbox should be checked: " + html, html.contains("[X]&#160;MIDDLE STATE"));
			}
			
		}.run();
	}
	
	
	@Test
	// note that in the future we may want to add the code so that unchecking a checkbox takes a patient out of a state
	// this test case can be modified when we add this functionality
	public void checkboxShouldNotRemovePatientInState() throws Exception {
		 
		transitionToState(START_STATE, FURTHER_PAST_DATE);
		transitionToState(MIDDLE_STATE, PAST_DATE);
		
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
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));
				request.addParameter(widgets.get("State:"), "");   
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient still be in the middle state
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, PAST_DATE);
				PatientState patientState = getPatientState(patientProgram, state, PAST_DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
			}
			
			public boolean doViewEncounter() {
				return true;
			}
			
			public void testViewingEncounter(Encounter encounter, String html) {
				Assert.assertTrue("Checkbox should be checked: " + html, html.contains("[X]&#160;MIDDLE STATE"));
			}
			
		}.run();
	}
	
	@Test
	public void hiddenTagShouldSetPatientInState() throws Exception {
		 
		transitionToState(START_STATE, PAST_DATE);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return XML_HIDDEN_FORM_NAME;
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:"};
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// hidden tag, so nothing gets submitted
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));   
			}
			
			@Override
			// note that we have to use an edit encounter to test, as currently the Regression Test helper does not properly mock hidden inputs on initial submittal
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:", "Provider:"};
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// hidden tag, so nothing gets submitted
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Date:"), dateAsString(DATE));   
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// patient should now be in the middle state
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(MIDDLE_STATE);
				PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
				PatientState patientState = getPatientState(patientProgram, state, DATE);
				Assert.assertNotNull(patientProgram);
				Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
				Assert.assertNull(patientState.getEndDate());
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
                request.addParameter(widgets.get("Date:"), dateAsString(DATE));
                request.addParameter(widgets.get("State:"), START_STATE);
                request.addParameter(widgets.get("AnotherState:"), "67337cdc-53ad-11e1-8cb6-00248140a5eb");    // workflow state 207 in regressionTestData
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();

                Assert.assertEquals(1, Context.getProgramWorkflowService().getPatientPrograms(patient, Context.getProgramWorkflowService().getProgram(10), null, null, null, null, false).size());

                // double check that states have been set
                ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
                ProgramWorkflowState anotherState = Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11e1-8cb6-00248140a5eb");
                PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
                PatientState patientState = getPatientState(patientProgram, state, DATE);
                PatientState anotherPatientState = getPatientState(patientProgram, anotherState, DATE);

                Assert.assertNotNull(patientProgram);
                Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
                Assert.assertEquals(dateAsString(DATE), dateAsString(patientProgram.getDateEnrolled()));
                Assert.assertEquals(dateAsString(DATE), dateAsString(anotherPatientState.getStartDate()));

            }
        }.run();
    }

    @Test
    public void shouldNotEnrollInProgramAgainIfPatientAlreadyEnrolledInProgram() throws Exception {

        // create a program enrollment for test patient
        PatientProgram patientProgram = new PatientProgram();
        patientProgram.setPatient(patient);
        patientProgram.setProgram(Context.getProgramWorkflowService().getProgram(10));
        patientProgram.setDateEnrolled(PAST_DATE);
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
                request.addParameter(widgets.get("Date:"), dateAsString(DATE));
                request.addParameter(widgets.get("State:"), START_STATE);
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();

                Assert.assertEquals(1, Context.getProgramWorkflowService().getPatientPrograms(patient, Context.getProgramWorkflowService().getProgram(10), null, null, null, null, false).size());

                // double check that state has been set
                ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
                PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
                PatientState patientState = getPatientState(patientProgram, state, DATE);

                Assert.assertNotNull(patientProgram);
                Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
                Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientProgram.getDateEnrolled()));
            }
        }.run();
    }

    @Test
    public void shouldNotEnrollInProgramOnEditIfPatientAlreadyEnrolledInProgram() throws Exception {

        // create a program enrollment for test patient
        PatientProgram patientProgram = new PatientProgram();
        patientProgram.setPatient(patient);
        patientProgram.setProgram(Context.getProgramWorkflowService().getProgram(10));
        patientProgram.setDateEnrolled(PAST_DATE);
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
                request.addParameter(widgets.get("Date:"), dateAsString(DATE));
                request.addParameter(widgets.get("State:"), "");
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();

                // sanity check
                Assert.assertEquals(1, Context.getProgramWorkflowService().getPatientPrograms(patient, Context.getProgramWorkflowService().getProgram(10), null, null, null, null, false).size());

                // sanity check: program should not yet be in the start state
                ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
                PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
                Assert.assertNull(patientProgram);
            }

            public boolean doEditEncounter() {
                return true;
            }

            public String[] widgetLabelsForEdit() {
                return new String[] { "Date:", "Location:", "Provider:", "State:" };
            }

            public void setupEditRequest(MockHttpServletRequest request, Map<String,String> widgets) {
                request.setParameter(widgets.get("Location:"), "2");
                request.setParameter(widgets.get("Provider:"), "502");
                request.setParameter(widgets.get("Date:"), dateAsString(DATE));
                request.setParameter(widgets.get("State:"), START_STATE);
            }

            public void testEditedResults(SubmissionResults results) {
                results.assertNoErrors();

                Assert.assertEquals(1, Context.getProgramWorkflowService().getPatientPrograms(patient, Context.getProgramWorkflowService().getProgram(10), null, null, null, null, false).size());

                // double check that state has been set
                ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
                PatientProgram patientProgram = getPatientProgramByState(results.getPatient(), state, DATE);
                PatientState patientState = getPatientState(patientProgram, state, DATE);

                Assert.assertNotNull(patientProgram);
                Assert.assertEquals(dateAsString(DATE), dateAsString(patientState.getStartDate()));
                Assert.assertEquals(dateAsString(PAST_DATE), dateAsString(patientProgram.getDateEnrolled()));
            }
        }.run();
    }

    /**
	 * @param session
	 */
	private void assertNotPresent(FormEntrySession session, String state) throws Exception {
		Assert.assertFalse("No " + state + " in result:" + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains(state));
	}
	
	/**
	 * @param session
	 */
	private void assertPresent(FormEntrySession session, String state) throws Exception {
		Assert.assertTrue(state + " in result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains(state));
	}
	
	// enroll the patient in the program associated with the specified state (but do NOT put the patient in that state)
	@SuppressWarnings("unused")
    private void enrollInProgram(String state) {
		enrollInProgram(state, new Date());
	}
	
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
	
	/**
	 * @param results
	 * @param state
	 * @return
	 */
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
	
	/**
	 * @param results
	 * @param state
	 * @return
	 */
	private PatientProgram getPatientProgramByState(Patient patient, ProgramWorkflowState state, Date activeDate) {
		List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(patient,
		    state.getProgramWorkflow().getProgram(), null, null, null, null, false);
		for (PatientProgram patientProgram : patientPrograms) {
			for (PatientState patientState : patientProgram.getStates()) {
				if (patientState.getState().equals(state)) {
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
	
	/**
	 * @param patientProgram
	 * @param state
	 * @param activeDate
	 * @return
	 */
	private PatientState getPatientState(PatientProgram patientProgram, ProgramWorkflowState state, Date activeDate) {
		for (PatientState patientState : patientProgram.getStates()) {
			if (patientState.getState().equals(state)) {
				if (activeDate != null) {
					if (patientState.getActive(activeDate)) {
						return patientState;
					}
				} else {
					return patientState;
				}
			}
		}
		return null;
	}
	
}
