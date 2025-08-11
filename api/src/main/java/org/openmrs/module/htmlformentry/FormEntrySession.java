package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.openmrs.Concept;
import org.openmrs.Condition;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Relationship;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.appointment.AppointmentsAbstractor;
import org.openmrs.module.htmlformentry.property.ExitFromCareProperty;
import org.openmrs.module.htmlformentry.velocity.VelocityContextContentProvider;
import org.openmrs.module.htmlformentry.widget.AutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.OrderWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This represents the multi-request transaction that begins the moment a user clicks on a form to
 * fill out or to view.
 * </p>
 * Creating one of these requires an HtmlForm object, or at least the xml from one. Creating a
 * FormEntrySession does the following things:
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
 * 	List&lt;FormSubmissionError&gt;
 * 	validationErrors = session.getSubmissionController().validateSubmission(session.getContext(),
 * 	    request);
 * 	if (validationErrors.size() == 0) {
 * 		session.prepareForSubmit();
 * 		session.getSubmissionController().handleFormSubmission(session, request);
 * 		session.applyActions();
 *     } else {
 * 		// display errors
 * 		// redisplay form
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
	
	// The default url to go to after saving or canceling the form, and is typically set by the web application in its
	// controller for filling out HTML Forms
	private String returnUrl;
	
	// The url to go to after saving (but not canceling). May be set by a form through the <redirectOnSave/> tag or a
	// post-submission action. Values will be substituted for {{patient.id}} and {{encounter.id}}.
	private String afterSaveUrlTemplate;
	
	private HtmlForm htmlForm;
	
	private long formModifiedTimestamp; // if we are not using sessions, and the structure of the form is modified while a user is filling one out, we need to be able to panic
	
	protected FormEntryContext context;
	
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
	 * Applications and UI Frameworks that embed HTML Forms may store context variables as attributes to
	 * make them available to tags
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
	private FormEntrySession(Patient patient, FormEntryContext.Mode mode, Location defaultLocation,
	    HttpSession httpSession) {
		context = Context.getRegisteredComponent("formEntryContextFactoryImpl", FormEntryContextFactory.class).create(mode);
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
		velocityEngine.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
		    "org.apache.velocity.util.introspection.SecureUberspector");
		
		try {
			velocityEngine.init();
		}
		catch (Exception e) {
			log.error("Error initializing Velocity engine", e);
		}
		velocityContext = new VelocityContext();
		velocityContext.put("locale", Context.getLocale());
		velocityContext.put("patient", patient);
		velocityContext.put("fn", new VelocityFunctions(this));
		velocityContext.put("user", Context.getAuthenticatedUser());
		velocityContext.put("session", this);
		velocityContext.put("context", context);
		velocityContext.put("formGeneratedDatetime", new Date());
		velocityContext.put("visit", context.getVisit());
		
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
		for (VelocityContextContentProvider provider : Context
		        .getRegisteredComponents(VelocityContextContentProvider.class)) {
			provider.populateContext(this, velocityContext);
		}
		
		htmlGenerator = new HtmlFormEntryGenerator();
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
		this(patient, Mode.ENTER, null, httpSession);
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
		this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated()
		        : htmlForm.getDateChanged()).getTime();
		form = htmlForm.getForm();
		
		velocityContext.put("form", form);
		submissionController = new FormSubmissionController();
		
		// avoid lazy initialization exceptions later
		if (form.getEncounterType() != null)
			form.getEncounterType().getName();
		
		xmlDefinition = htmlForm.getXmlData();
	}
	
	/**
	 * Creates a new HTML Form Entry session (in "Enter" mode) for the specified patient and using the
	 * HTML Form associated with the specified Form
	 *
	 * @param patient
	 * @param form
	 * @param httpSession
	 * @throws Exception
	 */
	public FormEntrySession(Patient patient, Form form, HttpSession httpSession) throws Exception {
		this(patient, Mode.ENTER, null, httpSession);
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
	public FormEntrySession(Patient patient, Encounter encounter, Mode mode, HtmlForm htmlForm, HttpSession httpSession)
	        throws Exception {
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
	    HttpSession httpSession, boolean automaticClientSideValidation, boolean clientSideValidationHints) throws Exception {
		this(patient, mode, defaultLocation, httpSession);
		this.context.setAutomaticClientSideValidation(automaticClientSideValidation);
		this.context.setClientSideValidationHints(clientSideValidationHints);
		this.htmlForm = htmlForm;
		if (htmlForm != null) {
			if (htmlForm.getId() != null)
				this.formModifiedTimestamp = (htmlForm.getDateChanged() == null ? htmlForm.getDateCreated()
				        : htmlForm.getDateChanged()).getTime();
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
		}
		catch (CannotBePreviewedException ex) {
			return "Cannot be previewed";
		}
		catch (Exception ex) {
			if (ex.getCause() != null && ex.getCause() instanceof CannotBePreviewedException) {
				return "Cannot be run in preview mode: " + velocityExpression;
			} else {
				log.error("Exception evaluating velocity expression", ex);
				return "Velocity Error! " + ex.getMessage();
			}
		}
	}
	
	/**
	 * Creates the HTML for a HTML Form given the xml for the form. This method uses the
	 * HtmlFormGenerator to process any HTML Form Entry-specific tags and returns pure HTML that can be
	 * rendered by a browser
	 *
	 * @param xml the xml string representing the form we wish to create
	 * @return broswer-renderable html
	 * @throws Exception <strong>Should</strong> return correct xml with a greater than character in an
	 *             excludeIf tag <strong>Should</strong> return correct xml with a greater than
	 *             character in an includeIf tag <strong>Should</strong> return correct xml with a
	 *             compound expression in an excludeIf tag <strong>Should</strong> return correct xml
	 *             with a compound expression in an includeIf tag
	 */
	public String createForm(String xml) throws Exception {
		if (htmlForm != null) {
			context.getSchema().setName(htmlForm.getName());
			context.setUnmatchedMode(false);
		}
		xml = htmlGenerator.substituteCharacterCodesWithAsciiCodes(xml);
		xml = htmlGenerator.stripComments(xml);
		xml = htmlGenerator.convertSpecialCharactersWithinLogicAndVelocityTests(xml);
		xml = htmlGenerator.applyRoleRestrictions(xml);
		xml = htmlGenerator.applyMacros(this, xml);
		xml = htmlGenerator.processPages(this, xml);
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
	 * If the html form contains both PatientTags and Encounter tags then initialize it with the Patient
	 * and Encounter associated with the Form else if htmlform only contains PatientTags then initialize
	 * it with the Patient associated with the Form.
	 */
	public void prepareForSubmit() {
		
		submissionActions = new FormSubmissionActions();
		
		if (hasPatientTag() && !hasEncouterTag()) {
			try {
				submissionActions.beginPerson(patient);
			}
			catch (InvalidActionException e) {
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
			}
			catch (InvalidActionException e) {
				log.error("Programming error: should be no errors starting a patient and encounter", e);
			}
		}
		
	}
	
	/**
	 * Applies all the actions associated with a form submission--that is, create/update any Persons,
	 * Encounters, and Obs in the database as necessary, and enroll Patient in any programs as needed
	 * <p/>
	 * TODO: This requires that...
	 *
	 * @throws BadFormDesignException
	 */
	public void applyActions() throws BadFormDesignException {
		// if any encounter to be created by this form is missing a required field, throw an error
		// (If there's a widget but it was left blank, that would have been caught earlier--this
		// is for when there was no widget in the first place.)
		// the change here assumes that the encounterLocation and encounterProvider tags are validated elsewhere since they are
		// not required
		
		{
			for (Encounter e : submissionActions.getEncountersToCreate()) {
				if (e.getEncounterDatetime() == null) {
					throw new BadFormDesignException(
					        "Please check the design of your form to make sure it has the tag <b>&lt;encounterDate/&gt</b>");
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
						        + o.getConcept().getName(Context.getLocale()));
				}
				if (o.getLocation() == null && o.getEncounter() != null) {
					o.setLocation(o.getEncounter().getLocation());
				}
				if (o.hasGroupMembers())
					toCheck.addAll(o.getGroupMembers());
			}
		}
		
		// Handle orders
		
		// First, we void any of the previous orders that are indicated, and keep track of which are voided
		Set<Integer> voidedOrders = new HashSet<>();
		if (submissionActions.getOrdersToVoid() != null) {
			for (Order orderToVoid : submissionActions.getOrdersToVoid()) {
				Context.getOrderService().voidOrder(orderToVoid, "Voided by htmlformentry");
				voidedOrders.add(orderToVoid.getOrderId());
			}
		}
		
		// Next, we handle any new and revised orders
		if (submissionActions.getOrdersToCreate() != null) {
			for (Order order : submissionActions.getOrdersToCreate()) {
				Order previousOrder = order.getPreviousOrder();
				// If the previousOrder was just voided, then set the previous order to that order's previous order
				if (previousOrder != null && voidedOrders.contains(previousOrder.getOrderId())) {
					previousOrder = previousOrder.getPreviousOrder();
				}
				// If at this point there is no previous order, then adjust the action
				boolean processOrder = true;
				if (previousOrder == null) {
					if (order.getAction() == Order.Action.DISCONTINUE) {
						processOrder = false; // There is nothing to discontinue, it has already been voided, so do nothing
					} else {
						order.setAction(Order.Action.NEW); // If this was a revision, now it is new as previous is voided
					}
					
				}
				if (processOrder) {
					// If this is a RENEW, this isn't supported by the core OrderService, so we have to manually set dateStopped
					if (order.getAction() == Order.Action.RENEW) {
						Field dateStoppedField = ReflectionUtils.findField(Order.class, "dateStopped");
						dateStoppedField.setAccessible(true);
						// To be consistent with OrderService, set this to the second prior to the order activation date
						Date dateStopped = DateUtils.addSeconds(order.getDateActivated(), -1);
						ReflectionUtils.setField(dateStoppedField, previousOrder, dateStopped);
					}
					order.setPreviousOrder(previousOrder);
					order.setEncounter(encounter);
					if (encounter.getEncounterDatetime().after(order.getDateActivated())) {
						// HTML-834, the encounterDate time got reset to current time but the order.dateActicated was set at midnight
						if (DateUtils.isSameDay(encounter.getEncounterDatetime(), order.getDateActivated())
						        && !HtmlFormEntryUtil.hasTimeComponent(order.getDateActivated())) {
							// if encounter and order are on the same date and the dateActivated is midnight
							order.setDateActivated(encounter.getEncounterDatetime());
						}
					}
					encounter.addOrder(order);
				}
			}
		}
		
		// Handle conditions
		if (submissionActions.getConditionsToVoid() != null) {
			for (Condition condition : submissionActions.getConditionsToVoid()) {
				Context.getConditionService().voidCondition(condition, "Voided by htmlformentry");
			}
		}
		if (submissionActions.getConditionsToCreate() != null) {
			for (Condition condition : submissionActions.getConditionsToCreate()) {
				condition.setEncounter(encounter);
				encounter.addCondition(condition);
			}
		}
		
		Person newlyCreatedPerson = null;
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
				newlyCreatedPerson = Context.getPersonService().savePerson(p);
			}
		}
		if (submissionActions.getEncountersToCreate() != null) {
			for (Encounter e : submissionActions.getEncountersToCreate()) {
				if (form != null) {
					e.setForm(form);
					if (form.getEncounterType() != null)
						e.setEncounterType(form.getEncounterType());
				}
				// Due to the way ObsValidator works, if the associated person has been newly created, re-set it on the Obs
				if (newlyCreatedPerson != null) {
					for (Obs o : e.getAllObs(true)) {
						if (o.getPerson().equals(newlyCreatedPerson)) {
							o.setPerson(newlyCreatedPerson);
						}
					}
				}
				
				Context.getEncounterService().saveEncounter(encounter);
			}
		}
		
		// handle appointments (needs to happen after encounter is saved?)
		if (submissionActions.getAppointmentsToMarkCheckedInAndAssociateWithEncounter() != null) {
			new AppointmentsAbstractor().markAppointmentsAsCheckedInAndAssociateWithEncounter(
			    submissionActions.getAppointmentsToMarkCheckedInAndAssociateWithEncounter(), encounter);
		}
		
		if (submissionActions.getAppointmentsToDisassociateFromEncounter() != null) {
			new AppointmentsAbstractor().disassociateAppointmentsFromEncounter(
			    submissionActions.getAppointmentsToDisassociateFromEncounter(), encounter);
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
		
		// Handle program enrollments and state transitions
		
		List<PatientProgram> programsToCreate = submissionActions.getPatientProgramsToCreate();
		List<PatientProgram> programsToUpdate = submissionActions.getPatientProgramsToUpdate();
		List<PatientProgram> programsToComplete = submissionActions.getPatientProgramsToComplete();
		
		/*
		 * This iterates over the programsToCreate.  And for each of them, it compares the PatientProgram to the
		 * existing PatientProgram enrollments for the same patient and program.  If the patient is already actively
		 * enrolled in the on the new program's enrollment date, it removes the PatientProgram from the
		 * programsToCreate list and adds the matching, existing program to the programsToUpdate list
		 * If the patient does not have an active enrollment, but has an PatientProgram enrollment that starts later than
		 * the enrollment date of the program to create, remove the program to create, update the existing future program
		 * by shifting it's enrollment date to start when the new program was intended to start, then add to the
		 * programs to update.
		 * (TODO decide if this is the correct logic)
		 */
		for (Iterator<PatientProgram> ppCreateIter = programsToCreate.iterator(); ppCreateIter.hasNext();) {
			PatientProgram toCreate = ppCreateIter.next();
			if (toCreate.getDateEnrolled() == null) {
				toCreate.setDateEnrolled(encounter.getEncounterDatetime());
			}
			PatientProgram existing = HtmlFormEntryUtil.getCurrentOrNextFutureProgramEnrollment(toCreate.getPatient(),
			    toCreate.getProgram(), toCreate.getDateEnrolled());
			if (existing != null) {
				// Shift the existing enrollment date earlier if needed to match the new enrollment date
				if (toCreate.getDateEnrolled().before(existing.getDateEnrolled())) {
					existing.setDateEnrolled(toCreate.getDateEnrolled());
				}
				programsToUpdate.add(existing);
				ppCreateIter.remove();
			}
		}
		
		// Iterate over programs to complete and set completion date and outcome if necessary from Obs on form
		for (PatientProgram pp : programsToComplete) {
			if (pp.getDateCompleted() == null) {
				pp.setDateCompleted(encounter.getEncounterDatetime());
			}
			// If an appropriate outcome has been recorded, set this on the patient program
			Concept outcomesConcept = pp.getProgram().getOutcomesConcept();
			if (outcomesConcept != null) {
				List<Obs> outcomeObs = findObsForConcept(outcomesConcept, submissionActions.getObsToCreate());
				if (outcomeObs.size() == 1) {
					pp.setOutcome(outcomeObs.get(0).getValueCoded());
				} else if (outcomeObs.size() > 1) {
					throw new IllegalStateException(
					        "Unable to complete patient program as multiple outcome observations are recorded: "
					                + outcomeObs);
				}
			}
		}
		
		// Update patient program updates with state changes
		for (ProgramWorkflowState state : submissionActions.getProgramWorkflowStatesToTransition()) {
			
			// This will be the patient program that we transition to the given state
			PatientProgram pp = null;
			
			ProgramWorkflow workflow = state.getProgramWorkflow();
			Program program = workflow.getProgram();
			Date previousEncounterDate = context.getPreviousEncounterDate();
			Date encounterDate = encounter.getEncounterDatetime();
			
			// Try to determine if this is intended as an edit to an existing state
			// by checking if a state in this workflow started at the same date(time) as the previous encounter date
			PatientState stateToEdit = null;
			if (Mode.EDIT.equals(context.getMode()) && previousEncounterDate != null) {
				PatientState ps = HtmlFormEntryUtil.getPatientStateOnDate(patient, workflow, previousEncounterDate);
				if (ps != null && ps.getStartDate() != null) {
					Date stateDate = ps.getStartDate();
					if (HtmlFormEntryUtil.hasTimeComponent(stateDate)) {
						if (DateUtils.isSameInstant(stateDate, previousEncounterDate)) {
							stateToEdit = ps;
						}
					} else {
						if (DateUtils.isSameDay(stateDate, previousEncounterDate)) {
							stateToEdit = ps;
						}
					}
				}
			}
			
			// If this _is_ determined to be an edit to an existing state, edit it
			if (stateToEdit != null) {
				
				// If we are editing a state, makes sure the associated patient program is marked to update
				pp = stateToEdit.getPatientProgram();
				if (!programsToUpdate.contains(pp)) {
					programsToUpdate.add(pp);
				}
				
				// If the encounter date has shifted earlier, and there is an existing patient state on the
				// new encounter date, and it differs from the state on the old encounter date, end it, and void
				// any of the states for this workflow that may have started between the new and old encounter dates
				if (encounterDate.before(previousEncounterDate)) {
					PatientState existing = HtmlFormEntryUtil.getPatientStateOnDate(pp, workflow, encounterDate);
					if (existing != null && !existing.equals(stateToEdit)) {
						existing.setEndDate(encounterDate);
					}
					for (PatientState ps : pp.statesInWorkflow(workflow, false)) {
						if (!ps.equals(stateToEdit)
						        && (ps.getStartDate().after(encounterDate)
						                || ps.getStartDate().compareTo(encounterDate) == 0)
						        && ps.getStartDate().before(previousEncounterDate)) {
							ps.setVoided(true);
							ps.setVoidedBy(Context.getAuthenticatedUser());
							ps.setVoidReason("voided during htmlformentry submission");
						}
					}
				}
				
				// if the encounter date has been moved later
				if (encounterDate.after(previousEncounterDate)) {
					// make sure we aren't trying to move the state start date past its end date
					if (stateToEdit.getEndDate() != null && encounterDate.after(stateToEdit.getEndDate())) {
						throw new FormEntryException("Cannot move encounter date ahead of end date of current active state");
					}
					
					// if there is a state that ended on the previous encounter date, its end date needs to be set to the new encounter date
					for (PatientState ps : pp.statesInWorkflow(workflow, false)) {
						if (!ps.equals(stateToEdit) && ps.getEndDate().compareTo(previousEncounterDate) == 0) {
							ps.setEndDate(encounterDate);
						}
					}
				}
				
				// change the state if necessary
				if (!state.equals(stateToEdit.getState())) {
					stateToEdit.setState(state);
				}
				
				// update the state start date
				stateToEdit.setStartDate(encounterDate);
				
				// roll the program enrollment date earlier if necessary
				if (pp.getDateEnrolled().after(stateToEdit.getStartDate())) {
					pp.setDateEnrolled(stateToEdit.getStartDate());
				}
			}
			// Otherwise, if this is not determined to be an edit to an existing state
			// determine if the associated PatientProgram is already set to be created, updated, or completed
			else {
				if (pp == null) {
					pp = HtmlFormEntryUtil.getPatientProgramByProgram(programsToCreate, program);
				}
				if (pp == null) {
					pp = HtmlFormEntryUtil.getPatientProgramByProgram(programsToUpdate, program);
				}
				if (pp == null) {
					pp = HtmlFormEntryUtil.getPatientProgramByProgram(programsToComplete, program);
				}
				
				// If not already in the programs to create, update, or complete, see if there is already an active program
				// We consider here both programs that are active on the encounter date and programs that start after
				// the encounter date, as otherwise we'd have overlapping or abutting enrollments in the same program
				if (pp == null) {
					pp = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(patient, program, encounterDate);
					if (pp != null) {
						programsToUpdate.add(pp);
					} else {
						pp = new PatientProgram();
						pp.setPatient(patient);
						pp.setProgram(program);
						pp.setDateEnrolled(encounterDate);
						
						// If there is another enrollment in the same program in the future, end this new enrollment at the start of that enrollment
						PatientProgram nextEnrollment = HtmlFormEntryUtil.getCurrentOrNextFutureProgramEnrollment(patient,
						    program, encounterDate);
						if (nextEnrollment != null && nextEnrollment.getDateEnrolled().after(encounterDate)) {
							pp.setDateCompleted(nextEnrollment.getDateEnrolled());
						}
						
						programsToCreate.add(pp);
					}
				}
				
				// If the state to transition into is not the same as the current state, transition into it
				PatientState stateOnEncounterDate = HtmlFormEntryUtil.getPatientStateOnDate(pp, workflow, encounterDate);
				if (stateOnEncounterDate == null || !stateOnEncounterDate.getState().equals(state)) {
					
					// Because htmlforms might update existing programs, and have retrospective entry, we do not
					// simply call transitionToState here on the patientProgram.  Rather, we iterate over the states
					// in the workflow, and try to insert the new state into the existing states where appropriate
					
					PatientState newState = new PatientState();
					newState.setPatientProgram(pp);
					newState.setState(state);
					newState.setStartDate(encounterDate);
					
					PatientState previousState = null;
					PatientState nextState = null;
					
					for (PatientState currentState : pp.statesInWorkflow(workflow, false)) {
						
						Date newStartDate = newState.getStartDate();
						Date currentStartDate = currentState.getStartDate();
						Date currentEndDate = currentState.getEndDate();
						
						if (currentEndDate != null) {
							if (currentEndDate.after(newStartDate)) {
								if (currentStartDate.after(newStartDate)) {
									nextState = currentState;
									break;
								} else {
									previousState = currentState;
								}
							} else {
								previousState = currentState;
							}
						} else if (currentStartDate.after(newStartDate)) {
							nextState = currentState;
							break;
						} else {
							previousState = currentState;
							nextState = null;
							break;
						}
					}
					
					if (nextState == null) {
						if (previousState != null) {
							previousState.setEndDate(newState.getStartDate());
						}
					} else {
						if (previousState != null) {
							previousState.setEndDate(newState.getStartDate());
						}
						newState.setEndDate(nextState.getStartDate());
					}
					
					pp.getStates().add(newState);
					
					if (encounterDate.before(pp.getDateEnrolled())) {
						pp.setDateEnrolled(encounterDate);
					}
				}
			}
		}
		
		// Save all program changes to the database
		for (PatientProgram pp : programsToCreate) {
			Context.getProgramWorkflowService().savePatientProgram(pp);
		}
		
		for (PatientProgram pp : programsToComplete) {
			Context.getProgramWorkflowService().savePatientProgram(pp);
		}
		
		for (PatientProgram pp : programsToUpdate) {
			Context.getProgramWorkflowService().savePatientProgram(pp);
		}
		
		ObsService obsService = Context.getObsService();
		
		boolean patientUpdateRequired = submissionActions.getPatientUpdateRequired();
		
		if (submissionActions.getObsToVoid() != null) {
			for (Obs o : submissionActions.getObsToVoid()) {
				if (o.getEncounter() == null) {
					patientUpdateRequired = true;
				}
				if (log.isDebugEnabled())
					log.debug("voiding obs: " + o.getObsId());
				voidObsAndChildren(o);
				// if o was in a group and that group has no obs left, void the group
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
					}
					catch (Exception ex) {
						throw new RuntimeException("Unable to void encounter.", ex);
					}
				}
				Context.getEncounterService().saveEncounter(encounter);
			} else if (submissionActions.getObsToCreate() != null) {
				// this may not work right due to savehandlers (similar error to HTML-135) but this branch is
				// unreachable until html forms are allowed to edit data without an encounter
				for (Obs o : submissionActions.getObsToCreate())
					obsService.saveObs(o, "Created by htmlformentry");
			}
		}
		
		if (submissionActions.getIdentifiersToVoid() != null) {
			for (PatientIdentifier patientIdentifier : submissionActions.getIdentifiersToVoid()) {
				patientIdentifier.setVoided(true);
				patientIdentifier.setVoidedBy(Context.getAuthenticatedUser());
				patientIdentifier.setVoidReason(getForm().getName()); // Use form name as reason
				patientIdentifier.setDateVoided(new Date());
			}
		}
		
		// save the patient
		// TODO: we are having some issues here when updating a Patient and an Encounter via an HTML form due recently discovered problems with the way
		// we are using Hibernate.  We rely on Spring AOP saveHandlers and the save methods themselves to set some key parameters like date created--and
		// sometimes a flush can be called before these methods are called. This should be resolved once we move save handling out of Spring AOP and
		// into a Hibernate Interceptor (which happens in 1.9)
		if (patient != null && patientUpdateRequired) {
			Context.getPersonService().savePerson(patient);
		}
		
		// exit the patient from care or process patient's death
		if (submissionActions.getExitFromCareProperty() != null) {
			ExitFromCareProperty exitFromCareProperty = submissionActions.getExitFromCareProperty();
			if (exitFromCareProperty.getCauseOfDeathConcept() != null) {
				Context.getPatientService().processDeath(this.getPatient(), exitFromCareProperty.getDateOfExit(),
				    exitFromCareProperty.getCauseOfDeathConcept(), exitFromCareProperty.getOtherReason());
			} else {
				HtmlFormEntryService hfes = Context.getService(HtmlFormEntryService.class);
				hfes.exitFromCare(this.getPatient(), exitFromCareProperty.getDateOfExit(),
				    exitFromCareProperty.getReasonExitConcept());
			}
		}
		
		// handle any custom actions (for an example of a custom action, see: https://github.com/PIH/openmrs-module-appointmentschedulingui/commit/e2cda8de1caa8a45d319ae4fbf7714c90c9adb8b)
		if (submissionActions.getCustomFormSubmissionActions() != null) {
			for (CustomFormSubmissionAction customFormSubmissionAction : submissionActions
			        .getCustomFormSubmissionActions()) {
				customFormSubmissionAction.applyAction(this);
			}
		}
		
	}
	
	private void voidObs(Obs obsToVoid) {
		if (BooleanUtils.isNotTrue(obsToVoid.getVoided())) {
			obsToVoid.setVoided(true);
			obsToVoid.setDateVoided(new Date());
			obsToVoid.setVoidedBy(Context.getAuthenticatedUser());
			obsToVoid.setVoidReason("htmlformentry");
		}
	}
	
	private void voidObsAndChildren(Obs obsToVoid) {
		if (BooleanUtils.isNotTrue(obsToVoid.getVoided())) {
			obsToVoid.setVoided(true);
			obsToVoid.setDateVoided(new Date());
			obsToVoid.setVoidedBy(Context.getAuthenticatedUser());
			obsToVoid.setVoidReason("htmlformentry");
		}
		if (obsToVoid.isObsGrouping()) {
			for (Obs childObs : obsToVoid.getGroupMembers()) {
				voidObsAndChildren(childObs);
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
			// probably should be able to just test if group.getGroupMembers() == 0 since
			// getGroupMembers only returns non-voided members?
			boolean allObsVoided = true;
			for (Obs member : group.getGroupMembers()) {
				allObsVoided = allObsVoided && BooleanUtils.isTrue(member.getVoided());
			}
			if (allObsVoided) {
				voidObs(group);
			}
			voidObsGroupIfAllChildObsVoided(group.getObsGroup());
		}
	}
	
	/**
	 * @return any obs from the passed list whose question is the passed concept
	 */
	private List<Obs> findObsForConcept(Concept concept, List<Obs> obs) {
		List<Obs> ret = new ArrayList<Obs>();
		if (concept != null) {
			for (Obs o : obs) {
				if (o.getConcept().equals(concept)) {
					ret.add(o);
				}
			}
		}
		return ret;
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
	 * Return the form display HTML associated with the session. This has the important side-effect of
	 * having tags populate the submissionActions list, so you must ensure this is called before you
	 * attempt to validate or process a form's submission. The first time you call this method on an
	 * instance will generate the HTML and cache it, so that subsequent calls are fast (and so that the
	 * submissionActions list is only populated once).
	 */
	public String getHtmlToDisplay() throws Exception {
		if (htmlToDisplay == null) {
			htmlToDisplay = createForm(xmlDefinition);
		}
		return htmlToDisplay;
	}
	
	/**
	 * Creates the Javascript necessary to set form fields to the values entered during last submission
	 * Used to maintain previously-entered field values when redisplaying a form with validation errors
	 */
	public String getSetLastSubmissionFieldsJavascript() {
		HttpServletRequest lastSubmission = submissionController.getLastSubmission();
		if (lastSubmission == null) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder();
			
			// Get all of the widgets registered, but remove those Widgets that are handled directly by other widgets
			Set<OrderWidget> orderWidgets = new HashSet<>();
			Map<Widget, String> widgets = new HashMap<>(context.getFieldNames());
			for (Widget w : context.getFieldNames().keySet()) {
				if (w instanceof OrderWidget) {
					orderWidgets.add((OrderWidget) w);
				} else {
					widgets.put(w, context.getFieldNames().get(w));
				}
			}
			for (OrderWidget orderWidget : orderWidgets) {
				widgets.keySet().removeAll(orderWidget.getWidgets().values());
			}
			
			// iterate through all the widgets and set their values based on the values in the last submission
			// if there is no value in the last submission, explicitly set the value as empty to override any default values
			for (Map.Entry<Widget, String> entry : widgets.entrySet()) {
				Widget widget = entry.getKey();
				String widgetFieldName = entry.getValue();
				String val = lastSubmission.getParameter(widgetFieldName);
				
				// note that for each widget we set, we also trigger the change event on that widget
				// this is so any custom change handlers that a widget or tag may configure are called
				// when we set a value here; this is specifically used to make sure we trigger the change
				// handlers configured by the <exitFromCare> tag
				
				if (val != null) {
					// special case to set the display field when autocomplete is used
					if (AutocompleteWidget.class.isAssignableFrom(widget.getClass())) {
						Class widgetClass = ((AutocompleteWidget) widget).getOptionClass();
						
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
								        + (location == null ? "" : JavaScriptUtils.javaScriptEscape(location.getName()))
								        + "\");\n");
								sb.append(
								    "$j('#" + widgetFieldName + "_hid" + "').val(\""
								            + (location == null ? ""
								                    : JavaScriptUtils.javaScriptEscape(location.getId().toString()))
								            + "\");\n");
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
								        + (provider == null ? ""
								                : JavaScriptUtils.javaScriptEscape(provider.getPersonName().getFullName()))
								        + "\");\n");
								sb.append(
								    "$j('#" + widgetFieldName + "_hid" + "').val(\""
								            + (provider == null ? ""
								                    : JavaScriptUtils.javaScriptEscape(provider.getId().toString()))
								            + "\");\n");
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
						        + (concept == null ? "" : JavaScriptUtils.javaScriptEscape(concept.getDisplayString()))
						        + "\");\n");
						sb.append("$j('#" + widgetFieldName + "_hid" + "').val(\""
						        + (concept == null ? "" : JavaScriptUtils.javaScriptEscape(concept.getId().toString()))
						        + "\");\n");
						sb.append("$j('#" + widgetFieldName + "').change();\n");
					} else {
						// set the value of the widget based on it's name
						sb.append(
						    "setValueByName('" + widgetFieldName + "', '" + JavaScriptUtils.javaScriptEscape(val) + "');\n");
						sb.append("$j('#" + widgetFieldName + "').change();\n");
					}
					
				} else {
					if (AutocompleteWidget.class.isAssignableFrom(widget.getClass())) {
						sb.append("$j('#" + widgetFieldName + "').val('');\n");
						sb.append("$j('#" + widgetFieldName + "_hid" + "').val('');\n");
						sb.append("$j('#" + widgetFieldName + "').change();\n");
					} else if (ConceptSearchAutocompleteWidget.class.isAssignableFrom(widget.getClass())) {
						sb.append("$j('#" + widgetFieldName + "').val('');\n");
						sb.append("$j('#" + widgetFieldName + "_hid" + "').val('');\n");
						sb.append("$j('#" + widgetFieldName + "').change();\n");
					} else {
						sb.append("setValueByName('" + widgetFieldName + "', '');\n");
						sb.append("$j('#" + widgetFieldName + "').change();\n");
					}
				}
			}
			
			// Put these at the end to ensure they load after the encounterDate and other widgets
			for (OrderWidget orderWidget : orderWidgets) {
				sb.append(orderWidget.getLastSubmissionJavascript(context, lastSubmission));
			}
			return sb.toString();
		}
	}
	
	/**
	 * Returns a fragment of javascript that will display any error widgets that had errors on the last
	 * submission.
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
	 * Calculates the date an encounter was last modified by checking the creation and voided times of
	 * all Obs and Orders associated with the Encounter
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
	
	public void setAttributes(Map<String, Object> moreAttributes) {
		if (moreAttributes != null) {
			attributes.putAll(moreAttributes);
		}
	}
	
	public void addToVelocityContext(String key, Object value) {
		velocityContext.put(key, value);
	}
	
	public String getAfterSaveUrlTemplate() {
		return afterSaveUrlTemplate;
	}
	
	/**
	 * After successfully submitting and saving a form, go to this url. (Null means that the web
	 * application should decide, based on its standard workflow.) This will be prepended with
	 * CONTEXTPATH/ and appropriate values will be substituted in for "{{patient.id}}" and
	 * "{{encounter.id}}"
	 *
	 * @param afterSaveUrlTemplate
	 */
	public void setAfterSaveUrlTemplate(String afterSaveUrlTemplate) {
		this.afterSaveUrlTemplate = afterSaveUrlTemplate;
	}
	
	public String getXmlDefinition() {
		return xmlDefinition;
	}
	
	public void setForm(Form form) {
		this.form = form;
	}
	
	public void setHtmlForm(HtmlForm htmlForm) {
		this.htmlForm = htmlForm;
		if (form != null) {
			this.htmlForm.setForm(form);
		} else {
			this.form = htmlForm.getForm();
		}
	}
	
	public String getPatientPersonName() {
		return StringEscapeUtils.escapeHtml(patient.getPersonName().getFullName());
	}
	
	public String getFormName() {
		return StringEscapeUtils.escapeHtml(form.getName());
	}
	
	public String getEncounterFormName() {
		return StringEscapeUtils.escapeHtml(encounter.getForm().getName());
	}
	
	public String getFormEncounterTypeName() {
		return StringEscapeUtils.escapeHtml(form.getEncounterType().getName());
	}
	
	public String getEncounterEncounterTypeName() {
		return StringEscapeUtils.escapeHtml(encounter.getEncounterType().getName());
	}
	
	public String getEncounterLocationName() {
		return StringEscapeUtils.escapeHtml(encounter.getLocation() == null ? "" : encounter.getLocation().getName());
	}
	
	/**
	 * Generates the form path based on the form name, form version, form field path and control
	 * counter. The form path will have the following format: "MyForm.1.0/my_condition_tag-0"
	 *
	 * @param controlId The control id, eg "my_condition_tag"
	 * @param controlCounter The control counter, an integer
	 * @return The constructed form path
	 */
	public String generateControlFormPath(String controlId, Integer controlCounter) {
		String formField = "";
		
		// Validate if the form is not null
		if (this.getForm() == null) {
			throw new IllegalStateException("The form entry session has a null form.");
		}
		
		// Create form path
		String formName = this.getForm().getName();
		String formVersion = this.getForm().getVersion();
		formField = formName + "." + formVersion + "/";
		
		// Create control form path
		formField += controlId + "-" + controlCounter;
		
		return formField;
	}
	
	public TagRegister getTagRegister() {
		return htmlGenerator.getTagRegister();
	}
}
