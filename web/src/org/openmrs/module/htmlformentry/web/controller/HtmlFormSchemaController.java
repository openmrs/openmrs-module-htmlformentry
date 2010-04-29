package org.openmrs.module.htmlformentry.web.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Handles the display of an HTML Form Schema.
 * <p/>
 * Handles {@code htmlFormSchema.form} requests. Renders view {@code htmlFormSchema.jsp}.
 */
public class HtmlFormSchemaController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        super.initBinder(request, binder);
    }
    
    @Override
    protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
        Map<String, Object> ret = new HashMap<String, Object>();
        String message = "";
        String filePath = request.getParameter("filePath");
        String id = request.getParameter("id");
        String xml = null;
        if (StringUtils.hasText(filePath)) {
        	ret.put("filePath", filePath);
        	try {
        		File f = new File(filePath);
        		if (f != null && f.exists()) {
        			xml = OpenmrsUtil.getFileAsString(f);
        		}
        		else {
        			message = "Please specify a valid file path.";
        		}
        	}
        	catch (Exception e) {
        		log.error("An error occurred while loading the html.", e);
        		message = "An error occurred while loading the html. " + e.getMessage();
        	}
        }
        else if (StringUtils.hasText(id)) {
        	HtmlForm form = Context.getService(HtmlFormEntryService.class).getHtmlForm(Integer.parseInt(id));
        	xml = form.getXmlData();
        }
        else {
        	message = "You must specify a file path to preview from file";
        }
        
		Patient p = HtmlFormEntryUtil.getFakePerson();
		HtmlForm fakeForm = new HtmlForm();
		fakeForm.setXmlData(xml);
        FormEntrySession fes = new FormEntrySession(p, null, Mode.ENTER, fakeForm);
        HtmlFormSchema schema = fes.getContext().getSchema();
        ret.put("schema", schema);
        ret.put("message", message);
        
        return ret;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
    	return "";
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object commandObject, BindException errors) throws Exception {
    	return new ModelAndView(new RedirectView(getSuccessView()));
    }
}
