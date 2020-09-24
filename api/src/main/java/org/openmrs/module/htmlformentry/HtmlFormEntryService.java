package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.module.htmlformentry.handler.TagHandler;

/**
 * Defines the services provided by the HTML Form Entry module
 */
public interface HtmlFormEntryService extends OpenmrsService {
	
	/**
	 * Retrieves the HTML Form with the specified id
	 * 
	 * @return the HTML Form with the specified id
	 */
	HtmlForm getHtmlForm(Integer id);
	
	/**
	 * Retrieves the HTML Form with the specified uuid
	 * 
	 * @return the HtmlForm with the given uuid
	 */
	HtmlForm getHtmlFormByUuid(String uuid);
	
	/**
	 * Retrieves the most-recently-created HtmlForm for the given Form
	 * 
	 * @return the most-recently-created HtmlForm for the given Form
	 */
	HtmlForm getHtmlFormByForm(Form form);
	
	/**
	 * Retrieves all HTML Forms in the system
	 * 
	 * @return a list of all HTML Forms in the system <strong>Should</strong> return all html forms
	 */
	List<HtmlForm> getAllHtmlForms();
	
	/**
	 * Saves the specified HTML Form to the database
	 * 
	 * @param form the HTML Form to save
	 * @return the HTML Form saved
	 */
	HtmlForm saveHtmlForm(HtmlForm form);
	
	/**
	 * Purges the specified HTML Form from the database
	 * 
	 * @param form the HTML Form to purge
	 */
	void purgeHtmlForm(HtmlForm form);
	
	/**
	 * Add a tag name and handler to the Service
	 * 
	 * @param tagName the tag name
	 * @param handler the tag handler to associate with the tag name
	 */
	void addHandler(String tagName, TagHandler handler);
	
	/**
	 * Get a tag handler by tag name
	 * 
	 * @param tagName the tag name
	 * @return the tag handler associated with the tag name
	 */
	TagHandler getHandlerByTagName(String tagName);
	
	/**
	 * Returns a map of all tag handlers
	 * 
	 * @return a map of all tag handlers
	 */
	Map<String, TagHandler> getHandlers();
	
	/**
	 * In version 1.7 of the module we drop the name and description properties of the HtmlForm object,
	 * because those should really come from the underlying Form. However we don't actually drop the
	 * columns yet, to allow the user to migrate any names and descriptions that may differ. Once
	 * they've migrated the name and description for a form (or said they don't want to) we null out the
	 * name and description columns from the html_form table.
	 * 
	 * @return true if there are some forms who haven't had their name and description migrated
	 */
	boolean needsNameAndDescriptionMigration();
	
	/**
	 * @param form the HTML Form this default content is being created for
	 * @return Example HTML Form content, to be used when a user creates a new form
	 */
	String getStartingFormXml(HtmlForm form);
	
	/**
	 * Returns a list of PersonStubs of Users by Role, which provide personId, givenName, familyName,
	 * middleName, familyName2 Passing null into this method returns a PersonStub for all users
	 * 
	 * @return a List<PersonStub>
	 */
	List<PersonStub> getUsersAsPersonStubs(String roleName);
	
	/**
	 * Given a uuid, fetch the OpenMRS object associated with that uuid
	 */
	OpenmrsObject getItemByUuid(Class<? extends OpenmrsObject> type, String uuid);
	
	/**
	 * Given an id and a class, fetch the OpenMRS object associated with that id
	 */
	OpenmrsObject getItemById(Class<? extends OpenmrsObject> type, Integer id);
	
	/**
	 * Given a name and a class, fetch the OpenMRS object associated with that id
	 */
	OpenmrsObject getItemByName(Class<? extends OpenmrsMetadata> type, String name);
	
	/**
	 * Returns a list of Person ids of people having a given attribute type (passed in using the
	 * person_attribute_id). The method also takes in a value of the attribute to match to, if left null
	 * then all people having the given attribute will be returned.
	 * 
	 * @param attribute the name of the person_attribute
	 * @param attributeValue optional value to match against the person attribute
	 * @return a List<Integer>
	 */
	List<Integer> getPersonIdsHavingAttributes(String attribute, String attributeValue);
	
	/**
	 * Returns a list of Person stubs for people matching the attributes and programs parameters passed
	 * in. The method takes in multiple attributes and program ids, if the corresponding attribute value
	 * is null then all people having that attribute will be returned. The attributeValues list should
	 * either be null or contain the same number of values as the attributeIds list.
	 * 
	 * @param attributeIds the optional list of Person_attribute_id for the given person attribute
	 * @param attributeValues the list of optional values to match against the person attribute
	 * @param programIds the optional list of programIds
	 * @param personsToExclude optional list of persons to exclude from the results list
	 * @return a List<Integer>
	 */
	List<PersonStub> getPeopleAsPersonStubs(List<String> attributeIds, List<String> attributeValues, List<String> programIds,
	        List<Person> personsToExclude);
	
	/**
	 * Apply the actions in the FormEntrySession
	 */
	void applyActions(FormEntrySession session) throws BadFormDesignException;
	
	/**
	 * This takes an archived Form Data and attempt to reprocess it.
	 * 
	 * @param argument is either a file path or a the content of the file
	 * @param isPath when set to true means the first argument is file path.
	 */
	void reprocessArchivedForm(String argument, boolean isPath) throws Exception;
	
	void reprocessArchivedForm(String path) throws Exception;
	
	/**
	 * This returns a single Concept represented by the mapping String sourceNameOrHl7Code:referenceTerm
	 * If multiple Concepts match the given mapping, then: - if only one of these is non-retired, return
	 * that concept - otherwise, throw an ApiException
	 */
	Concept getConceptByMapping(String sourceNameOrHl7CodeAndTerm);
	
	/**
	 * Clears the concept mapping cache if needed to ensure no stale mappings exist
	 */
	void clearConceptMappingCache();
	
	/**
	 * Return a List of patient ids who have ever been enrolled in the given program
	 */
	Set<Integer> getPatientIdHavingEnrollments(Program program);
	
	/**
	 * Removed from OpenMRS core in 2.x, added back in here to support this legacy functionality and
	 * exitFromCare tag
	 */
	void exitFromCare(Patient patient, Date dateExited, Concept reasonForExit) throws APIException;
	
}
