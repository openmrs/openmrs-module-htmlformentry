package org.openmrs.module.htmlformentry.extension.html;

import org.openmrs.module.Extension;
import org.openmrs.module.web.extension.PatientDashboardTabExt;

/**
 * Adds a Html Form Entry tab to the Patient Dashboard
 */
public class HtmlFormEntryPatientDashboardTabExt extends PatientDashboardTabExt {

    public Extension.MEDIA_TYPE getMediaType() {
        return Extension.MEDIA_TYPE.html;
    }
    
    @Override
    public String getPortletUrl() {
        return "patientHtmlForms";
    }

    @Override
    public String getRequiredPrivilege() {
        return "Patient Dashboard - View Html Forms Section";
    }

    @Override
    public String getTabId() {
        return "patientHtmlForms";
    }

    @Override
    public String getTabName() {
        return "htmlformentry.patientDashboard.forms";
    }
    
}
