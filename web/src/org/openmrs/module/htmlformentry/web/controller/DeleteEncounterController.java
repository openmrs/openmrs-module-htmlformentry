package org.openmrs.module.htmlformentry.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Used to delete an encounter. Handles {@code deleteEncounter.form}.
 * <p/>
 * Format: {@code deleteEncounters.form?encounterId=123&reason=reason_for_voiding}.
 * <p/>
 * Redirects to the dashboard for the current Patient.
 */
public class DeleteEncounterController implements Controller {

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer encId = Integer.valueOf(request.getParameter("encounterId"));
        Encounter enc = Context.getEncounterService().getEncounter(encId);
        Integer ptId = enc.getPatientId();
        Context.getEncounterService().voidEncounter(enc, request.getParameter("reason"));
        String returnUrl = request.getContextPath() + "/patientDashboard.form?patientId=" + ptId; 
        if (StringUtils.hasText(request.getParameter("returnUrl"))) {
            returnUrl = request.getParameter("returnUrl");
        }
        return new ModelAndView(new RedirectView(returnUrl));
    }

}
