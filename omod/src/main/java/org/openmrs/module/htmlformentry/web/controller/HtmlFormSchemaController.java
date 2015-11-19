package org.openmrs.module.htmlformentry.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import javax.servlet.http.HttpSession;

/**
 * Handles the display of an HTML Form Schema.
 * <p/>
 * Handles {@code htmlFormSchema.form} requests. Renders view {@code htmlFormSchema.jsp}.
 */
@Controller
public class HtmlFormSchemaController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    @RequestMapping("/module/htmlformentry/htmlFormSchema")
    public void viewSchema(@RequestParam(value="id", required=false) Integer id,
                           @RequestParam(value="filePath", required=false) String filePath,
                           Model model, HttpSession httpSession) throws Exception {
        String message = "";
        String xml = null;
        if (StringUtils.hasText(filePath)) {
        	model.addAttribute("filePath", filePath);
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
        else if (id != null) {
        	HtmlForm form = Context.getService(HtmlFormEntryService.class).getHtmlForm(id);
        	xml = form.getXmlData();
        }
        else {
        	message = "You must specify a file path to preview from file";
        }

        model.addAttribute("schema", generateSchema(xml, httpSession));
        model.addAttribute("message", message);
    }

    private HtmlFormSchema generateSchema(String xml, HttpSession httpSession) throws Exception {
        Patient p = HtmlFormEntryUtil.getFakePerson();
        HtmlForm fakeForm = new HtmlForm();
        fakeForm.setXmlData(xml);
        FormEntrySession fes = new FormEntrySession(p, null, FormEntryContext.Mode.ENTER, fakeForm, httpSession);
        fes.getHtmlToDisplay();
        return fes.getContext().getSchema();
    }
}
