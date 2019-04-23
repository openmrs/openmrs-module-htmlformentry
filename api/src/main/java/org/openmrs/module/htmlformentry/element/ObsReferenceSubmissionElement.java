package org.openmrs.module.htmlformentry.element;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ObsReferenceSubmissionElement extends ObsSubmissionElement {

    private Obs referenceObs = null;

    private Boolean prepopulateDataEntryWidget = false;

    private Boolean showReferenceMessage = true;

    private Boolean restrictDataEntry = false;

    private Boolean allowDataEntryOverride = false;

    private String overrideLabel = "Override";

    private String message = "(Value of {{value}} recorded as part of {{encounterType}} on {{encounterDate}})";

    public ObsReferenceSubmissionElement(FormEntryContext context, Map<String, String> parameters) {

        super(context, parameters);

        if (StringUtils.isNotEmpty(parameters.get("referenceMessage"))) {
            message = parameters.get("referenceMessage");
        }

        if (StringUtils.isNotEmpty(parameters.get("prepopulateDataEntryWidget"))) {
            prepopulateDataEntryWidget = StringUtils.equalsIgnoreCase(parameters.get("prepopulateDataEntryWidget"), "true");
        }

        if (StringUtils.isNotEmpty(parameters.get("showReferenceMessage"))) {
            showReferenceMessage = StringUtils.equalsIgnoreCase(parameters.get("showReferenceMessage"), "true");
        }

        if (StringUtils.isNotEmpty(parameters.get("restrictDataEntry"))) {
            restrictDataEntry = StringUtils.equalsIgnoreCase(parameters.get("restrictDataEntry"), "true");
        }

        if (StringUtils.isNotEmpty(parameters.get("allowDataEntryOverride"))) {
            allowDataEntryOverride = StringUtils.equalsIgnoreCase(parameters.get("allowDataEntryOverride"), "true");
        }

        if (StringUtils.isNotEmpty(parameters.get("overrideLabel"))) {
            overrideLabel = parameters.get("overrideLabel");
        }

        String conceptId = parameters.get("conceptId");
        Concept concept = null;

        // TODO handle error cases
        if (StringUtils.isNotBlank(conceptId)) {
            concept = HtmlFormEntryUtil.getConcept(conceptId);
        }

        // TODO handle error cases
        if (concept != null && context.getExistingEncounter()  != null) {
            List<Obs> obsList = Context.getObsService().getObservations(
                    Collections.singletonList((Person) context.getExistingPatient()),
                    null, Collections.singletonList(concept),
                    null, null, null, null, 1, null,
                    new DateTime(context.getExistingEncounter().getEncounterDatetime()).withTime(0, 0, 0, 0).toDate(),
                    new DateTime(context.getExistingEncounter().getEncounterDatetime()).withTime(23,59,59,999).toDate(),
                    false);

            if (!obsList.isEmpty()) {
                referenceObs = obsList.get(0);
            }
        }
    }

    @Override
    public String generateHtml(FormEntryContext context) {

        if (context.getMode().equals(FormEntryContext.Mode.VIEW) || prepopulateDataEntryWidget) {
            if (this.valueWidget instanceof NumberFieldWidget) {
                if (((NumberFieldWidget) this.valueWidget).getInitialValue() == null && referenceObs != null) {
                    (this.valueWidget).setInitialValue(referenceObs.getValueNumeric());
                }
            }

            if (this.valueWidget instanceof SingleOptionWidget) {
                if (((SingleOptionWidget) this.valueWidget).getInitialValue() == null && referenceObs != null) {
                    (this.valueWidget).setInitialValue(referenceObs.getValueCoded());
                }
            }

            if (this.valueWidget instanceof TextFieldWidget) {
                if (((TextFieldWidget) this.valueWidget).getInitialValue() == null && referenceObs != null) {
                    (this.valueWidget).setInitialValue(referenceObs.getValueText());
                }
            }

            // TODO also handle DateTime and Time widgets
            if (this.valueWidget instanceof DateWidget) {
                if (((DateWidget) this.valueWidget).getInitialValue() == null && referenceObs != null) {
                    (this.valueWidget).setInitialValue(referenceObs.getValueDatetime());
                }
            }
        }

        String html = super.generateHtml(context);

        // if we are in enter or edit mode and have a reference obs but no existing obs, display reference message
        if ((context.getMode().equals(FormEntryContext.Mode.ENTER) || (context.getMode().equals(FormEntryContext.Mode.EDIT)))
            && referenceObs != null && this.getExistingObs() == null) {

            String fieldName = context.getFieldName(this.valueWidget) + "-restrict";

            if (restrictDataEntry) {
                html = "<span id=\"" + fieldName + "\" style=\"display:none\">" + html + "</span>";
            }

            if (showReferenceMessage) {
                // TODO this is pretty quick-and-dirty, and not fully localized; add better templating in the future?
                String value = referenceObs.getValueAsString(Context.getLocale());
                if (showUnits) {
                    value = value + " " + this.getUnits(context);
                }

                message = StringUtils.replace(message, "{{value}}", value);
                if (referenceObs.getEncounter() != null) {
                    message = StringUtils.replace(message, "{{encounterType}}", referenceObs.getEncounter().getEncounterType().getName());
                    message = StringUtils.replace(message, "{{encounterDate}}", dateFormat().format(referenceObs.getEncounter().getEncounterDatetime()));
                }

                html = html + "<span>" + message + "</span>";
            }

            if (restrictDataEntry && allowDataEntryOverride) {
                html = html + " <button type=\"button\" onclick=\"jQuery('#" + fieldName + "').show()\">" + overrideLabel +"</button>";
            }
        }

        return html;
    }

    private SimpleDateFormat dateFormat() {
        String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT);
        if (StringUtils.isNotEmpty(df)) {
            return new SimpleDateFormat(df, Context.getLocale());
        } else {
            return Context.getDateFormat();
        }
    }
}
