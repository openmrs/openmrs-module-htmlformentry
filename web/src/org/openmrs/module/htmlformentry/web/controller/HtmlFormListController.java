package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Lists all the HTML Forms in the database.
 * <p/>
 * Handles {@code htmlForm.list} requests. Renders view {@code htmlFormList.jsp}.
 */
public class HtmlFormListController extends SimpleFormController {

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        return HtmlFormEntryUtil.getService().getAllHtmlForms();
    }
    
}
