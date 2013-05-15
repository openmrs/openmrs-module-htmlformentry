package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.Translator;

/**
 * Handles the {@code <submit>} tag
 */
public class SubmitButtonHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
            Map<String, String> parameters) {

        String submitLabel = null;
        String submitClass = "submitButton";

    	//handle defaults
    	if (session.getContext().getMode() == Mode.VIEW) {
            return "";
        }
    	else if (session.getContext().getMode() == Mode.EDIT) {
    		submitLabel = Context.getMessageSourceService().getMessage("htmlformentry.saveChangesButton");
        } else {
        	submitLabel = Context.getMessageSourceService().getMessage("htmlformentry.enterFormButton");
        }

    	//check for custom label & style
        if (parameters.containsKey("submitLabel")) {
        	submitLabel =  parameters.get("submitLabel");
        }
        if (parameters.containsKey("submitCode")) {
	    	Translator trans = session.getContext().getTranslator();
	    	submitLabel = trans.translate(Context.getLocale().toString(), parameters.get("submitCode"));
        }
        if (parameters.containsKey("submitClass")) {
	    	submitClass = submitClass + " " + parameters.get("submitClass");  //append any other classes specified
        }
        if (parameters.containsKey("class")) {
            submitClass = submitClass + " " + parameters.get("class");  // append any other classes specified
        }
	
        //render it
    	return "<input type=\"button\" class=\"" + submitClass + "\" value=\"" + submitLabel + "\" onClick=\"submitHtmlForm()\"/>";
    }

}
