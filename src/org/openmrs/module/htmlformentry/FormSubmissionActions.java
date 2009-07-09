package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

/**
 * When you try to submit a form, this class is used to hold all the actions that will eventually
 * be committed to the database in a unit.
 * 
 * This class stores state about where in the form we are (e.g. Person->Encounter->ObsGroup so that
 * an <obs/> element just has to call createObs() on this class, and it will automatically be put in
 * the correct encounter or obs group.
 *  
 * This class is not responsible for applying its own actions. That is done elsewhere in the framework.
 */
public class FormSubmissionActions {
    
    protected final Log log = LogFactory.getLog(getClass());
    
    private List<Person> personsToCreate = new Vector<Person>();
    private List<Encounter> encountersToCreate = new Vector<Encounter>();
    private List<Encounter> encountersToEdit = new Vector<Encounter>();
    private List<Obs> obsToCreate = new Vector<Obs>();
    private List<Obs> obsToVoid = new Vector<Obs>();
    private List<Order> ordersToCreate = new Vector<Order>();
    
    Stack<Object> stack = new Stack<Object>(); // a snapshot might look something like { Patient, Encounter, ObsGroup }
    
    public FormSubmissionActions() { }

    public void beginPerson(Person person) throws InvalidActionException {
        // person has to be at the top of the stack
        if (stack.size() > 0)
            throw new InvalidActionException("Person can only go on the top of the stack");
        if (person.getPersonId() == null && !personsToCreate.contains(person))
            personsToCreate.add(person);
        stack.push(person);
    }
    
    public void endPerson() throws InvalidActionException {
        if (!stackContains(Person.class))
            throw new InvalidActionException("No Person on the stack");
        while (true) {
            Object o = stack.pop();
            if (o instanceof Person)
                break;
        }
    }
    
    public void beginEncounter(Encounter encounter) throws InvalidActionException {
        // there needs to be a Person on the stack before this
        if (!stackContains(Person.class))
            throw new InvalidActionException("No Person on the stack");
        if (encounter.getEncounterId() == null && !encountersToCreate.contains(encounter))
            encountersToCreate.add(encounter);
        encounter.setPatient(highestOnStack(Patient.class));
        stack.push(encounter);
    }
    
    public void endEncounter() throws InvalidActionException {
        if (!stackContains(Encounter.class))
            throw new InvalidActionException("No Encounter on the stack");
        while (true) {
            Object o = stack.pop();
            if (o instanceof Encounter)
                break;
        }
    }
    
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
        stack.push(group);
    }
    
    private void addObsToEncounterIfNotAlreadyThere(Encounter encounter, Obs group) {
        for (Obs obs : encounter.getObsAtTopLevel(true)) {
            if (obs.equals(group))
                return;
        }
        encounter.addObs(group);
    }

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
    
    public Person getCurrentPerson() {
        return highestOnStack(Person.class);
    }
    
    public Encounter getCurrentEncounter() {
        return highestOnStack(Encounter.class);
    }
        
    private <T> T highestOnStack(Class<T> clazz) {
        for (ListIterator<Object> iter = stack.listIterator(stack.size()); iter.hasPrevious(); ) {
            Object o = iter.previous();
            if (clazz.isAssignableFrom(o.getClass()))
                return (T) o;
        }
        return null;
    }

    private boolean stackContains(Class<?> clazz) {
        for (Object o : stack) {
            if (clazz.isAssignableFrom(o.getClass()))
                return true;
        }
        return false;
    }
        
    public Obs createObs(Concept concept, Object value, Date datetime) {
        if (value == null || "".equals(value))
            throw new IllegalArgumentException("Cannot create Obs with null or blank value");
        
        Obs obs = HtmlFormEntryUtil.createObs(concept, value, datetime);

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
    
    public void modifyObs(Obs existingObs, Concept concept, Object newValue, Date newDatetime) {
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
        Obs newObs = HtmlFormEntryUtil.createObs(concept, newValue, newDatetime);
        String oldString = existingObs.getValueAsString(Context.getLocale());
        String newString = newObs.getValueAsString(Context.getLocale());
        if (log.isDebugEnabled()) {
            log.debug("For concept " + concept.getBestName(Context.getLocale()) + ": " + oldString + " -> " + newString);
        }
        boolean valueChanged = !newString.equals(oldString);
        // TODO: handle dates that may equal encounter date
        boolean dateChanged = dateChangedHelper(existingObs.getObsDatetime(), newObs.getObsDatetime());
        if (valueChanged || dateChanged) {
            if (log.isDebugEnabled()) {
                log.debug("CHANGED: " + printObsHelper(existingObs));
            }
            // TODO: really the voided obs should link to the new one, but this is a pain to implement due to the dreaded error: org.hibernate.NonUniqueObjectException: a different object with the same identifier value was already associated with the session
            obsToVoid.add(existingObs);
            createObs(concept, newValue, newDatetime);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("SAME: " + printObsHelper(existingObs));
            }
        }
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

    private String printObsHelper(Obs obs) {
        // TODO Auto-generated method stub
        return obs.getConcept().getBestName(Context.getLocale()) + " = " + obs.getValueAsString(Context.getLocale());
    }

    public List<Person> getPersonsToCreate() {
        return personsToCreate;
    }

    public void setPersonsToCreate(List<Person> personsToCreate) {
        this.personsToCreate = personsToCreate;
    }

    public List<Encounter> getEncountersToCreate() {
        return encountersToCreate;
    }

    public void setEncountersToCreate(List<Encounter> encountersToCreate) {
        this.encountersToCreate = encountersToCreate;
    }

    public List<Encounter> getEncountersToEdit() {
        return encountersToEdit;
    }

    public void setEncountersToEdit(List<Encounter> encountersToEdit) {
        this.encountersToEdit = encountersToEdit;
    }

    public List<Obs> getObsToCreate() {
        return obsToCreate;
    }

    public void setObsToCreate(List<Obs> obsToCreate) {
        this.obsToCreate = obsToCreate;
    }

    public List<Obs> getObsToVoid() {
        return obsToVoid;
    }

    public void setObsToVoid(List<Obs> obsToVoid) {
        this.obsToVoid = obsToVoid;
    }

    public List<Order> getOrdersToCreate() {
        return ordersToCreate;
    }

    public void setOrdersToCreate(List<Order> ordersToCreate) {
        this.ordersToCreate = ordersToCreate;
    }

}
