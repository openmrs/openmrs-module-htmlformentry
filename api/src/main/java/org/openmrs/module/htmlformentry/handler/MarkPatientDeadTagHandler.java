package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class MarkPatientDeadTagHandler extends SubstitutionTagHandler {

    @Autowired
    private PatientService patientService;

    public void setPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException {
        boolean deathDateFromEncounter = parseBooleanAttribute(parameters.get("deathDateFromEncounter"), true);
        boolean preserveExistingDeathDate = parseBooleanAttribute(parameters.get("preserveExistingDeathDate"), false);

        Action action = new Action();
        action.setDeathDateFromEncounter(deathDateFromEncounter);
        action.setPreserveExistingDeathDate(preserveExistingDeathDate);

        controllerActions.addAction(action);

        return "";
    }

    /**
     * This method is only for testing, e.g. to construct an action to match against
     * @return
     */
    public Action newAction() {
        return new Action();
    }

    public class Action implements FormSubmissionControllerAction, CustomFormSubmissionAction {

        private boolean deathDateFromEncounter;
        private boolean preserveExistingDeathDate;

        @Override
        public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
            // this can't fail validation since there is no user-controller input
            return null;
        }

        @Override
        public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
            session.getSubmissionActions().addCustomFormSubmissionAction(this);
        }

        @Override
        public void applyAction(FormEntrySession session) {
            Patient patient = session.getPatient();
            Encounter encounter = session.getEncounter();
            patient.setDead(true);
            if (deathDateFromEncounter) {
                if (patient.getDeathDate() == null || !preserveExistingDeathDate) {
                    patient.setDeathDate(encounter.getEncounterDatetime());
                }
            }
            patientService.savePatient(patient);
        }

        public boolean isDeathDateFromEncounter() {
            return deathDateFromEncounter;
        }

        public void setDeathDateFromEncounter(boolean deathDateFromEncounter) {
            this.deathDateFromEncounter = deathDateFromEncounter;
        }

        public boolean isPreserveExistingDeathDate() {
            return preserveExistingDeathDate;
        }

        public void setPreserveExistingDeathDate(boolean preserveExistingDeathDate) {
            this.preserveExistingDeathDate = preserveExistingDeathDate;
        }
    }

}
