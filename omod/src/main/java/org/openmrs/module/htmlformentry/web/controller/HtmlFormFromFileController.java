package org.openmrs.module.htmlformentry.web.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The controller for previewing a HtmlForm by loading the xml file that defines that HtmlForm from disk.
 * <p/>
 * Handles {@code htmlFormFromFile.form} requests. Renders view {@code htmlFormFromFile.jsp}.
 */
@Controller
public class HtmlFormFromFileController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());   
    
    @RequestMapping("/module/htmlformentry/htmlFormFromFile.form")
    public void handleRequest(Model model,
                              @RequestParam(value="filePath", required=false) String filePath,
                              @RequestParam(value="patientId", required=false) Integer pId) throws Exception {
    	log.debug("In reference data...");
        model.addAttribute("previewHtml", "");
        String message = "";
        
        if (StringUtils.hasText(filePath)) {
        	model.addAttribute("filePath", filePath);
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
                    if (pId != null) {
                    	p = Context.getPatientService().getPatient(pId);
                    }
                    else {
                    	p = HtmlFormEntryUtil.getFakePerson();
                    }
                    HtmlForm fakeForm = new HtmlForm();
                    fakeForm.setXmlData(xml);
                    FormEntrySession fes = new FormEntrySession(p, null, Mode.ENTER, fakeForm);
                    model.addAttribute("previewHtml", fes.getHtmlToDisplay());
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
        model.addAttribute("message", message);
    }

}
