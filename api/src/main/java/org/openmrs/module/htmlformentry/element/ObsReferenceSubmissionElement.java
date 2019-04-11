package org.openmrs.module.htmlformentry.element;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ObsReferenceSubmissionElement extends ObsSubmissionElement {

    Obs referenceObs = null;

    String message = "Value of {{value}} recorded as part of {{encounterType}}";

    public ObsReferenceSubmissionElement(FormEntryContext context, Map<String, String> parameters) {

        super(context, parameters);

        if (StringUtils.isNotEmpty(parameters.get("referenceMessage"))) {
            message = parameters.get("referenceMessage");
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

        if (context.getMode().equals(FormEntryContext.Mode.VIEW)) {
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
        }

        String html = super.generateHtml(context);

        // if we are in enter or edit mode and have a reference obs and message, display the message
        if ((context.getMode().equals(FormEntryContext.Mode.ENTER) || (context.getMode().equals(FormEntryContext.Mode.EDIT)))
            && referenceObs != null && StringUtils.isNotBlank(message)) {

            // TODO this is pretty quick-and-dirty, and not localized; add better templating in the future?
            message = StringUtils.replace(message, "{{value}}", referenceObs.getValueAsString(Context.getLocale()));
            if (referenceObs.getEncounter() != null) {
                message = StringUtils.replace(message, "{{encounterType}}", referenceObs.getEncounter().getEncounterType().getName());
            }

            html = html + "<span>" + message + "</span>";
        }

        return html;
    }
}
