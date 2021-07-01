package org.openmrs.module.htmlformentry.action;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.InvalidActionException;
import org.openmrs.module.htmlformentry.schema.ObsGroup;

/**
 * Defines the actions to take when submitting or validating an ObsGroup
 */
public class ObsGroupAction implements FormSubmissionControllerAction {
	
	/**
	 * Creates an ObsGroupAction that should be applied at the start of an ObsGroup
	 * 
	 * @param groupingConcept the concept of the parent Obs
	 * @param existingGroup the parent Obs
	 * @return a new ObsGroupAction
	 */
	public static ObsGroupAction start(Concept groupingConcept, Obs existingGroup, ObsGroup ogSchemaObj) {
		return new ObsGroupAction(groupingConcept, existingGroup, ogSchemaObj, true);
	}
	
	/**
	 * Creates an ObsGroupAction that should be applied at the end of an ObsGroup
	 * 
	 * @return
	 */
	public static ObsGroupAction end(ObsGroup ogSchemaObj) {
		return new ObsGroupAction(null, null, ogSchemaObj, false);
	}
	
	//------------------------------------
	
	private Concept groupingConcept;
	
	private Obs existingGroup;
	
	private boolean start;
	
	private ObsGroup obsGroupSchemaObject;
	
	private ObsGroupAction(Concept groupingConcept, Obs existingGroup, ObsGroup ogSchemaObj, boolean start) {
		this.groupingConcept = groupingConcept;
		this.existingGroup = existingGroup;
		this.start = start;
		this.obsGroupSchemaObject = ogSchemaObj;
		
		if (this.groupingConcept != null)
			this.groupingConcept.getDatatype();
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		// this cannot fail validation
		return null;
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		try {
			if (start) {
				if (existingGroup != null) {
					session.getSubmissionActions().beginObsGroup(existingGroup);
				} else {
					Obs obsGroup = new Obs();
					obsGroup.setConcept(groupingConcept);
					session.getSubmissionActions().beginObsGroup(obsGroup);
				}
			} else {
				if (obsGroupSchemaObject.gethiddenObs() != null) {
					// existingGroup seems never to exist. Not sure if something needs to be done to handle it,
					// as is done in the other `if` branches.
					Concept hiddenObsConcept = obsGroupSchemaObject.gethiddenObs().getKey();
					Concept hiddenObsValue = obsGroupSchemaObject.gethiddenObs().getValue();
					// If the obs group will have any members, we want to ensure that the hidden obs is present.
					// Otherwise, we want to ensure that the hidden obs is absent.
					// 
					// To do this, we construct a list of the expected members which are not the hidden obs.
					// This is the current members (which includes those to be added), minus those that will
					// be voided, minus the hidden obs. In the process we find out whether the hidden obs
					// already exists, which informs whether we need to create or void it.
					ArrayList<Obs> members = new ArrayList<>();
					Obs currentObsGroup = session.getSubmissionActions().getCurrentObsGroup();
					if (currentObsGroup.getGroupMembers() != null) {
						members.addAll(currentObsGroup.getGroupMembers());
					}
					members.removeAll(session.getSubmissionActions().getObsToVoid());
					Obs existinghiddenObs = null;
					for (Obs member : members) {
						if (member.getConcept() == hiddenObsConcept && member.getValueCoded() == hiddenObsValue) {
							existinghiddenObs = member;
							members.remove(existinghiddenObs);
							break;
						}
					}
					if (!members.isEmpty() && existinghiddenObs == null) {
						session.getSubmissionActions().createObs(hiddenObsConcept, hiddenObsValue, null, null, null);
					} else if (members.isEmpty() && existinghiddenObs != null) {
						session.getSubmissionActions().modifyObs(existinghiddenObs, hiddenObsConcept, null, null, null,
						    null);
					}
				}
				session.getSubmissionActions().endObsGroup();
			}
		}
		catch (InvalidActionException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public Concept getGroupingConcept() {
		return groupingConcept;
	}
	
	public Obs getExistingGroup() {
		return existingGroup;
	}
	
	public ObsGroup getObsGroupSchemaObject() {
		return obsGroupSchemaObject;
	}
	
	public void setObsGroupSchemaObject(ObsGroup obsGroupSchemaObject) {
		this.obsGroupSchemaObject = obsGroupSchemaObject;
	}
	
}
