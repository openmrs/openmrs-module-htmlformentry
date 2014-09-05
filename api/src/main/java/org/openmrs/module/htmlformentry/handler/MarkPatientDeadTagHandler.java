package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    protected List<AttributeDescriptor> createAttributeDescriptors() {
        return Arrays.asList(new AttributeDescriptor("causeOfDeathFromObs", Concept.class));
    }

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException {
        boolean deathDateFromEncounter = parseBooleanAttribute(parameters.get("deathDateFromEncounter"), true);
        boolean preserveExistingDeathDate = parseBooleanAttribute(parameters.get("preserveExistingDeathDate"), false);
        boolean preserveExistingCauseOfDeath = parseBooleanAttribute(parameters.get("preserveExistingCauseOfDeath"), false);
        Concept causeOfDeathFromObs = null;
        if (parameters.containsKey("causeOfDeathFromObs")) {
            causeOfDeathFromObs = HtmlFormEntryUtil.getConcept(parameters.get("causeOfDeathFromObs"));
        }

        Action action = new Action();
        action.setDeathDateFromEncounter(deathDateFromEncounter);
        action.setPreserveExistingDeathDate(preserveExistingDeathDate);
        action.setPreserveExistingCauseOfDeath(preserveExistingCauseOfDeath);
        action.setCauseOfDeathFromObs(causeOfDeathFromObs);

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
        private Concept causeOfDeathFromObs;

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
                Concept causeOfDeath = null;
                if (causeOfDeathFromObs != null) {
                    causeOfDeath = findObsCodedValue(session.getEncounter(), causeOfDeathFromObs);
                }
                if (causeOfDeath == null) {
                    // don't overwrite with Unknown, even if we haven't specifically said to preserve existing
                    causeOfDeath = patient.getCauseOfDeath();
                }
                if (causeOfDeath == null) {
                    causeOfDeath = getUnknownConcept();
                }
                patient.setCauseOfDeath(causeOfDeath);
            }
            patientService.savePatient(patient);
        }

        private Concept findObsCodedValue(Encounter encounter, Concept concept) {
            for (Obs candidate : encounter.getAllObs(false)) {
                if (candidate.getConcept().equals(concept)) {
                    return candidate.getValueCoded();
                }
            }
            return null;
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

        public Concept getCauseOfDeathFromObs() {
            return causeOfDeathFromObs;
        }

        public void setCauseOfDeathFromObs(Concept causeOfDeathFromObs) {
            this.causeOfDeathFromObs = causeOfDeathFromObs;
        }
    }

}
