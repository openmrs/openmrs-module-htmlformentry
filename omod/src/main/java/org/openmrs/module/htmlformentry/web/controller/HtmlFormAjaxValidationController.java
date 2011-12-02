package org.openmrs.module.htmlformentry.web.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HtmlFormAjaxValidationController {

	/**
	 * Retreives the localized message for the passed message code and sends it to the output stream
	 * @throws IOException 
	 */
	@RequestMapping("/module/htmlformentry/localizedMessage")
	public void localizedMessage(ModelMap model, HttpServletRequest request, HttpServletResponse response, @RequestParam String messageCode) throws IOException {
		response.setContentType("text/html");
		ServletOutputStream out = response.getOutputStream();
		out.print(Context.getMessageSourceService().getMessage(messageCode));
	}
	
	
	/**
	 * lastEnteredForm
	 */
	@RequestMapping("/module/htmlformentry/lastEnteredForm")
	public void duplicateForm(
			ModelMap model,
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(required = true, value = "formId") Integer formId,
			@RequestParam(required = true, value = "patientId") Integer patientId,
			@RequestParam(required = true, value = "date") String date,
			@RequestParam(required = false, value = "dateFormat") String dateFormat)
			throws Exception {

		response.setContentType("text/html");
		ServletOutputStream out = response.getOutputStream();
		List<Locale> l = new Vector<Locale>();
		l.add(Context.getLocale());

		List<Encounter> encounters = Context.getEncounterService().getEncountersByPatientId(patientId);
		
		// if the AJAX call has passed a date format, use that, otherwise use the standard date format to parse the date
		SimpleDateFormat dateFormatter;
		if (dateFormat != null) {
			dateFormatter = new SimpleDateFormat(dateFormat);
		}
		else {
			dateFormatter = Context.getDateFormat();
		}
		Date dateToCheck = dateFormatter.parse(date);
		
		boolean duplicate = false;
		
		for(Encounter enc: encounters)
		{
			if(enc.getForm() != null)
			{
				Form encForm = enc.getForm();
				HtmlForm htmlForm = HtmlFormEntryUtil.getService().getHtmlFormByForm(encForm);
				if(htmlForm != null && htmlForm.getId().equals(formId) && dateToCheck.compareTo(enc.getEncounterDatetime()) == 0)
				{
					duplicate = true;
				}
			}
		}
		
		if(duplicate)
		{
			out.print("true");
		}
		else
		{
			out.print("false");
		}
	}
}
