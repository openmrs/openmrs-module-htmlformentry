package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.action.RepeatControllerAction;
import org.openmrs.util.OpenmrsUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates how to validate and submit a form.
 * <p/>
 * When going through XML/HTML substitution to build a form, one of these is created as a side-effect.
 */
public class FormSubmissionController {
    
    private List<FormSubmissionControllerAction> actions = new ArrayList<FormSubmissionControllerAction>();
    private transient List<FormSubmissionError> lastSubmissionErrors;
    private transient HttpServletRequest lastSubmission;
    private RepeatControllerAction repeat = null;

    private static Log log = LogFactory.getLog(FormSubmissionController.class);
    
    public FormSubmissionController() {
    }
    
    /**
     * Adds a {@see RepeatControllerAction} to the list of submission actions.
     * 
     * @param repeat the repeat controller action to add
     */
    public void startRepeat(RepeatControllerAction repeat) {
        if (this.repeat != null)
            throw new IllegalArgumentException("Nested Repeating elements are not yet implemented");
        addAction(repeat);
        this.repeat = repeat;
    }
    
    /**
     * Marks the end of the a repeat. This has to be specified because nested repeating elements are not yet implemented.
     */
    public void endRepeat() {
        if (this.repeat == null)
            throw new IllegalArgumentException("No Repeating element is open now");
        this.repeat = null;
    }
    
    /**
     * Adds a FormSubmissionControllerAction to the list of submission actions.
     * 
     * @param the form submission controller action to add
     */
    public void addAction(FormSubmissionControllerAction action) {
        actions.add(action);
    }
    
    /**
     * Validates a form submission, given a Form Entry Context.
     * <p/>
     * This method cycles through all the FormSubmissionControllerActions and calls their validateSubmission method, 
     * adding any errors to the error list.
     * 
     * @param context the Form Entry Context 
     * @param submission the submission to validate
     * @return list of all validation errors
     */
    public List<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        lastSubmission = submission;
        lastSubmissionErrors = new ArrayList<FormSubmissionError>();
        for (FormSubmissionControllerAction element : actions) {
            
            Collection<FormSubmissionError> errs = element.validateSubmission(context, submission);
            if (errs != null) {
                lastSubmissionErrors.addAll(errs);
            }
        }
        return lastSubmissionErrors;
    }
    
    /**
     * Processes a form submission, given a Form Entry Session.
     * <p/>
     * This method cycles through all the FormSubmissionControllerActions and calls all their handleSubmission methods.
     * 
     * @param session the Form Entry Session
     * @param submission
     */
    public void handleFormSubmission(FormEntrySession session, HttpServletRequest submission) throws Exception{
        lastSubmission = submission;
        //Serialize when opted in.
        String optedIn = Context.getAdministrationService().getGlobalProperty("htmlformentry.archiveHtmlForms","No");
        if(Boolean.parseBoolean(optedIn)) {
            //Try to serialize
            try {
                Patient patient = session.getPatient();
                Encounter encounter = session.getEncounter();
                SerializableFormObject formObject = null;
                if (patient != null && encounter != null) {
                    formObject = new SerializableFormObject(session.getXmlDefinition(),
                            submission.getParameterMap(), patient.getPatientIdentifier().getIdentifier(),
                            patient.getUuid(), encounter.getUuid(),session.getHtmlFormId());
                } else if(patient==null || encounter==null) {
                    if(log.isDebugEnabled()) log.debug("Either patient or encounter or both are null");

                    //Serialize anyway
                    formObject = new SerializableFormObject(session.getXmlDefinition(), submission.getParameterMap(),
                                    session.getHtmlFormId());
                }
                if (formObject != null) {
                    serializeFormData(formObject);
                }
            } finally {
                //Submit even when you are not able to serialize.
                for (FormSubmissionControllerAction element : actions) {
                    element.handleSubmission(session, submission);
                }
            }
        } else {  //Just submit
            for (FormSubmissionControllerAction element : actions) {
                element.handleSubmission(session, submission);
            }
        }
    }
    
    /**
     * Returns the last submission processed by handleFormSubmission.
     * 
     * @return the last submission processed
     */
    public HttpServletRequest getLastSubmission() {
        return lastSubmission;
    }

    /**
     * Returns the last set of submission errors generated by validateSubmission
     * 
     * @return the last set of submission errors
     */
    public List<FormSubmissionError> getLastSubmissionErrors() {
        return lastSubmissionErrors;
    }
    
    /**
     * 
     * Returns the List of FormSubmissionControllerActions
     * 
     * @return the FormSubmissionControllerAction List
     */
    public List<FormSubmissionControllerAction> getActions() {
               return actions;
    }

    /**
     * Serializes an object formed by pairing the HttpServletRequest & FormEntrySession objects necessary for form
     * submission
     * @param submittedData  SerializableFormObject
     * @throws Exception
     */
    protected void serializeFormData(final SerializableFormObject submittedData) throws Exception {
        //Get archive Directory
        String path = HtmlFormEntryUtil.getArchiveDirPath();

        //Ignore if no path specified
        if(path==null)return;

        File   file = new File(path);
        if(file.exists()) {
            if(!file.isDirectory()) {
                throw new APIException("The specified archive is not a directory, please use a proper directory");
            }
            //Proceed if it is a directory
            if(!file.canWrite()) {
                throw new APIException("The Archive directory is not writable, check the directory permissions");
            }
        }else{
            //Try to create the directory if it does not exist.
            if(!file.mkdirs()) {
                throw new APIException("Failed to create subdirectories. Make sure you have proper write " +
                        "permission set on the archive directory");
            }
        }

        //All exceptions have been accounted for
        submittedData.serializeToXml(path);
    }
}
