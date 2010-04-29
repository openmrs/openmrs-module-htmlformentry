package org.openmrs.module.htmlformentry.web.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The controller for previewing a HtmlForm by loading the xml file that defines that HtmlForm.
 * <p/>
 * Handles {@code htmlFormFromFile.form} requests. Renders view {@code htmlFormFromFile.jsp}.
 */
public class HtmlFormFromFileController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        super.initBinder(request, binder);
    }
    
    @Override
    protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
    	System.out.println("In reference data...");
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("previewHtml", "");
        String message = "";
        
        String filePath = request.getParameter("filePath");
        if (StringUtils.hasText(filePath)) {
        	ret.put("filePath", filePath);
        	try {
        		File f = new File(filePath);
        		if (f != null && f.exists() && f.canRead()) {
        			
        			StringBuffer sb = new StringBuffer(1000); 
        			
        			BufferedReader in =
        			   new BufferedReader(
        			       new InputStreamReader(new FileInputStream(f), "UTF-8"));
        			       				
        			String line;
        			
        			while ((line = in.readLine()) != null)
        				sb.append(line);
        			
        			in.close();
        			
        			String xml = sb.toString();
                    
        			Patient p = null;
                    String pId = request.getParameter("patientId");
                    if (StringUtils.hasText(pId)) {
                    	p = Context.getPatientService().getPatient(Integer.parseInt(pId));
                    }
                    else {
                    	p = HtmlFormEntryUtil.getFakePerson();
                    }
                    HtmlForm fakeForm = new HtmlForm();
                    fakeForm.setXmlData(xml);
                    FormEntrySession fes = new FormEntrySession(p, null, Mode.ENTER, fakeForm);
                    ret.put("previewHtml", fes.getHtmlToDisplay());
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
        else {
        	message = "You must specify a file path to preview from file";
        }
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
