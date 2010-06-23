package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

/**
 * When you try to submit a form, this class is used to hold all the actions that will eventually
 * be committed to the database in a unit.
 * 
 * This class stores state about where in the form we are (e.g. Person->Encounter->ObsGroup) so that
 * an <obs/> element just has to call createObs() on this class, and it will automatically be put in
 * the correct encounter or obs group.
 *  
 * This class is not responsible for applying its own actions. That is done elsewhere in the framework.
 */
public class FormSubmissionActions {
	
	/** Logger to use with this class */
    protected final Log log = LogFactory.getLog(getClass());
    
    private List<Person> personsToCreate = new Vector<Person>();
    private List<Encounter> encountersToCreate = new Vector<Encounter>();
    private List<Encounter> encountersToEdit = new Vector<Encounter>();
    private List<Obs> obsToCreate = new Vector<Obs>();
    private List<Obs> obsToVoid = new Vector<Obs>();
    private List<Order> ordersToCreate = new Vector<Order>();
    private List<PatientProgram> patientProgramsToCreate = new Vector<PatientProgram>();
    
    /** The stack where state is stored */
    private Stack<Object> stack = new Stack<Object>(); // a snapshot might look something like { Patient, Encounter, ObsGroup }
    
    public FormSubmissionActions() { }

    /**
     * Add a Person to the submission stack. A Person must be the first object added to the submission stack.
     *
     * @param Person person to add
     * @throws InvalidActionException
     */
    public void beginPerson(Person person) throws InvalidActionException {
        // person has to be at the top of the stack
        if (stack.size() > 0)
            throw new InvalidActionException("Person can only go on the top of the stack");
        if (person.getPersonId() == null && !personsToCreate.contains(person))
            personsToCreate.add(person);
        stack.push(person);
    }
    
    /**
     * Removes the most recently added Person from the submission stack. All other objects added after that Person are removed as well.
     * <p/>
     * (So, in the current one-person-per-form model, this would empty the entire submission stack)
     * 
     * @throws InvalidActionException
     */
    public void endPerson() throws InvalidActionException {
        if (!stackContains(Person.class))
            throw new InvalidActionException("No Person on the stack");
        while (true) {
            Object o = stack.pop();
            if (o instanceof Person)
                break;
        }
    }
    
    /**
     * Adds an Encounter to the submission stack
     * 
     * @param encounter the Encounter to add
     * @throws InvalidActionException
     */
    public void beginEncounter(Encounter encounter) throws InvalidActionException {
        // there needs to be a Person on the stack before this
        if (!stackContains(Person.class))
            throw new InvalidActionException("No Person on the stack");
        if (encounter.getEncounterId() == null && !encountersToCreate.contains(encounter))
            encountersToCreate.add(encounter);
        encounter.setPatient(highestOnStack(Patient.class));
        stack.push(encounter);
    }
    
    /**
     * Removes the most recently added Encounter from the submission stack.  All objects added after that Encounter are removed as well.
     * 
     * @throws InvalidActionException
     */
    public void endEncounter() throws InvalidActionException {
        if (!stackContains(Encounter.class))
            throw new InvalidActionException("No Encounter on the stack");
        while (true) {
            Object o = stack.pop();
            if (o instanceof Encounter)
                break;
        }
    }
    
    /**
     * Adds an Obs Group to the submission stack
     * 
     * @param group the Obs Group to add
     * @throws InvalidActionException
     */
    public void beginObsGroup(Obs group) throws InvalidActionException {
        // there needs to be a Person on the stack before this
        if (!stackContains(Person.class))
            throw new InvalidActionException("No Person on the stack");
        if (group.getObsId() == null && !obsToCreate.contains(group))
            obsToCreate.add(group);
        
        Person person = highestOnStack(Person.class);
        Encounter encounter = highestOnStack(Encounter.class);
        group.setPerson(person);
        if (encounter != null) {
            addObsToEncounterIfNotAlreadyThere(encounter, group);
        }
        //this is for obs groups within obs groups
        Object o = stack.peek();
        if (o instanceof Obs){
            Obs oParent = (Obs) o;
            group.setObsGroup(oParent);
        }
        stack.push(group);

    }
    
    /**
     * Utility function that adds a set of Obs to an Encounter, skipping Obs that are already part of the Encounter
     *  
     * @param encounter
     * @param group
     */
    private void addObsToEncounterIfNotAlreadyThere(Encounter encounter, Obs group) {
        for (Obs obs : encounter.getObsAtTopLevel(true)) {
            if (obs.equals(group))
                return;
        }
        encounter.addObs(group);
    }

    /**
     * Removes the most recently added ObsGroup from the submission stack.  All objects added after that ObsGroup are removed as well.
     *  
     * @throws InvalidActionException
     */
    public void endObsGroup() throws InvalidActionException {
     // there needs to be an Obs on the stack before this
        if (!stackContains(Obs.class))
            throw new InvalidActionException("No Obs on the stack");
        while (true) {
            Object o = stack.pop();
            if (o instanceof Obs)
                break;
        }
    }
    
