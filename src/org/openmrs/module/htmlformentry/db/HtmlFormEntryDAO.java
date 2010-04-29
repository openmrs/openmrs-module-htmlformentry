package org.openmrs.module.htmlformentry.db;

import java.util.List;

import org.openmrs.Form;
import org.openmrs.module.htmlformentry.HtmlForm;

/**
 * Defines the methods that must be implemented by the Data Access Object
 */
public interface HtmlFormEntryDAO {

	/**
	 * Retrieves the HtmlForm referenced by id
	 * 
	 * @param id
	 * @return HtmlForm
	 */
    public HtmlForm getHtmlForm(Integer id);
    
    /**
     * Retrieves the HtmlForm associated with a standard OpenMRS Form object
     * 
     * @param form
     * @return HtmlForm
     */
    public HtmlForm getHtmlFormByForm(Form form);

    /**
     * Saves an HtmlForm to the database
     * 
     * @param htmlForm
     * @return
     */
    public HtmlForm saveHtmlForm(HtmlForm htmlForm);

    /**
     * Deletes an HtmlForm from the database
     * 
     * @param htmlForm
     */
    public void deleteHtmlForm(HtmlForm htmlForm);

    /**
     * Returns all HtmlForms in the database
     * @return
     */
    public List<HtmlForm> getAllHtmlForms();

}
