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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.ConceptMap;
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
	
	public static final String START_STATE = "89d1a292-5140-11e1-a3e3-00248140a5eb";
	
	public static final String MIDDLE_STATE = "99f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String END_STATE = "8ef66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String DIFFERENT_PROGRAM_STATE = "72a90efc-5140-11e1-a3e3-00248140a5eb";
	
	public static final Date DATE = new Date();
	
	public static final Date PAST_DATE = new Date(DATE.getTime() - 31536000000L);
	
	public static final Date FUTURE_DATE = new Date(DATE.getTime() + 31536000000L);
	
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
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayOnlyInitialStateIfNotEnrolled() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		assertNotPresent(session, MIDDLE_STATE);
		assertNotPresent(session, END_STATE);
	}
	
	@Test
	public void shouldDisplayAllStatesIfInInitialState() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		assertPresent(session, MIDDLE_STATE);
		assertPresent(session, END_STATE);
	}
	
	@Test
	public void shouldNotDisplayRetiredStates() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertNotPresent(session, RETIRED_STATE);
	}
	
	@Test
	public void shouldDisplayDropdownIfNoStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("option"));
	}
	
	@Test
	public void shouldDisplayDropdownIfDropdownStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("option"));
	}
	
	@Test
	public void shouldDisplayRadioIfRadioStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"radio\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("type=\"radio\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStateIdAndCheckboxStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"checkbox\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStateIdAndHiddenStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"hidden\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfInvalidStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"invalid\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStates() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		assertPresent(session, END_STATE);
		assertNotPresent(session, MIDDLE_STATE);
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStatesGivenIds() throws Exception {
		transitionToState(START_STATE);
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"200,201\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		assertPresent(session, END_STATE);
		assertNotPresent(session, MIDDLE_STATE);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedInvalidStates() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + ",some invalid state,"
		        + END_STATE + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedDifferentProgramStates() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + ","
		        + DIFFERENT_PROGRAM_STATE + "," + END_STATE + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayOnlySpecifiedStateAndDefaultToCheckbox() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		assertNotPresent(session, MIDDLE_STATE);
		assertNotPresent(session, END_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("checkbox"));
	}
	
	@Test
	public void shouldDisplayStateSpecifiedByMapping() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_TEST_DATASET));
		
		ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(START_STATE);
		
		ConceptMap conceptMap = new ConceptMap();
		conceptMap.setConcept(state.getConcept());
		conceptMap.setSource(Context.getConceptService().getConceptSource(1));
		conceptMap.setSourceCode(state.getConcept().getId().toString());
		
		state.getConcept().addConceptMapping(conceptMap);
		Context.getConceptService().saveConcept(state.getConcept());
		
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"XYZ:10002\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
	}
	
	@Test
	public void shouldDisplayIfSpecifiedStateAndCheckboxStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" style=\"checkbox\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("checkbox"));
	}
	
	@Test
	public void shouldDisplayIfSpecifiedStateAndHiddenStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" style=\"hidden\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, START_STATE);
		Assert.assertTrue("Checkbox result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("hidden"));
	}
	
	@Test
	public void shouldDisplaySpecifiedStateWithWhitespaces() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"    " + START_STATE
		        + "      \"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedStateAndDropdownStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" style=\"dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedStateAndRadioStyle() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE
		        + "\" style=\"radio\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedInvalidState() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"invalid\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfSpecifiedDifferentProgramState() throws Exception {
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + DIFFERENT_PROGRAM_STATE
		        + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String endState = "Transition to end state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + ", " + endState + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, startState);
		assertPresent(session, endState);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNotEnoughStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfTooManyStateLabels() throws Exception {
		transitionToState(START_STATE);
		
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "," + END_STATE
		        + "\" stateLabels=\"" + startState + ",someState,someOtherState\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		assertPresent(session, startState);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateIdAndStateLabels() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabels=\""
		        + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateIdsAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateIds=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStatesAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateLabel=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoStatesAndStateLabels() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfStateLabelsAndStateLabel() throws Exception {
		String startState = "Transition to start state";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" stateId=\"" + START_STATE + "\" stateLabel=\""
		        + startState + "\" stateLabels=\"" + startState + "\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldDisplayLabel() throws Exception {
		String label = "Some label text";
		String htmlform = "<htmlform><workflowState workflowId=\"100\" label=\"" + label + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
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
		}.run();
	}
	
	/**
	 * @param session
	 */
	private void assertNotPresent(FormEntrySession session, String state) {
		Assert.assertFalse("No " + state + " in result:" + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains(state));
	}
	
	/**
	 * @param session
	 */
	private void assertPresent(FormEntrySession session, String state) {
		Assert.assertTrue(state + " in result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains(state));
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
