package org.openmrs.module.htmlformentry.impl;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;
import org.openmrs.module.htmlformentry.handler.TagHandler;

/**
 * Standard implementation of the HtmlFormEntryService
 */
public class HtmlFormEntryServiceImpl extends BaseOpenmrsService implements HtmlFormEntryService {
    
    private HtmlFormEntryDAO dao;
    private static Map<String, TagHandler> handlers = new LinkedHashMap<String, TagHandler>();

	/*
	 * Optimization to minimize database hits for the needs-name-and-description-migration check.
	 * Once all forms have been migrated, we no longer need to hit the database on further checks
	 * because there is no way to add more un-migrated forms. (In theory someone could add some 
	 * directly to the database, so we use an instance variable here that will be reset whenever
	 * the system is restarted or the module is reloaded.
	 */
    private boolean nameAndDescriptionMigrationDone = false;
    
    public void addHandler(String tagName, TagHandler handler) {
        handlers.put(tagName, handler);
    }
    
    public TagHandler getHandlerByTagName(String tagName){
        return handlers.get(tagName);
    }
    
    public Map<String, TagHandler> getHandlers(){
        return handlers;
    }
    
    /**
     * Sets the tag handlers 
     * 
     * @param handlersToSet
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
        Context.getFormService().saveForm(htmlForm.getForm());
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
    
	@Override
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
	
}
