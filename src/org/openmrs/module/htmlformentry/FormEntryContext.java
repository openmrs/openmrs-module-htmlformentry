package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;

/**
 * This class holds the context data around generating html widgets from tags like <obs .../> in an HtmlForm.
 * 
 * It allows you to register widgets, which assigns them an id/name in the generated html, and allows you to
 * look up those id/names later. It allows you specify which error widget goes for which widget, so that error
 * messages get displayed in the right place.
 * 
 * It also holds existing data for an encounter in View mode, so that widgets can be set with
 * their appropriate values.
 * 
 * TODO rename this class: it's really more of the html generation context than a form entry context
 * TODO move Mode class up to FormEntrySession instead?
 */
public class FormEntryContext {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    private Mode mode;
    private Map<Widget, String> fieldNames = new HashMap<Widget, String>();
    private Map<Widget, ErrorWidget> errorWidgets = new HashMap<Widget, ErrorWidget>();
    private Translator translator = new Translator();
    private HtmlFormSchema schema = new HtmlFormSchema();
    private ObsGroup activeObsGroup;
    
    public FormEntryContext(Mode mode) {
        this.mode = mode;
        setupExistingData(null);
    }
    
    public Mode getMode() {
        return mode;
    }
    
    private Integer sequenceNextVal = 1;
    
    public String registerWidget(Widget widget) {
        if (fieldNames.containsKey(widget))
            throw new IllegalArgumentException("This widget is already registered");
        int thisVal = 0;
        synchronized (sequenceNextVal) {
            thisVal = sequenceNextVal;
            sequenceNextVal = sequenceNextVal + 1;            
        }
        String fieldName = "w" + thisVal;
        fieldNames.put(widget, fieldName);
        log.trace("Registered widget " + widget.getClass() + " as " + fieldName);
        return fieldName;
    }
    
    public String registerErrorWidget(Widget widget, ErrorWidget errorWidget) {
        String errorWidgetId;
        if (!fieldNames.containsKey(errorWidget)) {
            errorWidgetId = registerWidget(errorWidget);
        } else {
            errorWidgetId = getFieldName(errorWidget);
        }
        errorWidgets.put(widget, errorWidget);
        
        return errorWidgetId;
    }

    public String getFieldName(Widget widget) {
        String fieldName = fieldNames.get(widget);
        if (fieldName == null)
            throw new IllegalArgumentException("Widget not registered");
        else
            return fieldName;
    }
    
    public String getErrorFieldId(Widget widget) {
        return getFieldName(errorWidgets.get(widget));
    }
    
    public Collection<String> getErrorDivIds() {
        Set<String> ret = new HashSet<String>();
        for (ErrorWidget e : errorWidgets.values())
            ret.add(getFieldName(e));
        return ret;
    }
    
    Stack<Concept> currentObsGroupConcepts = new Stack<Concept>();
    List<Obs> currentObsGroupMembers;
    
    public void beginObsGroup(Concept conceptSet) {
        currentObsGroupConcepts.push(conceptSet);
        activeObsGroup = new ObsGroup(conceptSet);
    }
    
    public ObsGroup getActiveObsGroup() {
    	return activeObsGroup;
    }
    
    public void setObsGroup(Obs group) {
        if (group == null) {
            currentObsGroupMembers = null;
        } else {
            currentObsGroupMembers = new ArrayList<Obs>();
            for (Obs o : group.getGroupMembers())
                currentObsGroupMembers.add(o);
        }
    }
    
    public void endObsGroup() {
        currentObsGroupMembers = null;
        getSchema().addField(activeObsGroup);
        activeObsGroup = null;
        if (currentObsGroupConcepts.pop() == null)
            throw new RuntimeException("Trying to close an obs group where none is open");
    }
    
    public List<Concept> getCurrentObsGroupConcepts() {
        return Collections.unmodifiableList(currentObsGroupConcepts);
    }
        
