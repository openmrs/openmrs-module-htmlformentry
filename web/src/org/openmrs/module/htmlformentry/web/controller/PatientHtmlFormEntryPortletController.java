package org.openmrs.module.htmlformentry.web.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.web.controller.PortletController;

public class PatientHtmlFormEntryPortletController extends PortletController {

    @Override
    protected void populateModel(HttpServletRequest request, Map<String, Object> model) {
        model.put("htmlForms", HtmlFormEntryUtil.getService().getAllHtmlForms());
    }

}
