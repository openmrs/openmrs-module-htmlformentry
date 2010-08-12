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
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The controller for entering/viewing a form. This should always be set to sessionForm=false.
 * <p/>
 * Handles {@code htmlFormEntry.form} requests. Renders view {@code htmlFormEntry.jsp}.
 * <p/>
 * TODO: This has a bit too much logic in the onSubmit method. Move that into the FormEntrySession.
 */
public class HtmlFormEntryController extends SimpleFormController {
    
    protected final Log log = LogFactory.getLog(getClass());
    
    private String closeDialogView;
    public final static String FORM_IN_PROGRESS_KEY = "HTML_FORM_IN_PROGRESS_KEY";
    public final static String FORM_IN_PROGRESS_VALUE = "HTML_FORM_IN_PROGRESS_VALUE";
    
    public void setCloseDialogView(String closeDialogView) {
    	this.closeDialogView = closeDialogView;
    }

    @Override
    protected FormEntrySession formBackingObject(HttpServletRequest request) throws Exception {
        long ts = System.currentTimeMillis();
        
        Integer encounterId = null;
        Encounter encounter = null;
        if (request.getParameter("encounterId") != null && !"".equals(request.getParameter("encounterId"))) {
            encounterId = Integer.valueOf(request.getParameter("encounterId"));
            encounter = Context.getEncounterService().getEncounter(encounterId);
        }
        
        Integer personId = null;
        Patient patient = null;
        if (encounter != null) {
        	patient = encounter.getPatient();
        	personId = patient.getPersonId();
        } else {
        	personId = Integer.valueOf(request.getParameter("personId"));
        	patient = Context.getPatientService().getPatient(personId);
            if (patient == null)
                throw new IllegalArgumentException("No patient with id " + personId);
        }
        
        HtmlForm htmlForm = null;
        
        if (encounter != null) {
            String formIdParam = request.getParameter("formId");
            if (StringUtils.hasText(formIdParam)) {
                Form form = Context.getFormService().getForm(Integer.parseInt(formIdParam));
                htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
            } else {
                htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(encounter.getForm());
            }
        	if (htmlForm == null)
                    throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm() + ") does not have an HtmlForm associated with it");
        } else {
	        String htmlFormIdParam = request.getParameter("htmlFormId");
	        if (StringUtils.hasText(htmlFormIdParam)) {
	        	htmlForm = HtmlFormEntryUtil.getService().getHtmlForm(Integer.valueOf(htmlFormIdParam));
	        }
	        String formIdParam = request.getParameter("formId");
	        if (StringUtils.hasText(formIdParam)) {
	        	Form form = Context.getFormService().getForm(Integer.parseInt(formIdParam));
	        	htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
	        }
	        if (htmlForm == null) {
	        	throw new IllegalArgumentException("You must specify either an htmlFormId or a formId");
	        }
        }

        FormEntrySession session;
        if (encounter != null) {
            Mode mode = Mode.VIEW;
            if ("EDIT".equals(request.getParameter("mode"))) {
                mode = Mode.EDIT;
            }
            session = new FormEntrySession(patient, encounter, mode, htmlForm);
        } else {
            session = new FormEntrySession(patient, htmlForm);
        }
        
        String returnUrl = request.getParameter("returnUrl");
        if (StringUtils.hasText(returnUrl)) {
            session.setReturnUrl(returnUrl);
        }

        // In case we're not using a sessionForm, we need to check for the case where the underlying form was modified while a user was filling a form out
        if (StringUtils.hasText(request.getParameter("formModifiedTimestamp"))) {
            long submittedTimestamp = Long.valueOf(request.getParameter("formModifiedTimestamp"));
            if (submittedTimestamp != session.getFormModifiedTimestamp()) {
                throw new RuntimeException(Context.getMessageSourceService().getMessage("htmlformentry.error.formModifiedBeforeSubmission"));
            }
        }

        // In case we're not using a sessionForm, we need to make sure this encounter hasn't been modified since the user opened it
        if (encounter != null) {
            try {
                long submittedTimestamp = Long.valueOf(request.getParameter("encounterModifiedTimestamp"));
                if (submittedTimestamp != session.getEncounterModifiedTimestamp()) {
                    throw new RuntimeException(Context.getMessageSourceService().getMessage("htmlformentry.error.encounterModifiedBeforeSubmission"));
                }
            } catch (NumberFormatException ex) {
                // this is being opened for the first time, no worries 
            }
        }
        
        Context.setVolatileUserData(FORM_IN_PROGRESS_KEY, session);
       
        log.warn("Took " + (System.currentTimeMillis() - ts) + " ms");
        return session;
    }

    @Override
    protected void onBindAndValidate(HttpServletRequest request,
            Object commandObject, BindException errors) throws Exception {
        FormEntrySession session = (FormEntrySession) commandObject;
        try {
            List<FormSubmissionError> validationErrors = session.getSubmissionController().validateSubmission(session.getContext(), request);
            if (validationErrors == null || validationErrors.size() == 0) {
                return;
            } else {
                errors.reject("Fix errors");
            }
        } catch (Exception ex) {
            log.error("Exception during form validation", ex);
            errors.reject("Exception during form validation, see log for more details: " + ex);
        }
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object commandObject, BindException errors)
            throws Exception {
        FormEntrySession session = (FormEntrySession) commandObject;
        try {
            session.prepareForSubmit();
            session.getSubmissionController().handleFormSubmission(session, request);
            if (session.getContext().getMode() == Mode.ENTER && (session.getSubmissionActions().getEncountersToCreate() == null || session.getSubmissionActions().getEncountersToCreate().size() == 0))
                throw new IllegalArgumentException("This form is not going to create an encounter"); 
            session.applyActions();
            String successView = session.getReturnUrlWithParameters();
            if (successView == null)
                successView = getSuccessView() + "?patientId=" + session.getPatient().getPersonId();
            if (StringUtils.hasText(request.getParameter("closeAfterSubmission"))) {
            	return new ModelAndView(closeDialogView, "dialogToClose", request.getParameter("closeAfterSubmission"));
            } else {
            	return new ModelAndView(new RedirectView(successView));
            }
        } catch (BadFormDesignException ex) {
            log.error("Bad Form Design:", ex);
            errors.reject(ex.getMessage());
            return showForm(request, response, errors);
        } catch (Exception ex) {
            log.error("Exception trying to submit form", ex);
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            errors.reject("Exception! " + ex.getMessage() + "<br/>" + sw.toString());
            return showForm(request, response, errors);
        }
    }
    
}
