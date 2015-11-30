package org.openmrs.module.htmlformentry.extension.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.web.FormEntryContext;
import org.openmrs.module.web.extension.FormEntryHandler;
import org.openmrs.util.PrivilegeConstants;

/**
 * Defines the extension point that allows HTML Forms to appear in 
 * the UI when the user indicates they want to enter a form, and wants to pick which form.
 * 
 * This extension is enabled by defining (uncommenting) it in the 
 * /metadata/config.xml file. 
 */
public class FormEntryHandlerExtension extends FormEntryHandler {

	/**
     * @see org.openmrs.module.web.extension.FormEntryModuleExtension#getFormEntryUrl()
     */
    @Override
    public String getFormEntryUrl() {
	    return "module/htmlformentry/htmlFormEntry.form";
    }
    
	/**
     * @see org.openmrs.module.web.extension.FormEntryModuleExtension#getViewFormUrl()
     */
    @Override
    public String getViewFormUrl() {
    	return "module/htmlformentry/htmlFormEntry.form";
    }

	/**
     * @see org.openmrs.module.web.extension.FormEntryHandler#getEditFormUrl()
     */
    @Override
    public String getEditFormUrl() {
    	return "module/htmlformentry/htmlFormEntry.form?mode=EDIT";
    }
    
    /**
     * @see org.openmrs.module.web.extension.FormEntryModuleExtension#getFormList()
     */
    @Override
    public List<Form> getFormsModuleCanEnter(FormEntryContext formEntryContext) {
    	return addAllHtmlForms(new ArrayList<Form>());
    }

	/**
     * @see org.openmrs.module.web.extension.FormEntryModuleExtension#getFormsModuleCanView()
     */
    @Override
    public Set<Form> getFormsModuleCanView() {
    	return addAllHtmlForms(new HashSet<Form>());
    }

	/**
     * @see org.openmrs.module.web.extension.FormEntryHandler#getFormsModuleCanEdit()
     */
    @Override
    public Set<Form> getFormsModuleCanEdit() {
    	return addAllHtmlForms(new HashSet<Form>());
    }

    private <C extends Collection<Form>> C addAllHtmlForms(C collection) {
    	boolean showUnpublished = Context.getAuthenticatedUser().hasPrivilege(PrivilegeConstants.VIEW_UNPUBLISHED_FORMS);
    	Set<Form> ret = new LinkedHashSet<Form>();
	    for (HtmlForm form : HtmlFormEntryUtil.getService().getAllHtmlForms()) {
	    	if (showUnpublished || form.getForm().getPublished())
	    		ret.add(form.getForm());
	    }
	    collection.addAll(ret);
	    return collection;
    }

}
