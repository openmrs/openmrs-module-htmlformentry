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
				
				ProgramWorkflowState state = HtmlFormEntryUtil.getState(stateId, workflow.getProgram());
				
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
		
		PatientState activePatientState = getActivePatientState(context.getExistingPatient(), encounterDatetime, workflow);
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
		
		if (tagParams.getStyle().equals("hidden")) {
			widget = new HiddenFieldWidget();
			//There is only one state
			Entry<String, ProgramWorkflowState> state = states.entrySet().iterator().next();
			widget.setInitialValue(state.getValue().getUuid());
		} else if (tagParams.getStyle().equals("checkbox")) {
			//There is only one state
			Entry<String, ProgramWorkflowState> state = states.entrySet().iterator().next();
			widget = new CheckboxWidget(state.getKey(), state.getValue().getUuid());
		} else {
			SingleOptionWidget singleOption;
			if (tagParams.getStyle().equals("dropdown")) {
				singleOption = new DropdownWidget();
				singleOption.addOption(new Option("", "", false));
			} else {
				singleOption = new RadioButtonsWidget();
			}
			
			for (Entry<String, ProgramWorkflowState> state : states.entrySet()) {
				boolean select = state.equals(currentState);
				singleOption.addOption(new Option(state.getKey(), state.getValue().getUuid(), select));
			}
			
			widget = singleOption;
		}
		
		if (currentState != null) {
			widget.setInitialValue(currentState.getUuid());
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
		PatientProgram patientProgram = HtmlFormEntryUtil.getPatientProgram(patient, workflow, encounterDatetime);
		if (patientProgram != null) {
			for (PatientState patientState : patientProgram.getStates()) {
				if (patientState.getState().getProgramWorkflow().equals(workflow)
				        && patientState.getActive(encounterDatetime)) {
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
				        .getEncounter().getEncounterDatetime(), workflow);
				if (!newState.equals(oldPatientState.getState())) {
					oldPatientState.setState(newState);
					session.getSubmissionActions().getPatientProgramsToUpdate().add(oldPatientState.getPatientProgram());
				}
			} else {
				ProgramWorkflowState state = Context.getProgramWorkflowService().getStateByUuid(stateUuid);
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
		if (tagParams.getLabel() != null) {
			ret.append(tagParams.getLabel());
		}
		ret.append(widget.generateHtml(context));
		return ret.toString();
	}
	
}
