package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * Handles the {@code <submit>} tag
 */
public class SubmitButtonHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {

        // TODO translate
        if (session.getContext().getMode() == Mode.VIEW) {
            return "";
        } else if (session.getContext().getMode() == Mode.EDIT) {
            return "<input type=\"button\" value=\"" + Context.getMessageSourceService().getMessage("htmlformentry.saveChangesButton") + "\" onClick=\"submitHtmlForm()\"/>";
        } else {
            return "<input type=\"button\" value=\"" + Context.getMessageSourceService().getMessage("htmlformentry.enterFormButton") + "\" onClick=\"submitHtmlForm()\"/>";
        }
        
    }

}
