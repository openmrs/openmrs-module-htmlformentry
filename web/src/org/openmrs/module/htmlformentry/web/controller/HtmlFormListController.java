package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class HtmlFormListController extends SimpleFormController {

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        return HtmlFormEntryUtil.getService().getAllHtmlForms();
    }
    
}
