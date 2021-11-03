package org.openmrs.module.htmlformentry;

/**
 * Classes that implement this interface can be added to the FormSubmissionActions via the
 * PostSubmissionAction tag. applyAction(session) method will then be called at the end of the
 * "applyActions" phase of form submission (see FormEntrySession.applyActions()) For an example of
 * usage, see:
 * https://github.com/PIH/openmrs-module-pihcore/blob/master/api/src/main/java/org/openmrs/module/pihcore/htmlformentry/action/ReopenVisitAction.java
 * https://github.com/PIH/openmrs-config-pihemr/blob/master/configuration/pih/htmlforms/admissionNote.xml#L17
 */
public interface CustomFormSubmissionAction {
	
	void applyAction(FormEntrySession session);
	
}
