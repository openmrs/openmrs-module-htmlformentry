package org.openmrs.module.htmlformentry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests of the various program-related tags
 */
public class EnrollInProgramTagTest extends BaseHtmlFormEntryTest {
	
	PatientService ps;
	
	ProgramWorkflowService pws;
	
	@Before
	public void loadConcepts() throws Exception {
		ps = Context.getPatientService();
		pws = Context.getProgramWorkflowService();
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void enrollInProgram_shouldEnrollInProgramOnEncounterDate() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldModifyEnrollmentDateOnEdit() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		
		final Date originalEncounterDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 00, 31);
		final Date earlierEncounterDate = cal.getTime();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(originalEncounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the original encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(originalEncounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// move the encounter date earlier
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(earlierEncounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterEdited();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the earlier encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(earlierEncounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
			
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldEnrollInProgramOnEncounterDateIfNoDateSpecified() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithShowDate";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldSetDateEnrolledAndTheInitialStatesWhenEnrollingAPatientInAProgram() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 00, 31);
		final Date enrollmentDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(enrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the user selected date should have been set
				Assert.assertEquals(ymdToDate(dateAsString(enrollmentDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
				
				//the initial state should have been set
				Assert.assertEquals(1, pps.get(0).getStates().size());
				Assert.assertEquals(200,
				    pps.get(0).getCurrentState(pws.getWorkflowByUuid("72a90efc-5140-11e1-a3e3-00248140a5eb")).getState()
				            .getId().intValue());
			}
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldNotMoveEnrollmentDateIfEnrolledBeforeNewEnrollmentDate() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/ProgramTagTest-otherPatientStates.xml");
		final Integer patientId = 2;
		final Integer idForStateStartedOnEnrollmentDate = 10;
		final Integer patientProgramId = 1;
		final Date encounterDate = new Date();
		final Date currentEnrollmentDate = pws.getPatientProgram(patientProgramId).getDateEnrolled();
		final Date newEnrollmentDate = new Date();
		
		//sanity check to make sure the test is valid
		Assert.assertTrue(currentEnrollmentDate.compareTo(newEnrollmentDate) < 0);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "editProgramDateEnrolledForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(newEnrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				PatientProgram pp = pws.getPatientProgram(patientProgramId);
				
				//the user selected date should NOT have been set, as it is after the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(currentEnrollmentDate)),
				    ymdToDate(dateAsString(pp.getDateEnrolled())));
				
				// make sure the state start dates have not been changed to the new enrollment date
				boolean stateStartedDuringEnrollmentHasNotChange = false;
				boolean otherStateHasNotChanged = false;
				for (PatientState patientState : pp.getStates()) {
					if (idForStateStartedOnEnrollmentDate.equals(patientState.getId())) {
						Assert.assertEquals(ymdToDate(dateAsString(currentEnrollmentDate)),
						    ymdToDate(dateAsString(patientState.getStartDate())));
						stateStartedDuringEnrollmentHasNotChange = true;
					} else {
						Assert.assertNotSame(ymdToDate(dateAsString(newEnrollmentDate)),
						    ymdToDate(dateAsString(patientState.getStartDate())));
						otherStateHasNotChanged = true;
					}
				}
				Assert.assertTrue(stateStartedDuringEnrollmentHasNotChange);
				Assert.assertTrue(otherStateHasNotChanged);
			}
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldMoveEnrollmentDateIfEnrolledAfterNewEnrollmentDate() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/ProgramTagTest-otherPatientStates.xml");
		final Integer patientId = 2;
		final Integer idForStateStartedOnEnrollmentDate = 10;
		final Integer patientProgramId = 1;
		final Date encounterDate = new Date();
		final Date currentEnrollmentDate = pws.getPatientProgram(patientProgramId).getDateEnrolled();
		Calendar cal = Calendar.getInstance();
		cal.set(2004, 00, 31);
		final Date newEnrollmentDate = cal.getTime();
		
		//sanity check to make sure the test is valid
		Assert.assertTrue(currentEnrollmentDate.compareTo(newEnrollmentDate) > 0);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "editProgramDateEnrolledForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(newEnrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				PatientProgram pp = pws.getPatientProgram(patientProgramId);
				
				//the user selected date shouldhave been set, as it is before the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(newEnrollmentDate)),
				    ymdToDate(dateAsString(pp.getDateEnrolled())));
				
				//the start dates for all states with start date equal to the enrollment date should have been
				//changed to the new enrollment date
				boolean foundStateStartedDuringEnrollment = false;
				boolean skippedOtherState = false;
				for (PatientState patientState : pp.getStates()) {
					if (idForStateStartedOnEnrollmentDate.equals(patientState.getId())) {
						Assert.assertEquals(ymdToDate(dateAsString(newEnrollmentDate)),
						    ymdToDate(dateAsString(patientState.getStartDate())));
						foundStateStartedDuringEnrollment = true;
					} else {
						Assert.assertNotSame(ymdToDate(dateAsString(newEnrollmentDate)),
						    ymdToDate(dateAsString(patientState.getStartDate())));
						skippedOtherState = true;
					}
				}
				Assert.assertTrue(foundStateStartedDuringEnrollment);
				Assert.assertTrue(skippedOtherState);
			}
		}.run();
	}
	
	@Test(expected = FormEntryException.class)
	public void enrollInProgram_shouldFailIfThereAreMultipleStatesInTheSameWorkflow() throws Exception {
		final Integer patientId = 2;
		final Date encounterDate = new Date();
		final Date enrollmentDate = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "invalidPatientStatesForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(enrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldNotDisplayEnrollmentDateWidgetIfShowDateFalse() throws Exception {
		String htmlform = "<htmlform>Enroll In Program: <enrollInProgram programId=\"1\" showDate=\"false\" stateIds=\"2\" /><submit/></htmlform>";
		FormEntrySession session = new FormEntrySession(Context.getPatientService().getPatient(2), htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("setupDatePicker") == -1);
	}
	
	@Test
	public void enrollInProgram_shouldEnrollAPatientWhenTheStateIsDefinedByAConceptMapping() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		
		final Date encounterDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 00, 31);
		final Date enrollmentDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramByConceptMappingForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(enrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				// make sure the state has been set
				Set<PatientState> states = pps.get(0).getStates();
				Assert.assertEquals(1, states.size());
				Assert.assertTrue(((PatientState) states.toArray()[0]).getState()
				        .equals(Context.getProgramWorkflowService().getStateByUuid("67337cdc-53ad-11a1-8cb6-00248140a5eb")));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldEnrollAPatientInAProgramWithMultipleWorkflowsWhenSettingSomeAndNotAllStates()
	        throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 00, 31);
		final Date enrollmentDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramWithMultipleWorkflowsForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(enrollmentDate));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the user selected date should have been set
				Assert.assertEquals(ymdToDate(dateAsString(enrollmentDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
				
				//the initial states should have been set for the 2 workflows in the form
				Assert.assertEquals(2, pps.get(0).getCurrentStates().size());
				ProgramWorkflow wf1 = pws.getWorkflowByUuid("67337cdc-53ad-11e1-8cb6-00248140a5eb");
				ProgramWorkflow wf2 = pws.getWorkflowByUuid("6de7ed10-53ad-11e1-8cb6-00248140a5eb");
				Assert.assertNotNull(pps.get(0).getCurrentState(wf1).getState());
				Assert.assertEquals(ymdToDate(dateAsString(enrollmentDate)),
				    ymdToDate(dateAsString(pps.get(0).getCurrentState(wf1).getStartDate())));
				Assert.assertNotNull(pps.get(0).getCurrentState(wf2).getState());
				Assert.assertEquals(ymdToDate(dateAsString(enrollmentDate)),
				    ymdToDate(dateAsString(pps.get(0).getCurrentState(wf2).getStartDate())));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldNotEnrollAPatientIfAlreadyEnrolledOnTheSelectedDate() throws Exception {
		final Integer patientId = 6;
		final Integer programId = 10;
		final Patient patient = Context.getPatientService().getPatient(patientId);
		Assert.assertEquals(0,
		    pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null, null, false).size());
		//enroll the patient in a test program
		PatientProgram pp = new PatientProgram();
		pp.setPatient(patient);
		Program program = pws.getProgram(programId);
		pp.setProgram(pws.getProgram(programId));
		final ProgramWorkflow wf = program.getWorkflow(100);
		final Date initialEnrollmentDate = new Date();
		pp.setDateEnrolled(initialEnrollmentDate);
		pp.transitionToState(wf.getState(200), initialEnrollmentDate);
		pws.savePatientProgram(pp);
		final ProgramWorkflowState originalState = pp.getCurrentState(wf).getState();
		final Integer patientProgramId = pp.getId();
		final Date completionDate = pp.getDateCompleted();
		
		//ensure the program is created
		Assert.assertEquals(1,
		    pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null, null, false).size());
		final Date encounterDate = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramForm";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//sanity check to ensure the patient is still enrolled in program
				Assert.assertNull(completionDate);
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(new Date()));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the user selected date should have been ignored
				Assert.assertEquals(ymdToDate(dateAsString(initialEnrollmentDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
				
				//The patient is still enrolled in the same program
				Assert.assertEquals(1,
				    pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null, null, false).size());
				
				//the state id in the form should have been ignored
				Assert.assertEquals(originalState, pws.getPatientProgram(patientProgramId).getCurrentState(wf).getState());
			}
		}.run();
	}
	
	@Test(expected = FormEntryException.class)
	public void enrollInProgram_shouldFailIfAnyOfTheStatesIsNotMarkedAsInitialAndThePatientIsNotEnrolledInTheProgram()
	        throws Exception {
		String htmlform = "<htmlform>Enroll In Program: <enrollInProgram programId=\"10\" showDate=\"true\" stateIds=\"201\" /><submit/></htmlform>";
		new FormEntrySession(Context.getPatientService().getPatient(6), htmlform, null).getHtmlToDisplay();
	}
	
	@Test
	public void editPatientProgram_shouldNotDisplayCheckboxWidgetIfShowCheckboxAbsent() throws Exception {
		final Integer patientId = 2;
		Patient patient = Context.getPatientService().getPatient(patientId);
		String htmlform = "<htmlform>Enroll In Program: <enrollInProgram programId=\"1\" stateIds=\"2\" /><submit/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("checkbox") == -1);
	}
	
	@Test
	public void enrollInProgram_shouldNotEnrollIfCheckboxUnchecked() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithShowCheckbox";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(0, pps.size());
			}
			
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldEnrollIfCheckboxChecked() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithShowCheckbox";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Check to Enroll:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Check to Enroll:"), "true");
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
			
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldCheckAndDisableCheckboxIfAlreadyEnrolled() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 1;
		Patient patient = Context.getPatientService().getPatient(patientId);
		// Sanity check
		Assert.assertTrue(
		    pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null, null, false).size() > 0);
		String htmlform = "<htmlform>Enroll In Program: <enrollInProgram programId=\"1\" stateIds=\"2\" showCheckbox=\"true\" /><submit/></htmlform>";
		// Create form and test
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertTrue("Did not find expected text in " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("disabled=\"disabled\""));
	}
	
	@Test
	public void enrollInProgram_shouldNotEnrollAPatientIfAlreadyEnrolledOnTheSelectedDateAndCheckboxPresent()
	        throws Exception {
		final Integer patientId = 2;
		final Integer programId = 1;
		final Integer patientProgramId = 1;
		final Patient patient = Context.getPatientService().getPatient(patientId);
		final PatientProgram pp = pws.getPatientProgram(patientProgramId);
		final Program program = Context.getProgramWorkflowService().getProgram(programId);
		final ProgramWorkflow wf = program.getWorkflow(100);
		final ProgramWorkflowState originalState = pp.getCurrentState(wf).getState();
		final Date completionDate = pp.getDateCompleted();
		final Date initialEnrollmentDate = pp.getDateEnrolled();
		
		// Sanity check
		Assert.assertTrue(
		    pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null, null, false).size() > 0);
		final Date encounterDate = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithShowDateAndCheckbox";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:", "Enrollment Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//sanity check to ensure the patient is still enrolled in program
				Assert.assertNull(completionDate);
				request.addParameter(widgets.get("Enrollment Date:"), dateAsString(new Date()));
				
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertTrue(pps.size() > 0);
				
				PatientProgram pp = pws.getPatientProgram(patientProgramId);
				Assert.assertTrue(pps.contains(pp));
				
				//the user selected date should have been ignored
				Assert.assertEquals(ymdToDate(dateAsString(initialEnrollmentDate)),
				    ymdToDate(dateAsString(pp.getDateEnrolled())));
				
				//the state id in the form should have been ignored
				Assert.assertEquals(originalState, pws.getPatientProgram(patientProgramId).getCurrentState(wf).getState());
			}
			
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldNotDisableCheckboxForPreviouslyCompletedProgram() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/ProgramTagTest-completedProgram.xml");
		final Integer patientId = 7;
		final Integer programId = 1;
		Patient patient = Context.getPatientService().getPatient(patientId);
		// Sanity check
		List<PatientProgram> completedPrograms = pws.getPatientPrograms(patient, pws.getProgram(programId), null, null, null,
		    null, false);
		Assert.assertEquals(1, completedPrograms.size());
		Assert.assertFalse(completedPrograms.get(0).getActive());
		// Create form and test
		String htmlform = "<htmlform>Enroll In Program: <enrollInProgram programId=\"1\" stateIds=\"2\" showCheckbox=\"true\" /><submit/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertFalse("Found unexpected content in " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("disabled"));
	}
	
	@Test
	public void enrollInProgram_shouldSetEnrollmentLocationBasedOnLocationTag() throws Exception {
		
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/encounterLocationTest.xml"); // provides a hierarchy of locations and tags
		
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithLocationTag";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "1001"); // 1001 is tagged as a "Visit Location" in encounterLocationTest.xml
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				Assert.assertEquals(1001, (int) pps.get(0).getLocation().getId());
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldSetEnrollmentLocationToParentLocationWithLocationTag() throws Exception {
		
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/encounterLocationTest.xml"); // provides a hierarchy of locations and tags
		
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithLocationTag";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "1010"); // 1010 is *not* tagged as a "Visit Location" in encounterLocationTest.xml, but has ancestors that are
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				Assert.assertEquals(1003, (int) pps.get(0).getLocation().getId()); // 1003 is the parent of 1010 and *is* tagged as a Visit Location
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
		}.run();
	}
	
	@Test
	public void enrollInProgram_shouldNotFailIfNoLocationMatchingTag() throws Exception {
		
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/encounterLocationTest.xml"); // provides a hierarchy of locations and tags
		
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithLocationTag";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2"); // not tagged as a visit location, nor does it have ancestors that are
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				Assert.assertEquals(1, pps.size());
				
				Assert.assertNull(pps.get(0).getLocation());
				
				//the encounter date should have been set as the enrollment date
				Assert.assertEquals(ymdToDate(dateAsString(encounterDate)),
				    ymdToDate(dateAsString(pps.get(0).getDateEnrolled())));
			}
		}.run();
	}
	
	@Test
	public void editPatientProgram_shouldNotChangeEnrollmentDate() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/encounterLocationTest.xml"); // provides a hierarchy of locations and tags
		
		final Integer patientId = 2;
		final Integer patientProgramId = 1;
		final Date currentEnrollmentDate = pws.getPatientProgram(patientProgramId).getDateEnrolled();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithLocationTag";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Enrollment Date:", "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(currentEnrollmentDate));
				request.setParameter(widgets.get("Encounter Location:"), "1001"); // 1001 is tagged as a "Visit Location" in encounterLocationTest.xml, however we don't expect the enrollment location to change
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				PatientProgram pp = Context.getProgramWorkflowService().getPatientProgram(1);
				Assert.assertNull(pp.getLocation());
			}
		}.run();
	}
}
