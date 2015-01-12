package org.openmrs.module.htmlformentry;

import java.util.List;
import java.util.Map;

import org.openmrs.Form;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Person;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.htmlformentry.element.PersonStub;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.springframework.transaction.annotation.Transactional;

/**
 * Defines the services provided by the HTML Form Entry module
 *
 */
public interface HtmlFormEntryService extends OpenmrsService {

	/**
	 * Retrieves the HTML Form with the specified id
	 * 
	 * @param id
	 * @return the HTML Form with the specified id
	 */
    @Transactional(readOnly=true)
    public HtmlForm getHtmlForm(Integer id);
    
    /**
     * Retrieves the HTML Form with the specified uuid
     * 
     * @param uuid
     * @return the HtmlForm with the given uuid
     * 
     */
    @Transactional(readOnly=true)
    public HtmlForm getHtmlFormByUuid(String uuid);

    /**
     * Retrieves the most-recently-created HtmlForm for the given Form
     * 
     * @param form
     * @return the most-recently-created HtmlForm for the given Form
     */
    @Transactional(readOnly=true)
    public HtmlForm getHtmlFormByForm(Form form);

    /**
     * Retrieves all HTML Forms in the system
    
     * @return a list of all HTML Forms in the system
     * @should return all html forms
     */
    @Transactional(readOnly=true)
    public List<HtmlForm> getAllHtmlForms();
    
    /**
     * Saves the specified HTML Form to the database
     * 
     * @param form the HTML Form to save
     * @return the HTML Form saved
     */
    @Transactional
    public HtmlForm saveHtmlForm(HtmlForm form);
    
    /**
     * Purges the specified HTML Form from the database
     * 
     * @param form the HTML Form to purge
     */
    @Transactional
    public void purgeHtmlForm(HtmlForm form);
    
    /**
     * Add a tag name and handler to the Service
     * 
     * @param tagName the tag name
     * @param handler the tag handler to associate with the tag name
     */
    @Transactional
    public void addHandler(String tagName, TagHandler handler);
    
    /**
     * 
     * Get a tag handler by tag name
     * 
     * @param tagName the tag name
     * @return the tag handler associated with the tag name
     */
    @Transactional(readOnly=true)
    public TagHandler getHandlerByTagName(String tagName);
    
    /**
     * 
     * Returns a map of all tag handlers
     * 
     * @return a map of all tag handlers
     */
    @Transactional(readOnly=true)
    public Map<String, TagHandler> getHandlers();

    /**
     * In version 1.7 of the module we drop the name and description properties of the
     * HtmlForm object, because those should really come from the underlying Form. However
     * we don't actually drop the columns yet, to allow the user to migrate any names and
     * descriptions that may differ. Once they've migrated the name and description for a
     * form (or said they don't want to) we null out the name and description columns from
     * the html_form table.
     * @return true if there are some forms who haven't had their name and description migrated
     */
    @Transactional(readOnly=true)
    public boolean needsNameAndDescriptionMigration();

    /**
     * @param form the HTML Form this default content is being created for
     * @return Example HTML Form content, to be used when a user creates a new form  
     */
	public String getStartingFormXml(HtmlForm form);
	
	/**
	 * 
	 * Returns a list of PersonStubs of Users by Role, which provide personId, givenName, familyName, middleName, familyName2
	 * Passing null into this method returns a PersonStub for all users
	 * 
	 * @param roleName
	 * @return a List<PersonStub>
	 */
	@Transactional(readOnly=true)
	public List<PersonStub> getUsersAsPersonStubs(String roleName);
	
	/**
	 * Given a uuid, fetch the OpenMRS object associated with that uuid
	 */
	public OpenmrsObject getItemByUuid(Class<? extends OpenmrsObject> type, String uuid);
	 
	/**
	 * Given an id and a class, fetch the OpenMRS object associated with that id
	 */
	public OpenmrsObject getItemById(Class<? extends OpenmrsObject> type, Integer id);
	
	/**
	 * Given a name and a class, fetch the OpenMRS object associated with that id
	 */
    public OpenmrsObject getItemByName(Class<? extends OpenmrsMetadata> type, String name);
    
    /**
	 * 
	 * Returns a list of Person ids of people having a given attribute type (passed in using the person_attribute_id).
	 * The method also takes in a value of the attribute to match to, if left null then all people having the 
	 * given attribute will be returned.
	 * 
	 * @param attribute the name of the person_attribute
	 * @param attributeValue optional value to match against the person attribute
	 * @return a List<Integer>
	 */
	@Transactional(readOnly=true)
	public List<Integer> getPersonIdsHavingAttributes(String attribute, String attributeValue);
	
	/**
	 * 
	 * Returns a list of Person stubs for people matching the attributes and programs parameters passed in.
	 * The method takes in multiple attributes and program ids, if the corresponding attribute value is null
	 * then all people having that attribute will be returned. The attributeValues list should either be null
	 * or contain the same number of values as the attributeIds list.
	 * 
	 * @param attributeIds the optional list of Person_attribute_id for the given person attribute
	 * @param attributeValues the list of optional values to match against the person attribute
	 * @param programIds the optional list of programIds 
	 * @param personsToExclude optional list of persons to exclude from the results list
	 * @return a List<Integer>
	 */
	public List<PersonStub> getPeopleAsPersonStubs(List<String> attributeIds, List<String> attributeValues, List<String> programIds, List<Person> personsToExclude);
	
	@Transactional
	public void applyActions(FormEntrySession session) throws BadFormDesignException;

    /**
     * This takes an archived Form Data and attempt to reprocess it.
     * @param argument is either a file path or a the content of the file
     * @param isPath  when set to true means the first argument is file path.
     * @throws Exception
     */
    public void reprocessArchivedForm(String argument,boolean isPath) throws Exception;

    public void reprocessArchivedForm(String path) throws Exception;
}
