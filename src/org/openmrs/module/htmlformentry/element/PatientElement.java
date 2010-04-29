package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.PersonNameWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.springframework.util.StringUtils;

/**
 * Holds the widgets used to represent a Patient, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for a Patient.
 */
public class PatientElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private boolean allowCreate;
	private boolean showWhenExists;
	private boolean display = false;
	private Widget nameWidget;
	private Widget identifierWidget;
	private Widget ageWidget;
	private Widget genderWidget;
	private ErrorWidget errorWidget;
	
	public PatientElement(FormEntryContext context, Map<String, String> attributes) {
	    allowCreate = Boolean.parseBoolean(attributes.get("allowCreate"));
	    showWhenExists = Boolean.parseBoolean(attributes.get("showWhenExists"));

	    if ((context.getExistingPatient() == null || context.getExistingPatient().getPatientId() == null) && allowCreate) {
			// no patient yet & we should create one
	    	display = true;
			errorWidget = new ErrorWidget();	        
			nameWidget = new PersonNameWidget();
			//identifierWidget = new IdentifierWidget();
			ageWidget = new NumberFieldWidget(0d, 200d, false);
			genderWidget = new DropdownWidget();
			((DropdownWidget) genderWidget).addOption(new Option());
			((DropdownWidget) genderWidget).addOption(new Option("Female", "F", false));
			((DropdownWidget) genderWidget).addOption(new Option("Male", "M", false));

	        context.registerWidget(errorWidget);
	        context.registerWidget(nameWidget);
	        context.registerWidget(ageWidget);
	        context.registerWidget(genderWidget);
	        context.registerErrorWidget(nameWidget, errorWidget);
	        context.registerErrorWidget(ageWidget, errorWidget);
	        context.registerErrorWidget(genderWidget, errorWidget);
	    }
    }

	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		MessageSourceService mss = Context.getMessageSourceService();
		if (display) {
			if (nameWidget != null) {
				sb.append(nameWidget.generateHtml(context)).append("<br/>");
			}
			if (ageWidget != null) {
				sb.append(mss.getMessage("Person.age") + ": ")
					.append(ageWidget.generateHtml(context))
					.append("<br/>");
			}
			if (genderWidget != null) {
				sb.append(mss.getMessage("Person.gender") + ": ")
					.append(genderWidget.generateHtml(context))
					.append("<br/>");
			}
			if (errorWidget != null) {
				sb.append(errorWidget.generateHtml(context));
			}
		}
		return sb.toString();
	}
	
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		if (nameWidget != null) {
			PersonName pn = (PersonName) nameWidget.getValue(context, submission);
			if (!StringUtils.hasText(pn.getFamilyName()) || !StringUtils.hasText(pn.getGivenName())) {
				ret.add(new FormSubmissionError(nameWidget, "required"));
			}
		}
		if (ageWidget != null && ageWidget.getValue(context, submission) == null) {
			ret.add(new FormSubmissionError(ageWidget, "required"));
		}
		if (genderWidget != null && genderWidget.getValue(context, submission) == null) {
			ret.add(new FormSubmissionError(genderWidget, "required"));
		}
		return ret;
	}
	
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		throw new RuntimeException("Not Yet Implemented");
	}
	
}
