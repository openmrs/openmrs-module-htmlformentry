package org.openmrs.module.htmlformentry;

import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class ProgramAttributeElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private Program program;
	private ProgramAttributeType programAttributeType;

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
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return null;
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {

	}

	@Override
	public String generateHtml(FormEntryContext context) {
		return null;
	}
}
