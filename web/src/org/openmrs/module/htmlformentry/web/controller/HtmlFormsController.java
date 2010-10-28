package org.openmrs.module.htmlformentry.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.EncounterType;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HtmlFormsController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(EncounterType.class, new EncounterTypeEditor());
	}
	
	/**
	 * List all HTML Forms
	 */
	@RequestMapping("/module/htmlformentry/htmlForms")
	public void manageHtmlForms(Model model) {
		model.addAttribute("forms", HtmlFormEntryUtil.getService().getAllHtmlForms());
	}	

}
