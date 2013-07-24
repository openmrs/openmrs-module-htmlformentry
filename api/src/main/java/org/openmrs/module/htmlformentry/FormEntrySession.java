package org.openmrs.module.htmlformentry;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Relationship;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.property.ExitFromCareProperty;
import org.openmrs.module.htmlformentry.velocity.VelocityContextContentProvider;
import org.openmrs.module.htmlformentry.widget.AutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;


/**
 * This represents the multi-request transaction that begins the moment a user clicks on a form to
 * fill out or to view. </p> Creating one of these requires an HtmlForm object, or at least the xml
 * from one. Creating a FormEntrySession does the following things:
 * <ol>
 * <li>If an existing encounter is provided (for viewing, as opposed to creation) then the
 * observations from that encounter are fetched such that they can be displayed by widgets.</li>
 * <li>Generates html to be displayed to the user.</li>
 * <li>Creates a FormSubmissionController, which is aware of all widgets in the form, and knows how
 * to validate them and handle their submission.</li>
 * </ol>
 * To validate and submit a form you need to do something like this:
 * <p/>
 * <pre>
 *  session.getHtmlToDisplay();
 * 	List&lt;FormSubmissionError&gt; validationErrors = session.getSubmissionController().validateSubmission(session.getContext(),
 * 	    request);
 * 	if (validationErrors.size() == 0) {
 * 		session.prepareForSubmit();
 * 		session.getSubmissionController().handleFormSubmission(session, request);
 * 		session.applyActions();
 *     } else {
 * 		// display errors
 * 		// redisplay form,
 *     }
 * }
 * </pre>
 */
public class FormEntrySession {

    /**
     * Logger to use with this class
     */
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

    // calling the getter will build this once, then cache it
    private String htmlToDisplay;

    private VelocityEngine velocityEngine;

    private VelocityContext velocityContext;

    private boolean voidEncounter = false;
    
    private String hasChangedInd = "false";

    private HttpSession httpSession;

    private String xmlDefinition;

    /**
     * Applications and UI Frameworks that embed HTML Forms may store context variables as attributes to make them available to tags
     */
    private Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Private constructor that creates a new Form Entry Session for the specified Patient in the
     * specified {@Mode}
     *
     * @param patient
     * @param mode
     * @param defaultLocation
     * @param httpSession
     */
    private FormEntrySession(Patient patient, FormEntryContext.Mode mode, Location defaultLocation, HttpSession httpSession) {
        context = new FormEntryContext(mode);
        context.setDefaultLocation(defaultLocation);
        context.setHttpSession(httpSession);
        this.httpSession = httpSession;
        this.patient = patient;

        context.setupExistingData(patient);
        velocityEngine = new VelocityEngine();

        // This code pattern is copied to HtmlFormEntryServiceImpl. Any bugfixes should be copied too.
        // #1953 - Velocity errors in HTML form entry
        velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.CommonsLogLogChute");
        velocityEngine.setProperty(CommonsLogLogChute.LOGCHUTE_COMMONS_LOG_NAME, "htmlformentry_velocity");

        try {
            velocityEngine.init();
        } catch (Exception e) {
            log.error("Error initializing Velocity engine", e);
        }
        velocityContext = new VelocityContext();
        velocityContext.put("locale", Context.getLocale());
        velocityContext.put("patient", patient);
        velocityContext.put("fn", new VelocityFunctions(this));
        velocityContext.put("user", Context.getAuthenticatedUser());
        velocityContext.put("session", this);
        velocityContext.put("context", context);

        {
            Map<String, List<String>> identifiers = new HashMap<String, List<String>>();
            if (patient != null) {
                for (PatientIdentifier id : patient.getActiveIdentifiers()) {
                    String idType = id.getIdentifierType().getName();
                    List<String> list = identifiers.get(idType);
                    if (list == null) {
                        list = new ArrayList<String>();
                        identifiers.put(idType, list);
                    }
                    list.add(id.getIdentifier());
                }
            }
            velocityContext.put("patientIdentifiers", identifiers);
        }

        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (patient != null) {
                for (PersonAttribute att : patient.getActiveAttributes()) {
                    String attName = att.getAttributeType().getName();
                    if (att.getValue() != null) {
                        attributes.put(attName.replaceAll("'", ""), att.getHydratedObject());
                    }
                }
            }
            velocityContext.put("personAttributes", attributes);
        }

