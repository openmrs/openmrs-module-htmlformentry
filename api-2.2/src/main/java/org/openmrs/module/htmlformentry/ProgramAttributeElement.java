package org.openmrs.module.htmlformentry;

import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProgramAttributeElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private Program program;
	private PatientProgram patientProgram;
	private ProgramAttributeType programAttributeType;
	private PatientProgramAttribute patientProgramAttribute;

	public ProgramAttributeElement(FormEntryContext context, Map<String, String> parameters) {
		try {
			program = HtmlFormEntryUtil.getProgram(parameters.get("programId"));
			if (program == null)
				throw new FormEntryException("");
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program in: " + parameters);
		}

		try {
			programAttributeType = HtmlFormEntryUtil2_2.getProgramAttributeType(parameters.get("programAttributeTypeId"));
			if (programAttributeType == null)
				throw new FormEntryException("");
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program attribute type in: " + parameters);
		}


		try {
			patientProgram = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(context.getExistingPatient(), program, context.getExistingEncounter().getEncounterDatetime());
			if (patientProgram == null)
				throw new FormEntryException("");

		}
		catch (Exception ex) {
			throw new IllegalArgumentException("The patient is not enrolled in a program defined in: " + parameters);
		}

		try {
			if (parameters.get("programAttribute") == null) {
				throw new FormEntryException("");
			}
			patientProgramAttribute = new PatientProgramAttribute();
			patientProgramAttribute.setPatientProgram(patientProgram);
			patientProgramAttribute.setAttributeType(programAttributeType);
			patientProgramAttribute.setValue(parameters.get("programAttribute"));

			// add the patientprogram attribute to the
			patientProgram.addAttribute(patientProgramAttribute);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program attribute value in: " + parameters);
		}
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return Collections.emptySet();
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		// check if the patient is enrolled in a program if not then enroll them, but not in view mode
		if (session.getContext().getMode() != FormEntryContext.Mode.VIEW) {
			List<PatientProgram> patientProgramsToUpdate = new ArrayList<PatientProgram>();
			patientProgramsToUpdate.add(patientProgram);
			session.getSubmissionActions().setPatientProgramsToUpdate(patientProgramsToUpdate);
		}
		System.out.println("The program attribute is to be updated");
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		return ret.toString();
	}
}
