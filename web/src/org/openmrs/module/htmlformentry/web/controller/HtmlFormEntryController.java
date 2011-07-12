package org.openmrs.module.htmlformentry.web.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ValidationException;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The controller for entering/viewing a form.
 * <p/>
 * Handles {@code htmlFormEntry.form} requests. Renders view {@code htmlFormEntry.jsp}.
 * <p/>
 * TODO: This has a bit too much logic in the onSubmit method. Move that into the FormEntrySession.
 */
@Controller
public class HtmlFormEntryController {
    
    protected final Log log = LogFactory.getLog(getClass());
    public final static String closeDialogView = "/module/htmlformentry/closeDialog";
    public final static String FORM_IN_PROGRESS_KEY = "HTML_FORM_IN_PROGRESS_KEY";
    public final static String FORM_IN_PROGRESS_VALUE = "HTML_FORM_IN_PROGRESS_VALUE";
    public final static String FORM_PATH = "/module/htmlformentry/htmlFormEntry";
   
    @RequestMapping(method=RequestMethod.GET, value=FORM_PATH)
    public void showForm() {
    	// Intentionally blank. All work is done in the getFormEntrySession method 
    }
    
    @ModelAttribute("command")
    public FormEntrySession getFormEntrySession(HttpServletRequest request,
                                                /*@RequestParam(value="mode", required=false) String modeParam,*/
                                                /*@RequestParam(value="encounterId", required=false) Integer encounterId,*/
                                                @RequestParam(value="patientId", required=false) Integer patientId,
                                                @RequestParam(value="personId", required=false) Integer personId,
                                                @RequestParam(value="formId", required=false) Integer formId,
                                                @RequestParam(value="htmlformId", required=false) Integer htmlFormId,
                                                @RequestParam(value="returnUrl", required=false) String returnUrl,
                                                @RequestParam(value="formModifiedTimestamp", required=false) Long formModifiedTimestamp,
                                                @RequestParam(value="encounterModifiedTimestamp", required=false) Long encounterModifiedTimestamp) throws Exception {
    	// @RequestParam doesn't pick up query parameters (in the url) in a POST, so I'm handling encounterId and modeParam specially:
    	String modeParam = request.getParameter("mode");
    	Integer encounterId = null;
    	if (StringUtils.hasText(request.getParameter("encounterId")))
    		encounterId = Integer.valueOf(request.getParameter("encounterId"));
    	
        long ts = System.currentTimeMillis();
        
        Mode mode = Mode.VIEW;
		if ("enter".equalsIgnoreCase(modeParam)) {
			mode = Mode.ENTER;
		}
		else if ("edit".equalsIgnoreCase(modeParam)) {
            mode = Mode.EDIT;            
		}
		
        Encounter encounter = null;
        if (encounterId != null) {
            encounter = Context.getEncounterService().getEncounter(encounterId);
        }
        
        Patient patient = null;
        if (encounter != null) {
        	patient = encounter.getPatient();
        	personId = patient.getPersonId();
        } else {
			// register module uses patientId, htmlformentry uses personId
			if (patientId != null) {
				personId = patientId;
			}
			patient = Context.getPatientService().getPatient(personId);
			if (mode != Mode.ENTER && patient == null)
				throw new IllegalArgumentException("No patient with id " + personId);
        }
        
        HtmlForm htmlForm = null;
        
        if (encounter != null) {
            if (formId != null) {
                Form form = Context.getFormService().getForm(formId);
                htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
                if (htmlForm == null)
            		throw new IllegalArgumentException("No HtmlForm associated with formId " + formId);
            } else {
                htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(encounter.getForm());
                if (htmlForm == null)
            		throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm() + ") does not have an HtmlForm associated with it");
            }
        } else {
	        if (htmlFormId != null) {
	        	htmlForm = HtmlFormEntryUtil.getService().getHtmlForm(htmlFormId);
	        } else if (formId != null) {
	        	Form form = Context.getFormService().getForm(formId);
	        	htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
	        }
	        if (htmlForm == null) {
	        	throw new IllegalArgumentException("You must specify either an htmlFormId or a formId for a valid html form");
	        }
        }
        
