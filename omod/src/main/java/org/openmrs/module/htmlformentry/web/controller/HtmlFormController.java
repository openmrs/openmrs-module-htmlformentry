package org.openmrs.module.htmlformentry.web.controller;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.HtmlFormValidator;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.openmrs.web.WebConstants;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

@Controller
public class HtmlFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
	

    @InitBinder
	public void initBinder(WebDataBinder binder) {
    	binder.registerCustomEditor(EncounterType.class, new EncounterTypeEditor());
        binder.registerCustomEditor(java.util.Date.class, 
                new CustomDateEditor(SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Context.getLocale()), true));
	}

    
    @ModelAttribute("encounterTypes")
	List<EncounterType> getEncounterTypes() {
		return Context.getEncounterService().getAllEncounterTypes();
	}
	
    @ModelAttribute("htmlForm")
    HtmlForm formBackingObject(@RequestParam(value="id", required=false) Integer id) {
    	if (id != null) {
            HtmlForm hf = HtmlFormEntryUtil.getService().getHtmlForm(id);
            // avoid LazyInitializationException
            hf.getForm().getFormFields().size();
            return hf;
        } else {
        	HtmlForm hf = new HtmlForm();
        	hf.setForm(new Form());
        	return hf;
        }
    }
    
	/**
	 * Show a single HTML Form
	 */
	@RequestMapping(value="/module/htmlformentry/htmlForm", method=RequestMethod.GET)
	public void showHtmlForm(Model model,
                             HttpSession httpSession,
	                         @ModelAttribute("htmlForm") HtmlForm htmlForm) {
		HtmlForm hf = (HtmlForm) model.asMap().get("htmlForm");
		if (hf.getId() == null) {
			model.addAttribute("previewHtml", "");
		} else {
			try {
                Patient demo = HtmlFormEntryUtil.getFakePerson();
                FormEntrySession fes = new FormEntrySession(demo, hf.getXmlData(), httpSession);
                String html = fes.getHtmlToDisplay();
                if (fes.getFieldAccessorJavascript() != null) {
                	html += "<script>" + fes.getFieldAccessorJavascript() + "</script>";
                }
                model.addAttribute("previewHtml", html);
            } catch (Exception ex) {
                log.warn("Error rendering html form", ex);
                model.addAttribute("previewHtml", "Error! " + ex);
            }
		}
	}

	
	/**
	 * Save changes to an HTML Form
	 */
	@RequestMapping(value="/module/htmlformentry/htmlForm", method=RequestMethod.POST)
	public String saveHtmlForm(Model model,
	                           @ModelAttribute("htmlForm") HtmlForm htmlForm, BindingResult result,
	                           WebRequest request) {
		HtmlFormEntryService service = HtmlFormEntryUtil.getService();
		if (htmlForm.getId() == null && StringUtils.isBlank(htmlForm.getXmlData())) {
			htmlForm.setXmlData(service.getStartingFormXml(htmlForm));
		}
		HtmlFormValidator validator = new HtmlFormValidator();
		validator.validate(htmlForm, result);
		if (validator.getHtmlFormWarnings().size() > 0) {
			request.setAttribute("tagWarnings", validator.getHtmlFormWarnings(), WebRequest.SCOPE_SESSION);
		}
		if (result.hasErrors()) {
			return null;
		} else {
	        htmlForm = service.saveHtmlForm(htmlForm);	        
	        request.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Saved " + htmlForm.getForm().getName() + " " + htmlForm.getForm().getVersion(), WebRequest.SCOPE_SESSION);
			return "redirect:htmlForm.form?id=" + htmlForm.getId();
		}
	}
    
}
