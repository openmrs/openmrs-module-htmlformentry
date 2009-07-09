package org.openmrs.module.htmlformentry.impl;

import java.util.Date;
import java.util.List;

import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;

public class HtmlFormEntryServiceImpl extends BaseOpenmrsService implements
        HtmlFormEntryService {
    
    private HtmlFormEntryDAO dao;

    public void setDao(HtmlFormEntryDAO dao) {
        this.dao = dao;
    }
    
    public HtmlForm getHtmlForm(Integer id) {
        return dao.getHtmlForm(id);
    }
    
    public HtmlForm saveHtmlForm(HtmlForm htmlForm) {
        if (htmlForm.getCreator() == null)
            htmlForm.setCreator(Context.getAuthenticatedUser());
        if (htmlForm.getDateCreated() == null)
            htmlForm.setDateCreated(new Date());
        if (htmlForm.getId() != null) {
            htmlForm.setChangedBy(Context.getAuthenticatedUser());
            htmlForm.setDateChanged(new Date());
        }

        return dao.saveHtmlForm(htmlForm);
    }
    
    public void purgeHtmlForm(HtmlForm htmlForm) {
        dao.deleteHtmlForm(htmlForm);
    }

    public List<HtmlForm> getAllHtmlForms() {
        return dao.getAllHtmlForms();
    }

    public HtmlForm getHtmlFormByForm(Form form) {
        return dao.getHtmlFormByForm(form);
    }   

}
