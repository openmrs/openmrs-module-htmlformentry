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

    public ObsReferenceSubmissionElement(FormEntryContext context, Map<String, String> parameters) {

        super(context, parameters);

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

        // TODO handle edit mode--the following logic should only be in view mode
        // TODO how do we handle new encounters
        // TODO add "setInitialValue" to Widget?

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

        String html = super.generateHtml(context);
        return html;
    }
}
