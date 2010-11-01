package org.openmrs.module.htmlformentry.web.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/module/htmlformentry/migrateNamesAndDescriptions")
public class MigrateNamesAndDescriptionsController {

	/**
	 * Determines what stage of the migration we're at, and forwards to the correct JSP
	 * @param model
	 * @return
	 */
	@RequestMapping(method=RequestMethod.GET)
	public String showMigrationsNeeded(Model model) {
		HtmlFormEntryService service = HtmlFormEntryUtil.getService();		
		List<HtmlForm> allForms = service.getAllHtmlForms();
		
		// 1. figure out whether any htmlforms need splitting because they share a form
		{
			Map<Integer, List<HtmlForm>> duplicateForms = getDuplicateForms(allForms);
			if (duplicateForms.size() > 0) {
				model.addAttribute("duplicateForms", duplicateForms);
				return "/module/htmlformentry/migrateSplitHtmlFormsSharingForm";
			}
		}
		
		// 2. figure out whether any htmlforms have the same name/description as their underlying forms
		// (these can automatically have the name/description dropped)
		{
			List<HtmlForm> sameName = new ArrayList<HtmlForm>();
			List<HtmlForm> sameDescription = new ArrayList<HtmlForm>();
			for (HtmlForm htmlForm : allForms) {
				if (htmlForm.getName().equals(htmlForm.getDeprecatedName()))
					sameName.add(htmlForm);
				if (htmlForm.getDescription().equals(htmlForm.getDeprecatedDescription()))
					sameDescription.add(htmlForm);
			}
			if (sameName.size() > 0 || sameDescription.size() > 0) {
				model.addAttribute("sameName", sameName);
				model.addAttribute("sameDescription", sameDescription);
				return "/module/htmlformentry/migrateClearSameNamesAndDescriptions";
			}
		}
		
		// 3. let the user pick the right name and description for remaining forms
		{
			List<HtmlForm> migrationNeeded = new ArrayList<HtmlForm>();
			for (HtmlForm form : allForms) {
				if (form.getDeprecatedName() != null || form.getDeprecatedDescription() != null)
					migrationNeeded.add(form);
			}
			model.addAttribute("migrationNeeded", migrationNeeded);
			return "/module/htmlformentry/migrateNamesAndDescriptions";
		}
	}
	
	
	private Map<Integer, List<HtmlForm>> getDuplicateForms(List<HtmlForm> allForms) {
		Map<Integer, List<HtmlForm>> ret = new LinkedHashMap<Integer, List<HtmlForm>>();
		for (HtmlForm htmlForm : allForms) {
			Form form = htmlForm.getForm();
			List<HtmlForm> holder = (List<HtmlForm>) ret.get(form.getId());
			if (holder == null) {
				holder = new ArrayList<HtmlForm>();
				ret.put(form.getId(), holder);
			}
			holder.add(htmlForm);
		}
		// remove anything that doesn't appear multiple times
		for (Iterator<Map.Entry<Integer, List<HtmlForm>>> i = ret.entrySet().iterator(); i.hasNext(); ) {
			if (i.next().getValue().size() < 2)
				i.remove();
		}
		return ret;
    }
	
	
	/**
	 * Handles submission for splitting html forms that share an underlying form
	 * @param request
	 * @return
	 */
	@RequestMapping(method=RequestMethod.POST, params="migration=duplicateForms")
	public String splitDuplicateForms(WebRequest request) {
		HtmlFormEntryService service = HtmlFormEntryUtil.getService();
		Map<Integer, List<HtmlForm>> duplicates = getDuplicateForms(service.getAllHtmlForms());
		for (Map.Entry<Integer, List<HtmlForm>> e : duplicates.entrySet()) {
			Integer id = e.getKey();
			List<HtmlForm> htmlForms = e.getValue();
			String choice = request.getParameter("group." + id);
			try {
				Integer keepHtmlFormId = Integer.valueOf(choice);
				for (HtmlForm htmlForm : htmlForms) {
					if (!htmlForm.getId().equals(keepHtmlFormId)) {
						splitUnderlyingForm(htmlForm);
					}
				}
			} catch (NumberFormatException ex) { }
		}
	    return "redirect:migrateNamesAndDescriptions.form";
	}

	/**
	 * Clears deprecated names and descriptions (because they exactly matched the underlying form
	 * @param request
	 * @return
	 */
	@RequestMapping(method=RequestMethod.POST, params="migration=clearNamesAndDescriptions")
	public String clearNamesAndDescriptionsThatMatch(@RequestParam(value="clearName", required=false) List<Integer> clearNames,
	                                                 @RequestParam(value="clearDescription", required=false) List<Integer> clearDescriptions) {
		HtmlFormEntryService service = HtmlFormEntryUtil.getService();
		for (HtmlForm form : service.getAllHtmlForms()) {
			boolean needToSave = false;
			if (clearNames != null && clearNames.contains(form.getId())) {
				form.setDeprecatedName(null);
				needToSave = true;
			}
			if (clearDescriptions != null && clearDescriptions.contains(form.getId())) {
				form.setDeprecatedDescription(null);
				needToSave = true;
			}
			if (needToSave)
				service.saveHtmlForm(form);
		}
	    return "redirect:migrateNamesAndDescriptions.form";
	}

	/**
	 * Duplicates the Form that this HtmlForm points to, and points this HtmlForm to the new form.
	 * @param htmlForm
	 */
	private void splitUnderlyingForm(HtmlForm htmlForm) {
		Form oldForm = htmlForm.getForm();
	    Form newForm = Context.getFormService().duplicateForm(oldForm);
	    htmlForm.setForm(newForm);
	    HtmlFormEntryService service = HtmlFormEntryUtil.getService();
	    if (htmlForm.getDeprecatedName() != null) {
	    	newForm.setName(htmlForm.getDeprecatedName());
	    	htmlForm.setDeprecatedName(null);
	    }
	    service.saveHtmlForm(htmlForm);
    }

	
	/**
	 * Handles submission of user choices for which names and descriptions to use.
	 * @param request
	 * @return
	 */
	@RequestMapping(method=RequestMethod.POST, params="migration=namesAndDescriptions")
	public String doNameAndDescriptionMigration(WebRequest request) {
		HtmlFormEntryService service = HtmlFormEntryUtil.getService();
		
		for (HtmlForm htmlForm : service.getAllHtmlForms()) {
			boolean modified = false;

			String nameChoice = request.getParameter("name." + htmlForm.getId());
			if (StringUtils.isNotBlank(nameChoice)) {
				if (nameChoice.equals("html")) {
					// use the old value
					htmlForm.getForm().setName(htmlForm.getDeprecatedName());
					htmlForm.setDeprecatedName(null);
				} else if (nameChoice.equals("form")) {
					// clear the old value, since we don't want it
					htmlForm.setDeprecatedName(null);
				}
				modified = true;
			}
			
			String descriptionChoice = request.getParameter("description." + htmlForm.getId());
			if (StringUtils.isNotBlank(descriptionChoice)) {
				if (descriptionChoice.equals("html")) {
					// use the old value
					htmlForm.getForm().setDescription(htmlForm.getDeprecatedDescription());
					htmlForm.setDeprecatedDescription(null);
				} else if (descriptionChoice.equals("form")) {
					// clear the old value, since we don't want it
					htmlForm.setDeprecatedDescription(null);
				}
				modified = true;
			}
			
			if (modified) {
				service.saveHtmlForm(htmlForm);
			}
		}
		
		return "redirect:migrateNamesAndDescriptions.form";
	}
	
}
