package org.openmrs.module.htmlformentry.db;

import java.util.List;

import org.openmrs.Form;
import org.openmrs.module.htmlformentry.HtmlForm;

public interface HtmlFormEntryDAO {

    public HtmlForm getHtmlForm(Integer id);
    
    public HtmlForm getHtmlFormByForm(Form form);

    public HtmlForm saveHtmlForm(HtmlForm htmlForm);

    public void deleteHtmlForm(HtmlForm htmlForm);

    public List<HtmlForm> getAllHtmlForms();

}
