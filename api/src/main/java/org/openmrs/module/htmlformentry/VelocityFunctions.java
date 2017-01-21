package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.result.EmptyResult;
import org.openmrs.logic.result.Result;
import org.openmrs.module.htmlformentry.compatibility.EncounterServiceCompatibility;
import org.openmrs.util.LocaleUtility;


public class VelocityFunctions {
	
	private FormEntrySession session;
	private ObsService obsService;
	private LogicService logicService;
	private ProgramWorkflowService programWorkflowService;
	private AdministrationService administrationService;
	private LocationService locationService;
	
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

	private AdministrationService getAdministrationService() {
		if (administrationService == null)
			administrationService = Context.getAdministrationService();
		return administrationService;
	}
	
	private LocationService getLocationService() {
		if (locationService == null)
			locationService = Context.getLocationService();
		return locationService;
	}
	
	private void cannotBePreviewed() {
		if ("testing-html-form-entry".equals(session.getPatient().getUuid()))
			throw new CannotBePreviewedException();
    }
	
	/**
	 *
	 * @param locationIdentifier
	 * @return the location with the specified locationId, uuid or name.
	 */
	public Location location(String locationIdentifier) {
		return HtmlFormEntryUtil.getLocation(locationIdentifier);
    }

	public List<Obs> allObs(String conceptId) {

        if (session.getPatient() == null) {
			return new ArrayList<Obs>();
        }

        cannotBePreviewed();

        Patient p = session.getPatient();
		Concept concept = HtmlFormEntryUtil.getConcept(conceptId);

        if (p == null || concept == null) {
			return new ArrayList<Obs>();
        }
        else {
			return getObsService().getObservationsByPersonAndConcept(p, concept);
        }
	}

    public List<Obs> allObs(Integer conceptId) {
        return allObs(conceptId.toString());
    }


    /**
	 * @return the most recent obs given the passed conceptId
	 * @should return the most recent obs given the passed conceptId
	 */
	public Obs latestObs(String conceptId) {

        List<Obs> obs = allObs(conceptId);

        if (obs == null || obs.isEmpty()) {
			return null;
        }
		else {
			return obs.get(0);
        }

	}

    public Obs latestObs(Integer conceptId) {
        return latestObs(conceptId.toString());
    }

	/**
	 * @return the first obs given the passed conceptId
	 * @should return the first obs given the passed conceptId
	 */
	public Obs earliestObs(String conceptId) {

        List<Obs> obs = allObs(conceptId);

        if (obs == null || obs.isEmpty()) {
				return null;
        }
		else {
			return obs.get(obs.size() - 1);
        }

	}

    public Obs earliestObs(Integer conceptId) {
        return earliestObs(conceptId.toString());
    }

	/**
	 * @return the all the encounters of the specified type
	 * @should return all the encounters of the specified type
	 * @should return all encounters if no type specified
	 */
    public List<Encounter> allEncounters(String encounterTypeId){
		EncounterType encounterType = HtmlFormEntryUtil.getEncounterType(encounterTypeId);
		return getAllEncounters(encounterType);
    }
    
	private List<Encounter> getAllEncounters(EncounterType type) {
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
				
				EncounterServiceCompatibility esc = Context.getRegisteredComponent("htmlformentry.EncounterServiceCompatibility", EncounterServiceCompatibility.class);
				return esc.getEncounters(p, null, null, null, null, typeList, null, null, null, false);
			}
		}
	}
	
	/**
	 * @return the most recent encounter of the specified type
	 * @should return the most recent encounter of the specified type
	 * @should return the most recent encounter of any type if no type specified
	 */
	public Encounter latestEncounter(String encounterTypeId){
		EncounterType encounterType = null;
		if (StringUtils.isNotEmpty(encounterTypeId)) {
			encounterType = HtmlFormEntryUtil.getEncounterType(encounterTypeId);			
		}
		
		return getLatestEncounter(encounterType);
	}
	
	private Encounter getLatestEncounter(EncounterType type) {
		List<Encounter> encounters = getAllEncounters(type);
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
		return getLogicService().eval(session.getPatient().getPatientId(), lc);
	}
	
	public ProgramWorkflow getWorkflow(Integer id) {
		for (Program p : Context.getProgramWorkflowService().getAllPrograms()) {
			for (ProgramWorkflow w : p.getAllWorkflows()) {
				if (w.getProgramWorkflowId().equals(id)) {
					return w;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
    public PatientState currentProgramWorkflowStatus(Integer programWorkflowId) {
		Patient p = session.getPatient();
		if (p == null || p.getId() == null) {
			return null;
		}
		cannotBePreviewed();
		ProgramWorkflow workflow = getWorkflow(programWorkflowId); // not sure if and how I want to reference the UUID
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
						// (_should_ not be necessary, but partially due
						// to the ProgramLocation module, and Reopening of old
						// programs, and patient merges, it is here)
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

    /**
    * 
    * @return	concept of given id
    * @should	return Concept object against given concept code (id, uuid or mapping)
    */
    public Concept getConcept(String conceptCode) {
        return HtmlFormEntryUtil.getConcept(conceptCode);
    }

    /**
     * @param specification
     * @return a java Locale object for the given String specification
     */
    public Locale locale(String specification) {
        return LocaleUtility.fromSpecification(specification);
    }

    /**
     * @param date
     * @return date with any time component smaller than day set to zero
     */
    public Date startOfDay(Date date) {
        LocalDate day = new LocalDate(date.getTime());
        return day.toDate();
    }

    /**
     * Translates a message code based on current locale
     *
     * @param code
     * @return localzied message for the code
     */
    public String message(String code) {
        return session.getContext().getTranslator().translate(Context.getLocale().toString(), code);
    }

	/**
	 * Retrieves a global property value by name
	 * @param propertyName
	 * @return the value of the global property with the given name, or an empty string if not configured
	 */
	public String globalProperty(String propertyName) {
		return getAdministrationService().getGlobalProperty(propertyName, "");
	}

	/**
	 * Retrieves a global property value by name
	 * @param propertyName
	 * @param defaultValue
	 * @return the value of the global property with the given name, or the default value if not configured
	 */
	public String globalProperty(String propertyName, String defaultValue) {
		return getAdministrationService().getGlobalProperty(propertyName, defaultValue);
	}

    /**
     * Returns an arbitrary obs if there are multiple matches
     * @param encounter
     * @param conceptCode
     * @return an obs in encounter for the given concept, or null if none exists
     */
    public Obs getObs(Encounter encounter, String conceptCode) {
        if (encounter == null) {
            return null;
        }
        Concept concept = HtmlFormEntryUtil.getConcept(conceptCode);
        if (concept == null) {
            throw new IllegalArgumentException("No concept found for: " + conceptCode);
        }
        for (Obs candidate : encounter.getAllObs()) {
            if (candidate.getConcept().equals(concept)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * @param encounter
     * @param conceptCode
     * @return all obs in the encounter with the given concept
     */
    public List<Obs> allObs(Encounter encounter, String conceptCode) {
        if (encounter == null) {
            return null;
        }
        Concept concept = HtmlFormEntryUtil.getConcept(conceptCode);
        if (concept == null) {
            throw new IllegalArgumentException("No concept found for: " + conceptCode);
        }
        List<Obs> matches = new ArrayList<Obs>();
        for (Obs candidate : encounter.getAllObs()) {
            if (candidate.getConcept().equals(concept)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}
