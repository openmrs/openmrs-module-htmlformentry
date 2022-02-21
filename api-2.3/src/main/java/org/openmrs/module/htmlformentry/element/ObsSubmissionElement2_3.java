package org.openmrs.module.htmlformentry.element;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext2_3;
import org.openmrs.module.htmlformentry.FormEntrySession;

import java.util.Date;
import java.util.Map;

public class ObsSubmissionElement2_3 extends ObsSubmissionElement<FormEntryContext2_3> {
	
	private String controlId;
	
	public ObsSubmissionElement2_3(FormEntryContext2_3 context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	private String getControlFormPath(FormEntrySession session) {
		
		String controlId = getTagControlId();
		
		if (StringUtils.isBlank(controlId)) {
			return null;
		} else {
			return session.generateControlFormPath(getTagControlId(), 0);
		}
	}
	
	@Override
	protected void modifyObs(FormEntrySession session, Obs existingObs, Concept concept, Object value, Date obsDatetime,
	        String accessionNumberValue, String comment) {
		session.getSubmissionActions().modifyObs(existingObs, concept, value, obsDatetime, accessionNumberValue, comment,
		    getControlFormPath(session));
	}
	
	@Override
	protected void createObs(FormEntrySession session, Concept concept, Object value, Date obsDatetime,
	        String accessionNumberValue, String comment) {
		session.getSubmissionActions().createObs(concept, value, obsDatetime, accessionNumberValue, comment,
		    getControlFormPath(session));
	}
	
	/**
	 * Sets the existing obs as provided by the form entry context. Sets the existing obs to null if no
	 * obs in the context's existing encounter could be matched by control id.
	 *
	 * @param context The form entry context
	 */
	@Override
	protected void setExistingObs(FormEntryContext2_3 context, Map<String, String> parameters, boolean isAutocomplete) {
		this.controlId = parameters.get("controlId");
		
		if (StringUtils.isBlank(this.controlId)) {
			super.setExistingObs(context, parameters, isAutocomplete);
			return;
		}
		
		if (context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0) {
			super.existingObs = context.getObsFromCurrentGroup(this.controlId);
		} else if (concept != null) {
			super.existingObs = context.getObsFromExistingObs(super.concept, this.controlId);
		} else {
			super.existingObs = context.getObsFromExistingObs(this.controlId);
		}
	}
	
	public String getTagControlId() {
		return controlId;
	}
	
	public void setTagControlId(String controlId) {
		this.controlId = controlId;
	}
	
}