    /**
     * Returns the Person that was most recently added to the stack
     * 
     * @return the Person most recently added to the stack
     */
    public Person getCurrentPerson() {
        return highestOnStack(Person.class);
    }
    
    /**
     * Returns the Encounter that was most recently added to the stack
     * 
     * @return the Encounter most recently added to the stack
     */
    public Encounter getCurrentEncounter() {
        return highestOnStack(Encounter.class);
    }
    
    /**
     * Utility method that returns the object of a specified class that was most recently added to the stack
     */
    private <T> T highestOnStack(Class<T> clazz) {
        for (ListIterator<Object> iter = stack.listIterator(stack.size()); iter.hasPrevious(); ) {
            Object o = iter.previous();
            if (clazz.isAssignableFrom(o.getClass()))
                return (T) o;
        }
        return null;
    }

    /**
     * Utility method that tests whether there is an object of the specified type on the stack
     */
    private boolean stackContains(Class<?> clazz) {
        for (Object o : stack) {
            if (clazz.isAssignableFrom(o.getClass()))
                return true;
        }
        return false;
    }
       
    /**
     * Creates an new Obs and associates with the most recently added Person, Encounter, and ObsGroup (if applicable) on the stack.
     * <p/>
     * Note that this method does not actually commit the Obs to the database, but instead adds the Obs to a list of Obs to be added.
     * The changes are applied elsewhere in the framework.
     * 
     * @param concept concept associated with the Obs
     * @param value value for the Obs
     * @param datetime date information for the Obs
     * @param accessionNumber accession number for the Obs
     * @return the Obs to create
     */
    public Obs createObs(Concept concept, Object value, Date datetime, String accessionNumber) {
        if (value == null || "".equals(value))
            throw new IllegalArgumentException("Cannot create Obs with null or blank value");
        
        Obs obs = HtmlFormEntryUtil.createObs(concept, value, datetime, accessionNumber);

        Person person = highestOnStack(Person.class);
        if (person == null)
            throw new IllegalArgumentException("Cannot create an Obs outside of a Person.");
        Encounter encounter = highestOnStack(Encounter.class);
        Obs obsGroup = highestOnStack(Obs.class);
        
        if (person != null)
            obs.setPerson(person);
        
        if (encounter != null)
            encounter.addObs(obs);
        if (obsGroup != null) {
            obsGroup.addGroupMember(obs);
        } else {
            obsToCreate.add(obs);
        }
        return obs;
    }
    
    
    /**
     * Modifies an existing Obs.
     * <p/>
     * This method works by adding the current Obs to a list of Obs to void, and then adding the new Obs to a list of Obs to create. 
     * Note that this method does not commit the changes to the database--the changes are applied elsewhere in the framework.
     * 
     * @param existingObs the Obs to modify
     * @param concept concept associated with the Obs 
     * @param newValue the new value of the Obs
     * @param newDatetime the new date information for the Obs
     * @param accessionNumber new accession number for the Obs
     */
    public void modifyObs(Obs existingObs, Concept concept, Object newValue, Date newDatetime, String accessionNumber) {
        // if the concepts don't match then something has gone wrong, and we fail hard
        if (!existingObs.getConcept().getConceptId().equals(concept.getConceptId())) {
            throw new RuntimeException("Programming error somewhere in this module. Please report this to OpenMRS. " + existingObs.getConcept().getBestName(Context.getLocale()) + " != " + concept.getBestName(Context.getLocale()));
        }
        if (newValue == null || "".equals(newValue)) {
            // we want to delete the existing obs
            if (log.isDebugEnabled())
                log.debug("VOID: " + printObsHelper(existingObs));
            obsToVoid.add(existingObs);
            return;
        }
        Obs newObs = HtmlFormEntryUtil.createObs(concept, newValue, newDatetime, accessionNumber);
        String oldString = existingObs.getValueAsString(Context.getLocale());
        String newString = newObs.getValueAsString(Context.getLocale());
        if (log.isDebugEnabled()) {
            log.debug("For concept " + concept.getBestName(Context.getLocale()) + ": " + oldString + " -> " + newString);
        }
        boolean valueChanged = !newString.equals(oldString);
        // TODO: handle dates that may equal encounter date
        boolean dateChanged = dateChangedHelper(existingObs.getObsDatetime(), newObs.getObsDatetime());
        boolean accessionNumberChanged = accessionNumberChangedHelper(existingObs.getAccessionNumber(), newObs.getAccessionNumber());
        if (valueChanged || dateChanged || accessionNumberChanged) {
            if (log.isDebugEnabled()) {
                log.debug("CHANGED: " + printObsHelper(existingObs));
            }
            // TODO: really the voided obs should link to the new one, but this is a pain to implement due to the dreaded error: org.hibernate.NonUniqueObjectException: a different object with the same identifier value was already associated with the session
            obsToVoid.add(existingObs);
            createObs(concept, newValue, newDatetime, accessionNumber);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("SAME: " + printObsHelper(existingObs));
            }
        }
    }

    /**
     * Enrolls the Patient most recently added to the stack in the specified Program.
     * <p/>
     * Note that this method does not commit the program enrollment to the database but instead adds
     * the Program to a list of programs to add. The changes are applied elsewhere in the framework
     * 
     * @param program Program to enroll the patient in
     */
	public void enrollInProgram(Program program) {
        if (program == null)
            throw new IllegalArgumentException("Cannot enroll in a blank program");
        
        Patient patient = highestOnStack(Patient.class);
        if (patient == null)
            throw new IllegalArgumentException("Cannot enroll in a program outside of a Patient");
        Encounter encounter = highestOnStack(Encounter.class);
        if (encounter == null)
        	throw new IllegalArgumentException("Cannot enroll in a program outside of an Encounter");

        PatientProgram pp = new PatientProgram();
        pp.setPatient(patient);
        pp.setProgram(program);
        patientProgramsToCreate.add(pp);
    }
	
	/**
     * This method compares Timestamps to plain Dates by dropping the nanosecond precision
     */
    private boolean dateChangedHelper(Date oldVal, Date newVal) {
        if (newVal == null)
            return false;
        else
            return oldVal.getTime() != newVal.getTime();
    }
    
    private boolean accessionNumberChangedHelper(String oldVal, String newVal) {
        return !OpenmrsUtil.nullSafeEquals(oldVal, newVal);
    }

    private String printObsHelper(Obs obs) {
        return obs.getConcept().getBestName(Context.getLocale()) + " = " + obs.getValueAsString(Context.getLocale());
    }

    /**
     * Returns a list of all the Persons that need to be created to process form submission
     *
     * @return a list of all Persons to create
     */
    public List<Person> getPersonsToCreate() {
        return personsToCreate;
    }

    /**
     * Sets the list of Persons that need to be created to process form submission
     * 
     * @param personsToCreate the list of Persons to create
     */
    public void setPersonsToCreate(List<Person> personsToCreate) {
        this.personsToCreate = personsToCreate;
    }

    /**
     * Returns a list of all the Encounters that need to be created to process form submissions
     * 
     * @return a list of Encounters to create
     */
    public List<Encounter> getEncountersToCreate() {
        return encountersToCreate;
    }

    /**
     * Sets the list of Encounters that need to be created to process form submission
     * 
     * @param encountersToCreate the list of Encounters to create
     */
    public void setEncountersToCreate(List<Encounter> encountersToCreate) {
        this.encountersToCreate = encountersToCreate;
    }

    /**
     * Returns the list of Encounters that need to be edited to process form submission
     * 
     * @return the list of Encounters to edit
     */
    public List<Encounter> getEncountersToEdit() {
        return encountersToEdit;
    }

    /**
     * Sets the list of Encounters that need to be editing to process form submission
     * 
     * @param encountersToEdit the list of Encounters to edit
     */
    public void setEncountersToEdit(List<Encounter> encountersToEdit) {
        this.encountersToEdit = encountersToEdit;
    }
    
    /** 
     * Returns the list of Obs that need to be created to process form submission
     * 
     * @return the list of Obs to create
     */
    public List<Obs> getObsToCreate() {
        return obsToCreate;
    }
    
    /**
     * Sets the list of Obs that need to be created to process form submission
     * 
     * @param obsToCreate the list of Obs to create
     */
    public void setObsToCreate(List<Obs> obsToCreate) {
        this.obsToCreate = obsToCreate;
    }

    /**
     * Returns the list of Os that need to be voided to process form submission
     * 
     * @return the list of Obs to void
     */
    public List<Obs> getObsToVoid() {
        return obsToVoid;
    }

    /**
     * Sets the list Obs that need to be voided to process form submission
     * 
     * @param obsToVoid the list of Obs to void
     */
    public void setObsToVoid(List<Obs> obsToVoid) {
        this.obsToVoid = obsToVoid;
    }

    /**
     * Returns the list of Orders that need to be created to process form submission
     * 
     * @return the list of Orders to create
     */
    public List<Order> getOrdersToCreate() {
        return ordersToCreate;
    }

    /**
     * Sets the list of Orders that need to be created to process form submission
     * 
     * @param ordersToCreate the list of Orders to create
     */
    public void setOrdersToCreate(List<Order> ordersToCreate) {
        this.ordersToCreate = ordersToCreate;
    }
	
    /**
     * Returns the list of Patient Programs that need to be created to process form submission
     * 
     * @return the patientProgramsToCreate the list of Programs to create
     */
    public List<PatientProgram> getPatientProgramsToCreate() {
    	return patientProgramsToCreate;
    }
	
    /**
     * Sets the list of Patient Programs that need to be creatd to process form submission
     * 
     * @param patientProgramsToCreate the list of Programs to create
     */
    public void setPatientProgramsToCreate(List<PatientProgram> patientProgramsToCreate) {
    	this.patientProgramsToCreate = patientProgramsToCreate;
    }

}
