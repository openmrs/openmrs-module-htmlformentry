package org.openmrs.module.htmlformentry.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.customdatatype.datatype.FreeTextDatatype;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.SerializableFormObject;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_DESCRIPTION_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_ENCOUNTER_TYPE_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_PUBLISHED_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_DATA_TYPE_CLASS;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_DATA_TYPE_CONFIG;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_DELIMITER;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_HANDLER_CLASS;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_HANDLER_CONFIG;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_NAME;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_PREFIX;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RESOURCE_VALUE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_RETIRED_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_UUID_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_VERSION_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.HTML_FORM_TAG;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.HTML_FORM_UUID_ATTRIBUTE;

/**
 * Standard implementation of the HtmlFormEntryService
 */
public class HtmlFormEntryServiceImpl extends BaseOpenmrsService implements HtmlFormEntryService {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private HtmlFormEntryDAO dao;
	
	private static Map<String, TagHandler> handlers = new LinkedHashMap<String, TagHandler>();
	
	private String basicFormXmlTemplate;
	
	private Map<String, Integer> conceptMappingCache = new HashMap<String, Integer>();
	
	/*
	 * Optimization to minimize database hits for the needs-name-and-description-migration check.
	 * Once all forms have been migrated, we no longer need to hit the database on further checks
	 * because there is no way to add more un-migrated forms. (In theory someone could add some 
	 * directly to the database, so we use an instance variable here that will be reset whenever
	 * the system is restarted or the module is reloaded.
	 */
	private boolean nameAndDescriptionMigrationDone = false;
	
	@Override
	public void addHandler(String tagName, TagHandler handler) {
		handlers.put(tagName, handler);
	}
	
	@Override
	public TagHandler getHandlerByTagName(String tagName) {
		return handlers.get(tagName);
	}
	
	@Override
	public Map<String, TagHandler> getHandlers() {
		return handlers;
	}
	
	/**
	 * Sets the tag handlers
	 */
	public void setHandlers(Map<String, TagHandler> handlersToSet) {
		handlers.putAll(handlersToSet);
	}
	
