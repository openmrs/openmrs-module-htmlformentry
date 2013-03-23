package org.openmrs.module.htmlformentry;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.ObsService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.result.EmptyResult;
import org.openmrs.logic.result.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class VelocityFunctions {
	
	private FormEntrySession session;
	private ObsService obsService;
	private LogicService logicService;
	private ProgramWorkflowService programWorkflowService;
	
	public VelocityFunctions(FormEntrySession session) {
		this.session = session;
	}
	
	private ObsService getObsService() {
		if (obsService == null)
			obsService = Context.getObsService();
		return obsService;
	}
	
	private LogicService getLogicService() {
		if (logicService == null)
			logicService = Context.getLogicService();
		return logicService;
	}
	
	private ProgramWorkflowService getProgramWorkflowService() {
		if (programWorkflowService == null)
			programWorkflowService = Context.getProgramWorkflowService();
		return programWorkflowService;
	}
	
	private void cannotBePreviewed() {
		if ("testing-html-form-entry".equals(session.getPatient().getUuid()))
			throw new CannotBePreviewedException();
    }

	public List<Obs> allObs(Integer conceptId) {
		if (session.getPatient() == null)
			return new ArrayList<Obs>();
		cannotBePreviewed();
		Patient p = session.getPatient();
		if (p == null)
			return new ArrayList<Obs>();
		else
			return getObsService().getObservationsByPersonAndConcept(p, new Concept(conceptId));
	}
	
	/**
	 * @return the most recent obs given the passed conceptId
	 * @should return the most recent obs given the passed conceptId
	 */
	public Obs latestObs(Integer conceptId) {
		List<Obs> obs = allObs(conceptId);
		if (obs == null || obs.isEmpty())
			return null;
		else
			return obs.get(0);
	}
	
	/**
	 * @return the first obs given the passed conceptId
	 * @should return the first obs given the passed conceptId
	 */
	public Obs earliestObs(Integer conceptId) {
		List<Obs> obs = allObs(conceptId);
		if (obs == null || obs.isEmpty())
			return null;
		else
			return obs.get(obs.size() - 1);
	}
	
	/**
	 * @return the all the encounters of the specified type
	 * @should return all the encounters of the specified type
	 * @should return all encounters if no type specified
	 */
	public List<Encounter> allEncounters(EncounterType type) {
		if (session.getPatient() == null) {
			return new ArrayList<Encounter>();
		}
		cannotBePreviewed();
		Patient p = session.getPatient();
		if (p == null) {
			return new ArrayList<Encounter>();
		}
		else {
			if (type == null) {
				return Context.getEncounterService().getEncountersByPatient(p);
			}
			else {
				List<EncounterType> typeList = new ArrayList<EncounterType>();
				typeList.add(type);
				return Context.getEncounterService().getEncounters(p, null, null, null, null, typeList, null, false);
			}
		}
	}
	
	/**
	 * @return the most recent encounter of the specified type
	 * @should return the most recent encounter of the specified type
	 * @should return the most recent encounter of any type if no type specified
	 */
	public Encounter latestEncounter(EncounterType type) {
		List<Encounter> encounters = allEncounters(type);
		if (encounters == null || encounters.isEmpty()) {
			return null;
		}
		else {
			return encounters.get(encounters.size() - 1);
		}
	}
	
	/**
	 * @return the most recent encounter
	 * @should return the most recent encounter
	 */
	public Encounter latestEncounter() {
		return latestEncounter(null);
	}
	
	public Result logic(String expression) {
		if (session.getPatient() == null)
			return new EmptyResult();
		cannotBePreviewed();
		LogicCriteria lc = getLogicService().parse(expression);
		return getLogicService().eval(session.getPatient(), lc);
	}

	@SuppressWarnings("deprecation")
    public PatientState currentProgramWorkflowStatus(Integer programWorkflowId) {
		Patient p = session.getPatient();
		if (p == null || p.getId() == null) {
			return null;
		}
		cannotBePreviewed();
		ProgramWorkflow workflow = getProgramWorkflowService().getWorkflow(programWorkflowId); // not sure if and how I want to reference the UUID
		List<PatientProgram> pps = getProgramWorkflowService().getPatientPrograms(p, workflow.getProgram(), null, null,
		    null, null, false);
		PatientProgram mostRecentPatientProgram = null;
		for (PatientProgram pp : pps) {
			// try to figure out which program enrollment is active or the most
			// recent one; guess this would better fit somewhere in the
			// ProgramWorkflowServive
			if (!pp.isVoided()) {
				if (mostRecentPatientProgram == null) {
					mostRecentPatientProgram = pp;
				} else {
					if (mostRecentPatientProgram.getDateCompleted() != null && pp.getDateCompleted() == null) {
						// found an uncompleted one
						mostRecentPatientProgram = pp;
					} else if (mostRecentPatientProgram.getDateCompleted() != null && pp.getDateCompleted() != null
					        && pp.getDateCompleted().after(mostRecentPatientProgram.getDateCompleted())) {
						// pp was completed later
						// maybe the start date is also important
						mostRecentPatientProgram = pp;
					} else {
						// let the states decide for uncompleted programs
						// (_should_ not be necessary, but that's life,
						// partially due
						// to ProgramLocation module, or Reopening of old
						// programs, or patient merge)
						PatientState mostRecentState = mostRecentPatientProgram.getCurrentState(workflow);
						PatientState ps = pp.getCurrentState(workflow);
						if (mostRecentState == null || ps == null) {
							// just do nothing
						} else if (mostRecentState.getEndDate() != null && ps.getEndDate() == null) {
							mostRecentPatientProgram = pp;
						} else if (ps.getStartDate().after(mostRecentState.getStartDate())) {
							mostRecentPatientProgram = pp;
						}
					}
				}
			}
		}
		if (mostRecentPatientProgram != null) {
			PatientState ps = mostRecentPatientProgram.getCurrentState(workflow);
			if (ps != null && ps.getState() != null && ps.getState().getConcept().getName() != null) {
				return ps;
			}
		}
		return null;
	}

 /**
     *
     * @return   patient's age given in months
     * @should  return the ageInMonths accurately to the nearest month
     */

    public Integer patientAgeInMonths() {

        Patient patient = session.getPatient();
        if(patient == null || patient.getBirthdate() == null){
          return null;     // if there is error in patient's data return age as null
        }
        Date birthdate = patient.getBirthdate();
        DateTime today = new DateTime();
        DateTime dob = new DateTime(birthdate.getTime());
        return Months.monthsBetween(dob.toDateMidnight(), today.toDateMidnight()).getMonths();
    }

 /**
     *
     * @return   patient's age in days
     * @should  return the ageInDays accurately to the nearest date
     */
    public Integer patientAgeInDays(){

        Patient patient = session.getPatient();
        if(patient == null  || patient.getBirthdate() == null){
          return null;   // if there is error in patient's data return age as null
        }
        Date birthdate = patient.getBirthdate();
        DateTime today = new DateTime();
        DateTime dob = new DateTime(birthdate.getTime());
        return Days.daysBetween(dob.toDateMidnight(), today.toDateMidnight()).getDays();
    }

}
