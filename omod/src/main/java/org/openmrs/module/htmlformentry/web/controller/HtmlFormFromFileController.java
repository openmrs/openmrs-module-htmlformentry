package org.openmrs.module.htmlformentry.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * The controller for previewing a HtmlForm by loading the xml file that defines that HtmlForm from
 * disk.
 * <p/>
 * Handles {@code htmlFormFromFile.form} requests. Renders view {@code htmlFormFromFile.jsp}.
 */
@Controller
public class HtmlFormFromFileController {
	
	private static final String TEMP_HTML_FORM_FILE_PREFIX = "html_form_";
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping("/module/htmlformentry/htmlFormFromFile.form")
	public void handleRequest(Model model, @RequestParam(value = "filePath", required = false) String filePath,
	                          @RequestParam(value = "patientId", required = false) Integer pId,
	                          @RequestParam(value = "isFileUpload", required = false) boolean isFileUpload,
	                          HttpServletRequest request) throws Exception {

        Context.requirePrivilege("Manage Forms");
		
		if (log.isDebugEnabled())
			log.debug("In reference data...");
		
		model.addAttribute("previewHtml", "");
		String message = "";
		File f = null;
		try {
			if (isFileUpload) {
				MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
				MultipartFile multipartFile = multipartRequest.getFile("htmlFormFile");
				if (multipartFile != null) {
					//use the same file for the logged in user
					f = new File(SystemUtils.JAVA_IO_TMPDIR, TEMP_HTML_FORM_FILE_PREFIX
					        + Context.getAuthenticatedUser().getSystemId());
					if (!f.exists())
						f.createNewFile();
					
					filePath = f.getAbsolutePath();
					FileOutputStream fileOut = new FileOutputStream(f);
					IOUtils.copy(multipartFile.getInputStream(), fileOut);
					fileOut.close();
				}
			} else {
				if (StringUtils.hasText(filePath)) {
					f = new File(filePath);
				} else {
					message = "You must specify a file path to preview from file";
				}
			}
			
			if (f != null && f.exists() && f.canRead()) {
				model.addAttribute("filePath", filePath);
				
				StringWriter writer = new StringWriter();
				IOUtils.copy(new FileInputStream(f), writer, "UTF-8");
				String xml = writer.toString();
				
				Patient p = null;
				if (pId != null) {
					p = Context.getPatientService().getPatient(pId);
				} else {
					p = HtmlFormEntryUtil.getFakePerson();
				}
				HtmlForm fakeForm = new HtmlForm();
				fakeForm.setXmlData(xml);
				FormEntrySession fes = new FormEntrySession(p, null, Mode.ENTER, fakeForm, request.getSession());
				String html = fes.getHtmlToDisplay();
				if (fes.getFieldAccessorJavascript() != null) {
                	html += "<script>" + fes.getFieldAccessorJavascript() + "</script>";
                }
				model.addAttribute("previewHtml", html);
				//clear the error message
				message = "";
			} else {
				message = "Please specify a valid file path or select a valid file.";
			}
		}
		catch (Exception e) {
			log.error("An error occurred while loading the html.", e);
			message = "An error occurred while loading the html. " + e.getMessage();
		}
		
		model.addAttribute("message", message);
		model.addAttribute("isFileUpload", isFileUpload);
	}
}