        FormEntrySession session = null;
		if (mode == Mode.ENTER && patient == null) {
			patient = new Patient();			
		}
		if (encounter != null){
			session = new FormEntrySession(patient, encounter, mode, htmlForm);				
		} 
		else {
			session = new FormEntrySession(patient, htmlForm);
		}

        if (StringUtils.hasText(returnUrl)) {
            session.setReturnUrl(returnUrl);
        }

        // Since we're not using a sessionForm, we need to check for the case where the underlying form was modified while a user was filling a form out
        if (formModifiedTimestamp != null) {
            if (!OpenmrsUtil.nullSafeEquals(formModifiedTimestamp, session.getFormModifiedTimestamp())) {
                throw new RuntimeException(Context.getMessageSourceService().getMessage("htmlformentry.error.formModifiedBeforeSubmission"));
            }
        }

        // Since we're not using a sessionForm, we need to make sure this encounter hasn't been modified since the user opened it
        if (encounter != null) {
        	if (encounterModifiedTimestamp != null && !OpenmrsUtil.nullSafeEquals(encounterModifiedTimestamp, session.getEncounterModifiedTimestamp())) {
        		throw new RuntimeException(Context.getMessageSourceService().getMessage("htmlformentry.error.encounterModifiedBeforeSubmission"));
        	}
        }
        
        Context.setVolatileUserData(FORM_IN_PROGRESS_KEY, session);
       
        log.info("Took " + (System.currentTimeMillis() - ts) + " ms");
        
        return session;
    }
    
    /*
     * I'm using a return type of ModelAndView so I can use RedirectView rather than "redirect:" and preserve the fact that
     * returnUrl values from the pre-annotated-controller days will have the context path already
     */
    @RequestMapping(method=RequestMethod.POST, value=FORM_PATH)
    public ModelAndView handleSubmit(@ModelAttribute("command") FormEntrySession session,
                               Errors errors,
                               HttpServletRequest request,
                               Model model) throws Exception {
    	try {
            List<FormSubmissionError> validationErrors = session.getSubmissionController().validateSubmission(session.getContext(), request);
            if (validationErrors != null && validationErrors.size() > 0) {
                errors.reject("Fix errors");
            }
        } catch (Exception ex) {
            log.error("Exception during form validation", ex);
            errors.reject("Exception during form validation, see log for more details: " + ex);
        }
        
        if (errors.hasErrors()) {
        	return new ModelAndView(FORM_PATH, "command", session);
        }
        
        // no form validation errors, proceed with submission
        
        session.prepareForSubmit();

		if (session.getContext().getMode() == Mode.ENTER && session.hasPatientTag() && session.getPatient() == null 
				&& (session.getSubmissionActions().getPersonsToCreate() == null || session.getSubmissionActions().getPersonsToCreate().size() == 0))
			throw new IllegalArgumentException("This form is not going to create an Patient");

        if (session.getContext().getMode() == Mode.ENTER && session.hasEncouterTag() && (session.getSubmissionActions().getEncountersToCreate() == null || session.getSubmissionActions().getEncountersToCreate().size() == 0))
            throw new IllegalArgumentException("This form is not going to create an encounter"); 
        
    	try {
            session.getSubmissionController().handleFormSubmission(session, request);
            session.applyActions();
            String successView = session.getReturnUrlWithParameters();
            if (successView == null)
                successView = request.getContextPath() + "/patientDashboard.form" + getQueryPrameters(request, session);
            if (StringUtils.hasText(request.getParameter("closeAfterSubmission"))) {
            	return new ModelAndView(closeDialogView, "dialogToClose", request.getParameter("closeAfterSubmission"));
            } else {
            	return new ModelAndView(new RedirectView(successView));
            }
        } catch (ValidationException ex) {
            log.error("Invalid input:", ex);
            errors.reject(ex.getMessage());
        } catch (BadFormDesignException ex) {
            log.error("Bad Form Design:", ex);
            errors.reject(ex.getMessage());
        } catch (Exception ex) {
            log.error("Exception trying to submit form", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            errors.reject("Exception! " + ex.getMessage() + "<br/>" + sw.toString());
        }
        
        // if we get here it's because we caught an error trying to submit/apply
        return new ModelAndView(FORM_PATH, "command", session);
    }

	protected String getQueryPrameters(HttpServletRequest request, FormEntrySession formEntrySession) {
		return "?patientId=" + formEntrySession.getPatient().getPersonId();
	}
}
