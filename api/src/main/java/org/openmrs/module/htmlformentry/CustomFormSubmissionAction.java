package org.openmrs.module.htmlformentry;

/**
 * Classes that implement this interface can be added to the FormSubmissionActions via
 * submissionActions.addCustomFormSubmissionAction(CustomFormSubmissionAction). The applyAction(session) method will
 * then be called at the end of the "applyActions" phase of form submission (see FormEntrySession.applyActions())
 * For an example of usage, see: 
 * https://github.com/PIH/openmrs-module-appointmentschedulingui/commit/e2cda8de1caa8a45d319ae4fbf7714c90c9adb8b
 */
public interface CustomFormSubmissionAction {

    void applyAction(FormEntrySession session);

}
