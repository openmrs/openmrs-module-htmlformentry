package org.openmrs.module.htmlformentry.handler;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HtmlFormEntryUtil.class)
public class MarkPatientDeadTagHandlerTest {

    private FormSubmissionController submissionController;
    private FormEntrySession formEntrySession;
    private MarkPatientDeadTagHandler tagHandler;
    private Patient patient;
    private Encounter encounter;
    private PatientService patientService;
    private Concept unknownConcept;

    @Before
    public void setUp() {
        patient = new Patient();
        encounter = new Encounter();
        encounter.setPatient(patient);

        unknownConcept = new Concept();

        patientService = mock(PatientService.class);
        submissionController = mock(FormSubmissionController.class);

        formEntrySession = mock(FormEntrySession.class);
        when(formEntrySession.getSubmissionController()).thenReturn(submissionController);
        when(formEntrySession.getPatient()).thenReturn(patient);
        when(formEntrySession.getEncounter()).thenReturn(encounter);

        tagHandler = new MarkPatientDeadTagHandler() {
            @Override
            Concept getUnknownConcept() {
                return unknownConcept;
            }
        };
        tagHandler.setPatientService(patientService);
    }

    @Test
    public void testDefaultSetup() throws Exception {
        Map<String, String> arguments = new HashMap<String, String>();
        assertThat(tagHandler.getSubstitution(formEntrySession, submissionController, arguments), is(""));
        verify(submissionController).addAction(argThat(isDefaultAction()));
    }

    @Test
    public void testNoDateSetup() throws Exception {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("deathDateFromEncounter", "false");
        assertThat(tagHandler.getSubstitution(formEntrySession, submissionController, arguments), is(""));
        verify(submissionController).addAction(argThat(actionDoesNotSetDate()));
    }

    @Test
    public void testSetupWithCauseOfDeath() throws Exception {
        int CONCEPT_ID = 12345;
        Concept causeOfDeath = new Concept(CONCEPT_ID);

        mockStatic(HtmlFormEntryUtil.class);
        when(HtmlFormEntryUtil.getConcept("" + CONCEPT_ID)).thenReturn(causeOfDeath);

        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("causeOfDeathFromObs", "" + CONCEPT_ID);
        assertThat(tagHandler.getSubstitution(formEntrySession, submissionController, arguments), is(""));
        verify(submissionController).addAction(argThat(actionSetsCauseOfDeathFrom(causeOfDeath)));
    }

    @Test
    public void testSettingDateFromEncounter() throws Exception {
        Date deathDate = new Date();
        encounter.setEncounterDatetime(deathDate);
        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
        action.setDeathDateFromEncounter(true);

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getDeathDate(), is(deathDate));
        verify(patientService).savePatient(patient);
    }

    @Test
    public void testOverridingDateFromEncounter() throws Exception {
        Date newDeathDate = new Date();
        Date oldDeathDate = new Date(newDeathDate.getTime() - 1000000);

        patient.setDeathDate(oldDeathDate);
        encounter.setEncounterDatetime(newDeathDate);
        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
        action.setDeathDateFromEncounter(true);

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getDeathDate(), is(newDeathDate));
        verify(patientService).savePatient(patient);
    }

    @Test
    public void testNotOverridingDateFromEncounter() throws Exception {
        Date newDeathDate = new Date();
        Date oldDeathDate = new Date(newDeathDate.getTime() - 1000000);

        patient.setDeathDate(oldDeathDate);
        encounter.setEncounterDatetime(newDeathDate);
        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
        action.setDeathDateFromEncounter(true);
        action.setPreserveExistingDeathDate(true);

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getDeathDate(), is(oldDeathDate));
        verify(patientService).savePatient(patient);
    }

    @Test
    public void testNotSettingDate() throws Exception {
        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
        action.setDeathDateFromEncounter(true);

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getDeathDate(), nullValue());
        verify(patientService).savePatient(patient);
    }

    @Test
    public void testNotSettingCauseOfDeath() throws Exception {
        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getCauseOfDeath(), is(unknownConcept));
        verify(patientService).savePatient(patient);
    }

    @Test
    public void testSettingCauseOfDeath() throws Exception {
        Concept causeOfDeath = new Concept();
        Concept lungCancer = new Concept();
        Obs causeOfDeathObs = new Obs();
        causeOfDeathObs.setConcept(causeOfDeath);
        causeOfDeathObs.setValueCoded(lungCancer);
        encounter.addObs(causeOfDeathObs);

        MarkPatientDeadTagHandler.Action action = tagHandler.newAction();
        action.setCauseOfDeathFromObs(causeOfDeath);

        action.applyAction(formEntrySession);

        assertThat(patient.isDead(), is(true));
        assertThat(patient.getCauseOfDeath(), is(lungCancer));
        verify(patientService).savePatient(patient);
    }

    private Matcher<MarkPatientDeadTagHandler.Action> isDefaultAction() {
        return new ArgumentMatcher<MarkPatientDeadTagHandler.Action>() {
            @Override
            public boolean matches(Object o) {
                MarkPatientDeadTagHandler.Action action = (MarkPatientDeadTagHandler.Action) o;
                return action.isDeathDateFromEncounter() && !action.isPreserveExistingDeathDate();
            }
        };
    }

    private Matcher<MarkPatientDeadTagHandler.Action> actionDoesNotSetDate() {
        return new ArgumentMatcher<MarkPatientDeadTagHandler.Action>() {
            @Override
            public boolean matches(Object o) {
                MarkPatientDeadTagHandler.Action action = (MarkPatientDeadTagHandler.Action) o;
                return !action.isDeathDateFromEncounter();
            }
        };
    }

    private Matcher<MarkPatientDeadTagHandler.Action> actionSetsCauseOfDeathFrom(final Concept concept) {
        return new ArgumentMatcher<MarkPatientDeadTagHandler.Action>() {
            @Override
            public boolean matches(Object o) {
                MarkPatientDeadTagHandler.Action action = (MarkPatientDeadTagHandler.Action) o;
                return action.getCauseOfDeathFromObs() != null
                        && action.getCauseOfDeathFromObs().getConceptId().equals(concept.getConceptId());
            }
        };
    }

}