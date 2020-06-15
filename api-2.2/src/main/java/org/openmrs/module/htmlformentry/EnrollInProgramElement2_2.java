package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.PatientState;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.element.EnrollInProgramElement;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.util.OpenmrsUtil;

public class EnrollInProgramElement2_2 extends EnrollInProgramElement {
	
	private List<PatientProgramAttribute> patientProgramAttributes;
	
	private List<ProgramWorkflowState> states;
	
	private Program program;
	
	private CheckboxWidget checkToEnrollWidget;
	
	private ErrorWidget checkToEnrollErrorWidget;
	
	private DateWidget dateWidget;
	
	private ErrorWidget dateErrorWidget;
	
	private Stack<Object> stack = new Stack<Object>();
	
	private List<PatientProgram> patientProgramsToUpdate = new Vector<PatientProgram>();
	
	private List<PatientProgram> patientProgramsToCreate = new Vector<PatientProgram>();
	
	public EnrollInProgramElement2_2(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
		
		try {
			program = HtmlFormEntryUtil.getProgram(parameters.get("programId"));
			if (program == null)
				throw new FormEntryException("");
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program in: " + parameters);
		}
		
		if ("true".equalsIgnoreCase(parameters.get("showDate"))) {
			dateWidget = new DateWidget();
			dateErrorWidget = new ErrorWidget();
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, dateErrorWidget);
		}
		
