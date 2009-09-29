package org.openmrs.module.htmlformentry;

import java.util.List;
import java.util.Map;

import org.openmrs.Form;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.springframework.transaction.annotation.Transactional;

public interface HtmlFormEntryService extends OpenmrsService {

    @Transactional(readOnly=true)
    public HtmlForm getHtmlForm(Integer id);

    /**
     * Gets the most-recently-created HtmlForm for the given form
     * 
     * @param form
     * @return
     */
    @Transactional(readOnly=true)
    public HtmlForm getHtmlFormByForm(Form form);

    @Transactional(readOnly=true)
    public List<HtmlForm> getAllHtmlForms();
    
    @Transactional
    public HtmlForm saveHtmlForm(HtmlForm form);
    
    @Transactional
    public void purgeHtmlForm(HtmlForm form);
    
    /**
     * 
     * Add a tag name and handler to the Service
     * 
     * @param tagName
     * @param handler
     */
    @Transactional
    public void addHandler(String tagName, TagHandler handler);
    
    /**
     * 
     * Get a tag handler by tag name
     * 
     * @param tagName
     * @return
     */
    @Transactional
    public TagHandler getHandlerByTagName(String tagName);
    
    /**
     * 
     * Returns a map of all tag handlers
     * 
     * @return 
     */
    @Transactional
    public Map<String, TagHandler> getHandlers();

}
