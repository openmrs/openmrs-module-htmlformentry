package org.openmrs.module.htmlformentry.property;


import org.openmrs.Concept;
import java.util.Date;

/**
 * The property instance which keeps the details needed to exit a person from care center such as
 * date to exit the center, reason to exit the care center etc.
 */
public class ExitFromCareProperty {

    private Date dateOfExit;
    private Concept reasonExitConcept;

    public ExitFromCareProperty(Date dateOfExit, Concept reasonExitConcept) {

        this.dateOfExit = dateOfExit;
        this.reasonExitConcept = reasonExitConcept;
    }

    public Date getDateOfExit() {
        return dateOfExit;
    }

    public Concept getReasonExitConcept() {
        return reasonExitConcept;
    }

}