		if ("true".equalsIgnoreCase(parameters.get("showCheckbox"))) {
			checkToEnrollWidget = new CheckboxWidget();
			{ // If patient is already enrolled, check and disable the checkbox
				Patient patient = context.getExistingPatient();
				Date encounterDate = (Date) ObjectUtils.defaultIfNull(context.getPreviousEncounterDate(),
				    ObjectUtils.defaultIfNull(context.getDefaultEncounterDate(), new Date()));
				if (HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, encounterDate)) {
					checkToEnrollWidget.setInitialValue("true");
					checkToEnrollWidget.setDisabled(true);
				}
			}
			context.registerWidget(checkToEnrollWidget);
			checkToEnrollErrorWidget = new ErrorWidget();
			context.registerErrorWidget(checkToEnrollWidget, checkToEnrollErrorWidget);
		}
		
		String patientProgramAttributeIds = parameters.get("patientProgramAttributes");
		if (StringUtils.isNotBlank(patientProgramAttributeIds)) {
			patientProgramAttributes = new ArrayList<PatientProgramAttribute>();
			String[] patientProgramAttributesIdsUuidsOrNames = patientProgramAttributeIds.split(",");
			new HashSet<String>();
			
			for (String value : patientProgramAttributesIdsUuidsOrNames) {
				value = value.trim();
				PatientProgramAttribute programAttribute = HtmlFormEntryUtil2_2.getPatientProgramAttribute(value, program);
				if (programAttribute == null) {
					String errorMsgPart = "with an id or uuid";
					if (value.indexOf(":") > -1)
						throw new FormEntryException(
						        "Cannot find a program attribute " + errorMsgPart + " that matches '" + value + "'");
				}
				
				if (!patientProgramAttributes.contains(programAttribute))
					patientProgramAttributes.add(programAttribute);
			}
		}
		
		String stateIdsStr = parameters.get("stateIds");
		if (StringUtils.isNotBlank(stateIdsStr)) {
			states = new ArrayList<ProgramWorkflowState>();
			String[] stateIdsUuidsOrPrefNames = stateIdsStr.split(",");
			// set to store unique work flow state combinations so as to determine multiple
			// states in same work flow
			Set<String> workflowsAndStates = new HashSet<String>();
			for (String value : stateIdsUuidsOrPrefNames) {
				value = value.trim();
				ProgramWorkflowState state = HtmlFormEntryUtil.getState(value, program);
				if (state == null) {
					String errorMsgPart = "with an id or uuid";
					if (value.indexOf(":") > -1)
						errorMsgPart = "associated to a concept with a concept mapping";
					throw new FormEntryException(
					        "Cannot find a program work flow state " + errorMsgPart + " that matches '" + value + "'");
				} else if (!state.getInitial()) {
					throw new FormEntryException(
					        "The program work flow state that matches '" + value + "' is not marked as initial");
				} else if (!workflowsAndStates.add(state.getProgramWorkflow().getUuid())) {
					throw new FormEntryException("A patient cannot be in multiple states in the same workflow");
				}
				if (!states.contains(state))
					states.add(state);
			}
			
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		// Only enroll if we are not in view mode and either the checkbox is checked or
		// it doesn't exist
		if (session.getContext().getMode() != Mode.VIEW && (checkToEnrollWidget == null
		        || "true".equals(checkToEnrollWidget.getValue(session.getContext(), submission)))) {
			Date selectedDate = null;
			if (dateWidget != null) {
				selectedDate = dateWidget.getValue(session.getContext(), submission);
			}
			enrollInProgram(program, selectedDate, states, patientProgramAttributes);
		}
	}
	
	private void enrollInProgram(Program program, Date enrollmentDate, List<ProgramWorkflowState> states,
	        List<PatientProgramAttribute> patientProgramAttributes) {
		if (program == null)
			throw new IllegalArgumentException("Cannot enroll in a blank program");
		
		Patient patient = highestOnStack(Patient.class);
		if (patient == null)
			throw new IllegalArgumentException("Cannot enroll in a program outside of a Patient");
		Encounter encounter = highestOnStack(Encounter.class);
		
		// if an enrollment date has not been specified, enrollment date is the
		// encounter date
		enrollmentDate = (enrollmentDate != null) ? enrollmentDate
		        : (encounter != null) ? encounter.getEncounterDatetime() : null;
		
		if (enrollmentDate == null)
			throw new IllegalArgumentException(
			        "Cannot enroll in a program without specifying an Encounter Date or Enrollment Date");
		
		// only need to do some if the patient is not enrolled in the specified program
		// on the specified date
		if (!HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, enrollmentDate)) {
			
			// see if the patient is enrolled in this program in the future
			PatientProgram pp = HtmlFormEntryUtil.getClosestFutureProgramEnrollment(patient, program, enrollmentDate);
			
			if (pp != null) {
				// set the start dates of all states with a start date equal to the enrollment
				// date to the selected date
				for (PatientState patientState : pp.getStates()) {
					if (OpenmrsUtil.nullSafeEquals(patientState.getStartDate(), pp.getDateEnrolled())) {
						patientState.setStartDate(enrollmentDate);
					}
				}
				
				// set the program enrollment date to the newly selected date
				pp.setDateEnrolled(enrollmentDate);
				
				patientProgramsToUpdate.add(pp);
			}
			// otherwise, create the new program
			else {
				pp = new PatientProgram();
				pp.setPatient(patient);
				pp.setProgram(program);
				if (enrollmentDate != null)
					pp.setDateEnrolled(enrollmentDate);
				
				if (states != null) {
					for (ProgramWorkflowState programWorkflowState : states) {
						pp.transitionToState(programWorkflowState, enrollmentDate);
					}
				}
				
				if (patientProgramAttributes != null) {
					for (PatientProgramAttribute pogramattribute : patientProgramAttributes) {
						pp.setAttribute(pogramattribute);
					}
				}
				
				patientProgramsToCreate.add(pp);
			}
			
		}
	}
	
	/**
	 * Utility method that returns the object of a specified class that was most recently added to the
	 * stack
	 */
	@SuppressWarnings("unchecked")
	private <T> T highestOnStack(Class<T> clazz) {
		for (ListIterator<Object> iter = stack.listIterator(stack.size()); iter.hasPrevious();) {
			Object o = iter.previous();
			if (clazz.isAssignableFrom(o.getClass()))
				return (T) o;
		}
		return null;
	}
	
}
