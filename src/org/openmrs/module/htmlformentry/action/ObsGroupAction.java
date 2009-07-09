package org.openmrs.module.htmlformentry.action;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.InvalidActionException;

public class ObsGroupAction implements FormSubmissionControllerAction {

    public static ObsGroupAction start(Concept groupingConcept, Obs existingGroup) {
        return new ObsGroupAction(groupingConcept, existingGroup, true);
    }

    public static ObsGroupAction end() {
        return new ObsGroupAction(null, null, false);
    }
    
    //------------------------------------
    
    private Concept groupingConcept;
    private Obs existingGroup;
    private boolean start;
    
    private ObsGroupAction(Concept groupingConcept, Obs existingGroup, boolean start) {
        this.groupingConcept = groupingConcept;
        this.existingGroup = existingGroup;
        this.start = start;

        if (this.groupingConcept != null)
            this.groupingConcept.getDatatype();
    }

    public Collection<FormSubmissionError> validateSubmission(
            FormEntryContext context, HttpServletRequest submission) {
        // this cannot fail validation
        return null;
    }

    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        try {
            if (start) {
                if (existingGroup != null) {
                    session.getSubmissionActions().beginObsGroup(existingGroup);
                } else {
                    Obs obsGroup = new Obs();
                    obsGroup.setConcept(groupingConcept);
                    session.getSubmissionActions().beginObsGroup(obsGroup);
                }
            } else {
                session.getSubmissionActions().endObsGroup();
            }
        } catch (InvalidActionException ex) {
            throw new RuntimeException(ex);
        }
    }

}
