package org.openmrs.module.htmlformentry.test;

import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntrySession;

public class TestCustomSubmissionAction implements CustomFormSubmissionAction {

    private static int numberOfCalls = 0;

    @Override
    public void applyAction(FormEntrySession session) {
        numberOfCalls += 1;
    }

    public static int getNumberOfCalls() {
        return numberOfCalls;
    }

}
