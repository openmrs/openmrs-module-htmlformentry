package org.openmrs.module.htmlformentry;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Relationship;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;

/**
 * This represents the multi-request transaction that begins the moment a user clicks on a form to fill out or to view.
 * 
 * Creating one of these requires an HtmlForm object, or at least the xml from one. Creating a FormEntrySession does the following things:
 * 1. Applies macros from the <macros>...</macros> section of the xml document, if one exists.
 * 2. If an existing encounter is provided (for viewing, as opposed to creation) then the observations from that encounter are fetched such that they can be displayed by widgets.
 * 3. Generates html to be displayed to the user.
 * 4. Creates a FormSubmissionController, which is aware of all widgets in the form, and knows how to validate them and handle their submission.
 * 
 * To validate and submit a form you need to do something like this:
 *     List<FormSubmissionError> validationErrors = session.getSubmissionController().validateSubmission(session.getContext(), request);
 *     if (validationErrors.size() == 0) {
 *         session.prepareForSubmit();
 *         session.getSubmissionController().handleFormSubmission(session, request);
 *         session.applyActions();
 *     } else {
 *         // display errors
 *         // redisplay form, 
 *         }
 *
 * TODO implement the "Edit Existing Form" use case
 */
public class FormEntrySession {

    protected final Log log = LogFactory.getLog(getClass());
    
    private Form form;
    private Encounter encounter;
    private long encounterModifiedTimestamp; // if two people try to edit this form simultaneously, we need to be able to panic
    private Patient patient;
    private String returnUrl;
    
    private HtmlForm htmlForm;
    private long formModifiedTimestamp; // if we are not using sessions, and the structure of the form is modified while a user is filling one out, we need to be able to panic
    private FormEntryContext context;
    private HtmlFormEntryGenerator htmlGenerator;
    private FormSubmissionController submissionController;
    private FormSubmissionActions submissionActions;
    private String htmlToDisplay;

    private VelocityEngine velocityEngine;
    private VelocityContext velocityContext;

    private FormEntrySession(Patient patient) {
        this.patient = patient;
        velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init();
        }
        catch (Exception e) {
            log.error("Error initializing Velocity engine", e);
        }
        velocityContext = new VelocityContext();
        velocityContext.put("locale", Context.getLocale());
        velocityContext.put("patient", patient);
        
