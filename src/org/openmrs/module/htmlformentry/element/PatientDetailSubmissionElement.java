package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.PersonNameWidget;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

/**
 * Holds the widgets used to represent Patient Details, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for Patient Details.
 */
public class PatientDetailSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private PersonNameWidget nameWidget;
	private ErrorWidget nameErrorWidget;
	private NumberFieldWidget ageWidget;
	private ErrorWidget ageErrorWidget;
	private DropdownWidget genderWidget;
	private ErrorWidget genderErrorWidget;
	
	public PatientDetailSubmissionElement(FormEntryContext context, Map<String, Object> parameters) {
		if ("name".equals(parameters.get("type"))) {
			nameWidget = new PersonNameWidget();
			if (context.getExistingPatient() != null)
				nameWidget.setInitialValue(context.getExistingPatient().getPersonName());
			nameErrorWidget = new ErrorWidget();
			context.registerWidget(nameWidget);
			context.registerErrorWidget(nameWidget, nameErrorWidget);
		} else if ("age".equals(parameters.get("type"))) {
			ageWidget = new NumberFieldWidget(0d, 200d, false);
			if (context.getExistingPatient() != null)
				ageWidget.setInitialValue(context.getExistingPatient().getAge());
			ageErrorWidget = new ErrorWidget();
			context.registerWidget(ageWidget);
			context.registerErrorWidget(ageWidget, ageErrorWidget);
		} else if ("gender".equals(parameters.get("type"))) {
			MessageSourceService msg = Context.getMessageSourceService();
			genderWidget = new DropdownWidget();
			genderWidget.addOption(new Option());
			genderWidget.addOption(new Option(msg.getMessage("Patient.gender.female"), "F", false));
			genderWidget.addOption(new Option(msg.getMessage("Patient.gender.male"), "M", false));
			if (context.getExistingPatient() != null)
				genderWidget.setInitialValue(context.getExistingPatient().getGender());
			genderErrorWidget = new ErrorWidget();
			context.registerWidget(genderWidget);
			context.registerErrorWidget(genderWidget, genderErrorWidget);
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (nameWidget != null) {
			ret.append(nameWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
            	ret.append(nameErrorWidget.generateHtml(context));
		}
		if (ageWidget != null) {
			ret.append(ageWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
            	ret.append(ageErrorWidget.generateHtml(context));
		}
		if (genderWidget != null) {
			ret.append(genderWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
            	ret.append(genderErrorWidget.generateHtml(context));
		}
		return ret.toString();
	}

	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		try {
            if (nameWidget != null) {
                PersonName name = nameWidget.getValue(context, submission);
                if (name == null || name.getGivenName() == null || name.getFamilyName() == null)
                    throw new Exception("htmlformentry.error.required");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(nameErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
		try {
            if (ageWidget != null) {
        		Number age = ageWidget.getValue(context, submission);
                if (age == null)
                    throw new Exception("htmlformentry.error.required");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(ageErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
        try {
            if (genderWidget != null) {
        		String gender = genderWidget.getValue(context, submission);
                if (!StringUtils.hasText(gender))
                    throw new Exception("htmlformentry.error.required");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(genderErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
        return ret;
    }

	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
	 */
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
	    if (nameWidget != null) {
	    	PersonName pn = nameWidget.getValue(session.getContext(), submission);
	    	Person person = session.getSubmissionActions().getCurrentPerson();
	    	if (person.getPersonName() == null) {
	    		person.addName(pn);
	    	} else {
	    		// TODO need an option here for add versus replace
	    		person.getPersonName().setVoided(true);
	    		person.getPersonName().setVoidedBy(Context.getAuthenticatedUser());
	    		person.getPersonName().setVoidReason(session.getForm().getName() + " on " + new Date());
	    	}
	    }
	    if (ageWidget != null) {
	    	Double age = ageWidget.getValue(session.getContext(), submission);
	    	Person person = session.getSubmissionActions().getCurrentPerson();
	    	if (person.getAge() == null || Math.abs(person.getAge() - age) < 2) {
	    		// TODO the comparison above is not "correct"
	    		person.setBirthdateFromAge(age.intValue(), session.getSubmissionActions().getCurrentEncounter().getEncounterDatetime());	    		
	    	}
	    }
	    if (genderWidget != null) {
	    	String gender = genderWidget.getValue(session.getContext(), submission);
	    	Person person = session.getSubmissionActions().getCurrentPerson();
	    	if (!OpenmrsUtil.nullSafeEquals(person.getGender(), gender)) {
	    		person.setGender(gender);
	    	}
	    }
    }
	
}
