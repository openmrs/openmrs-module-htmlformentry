package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.export.InitializerExportUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HtmlFormExportController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * This exports all of the htmlforms to a zip file containing initializer-friendly xml files
	 */
	@RequestMapping(value = "/module/htmlformentry/exportAllFormsForInitializer", method = RequestMethod.GET)
	public void exportAllFormsForInitializer(HttpServletResponse response,
	        @RequestParam(value = "useSubstitutions", required = false) boolean useSubstitutions) throws Exception {
		
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=exportForInitializer.zip");
		response.setHeader("Pragma", "no-cache");
		InitializerExportUtil.writeAllHtmlFormsAsZip(useSubstitutions, response.getOutputStream());
	}
	
	/**
	 * This exports the specified htmlform as an initializer-friendly xml file
	 */
	@RequestMapping(value = "/module/htmlformentry/exportFormForInitializer", method = RequestMethod.GET)
	public void exportFormForInitializer(HttpServletResponse response, @RequestParam(value = "formId") Integer formId,
	        @RequestParam(value = "useSubstitutions", required = false) boolean useSubstitutions) throws Exception {
		
		HtmlForm form = Context.getService(HtmlFormEntryService.class).getHtmlForm(formId);
		String fileName = InitializerExportUtil.getFileNameForForm(form);
		String xml = InitializerExportUtil.getXmlForInitializer(form, useSubstitutions);
		response.setContentType("text/xml");
		response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xml");
		response.setHeader("Pragma", "no-cache");
		IOUtils.write(xml, response.getOutputStream());
	}
}
