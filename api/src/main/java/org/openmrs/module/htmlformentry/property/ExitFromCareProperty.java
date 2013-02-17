package org.openmrs.module.htmlformentry.property;


import java.util.Date;

import org.openmrs.Concept;

/**
 * The property instance which keeps the details needed to exit a person from care center such as
 * date to exit the center, reason to exit the care center etc.
 */
public class ExitFromCareProperty {

    private Date dateOfExit;
    private Concept reasonExitConcept;
    private Concept causeOfDeathConcept;
    private String otherReason;

    public ExitFromCareProperty(Date dateOfExit, Concept reasonExitConcept, Concept causeOfDeathConcept, String otherReason) {

        this.dateOfExit = dateOfExit;
        this.reasonExitConcept = reasonExitConcept;
        this.causeOfDeathConcept = causeOfDeathConcept;
        this.otherReason = otherReason;
    }

    public Date getDateOfExit() {
        return dateOfExit;
    }

    public Concept getReasonExitConcept() {
        return reasonExitConcept;
    }

    public Concept getCauseOfDeathConcept() {
        return causeOfDeathConcept;
    }

    public String getOtherReason() {
        return otherReason;
    }


}