	/**
	 * Sets the DAO
	 * 
	 * @param dao
	 */
	public void setDao(HtmlFormEntryDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @return the basicFormXmlTemplate
	 */
	public String getBasicFormXmlTemplate() {
		if (basicFormXmlTemplate == null) {
			try (InputStream is = getClass().getClassLoader().getResourceAsStream("basicFormXmlTemplate.xml")) {
				basicFormXmlTemplate = IOUtils.toString(is, "UTF-8");
			}
			catch (Exception e) {
				log.warn("Unable to load basic form template from classpath", e);
			}
		}
		return basicFormXmlTemplate;
	}
	
	/**
	 * @param basicFormXmlTemplate the basicFormXmlTemplate to set
	 */
	public void setBasicFormXmlTemplate(String basicFormXmlTemplate) {
		this.basicFormXmlTemplate = basicFormXmlTemplate;
	}
	
	@Override
	@Transactional(readOnly = true)
	public HtmlForm getHtmlForm(Integer id) {
		return dao.getHtmlForm(id);
	}
	
	@Override
	@Transactional(readOnly = true)
	public HtmlForm getHtmlFormByUuid(String uuid) {
		return dao.getHtmlFormByUuid(uuid);
	}
	
	/**
	 * For the given form xml, parse this into an htmlform and save
	 */
	@Override
	@Transactional
	public HtmlForm saveHtmlFormFromXml(String xmlData) {
		boolean isNewForm = false;
		boolean hasChanges = false;
		Document doc;
		try {
			doc = HtmlFormEntryUtil.stringToDocument(xmlData);
		}
		catch (Exception e) {
			throw new RuntimeException("Error parsing XML: " + xmlData, e);
		}
		Node htmlFormNode = HtmlFormEntryUtil.findChild(doc, HTML_FORM_TAG);
		if (htmlFormNode == null) {
			throw new RuntimeException("Could not find tag: " + HTML_FORM_TAG);
		}
		
		Map<String, String> htmlFormAttributes = HtmlFormEntryUtil.getNodeAttributes(htmlFormNode);
		
		// Create or update the form
		
		String formUuid = htmlFormAttributes.remove(FORM_UUID_ATTRIBUTE);
		if (formUuid == null) {
			throw new IllegalArgumentException(FORM_UUID_ATTRIBUTE + " is required");
		}
		Form form = Context.getFormService().getFormByUuid(formUuid);
		if (form == null) {
			form = new Form();
			form.setUuid(formUuid);
			isNewForm = true;
			hasChanges = true;
		}
		String formName = htmlFormAttributes.remove("formName");
		if (!OpenmrsUtil.nullSafeEquals(form.getName(), formName)) {
			form.setName(formName);
			hasChanges = true;
		}
		String formDescription = htmlFormAttributes.remove(FORM_DESCRIPTION_ATTRIBUTE);
		if (!OpenmrsUtil.nullSafeEquals(form.getDescription(), formDescription)) {
			form.setDescription(formDescription);
			hasChanges = true;
		}
		String formVersion = htmlFormAttributes.remove(FORM_VERSION_ATTRIBUTE);
		if (!OpenmrsUtil.nullSafeEquals(form.getVersion(), formVersion)) {
			form.setVersion(formVersion);
			hasChanges = true;
		}
		Boolean formPublished = "true".equalsIgnoreCase(htmlFormAttributes.remove(FORM_PUBLISHED_ATTRIBUTE));
		if (!OpenmrsUtil.nullSafeEquals(form.getPublished(), formPublished)) {
			form.setPublished(formPublished);
			hasChanges = true;
		}
		Boolean formRetired = "true".equalsIgnoreCase(htmlFormAttributes.remove(FORM_RETIRED_ATTRIBUTE));
		if (!OpenmrsUtil.nullSafeEquals(form.getRetired(), formRetired)) {
			form.setRetired(formRetired);
			if (formRetired && StringUtils.isBlank(form.getRetireReason())) {
				form.setRetireReason("Retired set in form xml");
			}
			hasChanges = true;
		}
		String formEncounterType = htmlFormAttributes.remove(FORM_ENCOUNTER_TYPE_ATTRIBUTE);
		EncounterType encounterType = null;
		if (formEncounterType != null) {
			encounterType = HtmlFormEntryUtil.getEncounterType(formEncounterType);
		}
		if (encounterType != null && !OpenmrsUtil.nullSafeEquals(form.getEncounterType(), encounterType)) {
			form.setEncounterType(encounterType);
			hasChanges = true;
		}
		
		// Create or update the htmlform
		
		HtmlForm htmlForm = (isNewForm ? null : getHtmlFormByForm(form));
		if (htmlForm == null) {
			htmlForm = new HtmlForm();
			hasChanges = true;
		}
		if (hasChanges) {
			htmlForm.setForm(form);
		}
		
		// if there is a html form uuid specified, make sure the htmlform uuid is set to that value
		String htmlformUuid = htmlFormAttributes.remove(HTML_FORM_UUID_ATTRIBUTE);
		if (StringUtils.isNotBlank(htmlformUuid) && !OpenmrsUtil.nullSafeEquals(htmlformUuid, htmlForm.getUuid())) {
			htmlForm.setUuid(htmlformUuid);
			hasChanges = true;
		}
		
		if (!OpenmrsUtil.nullSafeEquals(htmlForm.getRetired(), formRetired)) {
			htmlForm.setRetired(formRetired);
			if (formRetired && StringUtils.isBlank(htmlForm.getRetireReason())) {
				htmlForm.setRetireReason("Retired set in form xml");
			}
			hasChanges = true;
		}
		
		if (!StringUtils.trimToEmpty(htmlForm.getXmlData()).equals(StringUtils.trimToEmpty(xmlData))) {
			// trim because if the file ends with a newline the db will have trimmed it
			htmlForm.setXmlData(xmlData);
			hasChanges = true;
		}
		
		// Form resources are defined via attributes that start with resource-<unique-key>-<resource-property>
		
		Map<String, String> formResourceNamesToAttributePrefixes = new LinkedHashMap<>();
		for (String attribute : htmlFormAttributes.keySet()) {
			if (attribute.startsWith(FORM_RESOURCE_PREFIX + FORM_RESOURCE_DELIMITER)) {
				String[] parts = attribute.split(FORM_RESOURCE_DELIMITER, 3);
				if (parts.length < 3) {
					throw new RuntimeException("Could not parse form resource attribute: " + attribute);
				}
				if (parts[2].equals(FORM_RESOURCE_NAME)) {
					String prefix = parts[0] + FORM_RESOURCE_DELIMITER + parts[1] + FORM_RESOURCE_DELIMITER;
					formResourceNamesToAttributePrefixes.put(htmlFormAttributes.get(attribute), prefix);
				}
			}
		}
		
		Map<String, FormResource> resourcesByName = new HashMap<>();
		if (!isNewForm) {
			for (FormResource formResource : Context.getFormService().getFormResourcesForForm(form)) {
				resourcesByName.put(formResource.getName(), formResource);
			}
		}
		
		for (String resourceName : formResourceNamesToAttributePrefixes.keySet()) {
			String prefix = formResourceNamesToAttributePrefixes.get(resourceName);
			htmlFormAttributes.remove(prefix + FORM_RESOURCE_NAME);
			String resourceValue = htmlFormAttributes.remove(prefix + FORM_RESOURCE_VALUE);
			String resourceDataTypeClass = htmlFormAttributes.remove(prefix + FORM_RESOURCE_DATA_TYPE_CLASS);
			String resourceDataTypeConfig = htmlFormAttributes.remove(prefix + FORM_RESOURCE_DATA_TYPE_CONFIG);
			String resourceHandlerClass = htmlFormAttributes.remove(prefix + FORM_RESOURCE_HANDLER_CLASS);
			String resourceHandlerConfig = htmlFormAttributes.remove(prefix + FORM_RESOURCE_HANDLER_CONFIG);
			
			if (StringUtils.isBlank(resourceDataTypeClass)) {
				resourceDataTypeClass = FreeTextDatatype.class.getName();
			}
			
			FormResource resource = resourcesByName.get(resourceName);
			if (resource == null) {
				resource = new FormResource();
				resource.setName(resourceName);
				resource.setForm(form);
				resource.setValueReferenceInternal(resourceValue);
				resource.setDatatypeClassname(resourceDataTypeClass);
				resource.setDatatypeConfig(resourceDataTypeConfig);
				resource.setPreferredHandlerClassname(resourceHandlerClass);
				resource.setHandlerConfig(resourceHandlerConfig);
				resourcesByName.put(resourceName, resource);
				hasChanges = true;
			} else {
				if (StringUtils.isBlank(resourceValue)) {
					resourcesByName.remove(resourceName);
					hasChanges = true;
				} else {
					if (!resourceValue.equals(resource.getValueReference())) {
						resource.setValueReferenceInternal(resourceValue);
						hasChanges = true;
					}
					if (!resourceValue.equals(resource.getValueReference())) {
						resource.setValueReferenceInternal(resourceValue);
						hasChanges = true;
					}
					if (!OpenmrsUtil.nullSafeEquals(resource.getDatatypeClassname(), resourceDataTypeClass)) {
						resource.setDatatypeClassname(resourceDataTypeClass);
						hasChanges = true;
					}
					if (!OpenmrsUtil.nullSafeEquals(resource.getDatatypeConfig(), resourceDataTypeConfig)) {
						resource.setDatatypeConfig(resourceDataTypeConfig);
						hasChanges = true;
					}
					if (!OpenmrsUtil.nullSafeEquals(resource.getPreferredHandlerClassname(), resourceHandlerClass)) {
						resource.setPreferredHandlerClassname(resourceHandlerClass);
						hasChanges = true;
					}
					if (!OpenmrsUtil.nullSafeEquals(resource.getHandlerConfig(), resourceHandlerConfig)) {
						resource.setHandlerConfig(resourceHandlerConfig);
						hasChanges = true;
					}
				}
			}
		}
		
		if (!hasChanges) {
			return htmlForm;
		}
		return saveHtmlForm(htmlForm, resourcesByName.values());
	}
	
	@Override
	@Transactional
	public HtmlForm saveHtmlForm(HtmlForm htmlForm) {
		if (htmlForm.getCreator() == null)
			htmlForm.setCreator(Context.getAuthenticatedUser());
		if (htmlForm.getDateCreated() == null)
			htmlForm.setDateCreated(new Date());
		if (htmlForm.getId() != null) {
			htmlForm.setChangedBy(Context.getAuthenticatedUser());
			htmlForm.setDateChanged(new Date());
		}
		Context.getFormService().saveForm(htmlForm.getForm());
		return dao.saveHtmlForm(htmlForm);
	}
	
	@Override
	@Transactional
	public HtmlForm saveHtmlForm(HtmlForm htmlForm, Collection<FormResource> formResources) {
		htmlForm = Context.getService(HtmlFormEntryService.class).saveHtmlForm(htmlForm);
		Set<String> savedResourceNames = new HashSet<>();
		for (FormResource formResource : formResources) {
			Context.getFormService().saveFormResource(formResource);
			savedResourceNames.add(formResource.getName());
		}
		for (FormResource formResource : Context.getFormService().getFormResourcesForForm(htmlForm.getForm())) {
			if (!savedResourceNames.contains(formResource.getName())) {
				Context.getFormService().purgeFormResource(formResource);
			}
		}
		return htmlForm;
	}
	
	@Override
	@Transactional
	public void purgeHtmlForm(HtmlForm htmlForm) {
		dao.deleteHtmlForm(htmlForm);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<HtmlForm> getAllHtmlForms() {
		return dao.getAllHtmlForms();
	}
	
	@Override
	@Transactional(readOnly = true)
	public HtmlForm getHtmlFormByForm(Form form) {
		return dao.getHtmlFormByForm(form);
	}
	
	@Override
	@Transactional(readOnly = true)
	public boolean needsNameAndDescriptionMigration() {
		if (nameAndDescriptionMigrationDone) {
			return false;
		} else {
			boolean needsMigration = dao.needsNameAndDescriptionMigration();
			if (!needsMigration)
				nameAndDescriptionMigrationDone = true;
			return needsMigration;
		}
	}
	
	/**
	 * @see HtmlFormEntryService#getStartingFormXml(HtmlForm)
	 */
	@Override
	@Transactional(readOnly = true)
	public String getStartingFormXml(HtmlForm form) {
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
		    "org.apache.velocity.runtime.log.CommonsLogLogChute");
		velocityEngine.setProperty(CommonsLogLogChute.LOGCHUTE_COMMONS_LOG_NAME, "htmlformentry_velocity");
		try {
			velocityEngine.init();
		}
		catch (Exception e) {
			log.error("Error initializing Velocity engine", e);
		}
		
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("htmlForm", form);
		velocityContext.put("identifierTypes", Context.getPatientService().getAllPatientIdentifierTypes(false));
		velocityContext.put("personAttributeTypes", Context.getPersonService().getAllPersonAttributeTypes(false));
		
		StringWriter writer = new StringWriter();
		try {
			velocityEngine.evaluate(velocityContext, writer, "Basic HTML Form", getBasicFormXmlTemplate());
			return writer.toString();
		}
		catch (Exception ex) {
			log.error("Exception evaluating velocity expression", ex);
			return "<htmlform>Velocity Error! " + ex.getMessage() + "</htmlform>";
		}
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<PersonStub> getUsersAsPersonStubs(String roleName) {
		return dao.getUsersAsPersonStubs(roleName);
	}
	
	@Override
	@Transactional(readOnly = true)
	public OpenmrsObject getItemByUuid(Class<? extends OpenmrsObject> type, String uuid) {
		return dao.getItemByUuid(type, uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public OpenmrsObject getItemById(Class<? extends OpenmrsObject> type, Integer id) {
		return dao.getItemById(type, id);
	}
	
	@Override
	@Transactional(readOnly = true)
	public OpenmrsObject getItemByName(Class<? extends OpenmrsMetadata> type, String name) {
		return dao.getItemByName(type, name);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<Integer> getPersonIdsHavingAttributes(String attribute, String attributeValue) {
		return dao.getPersonIdHavingAttributes(attribute, attributeValue);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<PersonStub> getPeopleAsPersonStubs(List<String> attributes, List<String> attributeValues,
	        List<String> programIds, List<Person> personsToExclude) {
		List<PersonStub> stubs = new ArrayList<PersonStub>();
		
		Set<Integer> attributeMatches = null;
		Set<Integer> programMatches = null;
		
		if (attributes != null) {
			for (int i = 0; i < attributes.size(); i++) {
				String attr = attributes.get(i);
				String val = null;
				if (attributeValues != null && attributeValues.size() > i) {
					val = attributeValues.get(i);
				}
				
				Set<Integer> setOfIds = new HashSet<Integer>(getPersonIdsHavingAttributes(attr, val));
				if (attributeMatches != null) {
					attributeMatches.retainAll(setOfIds);
				} else {
					attributeMatches = setOfIds;
				}
			}
		}
		
		if (programIds != null) {
			for (String prog : programIds) {
				if (prog != null && prog.trim().length() > 0) {
					Program personProgram = HtmlFormEntryUtil.getProgram(prog);
					
					if (personProgram != null) {
						Set<Integer> matchingEnrollments = dao.getPatientIdHavingEnrollments(personProgram);
						if (programMatches != null) {
							programMatches.retainAll(matchingEnrollments);
						} else {
							programMatches = matchingEnrollments;
						}
					}
				}
			}
		}
		
		Set<Integer> results;
		
		// if no attributes specified, just use to the program matches
		if (attributes == null || attributes.isEmpty()) {
			results = programMatches;
		}
		// if no programs specified, just use the attribute matches
		else if (programIds == null || programIds.isEmpty()) {
			results = attributeMatches;
		}
		// otherwise, intersect the results
		else {
			results = programMatches;
			if (results != null) {
				results.retainAll(attributeMatches);
			}
		}
		
		if (results != null) {
			//now iterate through the results, returning person stubs
			for (Integer id : results) {
				Person person = Context.getPersonService().getPerson(id);
				if (person != null && !personsToExclude.contains(person)) {
					PersonStub pStub = new PersonStub();
					pStub.setGivenName(person.getGivenName());
					pStub.setFamilyName(person.getFamilyName());
					pStub.setMiddleName(person.getMiddleName());
					pStub.setId(id);
					stubs.add(pStub);
				}
			}
		}
		return stubs;
	}
	
	@Override
	@Transactional
	public void applyActions(FormEntrySession session) throws BadFormDesignException {
		// Wrapped in a transactional service method such that actions in it either pass or fail together. See TRUNK-3572
		session.applyActions();
	}
	
	@Override
	@Transactional
	public void reprocessArchivedForm(String argument, boolean isPath) throws Exception {
		SerializableFormObject formObject;
		if (isPath) {
			formObject = SerializableFormObject.deserializeXml(argument);
		} else {
			formObject = SerializableFormObject.deserializeXml(argument, false);
		}
		formObject.handleSubmission();
		
		//Save data to database
		HtmlFormEntryUtil.getService().applyActions(formObject.getSession());
	}
	
	@Override
	@Transactional
	public void reprocessArchivedForm(String path) throws Exception {
		reprocessArchivedForm(path, true);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Concept getConceptByMapping(String sourceNameOrHl7CodeAndTerm) {
		Concept ret = null;
		if (sourceNameOrHl7CodeAndTerm != null) {
			Integer cId = conceptMappingCache.get(sourceNameOrHl7CodeAndTerm);
			if (cId != null) {
				ret = Context.getConceptService().getConcept(cId);
			} else {
				String[] sourceCodeSplit = sourceNameOrHl7CodeAndTerm.split(":", 2);
				if (sourceCodeSplit.length != 2) {
					log.debug("Invalid concept mapping specified: " + sourceNameOrHl7CodeAndTerm);
				} else {
					String source = sourceCodeSplit[0].trim();
					String term = sourceCodeSplit[1].trim();
					List<Concept> concepts = Context.getConceptService().getConceptsByMapping(term, source, true);
					if (concepts != null && concepts.size() > 0) {
						Concept firstMatch = concepts.get(0);
						if (concepts.size() > 1) {
							Concept secondMatch = concepts.get(1);
							if (secondMatch.getRetired() == null || secondMatch.getRetired() == Boolean.FALSE) {
								throw new APIException(
								        "Multiple concepts found with mapping: " + sourceNameOrHl7CodeAndTerm);
							}
						}
						conceptMappingCache.put(sourceNameOrHl7CodeAndTerm, firstMatch.getConceptId());
						ret = firstMatch;
					}
				}
			}
		}
		return ret;
	}
	
	@Override
	public void clearConceptMappingCache() {
		conceptMappingCache = new HashMap<String, Integer>();
	}
	
	/**
	 * @see HtmlFormEntryService#getPatientIdHavingEnrollments(Program)
	 */
	@Override
	@Transactional(readOnly = true)
	public Set<Integer> getPatientIdHavingEnrollments(Program program) {
		return dao.getPatientIdHavingEnrollments(program);
	}
	
	/**
	 * Removed from OpenMRS core in 2.x, added back in here to support this legacy functionality and
	 * exitFromCare tag This is the way to establish that a patient has left the care center. This API
	 * call is responsible for:
	 * <ol>
	 * <li>Closing workflow statuses</li>
	 * <li>Terminating programs</li>
	 * <li>Discontinuing orders</li>
	 * <li>Flagging patient table</li>
	 * <li>Creating any relevant observations about the patient (if applicable)</li>
	 * </ol>
	 */
	@Authorized({ PrivilegeConstants.EDIT_PATIENTS })
	@Override
	@Transactional
	public void exitFromCare(Patient patient, Date dateExited, Concept reasonForExit) throws APIException {
		
		if (patient == null) {
			throw new APIException("Attempting to exit from care an invalid patient. Cannot proceed");
		}
		if (dateExited == null) {
			throw new APIException("Must supply a valid dateExited when indicating that a patient has left care");
		}
		if (reasonForExit == null) {
			throw new APIException(
			        "Must supply a valid reasonForExit (even if 'Unknown') when indicating that a patient has left care");
		}
		
		log.debug("Patient is exiting, so let's make sure there's an Obs for it");
		
		String codProp = Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare");
		Concept reasonForExitQuestion = Context.getConceptService().getConcept(codProp);
		
		if (reasonForExitQuestion != null) {
			List<Obs> obssExit = Context.getObsService().getObservationsByPersonAndConcept(patient, reasonForExitQuestion);
			if (obssExit != null) {
				if (obssExit.size() > 1) {
					log.error("Multiple reasons for exit (" + obssExit.size() + ")?  Shouldn't be...");
				} else {
					Obs obsExit = null;
					if (obssExit.size() == 1) {
						// already has a reason for exit - let's edit it.
						log.debug("Already has a reason for exit, so changing it");
						
						obsExit = obssExit.iterator().next();
						
					} else {
						// no reason for exit obs yet, so let's make one
						log.debug("No reason for exit yet, let's create one.");
						
						obsExit = new Obs();
						obsExit.setPerson(patient);
						obsExit.setConcept(reasonForExitQuestion);
						
						Location loc = Context.getLocationService().getDefaultLocation();
						
						if (loc != null) {
							obsExit.setLocation(loc);
						} else {
							log.error("Could not find a suitable location for which to create this new Obs");
						}
					}
					
					if (obsExit != null) {
						// put the right concept and (maybe) text in this
						// obs
						obsExit.setValueCoded(reasonForExit);
						obsExit.setValueCodedName(reasonForExit.getName()); // ABKTODO: presume current locale?
						obsExit.setObsDatetime(dateExited);
						Context.getObsService().saveObs(obsExit, "updated by HtmlFormEntryService.saveReasonForExit");
					}
				}
			}
		} else {
			log.debug("Reason for exit is null - should not have gotten here without throwing an error on the form.");
		}
		
		log.debug("Patient is exiting, trigger programs to close");
		
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		for (PatientProgram patientProgram : pws.getPatientPrograms(patient, null, null, null, null, null, false)) {
			//skip past patient programs that already completed
			if (patientProgram.getDateCompleted() == null) {
				Set<ProgramWorkflow> workflows = patientProgram.getProgram().getWorkflows();
				for (ProgramWorkflow workflow : workflows) {
					// (getWorkflows() is only returning over nonretired workflows)
					PatientState patientState = patientProgram.getCurrentState(workflow);
					
					// #1080 cannot exit patient from care
					// Should allow a transition from a null state to a terminal state
					// Or we should require a user to ALWAYS add an initial workflow/state when a patient is added to a program
					ProgramWorkflowState currentState = (patientState != null) ? patientState.getState() : null;
					ProgramWorkflowState transitionState = workflow.getState(reasonForExit);
					
					log.debug("Transitioning from current state [" + currentState + "]");
					log.debug("|---> Transitioning to final state [" + transitionState + "]");
					
					if (transitionState != null && workflow.isLegalTransition(currentState, transitionState)) {
						patientProgram.transitionToState(transitionState, dateExited);
						log.debug("State Conversion Triggered: patientProgram=" + patientProgram + " transition from "
						        + currentState + " to " + transitionState + " on " + dateExited);
					}
				}
				
				// #1068 - Exiting a patient from care causes "not-null property references
				// a null or transient value: org.openmrs.PatientState.dateCreated". Explicitly
				// calling the savePatientProgram() method will populate the metadata properties.
				//
				// #1067 - We should explicitly save the patient program rather than let
				// Hibernate do so when it flushes the session.
				Context.getProgramWorkflowService().savePatientProgram(patientProgram);
			}
		}
	}
}
