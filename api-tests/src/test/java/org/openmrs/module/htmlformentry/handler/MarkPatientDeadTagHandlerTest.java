package org.openmrs.module.htmlformentry.handler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

public class MarkPatientDeadTagHandlerTest {
	
	private FormSubmissionController submissionController;
	
	private FormEntrySession formEntrySession;
	
	private MarkPatientDeadTagHandler tagHandler;
	
	private Patient patient;
	
	private Encounter encounter;
	
	private PatientService patientService;
	
	private Concept unknownConcept;
	
	private Concept codedCauseOfDeath;
	
	private MockedStatic<HtmlFormEntryUtil> mockedHtmlFormEntryUtil;
	
	@Before
	public void setUp() {
		patient = new Patient();
		encounter = new Encounter();
		encounter.setPatient(patient);
		
		unknownConcept = new Concept();
		codedCauseOfDeath = new Concept(12345);
		
		patientService = mock(PatientService.class);
		submissionController = mock(FormSubmissionController.class);
		
		formEntrySession = mock(FormEntrySession.class);
		when(formEntrySession.getSubmissionController()).thenReturn(submissionController);
		when(formEntrySession.getPatient()).thenReturn(patient);
		when(formEntrySession.getEncounter()).thenReturn(encounter);
		
		mockedHtmlFormEntryUtil = mockStatic(HtmlFormEntryUtil.class);
		when(HtmlFormEntryUtil.getConcept("12345")).thenReturn(codedCauseOfDeath);
		
		tagHandler = new MarkPatientDeadTagHandler() {
			
			@Override
			Concept getUnknownConcept() {
				return unknownConcept;
			}
		};
		tagHandler.setPatientService(patientService);
	}
	
	@After
	public void tearDown() {
		mockedHtmlFormEntryUtil.close();
	}
	
	@Test
	public void testDefaultSetup() throws Exception {
		Map<String, String> arguments = new HashMap<>();
		Assert.assertEquals("", tagHandler.getSubstitution(formEntrySession, submissionController, arguments));
		verify(submissionController).addAction(argThat(isDefaultAction()));
	}
	
	@Test
	public void testNoDateSetup() throws Exception {
		Map<String, String> arguments = new HashMap<>();
		arguments.put("deathDateFromEncounter", "false");
		Assert.assertEquals("", tagHandler.getSubstitution(formEntrySession, submissionController, arguments));
		verify(submissionController).addAction(argThat(actionDoesNotSetDate()));
	}
	
	@Test
	public void testSetupWithShowCheckboxVisible() throws Exception {
		Map<String, String> arguments = new HashMap<>();
		arguments.put("showCheckbox", "true");
		when(formEntrySession.getContext()).thenReturn(mock(FormEntryContext.class));
		when(formEntrySession.getContext().getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		Assert.assertEquals(
		    "<input type=\"checkbox\" id=\"null\" name=\"null\" value=\"true\"/><input type=\"hidden\" name=\"_null\"/>",
		    tagHandler.getSubstitution(formEntrySession, submissionController, arguments));
	}
	
	@Test
	public void testSetupWithShowCheckboxNotVisible() throws Exception {
		Map<String, String> arguments = new HashMap<>();
		arguments.put("showCheckbox", "false");
		when(formEntrySession.getContext()).thenReturn(mock(FormEntryContext.class));
		when(formEntrySession.getContext().getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		Assert.assertEquals("", tagHandler.getSubstitution(formEntrySession, submissionController, arguments));
	}
	
	@Test
	public void testSetupWithCauseOfDeath() throws Exception {
		Map<String, String> arguments = new HashMap<>();
		arguments.put("causeOfDeathFromObs", "12345");
		Assert.assertEquals("", tagHandler.getSubstitution(formEntrySession, submissionController, arguments));
		verify(submissionController).addAction(argThat(actionSetsCauseOfDeathFrom()));
	}
	
	@Test
	public void testSettingDateFromEncounter() {
		Date deathDate = new Date();
		encounter.setEncounterDatetime(deathDate);
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		action.setDeathDateFromEncounter(true);
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertEquals(deathDate, patient.getDeathDate());
		verify(patientService).savePatient(patient);
	}
	
	@Test
	public void testOverridingDateFromEncounter() {
		Date newDeathDate = new Date();
		Date oldDeathDate = new Date(newDeathDate.getTime() - 1000000);
		
		patient.setDeathDate(oldDeathDate);
		encounter.setEncounterDatetime(newDeathDate);
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		action.setDeathDateFromEncounter(true);
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertEquals(newDeathDate, patient.getDeathDate());
		verify(patientService).savePatient(patient);
	}
	
	@Test
	public void testNotOverridingDateFromEncounter() {
		Date newDeathDate = new Date();
		Date oldDeathDate = new Date(newDeathDate.getTime() - 1000000);
		
		patient.setDeathDate(oldDeathDate);
		encounter.setEncounterDatetime(newDeathDate);
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		action.setDeathDateFromEncounter(true);
		action.setPreserveExistingDeathDate(true);
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertEquals(oldDeathDate, patient.getDeathDate());
		verify(patientService).savePatient(patient);
	}
	
	@Test
	public void testNotSettingDate() {
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		action.setDeathDateFromEncounter(true);
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertNull(patient.getDeathDate());
		verify(patientService).savePatient(patient);
	}
	
	@Test
	public void testNotSettingCauseOfDeath() {
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertTrue(unknownConcept == patient.getCauseOfDeath());
		verify(patientService).savePatient(patient);
	}
	
	@Test
	public void testSettingCauseOfDeath() {
		Concept causeOfDeath = new Concept();
		Concept lungCancer = new Concept();
		Obs causeOfDeathObs = new Obs();
		causeOfDeathObs.setConcept(causeOfDeath);
		causeOfDeathObs.setValueCoded(lungCancer);
		encounter.addObs(causeOfDeathObs);
		
		MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
		action.setCauseOfDeathFromObs(causeOfDeath);
		
		action.shouldPatientBeMarkedAsDeceased(formEntrySession, new MockMultipartHttpServletRequest());
		action.applyAction(formEntrySession);
		
		Assert.assertEquals(true, patient.getDead());
		Assert.assertTrue(lungCancer == patient.getCauseOfDeath());
		verify(patientService).savePatient(patient);
	}
	
	private ArgumentMatcher<MarkPatientDeadTagHandler.Action> isDefaultAction() {
		return action -> action.isDeathDateFromEncounter() && !action.isPreserveExistingDeathDate();
	}
	
	private ArgumentMatcher<MarkPatientDeadTagHandler.Action> actionDoesNotSetDate() {
		return action -> !action.isDeathDateFromEncounter();
	}
	
	private ArgumentMatcher<MarkPatientDeadTagHandler.Action> actionSetsCauseOfDeathFrom() {
		return action -> action.getCauseOfDeathFromObs() == codedCauseOfDeath;
	}
}