        // the relationship query only makes sense in the context of saved patients, so only call it if this patient
        // has already been persisted (i.e., assigned an id and uuid)
        if (patient != null && patient.getId() != null && patient.getUuid() != null
                && !("testing-html-form-entry".equals(patient.getUuid()))) {
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

		// finally allow modules to provide content to the velocity context
		for (VelocityContextContentProvider provider : Context.getRegisteredComponents(VelocityContextContentProvider.class)) {
			provider.populateContext(this, velocityContext);
		}

        htmlGenerator = new HtmlFormEntryGenerator();
    }


    /**
     * Private constructor that creates a new Form Entry Session for the specified Patient in the
     * specified {@Mode}
     *
     * @param patient
     * @param mode
     * @param httpSession
     */
    private FormEntrySession(Patient patient, FormEntryContext.Mode mode, HttpSession httpSession) {
        this(patient, mode, null, httpSession);
    }

    /**
     * Creates a new HTML Form Entry session (in "Enter" mode) for the specified Patient, using the
     * specified xml string to create the HTML Form object
     *
     * @param patient
     * @param xml
     * @param httpSession
     * @throws Exception
     */
    public FormEntrySession(Patient patient, String xml, HttpSession httpSession) throws Exception {
        this(patient, Mode.ENTER, httpSession);
        submissionController = new FormSubmissionController();

        this.xmlDefinition = xml;
    }

    /**
     * Creates a new HTML Form Entry session (in "Enter" mode) for the specified Patient, using the
     * specified HTML Form
     *
     * @param patient
     * @param htmlForm
     * @param httpSession
     * @throws Exception
     */
    public FormEntrySession(Patient patient, HtmlForm htmlForm, HttpSession httpSession) throws Exception {
        this(patient, htmlForm, Mode.ENTER, httpSession);
    }

    public FormEntrySession(Patient patient, HtmlForm htmlForm, Mode mode, HttpSession httpSession) throws Exception {
        this(patient, htmlForm, mode, null, httpSession, true, false);
    }

    public FormEntrySession(Patient patient, HtmlForm htmlForm, Mode mode, Location defaultLocation, HttpSession httpSession,
                            boolean automaticClientSideValidation, boolean clientSideValidationHints) throws Exception {
        this(patient, mode, defaultLocation, httpSession);
        this.context.setAutomaticClientSideValidation(automaticClientSideValidation);
        this.context.setClientSideValidationHints(clientSideValidationHints);
        this.htmlForm = htmlForm;
        this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated() : htmlForm
                .getDateChanged()).getTime();
        form = htmlForm.getForm();

        velocityContext.put("form", form);
        submissionController = new FormSubmissionController();

        // avoid lazy initialization exceptions later
        if (form.getEncounterType() != null)
            form.getEncounterType().getName();

