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
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 *
 */
public class WorkflowStateTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	public static final String START_STATE = "89d1a292-5140-11e1-a3e3-00248140a5eb";
	
	public static final String MIDDLE_STATE = "99f66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String END_STATE = "8ef66ca8-5140-11e1-a3e3-00248140a5eb";
	
	public static final String DIFFERENT_PROGRAM_STATE = "72a90efc-5140-11e1-a3e3-00248140a5eb";
	
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
		String htmlform = "<htmlform><workflowState workflowId=\"100\" style=\"checkbox\"/></htmlform>";
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
		assertPresent(session, "<spring:message code=\"" + label + "\" />");
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
		ProgramWorkflowState workflowState = Context.getProgramWorkflowService().getStateByUuid(state);
		
		PatientProgram patientProgram = new PatientProgram();
		patientProgram.setPatient(patient);
		patientProgram.setProgram(workflowState.getProgramWorkflow().getProgram());
		
		PatientState patientState = new PatientState();
		patientState.setPatientProgram(patientProgram);
		patientState.setState(workflowState);
		patientProgram.getStates().add(patientState);
		
		Context.getProgramWorkflowService().savePatientProgram(patientProgram);
	}
	
}
