package org.openmrs.module.htmlformentry;

import java.util.List;

import org.openmrs.Form;
import org.openmrs.api.OpenmrsService;
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
    
}