        xmlDefinition = htmlForm.getXmlData();
    }

    /**
     * Creates a new HTML Form Entry session (in "Enter" mode) for the specified patient and using
     * the HTML Form associated with the specified Form
     *
     * @param patient
     * @param form
     * @param httpSession
     * @throws Exception
     */
    public FormEntrySession(Patient patient, Form form, HttpSession httpSession) throws Exception {
        this(patient, Mode.ENTER, httpSession);
        this.form = form;

        velocityContext.put("form", form);
        submissionController = new FormSubmissionController();

        HtmlForm temp = HtmlFormEntryUtil.getService().getHtmlFormByForm(form);
        this.formModifiedTimestamp = (temp.getDateChanged() == null ? temp.getDateCreated() : temp.getDateChanged())
                .getTime();
        xmlDefinition = temp.getXmlData();
    }

    /**
     * Creates a new HTML Form Entry session for the specified patient, encounter, and {@see Mode},
     * using the specified HtmlForm and with default Location
     *
     * @param patient
     * @param encounter
     * @param mode
     * @param htmlForm
     * @param httpSession
     * @throws Exception
     */
    public FormEntrySession(Patient patient, Encounter encounter, Mode mode, HtmlForm htmlForm, HttpSession httpSession) throws Exception {
        this(patient, encounter, mode, htmlForm, null, httpSession, true, false);
    }

    /**
     * Creates a new HTML Form Entry session for the specified patient, encounter, and {@see Mode},
     * using the specified HtmlForm
     *
     * @param patient
     * @param encounter
     * @param mode
     * @param htmlForm
     * @param defaultLocation
     * @param httpSession
     * @throws Exception
     */
    public FormEntrySession(Patient patient, Encounter encounter, Mode mode, HtmlForm htmlForm, Location defaultLocation,
                            HttpSession httpSession, boolean automaticClientSideValidation,
                            boolean clientSideValidationHints) throws Exception {
        this(patient, mode, defaultLocation, httpSession);
        this.context.setAutomaticClientSideValidation(automaticClientSideValidation);
        this.context.setClientSideValidationHints(clientSideValidationHints);
        this.htmlForm = htmlForm;
        if (htmlForm != null) {
            if (htmlForm.getId() != null)
                this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated() : htmlForm
                        .getDateChanged()).getTime();
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

        submissionController = new FormSubmissionController();
        context.setupExistingData(encounter);
        this.xmlDefinition = htmlForm.getXmlData();
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

    /**
     * Evaluates a velocity expression and returns the result as a string
     *
     * @param velocityExpression
     * @returns
     */
    public String evaluateVelocityExpression(String velocityExpression) {
        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(velocityContext, writer, FormEntrySession.class.getName(), velocityExpression);
            return writer.toString();
        } catch (CannotBePreviewedException ex) {
            return "Cannot be previewed";
        } catch (Exception ex) {
            if (ex.getCause() != null && ex.getCause() instanceof CannotBePreviewedException) {
                return "Cannot be run in preview mode: " + velocityExpression;
            } else {
                log.error("Exception evaluating velocity expression", ex);
                return "Velocity Error! " + ex.getMessage();
            }
        }
    }

    /**
     * Creates the HTML for a HTML Form given the xml for the form This method uses the
     * HtmlFormGenerator to process any HTML Form Entry-specific tags and returns pure HTML that can
     * be rendered by a browser
     *
     * @param xml the xml string representing the form we wish to create
     * @return
     * @throws Exception
     * @should return correct xml with a greater than character in an excludeIf tag
     * @should return correct xml with a greater than character in an includeIf tag
     * @should return correct xml with a compound expression in an excludeIf tag
     * @should return correct xml with a compound expression in an includeIf tag
     */
    public String createForm(String xml) throws Exception {
        if (htmlForm != null) {
            context.getSchema().setName(htmlForm.getName());
            context.setUnmatchedMode(false);
        }
        xml = htmlGenerator.stripComments(xml);
        xml = htmlGenerator.applyIncludes(this, xml);
        xml = htmlGenerator.applyExcludes(this, xml);
        xml = htmlGenerator.applyRoleRestrictions(xml);
        xml = htmlGenerator.applyMacros(xml);
        xml = htmlGenerator.applyRepeats(xml);
        xml = htmlGenerator.applyTranslations(xml, context);
        xml = htmlGenerator.applyTags(this, xml);

        if (context.hasUnmatchedObsGroupEntities() && (context.getMode() == Mode.EDIT || context.getMode() == Mode.VIEW)) {
            if (context.getUnmatchedObsGroupEntities().size() > 1 && context.getExistingObsInGroupsCount() > 0)
                context.setGuessingInd(true);
            context.setUnmatchedMode(true);
            xml = htmlGenerator.applyUnmatchedTags(this, xml);
        }

        xml = htmlGenerator.wrapInDiv(xml);
        return xml;
    }

    /**
     * If the html form contains both PatientTags and Encounter tags then initialize it with the
     * Patient and Encounter associated with the Form else if htmlform only contains PatientTags
     * then initialize it with the Patient associated with the Form.
     */
    public void prepareForSubmit() {

        submissionActions = new FormSubmissionActions();

        if (hasPatientTag() && !hasEncouterTag()) {
            try {
                submissionActions.beginPerson(patient);
            } catch (InvalidActionException e) {
                log.error("Programming error: should be no errors starting a patient", e);
            }
        } else {
            if (context.getMode() == Mode.EDIT) {
                if (encounter == null)
                    throw new RuntimeException("Programming exception: encounter shouldn't be null in EDIT mode");
            } else {
                encounter = new Encounter();
            }
            try {
                submissionActions.beginPerson(patient);
                submissionActions.beginEncounter(encounter);
            } catch (InvalidActionException e) {
                log.error("Programming error: should be no errors starting a patient and encounter", e);
            }
        }

    }

    /**
     * Applies all the actions associated with a form submission--that is, create/update any
     * Persons, Encounters, and Obs in the database as necessary, and enroll Patient in any programs
     * as needed
     * <p/>
     * TODO: This requires that...
     *
     * @throws BadFormDesignException
     */
    public void applyActions() throws BadFormDesignException {
        // if any encounter to be created by this form is missing a required field, throw an error
        // (If there's a widget but it was left blank, that would have been caught earlier--this
        // is for when there was no widget in the first place.)

        {
            for (Encounter e : submissionActions.getEncountersToCreate()) {
                if (!HtmlFormEntryUtil.hasProvider(e) || e.getEncounterDatetime() == null || e.getLocation() == null) {
                    throw new BadFormDesignException(
                            "Please check the design of your form to make sure it has all three tags: <b>&lt;encounterDate/&gt</b>;, <b>&lt;encounterLocation/&gt</b>;, and <b>&lt;encounterProvider/&gt;</b>");
                }
            }
        }

        //if we're un-voiding an existing voided encounter.  This won't get hit 99.9% of the time.  See EncounterDetailSubmissionElement
        if (!voidEncounter && encounter != null && encounter.isVoided()) {
            encounter.setVoided(false);
            encounter.setVoidedBy(null);
            encounter.setVoidReason(null);
            encounter.setDateVoided(null);
        }

        // remove any obs groups that don't contain children
		HtmlFormEntryUtil.removeEmptyObs(submissionActions.getObsToCreate());

        // propagate encounterDatetime to Obs where necessary
        if (submissionActions.getObsToCreate() != null) {
            List<Obs> toCheck = new ArrayList<Obs>();
            toCheck.addAll(submissionActions.getObsToCreate());
            while (toCheck.size() > 0) {
                Obs o = toCheck.remove(toCheck.size() - 1);
                if (o.getObsDatetime() == null && o.getEncounter() != null) {
                    o.setObsDatetime(o.getEncounter().getEncounterDatetime());
                    if (log.isDebugEnabled())
                        log.debug("Set obsDatetime to " + o.getObsDatetime() + " for "
                                + o.getConcept().getBestName(Context.getLocale()));
                }
                if (o.getLocation() == null && o.getEncounter() != null) {
                    o.setLocation(o.getEncounter().getLocation());
                }
                if (o.hasGroupMembers())
                    toCheck.addAll(o.getGroupMembers());
            }
        }

        // propagate encounterDatetime to PatientPrograms where necessary
        if (submissionActions.getPatientProgramsToCreate() != null) {
            for (PatientProgram pp : submissionActions.getPatientProgramsToCreate()) {
                if (pp.getDateEnrolled() == null)
                    pp.setDateEnrolled(encounter.getEncounterDatetime());
            }
        }

        if (submissionActions.getPatientProgramsToComplete() != null) {
            for (PatientProgram pp : submissionActions.getPatientProgramsToComplete()) {
                if (pp.getDateCompleted() == null)
                    pp.setDateCompleted(encounter.getEncounterDatetime());
            }
        }

        // TODO wrap this in a transaction
        if (submissionActions.getPersonsToCreate() != null) {
            for (Person p : submissionActions.getPersonsToCreate()) {
                if (p instanceof Patient) {
                    Patient patient = (Patient) p;
                    PatientIdentifier patientIdentifier = patient.getPatientIdentifier();
                    if (!StringUtils.hasText(patient.getGivenName()) || !StringUtils.hasText(patient.getFamilyName())
                            || !StringUtils.hasText(patient.getGender()) || patient.getBirthdate() == null
                            || patientIdentifier == null || !StringUtils.hasText(patientIdentifier.getIdentifier())
                            || patientIdentifier.getIdentifierType() == null || patientIdentifier.getLocation() == null) {
                        throw new BadFormDesignException(
                                "Please check the design of your form to make sure the following fields are mandatory to create a patient: <br/><b>&lt;personName/&gt;</b>, <b>&lt;birthDateOrAge/&gt;</b>, <b>&lt;gender/&gt;</b>, <b>&lt;identifierType/&gt;</b>, <b>&lt;identifier/&gt;</b>, and <b>&lt;identifierLocation/&gt;</b>");
                    }
                }
                Context.getPersonService().savePerson(p);
            }
        }
        if (submissionActions.getEncountersToCreate() != null) {
            for (Encounter e : submissionActions.getEncountersToCreate()) {
                if (form != null) {
                    e.setForm(form);
                    if (form.getEncounterType() != null)
                        e.setEncounterType(form.getEncounterType());
                }
                Context.getEncounterService().saveEncounter(e);
            }
        }

        //deal with relationships
        if (submissionActions.getRelationshipsToCreate() != null) {
            for (Relationship r : submissionActions.getRelationshipsToCreate()) {
                if (log.isDebugEnabled()) {
                    log.debug("creating relationships" + r.getRelationshipType().getDescription());
                }
                Context.getPersonService().saveRelationship(r);
            }
        }

        if (submissionActions.getRelationshipsToVoid() != null) {
            for (Relationship r : submissionActions.getRelationshipsToVoid()) {
                if (log.isDebugEnabled()) {
                    log.debug("voiding relationships" + r.getId());
                }
                Context.getPersonService().voidRelationship(r, "htmlformentry");
            }
        }

        if (submissionActions.getRelationshipsToEdit() != null) {
            for (Relationship r : submissionActions.getRelationshipsToCreate()) {
                if (log.isDebugEnabled()) {
                    log.debug("editing relationships" + r.getId());
                }
                Context.getPersonService().saveRelationship(r);
            }
        }

        // program enrollments are trickier since we need to make sure the patient isn't already enrolled
        // 1. if the patient is already enrolled on the given date, just skip this
        // 2. if the patient is enrolled *after* the given date, shift the existing enrollment to start earlier. (TODO decide if this is right)
        // 3. otherwise just enroll them as requested
        if (submissionActions.getPatientProgramsToCreate() != null) {
            for (PatientProgram toCreate : submissionActions.getPatientProgramsToCreate()) {
                boolean skip = false;
                PatientProgram earliestAfter = null;
                List<PatientProgram> already = Context.getProgramWorkflowService().getPatientPrograms(toCreate.getPatient(),
                        toCreate.getProgram(), null, null, null, null, false);
                for (PatientProgram pp : already) {
                    if (pp.getActive(toCreate.getDateEnrolled())) {
                        skip = true;
                        break;
                    }
                    // if the existing one starts after toCreate
                    if (OpenmrsUtil.compare(pp.getDateEnrolled(), toCreate.getDateEnrolled()) > 0) {
                        if (earliestAfter == null
                                || OpenmrsUtil.compare(pp.getDateEnrolled(), earliestAfter.getDateEnrolled()) < 0) {
                            earliestAfter = pp;
                        }
                    }
                }
                if (skip) {
                    continue;
                }
                if (earliestAfter != null) {
                    // edit this enrollment to move its start date earlier
                    earliestAfter.setDateEnrolled(toCreate.getDateEnrolled());
                    Context.getProgramWorkflowService().savePatientProgram(earliestAfter);
                } else {
                    // just enroll as requested
                    Context.getProgramWorkflowService().savePatientProgram(toCreate);
                }
            }
        }

        //complete any necessary programs
        if (submissionActions.getPatientProgramsToComplete() != null) {
            for (PatientProgram toComplete : submissionActions.getPatientProgramsToComplete()) {
                Context.getProgramWorkflowService().savePatientProgram(toComplete);
            }
        }

        if (submissionActions.getPatientProgramsToUpdate() != null) {
            for (PatientProgram patientProgram : submissionActions.getPatientProgramsToUpdate()) {
                Context.getProgramWorkflowService().savePatientProgram(patientProgram);
            }
        }

        ObsService obsService = Context.getObsService();
        
        if (submissionActions.getObsToVoid() != null) {
            for (Obs o : submissionActions.getObsToVoid()) {
                if (log.isDebugEnabled())
                    log.debug("voiding obs: " + o.getObsId());
                obsService.voidObs(o, "htmlformentry");
                // if o was in a group and it has no obs left, void the group
				voidObsGroupIfAllChildObsVoided(o.getObsGroup());
            }
        }

        // If we're in EDIT mode, we have to save the encounter so that any new obs are created.
        // This feels a bit like a hack, but actually it's a good thing to update the encounter's dateChanged in this case. (PS- turns out there's no dateChanged on encounter up to 1.5.)
        // If there is no encounter (impossible at the time of writing this comment) we save the obs manually
        if (context.getMode() == Mode.EDIT) {
            if (encounter != null) {
                if (voidEncounter) {
                    try {
                        HtmlFormEntryUtil.voidEncounter(encounter, htmlForm, "voided via htmlformentry form submission");
                    } catch (Exception ex) {
                        throw new RuntimeException("Unable to void encounter.", ex);
                    }
                }
                Context.getEncounterService().saveEncounter(encounter);
            } else if (submissionActions.getObsToCreate() != null) {
                // this may not work right due to savehandlers (similar error to HTML-135) but this branch is
                // unreachable until html forms are allowed to edit data without an encounter
                for (Obs o : submissionActions.getObsToCreate())
                    obsService.saveObs(o, null);
            }
        }

        /*
           ObsService obsService = Context.getObsService();
           This should propagate from above
          if (submissionActions.getObsToCreate() != null) {
              for (Obs o : submissionActions.getObsToCreate())
                  Context.getObsService().saveObs(o, null);
          }
          */

        // save the patient
        // TODO: we are having some issues here when updating a Patient and an Encounter via an HTML form due recently discovered problems with the way
        // we are using Hibernate.  We rely on Spring AOP saveHandlers and the save methods themselves to set some key parameters like date created--and
        // sometimes a flush can be called before these methods are called. This should be resolved once we move save handling out of Spring AOP and
        // into a Hibernate Interceptor (which happens in 1.9)
        if (patient != null && submissionActions.getPatientUpdateRequired()) {
            Context.getPersonService().savePerson(patient);
        }

        // exit the patient from care or process patient's death
        if (submissionActions.getExitFromCareProperty() != null) {
            ExitFromCareProperty exitFromCareProperty =
                    submissionActions.getExitFromCareProperty();
            if (exitFromCareProperty.getCauseOfDeathConcept() != null) {
                Context.getPatientService().processDeath(this.getPatient(), exitFromCareProperty.getDateOfExit(),
                        exitFromCareProperty.getCauseOfDeathConcept(), exitFromCareProperty.getOtherReason());
            } else {
                Context.getPatientService().exitFromCare(this.getPatient(), exitFromCareProperty.getDateOfExit(), exitFromCareProperty.getReasonExitConcept());
            }

        }

    }

    /**
     * Returns true if group is an obs group that has no unvoided members.
     *
     * @param group
     * @return
     */
    private void voidObsGroupIfAllChildObsVoided(Obs group) {
		if (group != null) {

            // probably should be able to just tet if group.getGroupMembers() == 0 since
            // getGroupMembers only returns non-voided members?
			boolean allObsVoided = true;
			for (Obs member : group.getGroupMembers()) {
				allObsVoided = allObsVoided && member.isVoided();
			}
			if (allObsVoided) {
				Context.getObsService().voidObs(group, "htmlformentry");
			}
			voidObsGroupIfAllChildObsVoided(group.getObsGroup());
		}
    }

    /**
     * Returns the submission controller associated with the session
     */
    public FormSubmissionController getSubmissionController() {
        return submissionController;
    }

    /**
     * Returns the form entry context associated with the session
     */
    public FormEntryContext getContext() {
        return context;
    }

    /**
     * Returns the submission actions associated with the session
     */
    public FormSubmissionActions getSubmissionActions() {
        return submissionActions;
    }

    /**
     * Return the form display HTML associated with the session. This has the important side-effect of having tags
     * populate the submissionActions list, so you must ensure this is called before you attempt to validate or process
     * a form's submission.
     * The first time you call this method on an instance will generate the HTML and cache it, so that subsequent calls
     * are fast (and so that the submissionActions list is only populated once).
     */
    public String getHtmlToDisplay() throws Exception {
        if (htmlToDisplay == null) {
            htmlToDisplay = createForm(xmlDefinition);
        }
        return htmlToDisplay;
    }

    /**
     * Creates the Javascript necessary to set form fields to the values entered during last
     * submission Used to maintain previously-entered field values when redisplaying a form with
     * validation errors
     */
    public String getSetLastSubmissionFieldsJavascript() {
        HttpServletRequest lastSubmission = submissionController.getLastSubmission();
        if (lastSubmission == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();

            // iterate through all the widgets and set their values based on the values in the last submission
            // if there is no value in the last submission, explicitly set the value as empty to override any default values
            for (Map.Entry<Widget, String> entry : context.getFieldNames().entrySet()) {
                Widget widgetType = entry.getKey();
                String widgetFieldName = entry.getValue();
                String val = lastSubmission.getParameter(widgetFieldName);

                // note that for each widget we set, we also trigger the change event on that widget
                // this is so any custom change handlers that a widget or tag may configure are called
                // when we set a value here; this is specifically used to make sure we trigger the change
                // handlers configured by the <exitFromCare> tag

                if (val != null) {
                    // special case to set the display field when autocomplete is used
                    if (AutocompleteWidget.class.isAssignableFrom(widgetType.getClass())) {
                        Class widgetClass = ((AutocompleteWidget) widgetType).getOptionClass();

                        if (widgetClass != null) {

                            Object returnedObj = HtmlFormEntryUtil.convertToType(val.trim(), widgetClass);

                            if (widgetClass.getSimpleName().equals("Location")) {
                                Location location = null;
                                if (returnedObj != null) {
                                    location = (Location) returnedObj;
                                } else {
                                    //This should typically never happen,why is there no location with this id, we
                                    //should set val(locationId) to blank so that the hidden form field is blank too
                                    val = "";
                                }

                                sb.append("$j('#" + widgetFieldName + "').val(\""
                                        + (location == null ? "" : JavaScriptUtils.javaScriptEscape(location.getName())) + "\");\n");
                                sb.append("$j('#" + widgetFieldName + "_hid" + "').val(\""
                                        + (location == null ? "" : JavaScriptUtils.javaScriptEscape(location.getId().toString())) + "\");\n");
                                sb.append("$j('#" + widgetFieldName + "').change();\n");

                            } else if (widgetClass.getSimpleName().equals("Person")) {
                                Person provider = null;
                                if (returnedObj != null) {
                                    provider = (Person) returnedObj;
                                } else {
                                    //This should typically never happen,why is there no provider with this id, we
                                    //should set val(providerid) to blank so that the hidden form field is blank too
                                    val = "";
                                }
                                sb.append("$j('#" + widgetFieldName + "').val(\""
                                        + (provider == null ? "" : JavaScriptUtils.javaScriptEscape(provider.getPersonName().getFullName())) + "\");\n");
                                sb.append("$j('#" + widgetFieldName + "_hid" + "').val(\""
                                        + (provider == null ? "" : JavaScriptUtils.javaScriptEscape(provider.getId().toString())) + "\");\n");
                                sb.append("$j('#" + widgetFieldName + "').change();\n");
                            }
                        }
                    }

                    // special case to set the display field of the obs value widget when autocomplete is used with <obs> tag
                    else if (ConceptSearchAutocompleteWidget.class.isAssignableFrom(entry.getKey().getClass())) {

                        String conveptVal = lastSubmission.getParameter(widgetFieldName + "_hid");
                        Object returnedObj = HtmlFormEntryUtil.convertToType(conveptVal.trim(), Concept.class);
                        Concept concept = null;
                        if (returnedObj != null) {
                            concept = (Concept) returnedObj;
                        } else {
                            //This should typically never happen,why if there no obs with this id, we
                            //should set val(obsId) to blank so that the hidden form field is blank too
                            val = "";
                        }
                        sb.append("$j('#" + widgetFieldName + "').val(\""
                                + (concept == null ? "" : JavaScriptUtils.javaScriptEscape(concept.getDisplayString())) + "\");\n");
                        sb.append("$j('#" + widgetFieldName + "_hid" + "').val(\"" + (concept == null ? "" : JavaScriptUtils.javaScriptEscape(concept.getId().toString())) + "\");\n");
                        sb.append("$j('#" + widgetFieldName + "').change();\n");
                    } else {
                        // set the value of the widget based on it's name
                        sb.append("setValueByName('" + widgetFieldName + "', '" + JavaScriptUtils.javaScriptEscape(val)
                                + "');\n");
                        sb.append("$j('#" + widgetFieldName + "').change();\n");
                    }


                } else {
                    if (AutocompleteWidget.class.isAssignableFrom(widgetType.getClass())) {
                        sb.append("$j('#" + widgetFieldName + "').val('');\n");
                        sb.append("$j('#" + widgetFieldName + "_hid" + "').val('');\n");
                        sb.append("$j('#" + widgetFieldName + "').change();\n");
                    } else if (ConceptSearchAutocompleteWidget.class.isAssignableFrom(widgetType.getClass())) {
                        sb.append("$j('#" + widgetFieldName + "').val('');\n");
                        sb.append("$j('#" + widgetFieldName + "_hid" + "').val('');\n");
                        sb.append("$j('#" + widgetFieldName + "').change();\n");
                    } else {
                        sb.append("setValueByName('" + widgetFieldName + "', '');\n");
                        sb.append("$j('#" + widgetFieldName + "').change();\n");
                    }
                }
            }
            return sb.toString();
        }
    }

    /**
     * Returns a fragment of javascript that will display any error widgets that had errors on the
     * last submission.
     */
    public String getLastSubmissionErrorJavascript() {
        if (context.getMode() == Mode.VIEW) {
            // in VIEW mode there are no error widgets
            return "";
        }

        StringBuilder sb = new StringBuilder();
        List<FormSubmissionError> errs = submissionController.getLastSubmissionErrors();
        if (errs != null && errs.size() > 0) {
            for (FormSubmissionError error : errs) {
                if (error.getSourceWidget() != null)
                    sb.append("showError('" + context.getErrorFieldId(error.getSourceWidget()) + "', '" + error.getError()
                            + "');\n");
                else
                    sb.append("showError('" + error.getId() + "', '" + error.getError() + "');\n");
            }
        }
        return sb.toString();
    }

    /**
     * @return a fragment of javascript that tells the getValue and setValue methods how to work
     */
    public String getFieldAccessorJavascript() {
        StringBuilder ret = new StringBuilder();
        for (Map.Entry<String, String> e : context.getJavascriptFieldAccessorInfo().entrySet()) {
            ret.append("propertyAccessorInfo['" + e.getKey() + "'] = " + e.getValue() + "\n");
        }
        return ret.toString();
    }

    /**
     * Returns the Encounter associated with the session
     */
    public Encounter getEncounter() {
        return encounter;
    }

    /**
     * Returns the Patient associated with the session
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * Returns the Form associated with the session
     */
    public Form getForm() {
        return form;
    }

    /**
     * Returns the id of the HtmlForm associated with the session
     */
    public Integer getHtmlFormId() {
        return htmlForm == null ? null : htmlForm.getId();
    }

    /**
     * Returns the return Url associated with the session
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * Sets the return Url associated with the session
     *
     * @param returnUrl the returnUrl to set
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * Adds the patientId=xyz parameter to the returnUrl
     *
     * @return the returnUrl with patientId parameter
     */
    public String getReturnUrlWithParameters() {
        if (!StringUtils.hasText(returnUrl))
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
     * Returns form modified timestamp
     */
    public long getFormModifiedTimestamp() {
        return formModifiedTimestamp;
    }

    /**
     * Returns the encounter modified timestamp
     */
    public long getEncounterModifiedTimestamp() {
        return encounterModifiedTimestamp;
    }

    /**
     * Calculates the date an encounter was last modified by checking the creation and voided times
     * of all Obs and Orders associated with the Encounter
     *
     * @param encounter
     * @return last modified time, as a Long
     */
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

    public boolean hasEncouterTag() {
        for (String tag : HtmlFormEntryConstants.ENCOUNTER_TAGS) {
            tag = "<" + tag;
            if (htmlForm.getXmlData().contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPatientTag() {
        for (String tag : HtmlFormEntryConstants.PATIENT_TAGS) {
            tag = "<" + tag;
            if (htmlForm.getXmlData().contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public void setVoidEncounter(boolean voidEncounter) {
        this.voidEncounter = voidEncounter;
    }

    public String getHasChangedInd() {
		return hasChangedInd;
	}

	public void setHasChangedInd(String hasChangedInd) {
		this.hasChangedInd = hasChangedInd;
	}

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public void setAutomaticClientSideValidation(boolean automaticClientSideValidation) {
        context.setAutomaticClientSideValidation(automaticClientSideValidation);
    }

    public void setClientSideValidationHints(boolean clientSideValidationHints) {
        context.setClientSideValidationHints(true);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

}
