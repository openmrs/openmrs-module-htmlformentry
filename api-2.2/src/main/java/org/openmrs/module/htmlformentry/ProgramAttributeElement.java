package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.ObjectUtils;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.springframework.beans.factory.annotation.Autowired;

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
    private PatientProgramAttribute patientProgramAttribute;

    @Autowired
    ProgramWorkflowService programWorkflowService;

    public ProgramAttributeElement(FormEntryContext context, Map<String, String> parameters) {
        Patient patient = context.getExistingPatient();

        ProgramAttributeType programAttributeType;
        Date encounterDate = (Date) ObjectUtils.defaultIfNull(context.getPreviousEncounterDate(),
                ObjectUtils.defaultIfNull(context.getDefaultEncounterDate(), new Date()));

        try {
            program = HtmlFormEntryUtil.getProgram(parameters.get("programId"));
            if (program == null)
                throw new FormEntryException("");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Couldn't find program in: " + parameters);
        }

        try {
            programAttributeType = HtmlFormEntryUtil2_2.getProgramAttributeType(parameters.get("programAttributeTypeId"));
            if (programAttributeType == null)
                throw new FormEntryException("");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Couldn't find program attribute type in: " + parameters);
        }


        if (HtmlFormEntryUtil.isEnrolledInProgramOnDate(patient, program, encounterDate)) {
            patientProgram = HtmlFormEntryUtil.getPatientProgramByProgramOnDate(patient, program, encounterDate);
        } else {
            patientProgram = new PatientProgram();
        }

        if (parameters.get("programAttribute") == null) {
            return;
        }
        patientProgramAttribute = new PatientProgramAttribute();
        patientProgramAttribute.setPatientProgram(patientProgram);
        patientProgramAttribute.setAttributeType(programAttributeType);
        patientProgramAttribute.setValue(parameters.get("programAttribute"));

        // add the patientprogram attribute to the
        patientProgram.addAttribute(patientProgramAttribute);
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
    }

    @Override
    public String generateHtml(FormEntryContext context) {
        StringBuilder ret = new StringBuilder();
        return ret.toString();
    }
}
