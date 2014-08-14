package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class MarkPatientDeadTagHandler extends SubstitutionTagHandler {

    @Autowired
    private PatientService patientService;

    @Autowired
    @Qualifier("adminService")
    private AdministrationService administrationService;

    public void setPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException {
        boolean deathDateFromEncounter = parseBooleanAttribute(parameters.get("deathDateFromEncounter"), true);
        boolean preserveExistingDeathDate = parseBooleanAttribute(parameters.get("preserveExistingDeathDate"), false);
        boolean preserveExistingCauseOfDeath = parseBooleanAttribute(parameters.get("preserveExistingCauseOfDeath"), false);

        Action action = new Action();
        action.setDeathDateFromEncounter(deathDateFromEncounter);
        action.setPreserveExistingDeathDate(preserveExistingDeathDate);
        action.setPreserveExistingCauseOfDeath(preserveExistingCauseOfDeath);

        controllerActions.addAction(action);

        return "";
    }

    Concept getUnknownConcept() {
        try {
            String conceptId = administrationService.getGlobalProperty(HtmlFormEntryConstants.GP_UNKNOWN_CONCEPT);
            return HtmlFormEntryUtil.getConcept(conceptId);
        } catch (Exception ex) {
            throw new IllegalStateException("Error looking up \"Unknown\" concept for cause of death, which must be " +
                    "specified in a global property called \"" + HtmlFormEntryConstants.GP_UNKNOWN_CONCEPT + "\".", ex);
        }
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
        private boolean preserveExistingCauseOfDeath;

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
            if (patient.getCauseOfDeath() == null || !preserveExistingCauseOfDeath) {
                patient.setCauseOfDeath(getUnknownConcept());
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

        public boolean isPreserveExistingCauseOfDeath() {
            return preserveExistingCauseOfDeath;
        }

        public void setPreserveExistingCauseOfDeath(boolean preserveExistingCauseOfDeath) {
            this.preserveExistingCauseOfDeath = preserveExistingCauseOfDeath;
        }
    }

}
