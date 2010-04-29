package org.openmrs.module.htmlformentry.web.controller;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.propertyeditor.FormEditor;
import org.openmrs.web.WebConstants;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Handles {@code htmlForm.form} requests. Renders view {@code htmlForm.jsp}.
 */
public class HtmlFormController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        super.initBinder(request, binder);

        NumberFormat nf = NumberFormat.getInstance(Context.getLocale());
        binder.registerCustomEditor(java.lang.Integer.class,
                new CustomNumberEditor(java.lang.Integer.class, nf, true));
        binder.registerCustomEditor(java.util.Date.class, 
                new CustomDateEditor(SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Context.getLocale()), true));
        binder.registerCustomEditor(Form.class, new FormEditor());
    }
    
    @Override
    protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
        Map<String, Object> ret = new HashMap<String, Object>();
        
        if (request.getParameter("id") != null) {
            Integer id = Integer.valueOf(request.getParameter("id"));
            HtmlForm hf = HtmlFormEntryUtil.getService().getHtmlForm(id);
            try {
                Patient demo = HtmlFormEntryUtil.getFakePerson();
                FormEntrySession fes = new FormEntrySession(demo, hf.getXmlData());
                ret.put("previewHtml", fes.getHtmlToDisplay());
            } catch (Exception ex) {
                log.warn("Error rendering html form", ex);
                ret.put("previewHtml", "Error! " + ex);
            }
        } else {
            ret.put("previewHtml", "");
        }
        return ret;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        try {
            Integer id = Integer.valueOf(request.getParameter("id"));
            return HtmlFormEntryUtil.getService().getHtmlForm(id);
        } catch (Exception ex) {
            return new HtmlForm();
        }
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object commandObject, BindException errors)
            throws Exception {
        HtmlForm command = (HtmlForm) commandObject;
        command = HtmlFormEntryUtil.getService().saveHtmlForm(command);
        request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Saved " + command.getId());
        return new ModelAndView(new RedirectView(getSuccessView() + "?id=" + command.getId()));
    }
    
}
