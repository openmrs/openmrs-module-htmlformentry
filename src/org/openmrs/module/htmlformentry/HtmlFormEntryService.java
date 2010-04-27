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

}