    /**
     * Also removes the thing that is returned.
     */
    public Obs getObsFromCurrentGroup(Concept concept, Concept answerConcept) {
        if (currentObsGroupMembers == null)
            return null;
        for (Iterator<Obs> iter = currentObsGroupMembers.iterator(); iter.hasNext(); ) {
            Obs obs = iter.next();
            if (!obs.isVoided() &&
                    concept.getConceptId().equals(obs.getConcept().getConceptId()) &&
                    (answerConcept == null || answerConcept.equals(obs.getValueCoded()))) {
                iter.remove();
                return obs;
            }
        }
        return null;
    }
    
    Encounter existingEncounter;
    Map<Concept, List<Obs>> existingObs;
    List<Obs> existingObsInGroups;
    
    public void setupExistingData(Encounter encounter) {
        existingEncounter = encounter;
        existingObs = new HashMap<Concept, List<Obs>>();
        if (encounter != null) {
            for (Obs obs : encounter.getObsAtTopLevel(false)) {
                List<Obs> list = existingObs.get(obs.getConcept());
                if (list == null) {
                    list = new LinkedList<Obs>();
                    existingObs.put(obs.getConcept(), list);
                }
                list.add(obs);
            }
        }
        existingObsInGroups = new ArrayList<Obs>();
        if (encounter != null) {
            for (Obs parent : encounter.getObsAtTopLevel(false)) {
                if (parent.hasGroupMembers()) {
                    // TODO handle groups of groups
                    existingObsInGroups.add(parent);
                }
            }
        }
    }
    
    /**
     * Removes an Obs or ObsGroup of the relevant Concept from existingObs, and returns it. Use this version
     * for obs whose concept's datatype is not boolean.
     * 
     * @param question
     * @return
     */
    public Obs removeExistingObs(Concept question, Concept answer) {
        List<Obs> list = existingObs.get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (answer == null || OpenmrsUtil.nullSafeEquals(answer, test.getValueCoded())) {
                    iter.remove();
                    if (list.size() == 0)
                        existingObs.remove(question);
                    return test;
                }
            }
        }
        return null;
    }
    
    /**
     * Find obs with the given concept whose answer is equal to the given answer, returns one, and removes
     * it from the list.
     * Use this version for obs whose concept's datatype is boolean that are checkbox-style.
     * 
     * @param concept
     * @param parseBoolean
     * @return
     */
    public Obs removeExistingObs(Concept question, boolean answer) {
        List<Obs> list = existingObs.get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (answer == test.getValueAsBoolean()) {
                    iter.remove();
                    if (list.size() == 0)
                        existingObs.remove(question);
                    return test;
                }
            }
        }
        return null;
    }

    
    /**
     * Returns, and removes the first obs group that matches, i.e. all Obs in the group can
     * also belong to questionsAndAnswers.
     * 
     * For example you might have a group capable of holding (per questionsAndAnswers):
     *      Symptom Absent = Cough
     *      Symptom Present = Cough
     *      Symptom Duration = (null)
     * and if we have an obs group containing a subset of those
     *      Symptom Present = Cough
     *      Symptom Duration = 5
     * then we would return it.
     * 
     * @param requiredQuestionsAndAnswers
     * @return
     */
    public Obs findFirstMatchingObsGroup(List<ObsGroupComponent> questionsAndAnswers) {
        for (Iterator<Obs> iter = existingObsInGroups.iterator(); iter.hasNext(); ) {
            Obs group = iter.next();
            if (ObsGroupComponent.supports(questionsAndAnswers, group)) {
                iter.remove();
                return group;
            }
        }
        return null;
    }
        
    public Encounter getExistingEncounter() {
        return existingEncounter;
    }

    public Translator getTranslator() {
    	return translator;
    }
    
    public HtmlFormSchema getSchema() {
    	return schema;
    }
    
    public enum Mode {
        ENTER,
        EDIT,
        VIEW
    }
    
}
