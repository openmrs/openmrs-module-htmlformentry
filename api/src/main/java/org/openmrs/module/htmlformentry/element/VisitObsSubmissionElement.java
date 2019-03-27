package org.openmrs.module.htmlformentry.element;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.EncounterDataHolder;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.widget.EditableWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

import java.util.List;
import java.util.Map;

/**
 * Extends ObsSubmissionElement to support more complex behavior for Obs within a Visit
 */
public class VisitObsSubmissionElement extends ObsSubmissionElement {

	public VisitObsSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}

	@Override
	protected List<Obs> removeFromInitialData(FormEntryContext context, Map<String, String> parameters) {
		List<Obs> l = removeFromEncounterData(context.getCurrentEncounterData(), parameters);
		if (l.isEmpty()) {
			for (EncounterDataHolder edh : context.getCurrentVisitData()) {
				l = removeFromEncounterData(edh, parameters);
				if (l.size() > 0) {
					return l;
				}
			}
		}
		return l;
	}

	@Override
	protected void setWidgetValue(Widget widget, Object initialValue, FormEntryContext context) {
		if (widget instanceof EditableWidget) {
			if (getExistingObs() != null) {
				Encounter obsEncounter = getExistingObs().getEncounter();
				Encounter formEncounter = context.getCurrentEncounterData().getEncounter();
				if (formEncounter != null && obsEncounter != null && !formEncounter.equals(obsEncounter)) {
					((EditableWidget) widget).setViewOnly(true);
				}
			}
		}
		setWidgetValue(widget, initialValue, context);
	}
}
