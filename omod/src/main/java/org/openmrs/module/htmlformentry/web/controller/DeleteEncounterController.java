package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Used to delete an encounter. Handles {@code deleteEncounter.form}.
 * <p/>
 * Format: {@code POST deleteEncounters.form?encounterId=123&reason=reason_for_voiding}.
 * <p/>
 * Redirects to the dashboard for the current Patient.
 */
@Controller
public class DeleteEncounterController {

	@RequestMapping(method=RequestMethod.POST, value="/module/htmlformentry/deleteEncounter")
    public ModelAndView handleRequest(@RequestParam("encounterId") Integer encounterId,
    								  @RequestParam("htmlFormId") Integer htmlFormId,
                                      @RequestParam(value="reason", required=false) String reason,
                                      @RequestParam(value="returnUrl", required=false) String returnUrl,
                                      HttpServletRequest request) throws Exception {
        Encounter enc = Context.getEncounterService().getEncounter(encounterId);
        Integer ptId = enc.getPatient().getPatientId();
        HtmlFormEntryService hfes = Context.getService(HtmlFormEntryService.class);
        HtmlForm form = hfes.getHtmlForm(htmlFormId);
        HtmlFormEntryUtil.voidEncounter(enc, form, reason);
        Context.getEncounterService().saveEncounter(enc);
        if (!StringUtils.hasText(returnUrl)) {
        	returnUrl = request.getContextPath() + "/patientDashboard.form?patientId=" + ptId;
        }
        return new ModelAndView(new RedirectView(returnUrl));
    }

}
