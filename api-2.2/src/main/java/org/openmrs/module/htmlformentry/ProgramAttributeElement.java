package org.openmrs.module.htmlformentry;

import net.sf.saxon.type.SchemaURIResolver;
import org.apache.commons.lang.StringUtils;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
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

	private ProgramAttributeType programAttributeType;

	private ProgramWorkflowService programWorkflowService;

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
			programAttributeType = HTMLFormEntryUtil2_2.getProgramAttributeType(parameters.get("programAttributeTypeId"));
			if (programAttributeType == null)
				throw new FormEntryException("");

			// add the program attribute to the program
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Couldn't find program attribute in: " + parameters);
		}
	}

	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		return "";
	}

	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		// the programs to be created or updated
		List<PatientProgram> patientProgramList = new ArrayList<PatientProgram>(session.getSubmissionActions().getPatientProgramsToCreate());
		if (session.getContext().getMode() == Mode.EDIT) {
			patientProgramList = new ArrayList<PatientProgram>(session.getSubmissionActions().getPatientProgramsToUpdate());
		}

		// find the programs that the patient is enrolled into
		PatientProgram patientProgramToCreate = HtmlFormEntryUtil.getPatientProgramByProgram(patientProgramList, program);
		if (patientProgramToCreate == null) {
			throw new IllegalArgumentException("The patient is not enrolled in any program for the attributes to be set");
		}


	}

	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return Collections.emptySet();
	}

}