        {
            Map<String, List<String>> identifiers = new HashMap<String, List<String>>();
            for (PatientIdentifier id : patient.getActiveIdentifiers()) {
                String idType = id.getIdentifierType().getName();
                List<String> list = identifiers.get(idType);
                if (list == null) {
                    list = new ArrayList<String>();
                    identifiers.put(idType, list);
                }
                list.add(id.getIdentifier());
            }
            velocityContext.put("patientIdentifiers", identifiers);
        }
        
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            for (PersonAttribute att : patient.getActiveAttributes()) {
                String attName = att.getAttributeType().getName();
                if (att.getValue() != null) {
                	attributes.put(attName, att.getHydratedObject());
                }
            }
            velocityContext.put("personAttributes", attributes);
        }

        // For now, the relationship query breaks for non-saved patients, so do not run for Demo patient
        if (!"Demo".equals(patient.getGivenName())) {
            List<Relationship> rels = Context.getPersonService().getRelationshipsByPerson(patient);
            // TODO put this is core in relationship service
            Map<String, List<Person>> relMap = new HashMap<String, List<Person>>();
            for (Relationship rel : rels) {
                if (rel.getPersonA().getPersonId().equals(patient.getPersonId())) {
                    List<Person> list = relMap.get(rel.getRelationshipType().getbIsToA());
                    if (list == null) {
                        list = new ArrayList<Person>();
                        relMap.put(rel.getRelationshipType().getbIsToA(), list);
                    }                    
                    list.add(rel.getPersonB());
                } else {
                    List<Person> list = relMap.get(rel.getRelationshipType().getaIsToB());
                    if (list == null) {
                        list = new ArrayList<Person>();
                        relMap.put(rel.getRelationshipType().getaIsToB(), list);
                    }
                   list.add(rel.getPersonA());
                }
            }
            velocityContext.put("relationshipList", rels);
            velocityContext.put("relationshipMap", relMap);
        }
        
        htmlGenerator = new HtmlFormEntryGenerator();
    }
    
    public FormEntrySession(Patient patient, String xml) throws Exception {
        this(patient);
        context = new FormEntryContext(Mode.ENTER);
        submissionController = new FormSubmissionController();
        
        this.htmlToDisplay = createForm(xml);
    }
    
    public FormEntrySession(Patient patient, HtmlForm htmlForm) throws Exception {
        this(patient);
        this.htmlForm = htmlForm;
        this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated() : htmlForm.getDateChanged()).getTime();
        form = htmlForm.getForm();
        context = new FormEntryContext(Mode.ENTER);
        
        velocityContext.put("form", form);
        submissionController = new FormSubmissionController();
        
        // avoid lazy initialization exceptions later
        form.getEncounterType().getName();

        htmlToDisplay = createForm(htmlForm.getXmlData());
    }
    
    public FormEntrySession(Patient patient, Form form) throws Exception {
        this(patient);
        this.form = form;
        context = new FormEntryContext(Mode.ENTER);
        
        velocityContext.put("form", form);
        submissionController = new FormSubmissionController();

        HtmlForm temp = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
        this.formModifiedTimestamp = (temp.getDateChanged() == null ? temp.getDateCreated() : temp.getDateChanged()).getTime();
        htmlToDisplay = createForm(temp.getXmlData());
    }
    
    public FormEntrySession(Patient patient, Encounter encounter, Mode mode, HtmlForm htmlForm) throws Exception {
        this(patient);
        this.htmlForm = htmlForm;
        if (htmlForm != null) {
            if (htmlForm.getId() != null)
                this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated() : htmlForm.getDateChanged()).getTime();
            form = htmlForm.getForm();
            velocityContext.put("form", form);
            // avoid lazy initialization exceptions later
            if (form != null && form.getEncounterType() != null)
                form.getEncounterType().getName();
        }

        this.encounter = encounter;
        if (encounter != null) {
            velocityContext.put("encounter", encounter);
            encounterModifiedTimestamp = getEncounterModifiedDate(encounter);
        }

        context = new FormEntryContext(mode);
        submissionController = new FormSubmissionController();
        context.setupExistingData(encounter);
        this.htmlToDisplay = createForm(htmlForm.getXmlData());
    }

    /*
    public FormEntrySession(Patient patient, Encounter encounter, Mode mode, String htmlToDisplay) throws Exception {
        this(patient);
        this.encounter = encounter;
        context = new FormEntryContext(mode);
        
        velocityContext.put("encounter", encounter);
        submissionController = new FormSubmissionController();
        
        context.setupExistingData(encounter);
        this.htmlToDisplay = createForm(htmlToDisplay);
    }
    */
    
    public String evaluateVelocityExpression(String velocityExpression) {
        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(velocityContext, writer, FormEntrySession.class.getName(), velocityExpression);
            return writer.toString(); 
        } catch (Exception ex) {
            log.error("Exception evaluating velocity expression", ex);
            return "Velocity Error! " + ex.getMessage(); 
        }
    }

    public String createForm(String xml) throws Exception {
    	if (htmlForm != null) {
    		context.getSchema().setName(htmlForm.getName());
    	}
        xml = htmlGenerator.applyMacros(xml);
        xml = htmlGenerator.applyTemplates(xml);
        xml = htmlGenerator.applyTranslations(xml, context);
        xml = htmlGenerator.applyTags(this, xml);
        int endOfFirstTag = xml.indexOf('>');
        int startOfLastTag = xml.lastIndexOf('<');
        if (endOfFirstTag < 0 || startOfLastTag < 0 || endOfFirstTag > startOfLastTag)
            return "";
        xml = xml.substring(endOfFirstTag + 1, startOfLastTag);
        return xml;
    }
    
    public void prepareForSubmit() {
        if (context.getMode() == Mode.EDIT) {
            if (encounter == null)
                throw new RuntimeException("Programming exception: encounter shouldn't be null in EDIT mode");
        } else {
            encounter = new Encounter();
        }
        submissionActions = new FormSubmissionActions();
        try {
            submissionActions.beginPerson(patient);
            submissionActions.beginEncounter(encounter);
        } catch (InvalidActionException e) {
            log.error("Programming error: should be no errors starting a patient and encounter", e);
        }
    }
    
    public void applyActions() throws BadFormDesignException {
        // if any encounter to be created by this form is missing a required field, throw an error
        // (If there's a widget but it was left blank, that would have been caught earlier--this
        // is for when there was no widget in the first place.) 
        {
            for (Encounter e : submissionActions.getEncountersToCreate()) {
                if (e.getProvider() == null || e.getEncounterDatetime() == null || e.getLocation() == null) {
                    throw new BadFormDesignException("Please check the design of your form to make sure it has all three tags: <b>&lt;encounterDate/&gt</b>;, <b>&lt;encounterLocation/&gt</b>;, and <b>&lt;encounterProvider/&gt;</b>");
                }
            }
        }
        
        // remove any obs groups that don't contain children 
        for (Iterator<Obs> iter = submissionActions.getObsToCreate().iterator(); iter.hasNext(); ) {
            Obs o = iter.next();
            if (o.hasGroupMembers())
                continue;
            if (!StringUtils.hasText(o.getValueAsString(Context.getLocale()))) {
                // this has no value, and we already checked for children. So remove it.
                log.trace("Removing empty obs group");
                o.getEncounter().removeObs(o);
                iter.remove();
            }
        }

        // propagate encounterDatetime to Obs where necessary
        if (submissionActions.getObsToCreate() != null) {
            List<Obs> toCheck = new ArrayList<Obs>();
            toCheck.addAll(submissionActions.getObsToCreate());
            while (toCheck.size() > 0) {
                Obs o = toCheck.remove(toCheck.size() - 1);
                if (o.getObsDatetime() == null && o.getEncounter() != null) {
                    o.setObsDatetime(o.getEncounter().getEncounterDatetime());
                    if (log.isDebugEnabled())
                        log.debug("Set obsDatetime to " + o.getObsDatetime() + " for " + o.getConcept().getBestName(Context.getLocale()));
                }
                if (o.getLocation() == null && o.getEncounter() != null) {
                    o.setLocation(o.getEncounter().getLocation());
                }
                if (o.hasGroupMembers())
                    toCheck.addAll(o.getGroupMembers());
            }
        }
               
        // TODO wrap this in a transaction
        if (submissionActions.getPersonsToCreate() != null) {
            for (Person p : submissionActions.getPersonsToCreate())
                Context.getPersonService().savePerson(p);
        }
        if (submissionActions.getEncountersToCreate() != null) {
            for (Encounter e : submissionActions.getEncountersToCreate()) {
                if (form != null) {
                    e.setForm(form);
                    e.setEncounterType(form.getEncounterType());
                }
                Context.getEncounterService().saveEncounter(e);
            }
        }
        
        // If we're in EDIT mode, we have to save the encounter so that any new obs are created.
        // This feels a bit like a hack, but actually it's a good thing to update the encounter's dateChanged in this case. (PS- turns out there's no dateChanged on encounter up to 1.5.)
        if (context.getMode() == Mode.EDIT) {
            Context.getEncounterService().saveEncounter(encounter);
        }
                
        ObsService obsService = Context.getObsService();
        /* This should propagate from above
        if (submissionActions.getObsToCreate() != null) {
            for (Obs o : submissionActions.getObsToCreate())
                Context.getObsService().saveObs(o, null);
        }
        */
                
        if (submissionActions.getObsToVoid() != null) {
            for (Obs o : submissionActions.getObsToVoid()) {
                if (log.isDebugEnabled())
                    log.debug("voiding obs: " + o.getObsId());
                obsService.voidObs(o, "htmlformentry");
                // if o was in a group and it has no obs left, void the group
                if (noObsLeftInGroup(o.getObsGroup())) {
                    obsService.voidObs(o.getObsGroup(), "htmlformentry");
                }
            } 
        }
    }
    
    /**
     * Returns true if group is an obs group that has no unvoided members.
     * 
     * @param group
     * @return
     */
    private boolean noObsLeftInGroup(Obs group) {
        if (group == null)
            return false;
        for (Obs member : group.getGroupMembers()) {
            if (!member.isVoided())
                return false;
        }
        return true;
    }

    public FormSubmissionController getSubmissionController() {
        return submissionController;
    }

    public FormEntryContext getContext() {
        return context;
    }

    public FormSubmissionActions getSubmissionActions() {
        return submissionActions;
    }

    public String getHtmlToDisplay() {
        return htmlToDisplay;
    }
        
    public String getSetLastSubmissionFieldsJavascript() {
        HttpServletRequest lastSubmission = submissionController.getLastSubmission();
        if (lastSubmission == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (Enumeration e = lastSubmission.getParameterNames(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                if (name.startsWith("w")) {
                    String val = lastSubmission.getParameter(name);
                    if (StringUtils.hasText(val))
                        sb.append("DWRUtil.setValue('" + name + "', '" + JavaScriptUtils.javaScriptEscape(val) + "');\n");
                }
            }
            return sb.toString();
        }
    }
    
    public String getLastSubmissionErrorJavascript() {
        StringBuilder sb = new StringBuilder();
        for (String divId : context.getErrorDivIds())
            sb.append("clearError('" + divId + "');\n");
        List<FormSubmissionError> errs = submissionController.getLastSubmissionErrors(); 
        if (errs != null && errs.size() > 0) {
            for (FormSubmissionError error : errs) {
                if (error.getSourceWidget() != null)
                    sb.append("showError('" + context.getErrorFieldId(error.getSourceWidget()) + "', '" + error.getError() + "');\n");
                else
                    sb.append("showError('" + error.getId() + "', '" + error.getError() + "');\n");
            }
        }
        return sb.toString();
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public Patient getPatient() {
        return patient;
    }
    
    public Form getForm() {
        return form;
    }
    
    public Integer getHtmlFormId() {
        return htmlForm == null ? null : htmlForm.getId();
    }

    /**
     * @return the returnUrl
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * @param returnUrl the returnUrl to set
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * Adds the patientId=xyz parameter to the returnUrl
     * 
     * @return
     */
    public String getReturnUrlWithParameters() {
        if (returnUrl == null)
            return null;
        String ret = returnUrl;
        if (!ret.contains("?"))
            ret += "?";
        if (!ret.endsWith("?") && !ret.endsWith("&"))
            ret += "&";
        ret += "patientId=" + getPatient().getPatientId();
        return ret;
    }

    /**
     * @return the formModifiedTimestamp
     */
    public long getFormModifiedTimestamp() {
        return formModifiedTimestamp;
    }

    /**
     * @return the encounterModifiedTimestamp
     */
    public long getEncounterModifiedTimestamp() {
        return encounterModifiedTimestamp;
    }

    public static long getEncounterModifiedDate(Encounter encounter) {
        long ret = encounter.getDateCreated().getTime();
        if (encounter.getDateVoided() != null)
            ret = Math.max(ret, encounter.getDateVoided().getTime());
        for (Obs o : encounter.getAllObs(true)) {
            ret = Math.max(ret, o.getDateCreated().getTime());
            if (o.getDateVoided() != null)
                ret = Math.max(ret, o.getDateVoided().getTime());
        }
        for (Order o : encounter.getOrders()) {
            ret = Math.max(ret, o.getDateCreated().getTime());
            if (o.getDateVoided() != null)
                ret = Math.max(ret, o.getDateVoided().getTime());
        }
        return ret;
    }
    
}
