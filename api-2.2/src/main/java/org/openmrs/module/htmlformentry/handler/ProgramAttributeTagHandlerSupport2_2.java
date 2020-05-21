package org.openmrs.module.htmlformentry.handler;

import org.apache.commons.lang.ObjectUtils;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil2_2;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@OpenmrsProfile(openmrsPlatformVersion = "2.2.0")
public class ProgramAttributeTagHandlerSupport2_2 extends SubstitutionTagHandler implements ProgramAttributeTagHandlerSupport {

	@Autowired
	ProgramWorkflowService programWorkflowService;

	public void setProgramWorkflowService(ProgramWorkflowService programWorkflowService) {
		this.programWorkflowService = programWorkflowService;
	}

	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("programId", Program.class));
		attributeDescriptors.add(new AttributeDescriptor("programAttributeTypeId", ProgramAttributeType.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}

	@Override
	public String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> parameters) {
		Action action = new Action();
		action.setProgramAttributeValue(parameters.get("programAttribute"));
		action.setProgramId(parameters.get("programId"));
		action.setProgramAttributeTypeId(parameters.get("programAttributeTypeId"));

		controller.addAction(action);

		return "";
	}

	/**
	 * This method is only for testing, e.g. to construct an action to match against
	 */
	public Action newAction() {
		return new Action();
	}

	public class Action implements FormSubmissionControllerAction, CustomFormSubmissionAction {

		private String programAttributeValue;
		private String programId;
		private String programAttributeTypeId;

		public void setProgramId(String programId) {
			this.programId = programId;
		}

		public void setProgramAttributeTypeId(String programAttributeTypeId) {
			this.programAttributeTypeId = programAttributeTypeId;
		}

		@Override
		public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
			// this can't fail validation since there is no user-controller input
			return null;
		}

		@Override
		public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
			session.getSubmissionActions().addCustomFormSubmissionAction(this);
		}

		@Override
		public void applyAction(FormEntrySession session) {
			Patient patient = session.getPatient();
			Encounter encounter = session.getEncounter();
			ProgramAttributeType programAttributeType = new ProgramAttributeType();
			Date encounterDate = (Date) ObjectUtils.defaultIfNull(encounter.getEncounterDatetime(), new Date());
			PatientProgram patientProgram = new PatientProgram();
			patientProgram.setPatient(patient);
			patientProgram.setDateEnrolled(new Date());

			Program program;


			try {
				program = HtmlFormEntryUtil.getProgram(programId);
				if (program == null)
					throw new FormEntryException("");

				patientProgram.setProgram(program);
			} catch (Exception ex) {
				throw new IllegalArgumentException("Couldn't find program in with id " + programId);
			}

			try {
				programAttributeType = HtmlFormEntryUtil2_2.getProgramAttributeType(programAttributeTypeId);
				if (programAttributeType == null)
					throw new FormEntryException("");
			} catch (Exception ex) {
				throw new IllegalArgumentException("Couldn't find program attribute type with id " + programAttributeTypeId);
			}


			if (HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, encounterDate)) {
				patientProgram = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(patient, program, encounterDate);
			}

			PatientProgramAttribute patientProgramAttribute = new PatientProgramAttribute();
			patientProgramAttribute.setAttributeType(programAttributeType);
			patientProgramAttribute.setValue(this.programAttributeValue);

			patientProgram.setAttribute(patientProgramAttribute);

			programWorkflowService.savePatientProgram(patientProgram);
		}

		public void setProgramAttributeValue(String programAttributeValue) {
			this.programAttributeValue = programAttributeValue;
		}
	}
}
