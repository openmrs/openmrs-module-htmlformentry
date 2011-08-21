package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.List;
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
	public String getName() {
		return "Submit Button";
	}
	
	@Override
	public String getDescription() {
		return "You need to put a submit tag at the bottom of your form, or else your users will be very disappointed in you.";
	}
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attrs = new ArrayList<AttributeDescriptor>();
		
		attrs.add(new AttributeDescriptor("submitLabel", "Label", false, "Allows you to set the text of the submit button.", "text"));
		attrs.add(new AttributeDescriptor("submitCode", "Code", false, "Allows you to set the text of the submit button using an htmlformentry translation code", "text"));
		attrs.add(new AttributeDescriptor("submitStyle", "Style", false, " Allows you to set the style class for the submit button.  This attribute gets written into the rendered submit tag as class=\"&lt;&lt;submitStyle Value&gt;&gt;\".", "text"));
		
		return attrs;
	}
	
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
	    	submitClass = parameters.get("submitClass");
        }
	
        //render it
    	return "<input type=\"button\" class=\"" + submitClass + "\" value=\"" + submitLabel + "\" onClick=\"submitHtmlForm()\"/>";
    }

}
