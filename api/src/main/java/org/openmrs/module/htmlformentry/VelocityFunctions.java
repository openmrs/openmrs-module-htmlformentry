package org.openmrs.module.htmlformentry;

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
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.util.LocaleUtility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VelocityFunctions {
	
	private FormEntrySession session;
	
	private ObsService obsService;
	
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
	
	private Date parseDate(String dateString) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		if (StringUtils.isNotEmpty(dateString)) {
			return df.parse(dateString);
		}
		return null;
	}
	
	/**
	 * @param locationIdentifier
	 * @return the location with the specified locationId, uuid or name.
	 */
	public Location location(String locationIdentifier) {
		return HtmlFormEntryUtil.getLocation(locationIdentifier);
	}
	
	public List<Obs> allObs(String conceptId, Date latestDate) {
		
		if (session.getPatient() == null) {
			return new ArrayList<Obs>();
		}
		
		cannotBePreviewed();
		
		Patient p = session.getPatient();
		Concept concept = HtmlFormEntryUtil.getConcept(conceptId);
		
		if (p == null || concept == null) {
			return new ArrayList<Obs>();
		} else {
			List<Person> who = new ArrayList<Person>();
			who.add(p.getPerson());
			
			List<Concept> questions = new ArrayList<Concept>();
			questions.add(concept);
			return getObsService().getObservations(who, (List) null, questions, (List) null, (List) null, (List) null,
			    (List) null, (Integer) null, (Integer) null, (Date) null, latestDate, false);
		}
	}
	
	public List<Obs> allObs(String conceptId) {
		return allObs(conceptId, null);
	}
	
	public List<Obs> allObs(Integer conceptId) {
		return allObs(conceptId.toString(), null);
	}
	
	/**
	 * @return the most recent obs given the passed conceptId <strong>Should</strong> return the most
	 *         recent obs given the passed conceptId
	 */
	public Obs latestObs(String conceptId) {
		return latestObs(conceptId, null);
	}
	
	/**
	 * @return the most recent obs given the passed conceptId on or before the specified
	 *         date<strong>Should</strong> return the most recent obs given the passed conceptId
	 */
	public Obs latestObs(Integer conceptId, String latestDateString) throws ParseException {
		return latestObs(conceptId.toString(), parseDate(latestDateString));
	}
	
	public Obs latestObs(Integer conceptId, Date latestDate) {
		return latestObs(conceptId.toString(), latestDate);
	}
	
	public Obs latestObs(String conceptId, Date latestDate) {
		List<Obs> obs = allObs(conceptId, latestDate);
		
		if (obs == null || obs.isEmpty()) {
			return null;
		} else {
			return obs.get(0);
		}
	}
	
	public Obs latestObs(Integer conceptId) {
		return latestObs(conceptId.toString(), null);
	}
	
	/**
	 * @return the first obs given the passed conceptId <strong>Should</strong> return the first obs
	 *         given the passed conceptId
	 */
	public Obs earliestObs(String conceptId) {
		List<Obs> obs = allObs(conceptId, null);
		
		if (obs == null || obs.isEmpty()) {
			return null;
		} else {
			return obs.get(obs.size() - 1);
		}
		
	}
	
	public Obs earliestObs(Integer conceptId) {
		return earliestObs(conceptId.toString());
	}
	
	/**
	 * @return the all the encounters of the specified type <strong>Should</strong> return all the
	 *         encounters of the specified type <strong>Should</strong> return all encounters if no type
	 *         specified
	 */
	public List<Encounter> allEncounters(String encounterTypeId) {
		EncounterType encounterType = HtmlFormEntryUtil.getEncounterType(encounterTypeId);
		return getAllEncounters(encounterType);
	}
	
	private List<Encounter> getAllEncounters(EncounterType type) {
		return getAllEncounters(type, null);
	}
	
	private List<Encounter> getAllEncounters(EncounterType type, Date latestDate) {
		if (session.getPatient() == null) {
			return new ArrayList<Encounter>();
		}
		cannotBePreviewed();
		Patient p = session.getPatient();
		if (p == null) {
			return new ArrayList<Encounter>();
		} else {
			EncounterSearchCriteriaBuilder b = new EncounterSearchCriteriaBuilder();
			b.setPatient(p).setIncludeVoided(false);
			
			if (type != null) {
				List<EncounterType> typeList = new ArrayList<EncounterType>();
				typeList.add(type);
				b.setEncounterTypes(typeList);
			}
			
			if (latestDate != null) {
				b.setToDate(latestDate);
			}
			
			List<Encounter> encounters = Context.getEncounterService().getEncounters(b.createEncounterSearchCriteria());
			return encounters;
		}
	}
	
	/**
	 * @return the most recent encounter of the specified type <strong>Should</strong> return the most
	 *         recent encounter of the specified type <strong>Should</strong> return the most recent
	 *         encounter of any type if no type specified
	 */
	public Encounter latestEncounter(String encounterTypeId) {
		EncounterType encounterType = null;
		if (StringUtils.isNotEmpty(encounterTypeId)) {
			encounterType = HtmlFormEntryUtil.getEncounterType(encounterTypeId);
		}
		
		return getLatestEncounter(encounterType, null);
	}
	
	/**
	 * @return the most recent encounter before or on the specified date <strong>Should</strong> return
	 *         the most recent encounter of the specified type <strong>Should</strong> return the most
	 *         recent encounter of any type if no type specified
	 */
	public Encounter latestEncounterAtDate(String latestDateString) throws ParseException {
		return getLatestEncounter(null, parseDate(latestDateString));
	}
	
	public Encounter latestEncounterAtDate(Date latestDate) {
		return getLatestEncounter(null, latestDate);
	}
	
	/**
	 * @return the most recent encounter of the specified type before or up to the specified date
	 *         <strong>Should</strong> return the most recent encounter of the specified type
	 *         <strong>Should</strong> return the most recent encounter of any type if no type specified
	 */
	public Encounter latestEncounter(Integer encounterTypeId, Date latestDate) {
		return latestEncounter(encounterTypeId.toString(), latestDate);
	}
	
	public Encounter latestEncounter(String encounterTypeId, String latestDateString) throws ParseException {
		return latestEncounter(encounterTypeId, parseDate(latestDateString));
	}
	
	public Encounter latestEncounter(String encounterTypeId, Date latestDateString) {
		EncounterType encounterType = null;
		if (StringUtils.isNotEmpty(encounterTypeId)) {
			encounterType = HtmlFormEntryUtil.getEncounterType(encounterTypeId);
		}
		
		return getLatestEncounter(encounterType, latestDateString);
	}
	
	private Encounter getLatestEncounter(EncounterType type, Date latestDate) {
		List<Encounter> encounters = getAllEncounters(type, latestDate);
		if (encounters == null || encounters.isEmpty()) {
			return null;
		} else {
			return encounters.get(encounters.size() - 1);
		}
	}
	
	/**
	 * @return the most recent encounter <strong>Should</strong> return the most recent encounter
	 */
	public Encounter latestEncounter() {
		return latestEncounter((String) null, (Date) null);
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
		List<PatientProgram> pps = getProgramWorkflowService().getPatientPrograms(p, workflow.getProgram(), null, null, null,
		    null, false);
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
	
	@SuppressWarnings("deprecation")
	public PatientState currentProgramWorkflowStatus(Integer programWorkflowId, String latestDateString)
	        throws ParseException {
		return currentProgramWorkflowStatus(programWorkflowId, parseDate(latestDateString));
	}
	
	@SuppressWarnings("deprecation")
	public PatientState currentProgramWorkflowStatus(Integer programWorkflowId, Date latestDate) {
		if (latestDate == null) {
			// the date format is invalid or the string is empty so just call the original method
			return currentProgramWorkflowStatus(programWorkflowId);
		}
		Patient p = session.getPatient();
		if (p == null || p.getId() == null) {
			return null;
		}
		cannotBePreviewed();
		ProgramWorkflow workflow = getWorkflow(programWorkflowId); // not sure if and how I want to reference the UUID
		List<PatientProgram> pps = getProgramWorkflowService().getPatientPrograms(p, workflow.getProgram(), null, null, null,
		    null, false);
		PatientProgram mostRecentPatientProgram = null;
		for (PatientProgram pp : pps) {
			// try to figure out which program enrollment is active or the most
			// recent one; guess this would better fit somewhere in the
			// ProgramWorkflowServive
			if (!pp.isVoided()) {
				if (pp.getDateCompleted() == null) {
					mostRecentPatientProgram = pp;
				} else {
					if (mostRecentPatientProgram != null
					        && pp.getDateCompleted().after(mostRecentPatientProgram.getDateCompleted())) {
						// pp was completed after the most recent date
						// maybe the start date is also important
						mostRecentPatientProgram = pp;
					}
				}
			}
		}
		if (mostRecentPatientProgram != null) {
			// find the active state on the specified date
			for (PatientState state : mostRecentPatientProgram.getStates()) {
				if (state.getActive(latestDate)) {
					return state;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return patient's age given in months <strong>Should</strong> return the ageInMonths accurately
	 *         to the nearest month
	 */
	
	public Integer patientAgeInMonths() {
		
		Patient patient = session.getPatient();
		if (patient == null || patient.getBirthdate() == null) {
			return null; // if there is error in patient's data return age as null
		}
		Date birthdate = patient.getBirthdate();
		DateTime today = new DateTime();
		DateTime dob = new DateTime(birthdate.getTime());
		return Months.monthsBetween(dob.toDateMidnight(), today.toDateMidnight()).getMonths();
	}
	
	/**
	 * @return patient's age in days <strong>Should</strong> return the ageInDays accurately to the
	 *         nearest date
	 */
	public Integer patientAgeInDays() {
		
		Patient patient = session.getPatient();
		if (patient == null || patient.getBirthdate() == null) {
			return null; // if there is error in patient's data return age as null
		}
		Date birthdate = patient.getBirthdate();
		DateTime today = new DateTime();
		DateTime dob = new DateTime(birthdate.getTime());
		return Days.daysBetween(dob.toDateMidnight(), today.toDateMidnight()).getDays();
	}
	
	/**
	 * @return concept of given id <strong>Should</strong> return Concept object against given concept
	 *         code (id, uuid or mapping)
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
	 * 
	 * @param propertyName
	 * @return the value of the global property with the given name, or an empty string if not
	 *         configured
	 */
	public String globalProperty(String propertyName) {
		return getAdministrationService().getGlobalProperty(propertyName, "");
	}
	
	/**
	 * Retrieves a global property value by name
	 * 
	 * @param propertyName
	 * @param defaultValue
	 * @return the value of the global property with the given name, or the default value if not
	 *         configured
	 */
	public String globalProperty(String propertyName, String defaultValue) {
		return getAdministrationService().getGlobalProperty(propertyName, defaultValue);
	}
	
	/**
	 * Returns an arbitrary obs if there are multiple matches
	 * 
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
