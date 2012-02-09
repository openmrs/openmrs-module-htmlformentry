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

import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
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
				
				Integer id = null;
				try {
					id = Integer.valueOf(stateId);
				}
				catch (NumberFormatException e) {}
				
				boolean stateIdfound = false;
				
				for (ProgramWorkflowState workflowState : workflow.getStates()) {
					if (workflowState.getId().equals(id) || workflowState.getUuid().equals(stateId)) {
						String label = workflowState.getConcept().getName().getName();
						if (tagParams.getStateLabels() != null) {
							label = tagParams.getStateLabels().get(i);
						}
						states.put(label, workflowState);
						
						stateIdfound = true;
						break;
					}
				}
				
				if (!stateIdfound) {
					throw new IllegalArgumentException("workflow with id " + workflow.getId() + " does not have state id "
					        + stateId);
				}
			}
		} else {
			int i = 0;
			for (ProgramWorkflowState state : workflow.getStates()) {
				String label = state.getConcept().getName().getName();
				if (tagParams.getStateLabels() != null) {
					label = tagParams.getStateLabels().get(i);
				}
				states.put(label, state);
				i++;
			}
		}
		
		ProgramWorkflowState currentState = null;
		
		List<PatientProgram> programs = Context.getProgramWorkflowService().getPatientPrograms(context.getExistingPatient(),
		    workflow.getProgram(), null, null, null, null, false);
		
		Date encounterDatetime = new Date();
		if (context.getExistingEncounter() != null) {
			encounterDatetime = context.getExistingEncounter().getEncounterDatetime();
		}
		
		for (PatientProgram program : programs) {
			if (Boolean.TRUE.equals(program.getActive(encounterDatetime))) {
				PatientState state = program.getCurrentState(workflow);
				if (state != null) {
					currentState = state.getState();
					break;
				}
			}
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
		} else if (tagParams.getStyle().equals("checkbox")) {
			Entry<String, ProgramWorkflowState> state = states.entrySet().iterator().next();
			widget = new CheckboxWidget(state.getKey(), state.getValue().getUuid());
		} else {
			SingleOptionWidget singleOption;
			if (tagParams.getStyle().equals("dropdown")) {
				singleOption = new DropdownWidget();
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
		Object value = widget.getValue(session.getContext(), submission);
		
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (tagParams.getLabel() != null) {
			ret.append("<spring:message code=\"" + tagParams.getLabel() + "\" /> ");
		}
		ret.append(widget.generateHtml(context));
		return ret.toString();
	}
	
}
