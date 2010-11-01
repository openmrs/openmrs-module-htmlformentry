package org.openmrs.module.htmlformentry;

import java.util.List;
import java.util.Map;

import org.openmrs.Form;
import org.openmrs.api.OpenmrsService;
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
        
}
