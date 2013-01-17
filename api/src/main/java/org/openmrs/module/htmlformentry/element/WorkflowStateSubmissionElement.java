/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntryException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.tag.WorkflowStateTag;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.HiddenFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

/**
 *
 */
public class WorkflowStateSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private final WorkflowStateTag tagParams;
	
	private ProgramWorkflow workflow;
	
	private Map<String, ProgramWorkflowState> states;
	
	private PatientState activePatientState;
	
	private String label;
	
	private Widget widget;
	
	/**
	 * @param context
	 * @param parameters
	 */
	public WorkflowStateSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		tagParams = new WorkflowStateTag(parameters);
		
		workflow = HtmlFormEntryUtil.getWorkflow(tagParams.getWorkflowId());
		if (workflow == null) {
			throw new IllegalArgumentException("workflow does not exist: " + tagParams.getWorkflowId());
		}
		
		states = new LinkedHashMap<String, ProgramWorkflowState>();
		if (tagParams.getStateIds() != null) {
			for (int i = 0; i < tagParams.getStateIds().size(); i++) {
				String stateId = tagParams.getStateIds().get(i);
				
				ProgramWorkflowState state = HtmlFormEntryUtil.getState(stateId, workflow);
				
				if (state == null) {
					throw new IllegalArgumentException("workflow with id " + workflow.getId() + " does not have state id "
					        + stateId);
				}
				
				String label = state.getConcept().getName().getName();
				if (tagParams.getStateLabels() != null) {
					label = tagParams.getStateLabels().get(i);
				}
				states.put(label, state);
			}
		} else {
			for (ProgramWorkflowState state : workflow.getStates()) {
				if (state.isRetired()) {
					continue;
				}
				
				String label = state.getConcept().getName().getName();
				states.put(label, state);
			}
		}
		
		Date encounterDatetime = new Date();
		if (context.getExistingEncounter() != null) {
			encounterDatetime = context.getExistingEncounter().getEncounterDatetime();
		}
		
		ProgramWorkflowState currentState = null;
		
		activePatientState = getActivePatientState(context.getExistingPatient(), encounterDatetime, workflow);
		if (activePatientState != null) {
			currentState = activePatientState.getState();
		}
		
		if (currentState == null) {
			// Remove other than initial states
			for (Iterator<ProgramWorkflowState> it = states.values().iterator(); it.hasNext();) {
				ProgramWorkflowState state = it.next();
				if (Boolean.FALSE.equals(state.getInitial())) {
					it.remove();
				}
			}
		}
		
		if (tagParams.getLabelCode() != null) {
			label = context.getTranslator().translate(Context.getLocale().toString(), tagParams.getLabelCode());
		}
		
		if (label == null && tagParams.getLabelText() != null) {
			label = tagParams.getLabelText();
		}
		
		if (tagParams.getType().equals("hidden")) {
			widget = new HiddenFieldWidget();
			// there is only one state
			Entry<String, ProgramWorkflowState> state = states.entrySet().iterator().next();
			widget.setInitialValue(state.getValue().getUuid());
		} else if (tagParams.getType().equals("checkbox")) {
			// there is only one state
			Entry<String, ProgramWorkflowState> state = states.entrySet().iterator().next();
			widget = new CheckboxWidget(state.getKey(), state.getValue().getUuid());
		} else {
			SingleOptionWidget singleOption;
			if (tagParams.getType().equals("dropdown")) {
				singleOption = new DropdownWidget();
				singleOption.addOption(new Option("", "", false));
			} else {
				singleOption = new RadioButtonsWidget();
			}
			
			for (Entry<String, ProgramWorkflowState> state : states.entrySet()) {
				boolean select = state.getValue().equals(currentState);
				singleOption.addOption(new Option(state.getKey(), state.getValue().getUuid(), select));
			}
			
			widget = singleOption;
		}
		
		if (currentState != null) {
			if (widget instanceof CheckboxWidget) {
				// if this is a checkbox, we only want to set the initial value (ie, show the checkbox as checked) if the current state matches the state associated with the checkbox
				if (currentState.getUuid().equals(((CheckboxWidget) widget).getValue())) {
					widget.setInitialValue(currentState.getUuid());
				}
			}
			// "initialValue" has a different meaning for a hidden widget (it is set above)
			else if (!(widget instanceof HiddenFieldWidget)){
				widget.setInitialValue(currentState.getUuid());
			}
		}
		
		context.registerWidget(widget);
	}
	
	/**
	 * @param context
	 * @param encounterDatetime
	 * @param workflow
	 * @return
	 */
	private PatientState getActivePatientState(Patient patient, Date encounterDatetime, ProgramWorkflow workflow) {
		PatientProgram patientProgram = HtmlFormEntryUtil.getPatientProgramByWorkflow(patient, workflow);
		if (patientProgram != null) {
			for (PatientState patientState : patientProgram.statesInWorkflow(workflow,false)) {
				if (patientState.getActive(encounterDatetime)) {
					return patientState;
				}
			}
		}
		return null;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> errors = new ArrayList<FormSubmissionError>();
		return errors;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		String stateUuid = (String) widget.getValue(session.getContext(), submission);
		if (!StringUtils.isBlank(stateUuid)) {
			if (Mode.EDIT.equals(session.getContext().getMode())) {
				
				ProgramWorkflowState newState = Context.getProgramWorkflowService().getStateByUuid(stateUuid);
				PatientState oldPatientState = getActivePatientState(session.getContext().getExistingPatient(), session
				        .getContext().getPreviousEncounterDate(), workflow);
				
				// if no old state, simply transition to this new state
				if (oldPatientState == null) {

                    // if there is an active program enrollment in this program, add it to the programs to update (so that
                    // it is picked up by the FormSubmissionAction.transitionToState method and a new program is not created)
                    PatientProgram patientProgram = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(session.getPatient(),
                            newState.getProgramWorkflow().getProgram(), session.getEncounter().getEncounterDatetime());

                    if (patientProgram != null) {
                        session.getSubmissionActions().getPatientProgramsToUpdate().add(patientProgram);
                    }

					session.getSubmissionActions().transitionToState(newState);
				}
				else {
					PatientProgram patientProgram = oldPatientState.getPatientProgram();
					Date previousEncounterDate = session.getContext().getPreviousEncounterDate();
					Date newEncounterDate = session.getEncounter().getEncounterDatetime();
					
					// if the encounter date has been moved earlier
					if (previousEncounterDate != null  && newEncounterDate.before(previousEncounterDate)) {
						
						// if there is an existing patient state on the new encounter date and it differs from the state on the old encounter date
						// we need to end it
						PatientState existingPatientStateOnNewEncounterDate = getActivePatientState(session.getContext().getExistingPatient(), newEncounterDate, workflow);
						if (existingPatientStateOnNewEncounterDate != null &&  !existingPatientStateOnNewEncounterDate.equals(oldPatientState)) {
							existingPatientStateOnNewEncounterDate.setEndDate(newEncounterDate);
						}
						
						// we also need to void any other states for this workflow that may have started between the new and old encounter dates
						for (PatientState state : patientProgram.statesInWorkflow(workflow, false)) {
							if (!state.equals(oldPatientState) && (state.getStartDate().after(newEncounterDate) || state.getStartDate().compareTo(newEncounterDate) == 0) 
									&& state.getStartDate().before(previousEncounterDate)) {
								state.setVoided(true);
								state.setVoidedBy(Context.getAuthenticatedUser());
								state.setVoidReason("voided during htmlformentry submission");
							}
						}
					}
					
					// if the encounter date has been moved later
					if (previousEncounterDate != null && newEncounterDate.after(previousEncounterDate)) {
						// make sure we aren't trying to move the state start date past its end date
						if (oldPatientState.getEndDate() != null && newEncounterDate.after(oldPatientState.getEndDate())) {
							throw new FormEntryException("Cannot move encounter date ahead of end date of current active state");
						}
						
						// if there is a state that ended on the previous encounter date, its end date needs to be set to the new encounter date
						for (PatientState state : patientProgram.statesInWorkflow(workflow, false)) {
							if (!state.equals(oldPatientState) && state.getEndDate().compareTo(previousEncounterDate) == 0) {
								state.setEndDate(newEncounterDate);
							}
						}
					}
					
					// change the state if necessary
					if (!newState.equals(oldPatientState.getState())) {
						oldPatientState.setState(newState);
					}
					
					// update the state start date
					oldPatientState.setStartDate(newEncounterDate);
					
					// roll the program enrollment date earlier if necessary
					if (oldPatientState.getPatientProgram().getDateEnrolled().after(oldPatientState.getStartDate())) {
						oldPatientState.getPatientProgram().setDateEnrolled(oldPatientState.getStartDate());
					}
					
					session.getSubmissionActions().getPatientProgramsToUpdate().add(oldPatientState.getPatientProgram());
				}
			} else {   // handle ENTER state
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(stateUuid);

                // if there is an active program enrollment in the state, add it to the programs to update (so that
                // it is picked up by the FormSubmissionAction.transitionToState method and a new program is not created)
                PatientProgram patientProgram = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(session.getPatient(),
                        state.getProgramWorkflow().getProgram(), session.getEncounter().getEncounterDatetime());

                if (patientProgram != null) {
                    session.getSubmissionActions().getPatientProgramsToUpdate().add(patientProgram);
                }

				session.getSubmissionActions().transitionToState(state);
			}
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (label != null) {
			ret.append(label);
		}
		ret.append(widget.generateHtml(context));
		return ret.toString();
	}
	
}
