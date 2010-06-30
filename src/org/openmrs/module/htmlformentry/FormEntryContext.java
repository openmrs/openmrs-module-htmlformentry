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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.module.htmlformentry.schema.RptGroup;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;

/**
 * This class holds the context data around generating html widgets from tags in an HtmlForm.
 * <p/>
 * It allows you to register widgets, which assigns them an id/name in the generated html, and allows you to
 * look up those id/names later. It allows you specify which error widget goes for which widget, so that error
 * messages get displayed in the right place.
 * <p/>
 * It also holds existing data for an encounter in View mode, so that widgets can be set with
 * their appropriate values.
 * <p/>
 * TODO rename this class: it's really more of the html generation context than a form entry context
 * </p>
 * TODO move Mode class up to FormEntrySession instead?
 */
public class FormEntryContext {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    private Mode mode;
    private Map<Widget, String> fieldNames = new HashMap<Widget, String>();
    private Map<Widget, ErrorWidget> errorWidgets = new HashMap<Widget, ErrorWidget>();
    
    /* the list to store all newrptgroup in this form */
    private List<RptGroup> exsistingRptGroups = new ArrayList<RptGroup>();
    private Translator translator = new Translator();
    private HtmlFormSchema schema = new HtmlFormSchema();
    private ObsGroup activeObsGroup;
    
    /* for the newrepeat tag, used to see if we are inside a newrepeat tag  */
    private RptGroup activeRptGroup;
    
    /*we only need the parameter*/
    private HttpServletRequest request;

	private Patient existingPatient;
    private Encounter existingEncounter;
    private Map<Concept, List<Obs>> existingObs;
    private List<Obs> existingObsInGroups;
    
    private Stack<Concept> currentObsGroupConcepts = new Stack<Concept>();
    private List<Obs> currentObsGroupMembers;


    public FormEntryContext(Mode mode) {
        this.mode = mode;
        setupExistingData((Encounter) null);
    }
    
    /**
     * Gets the {@see Mode} associated with this Context
     * @return the {@see Mode} associatd with this Context
     */
    public Mode getMode() {
        return mode;
    }
    
	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
    
    private Integer sequenceNextVal = 1;
    
    /* the counter to realize the naming as
     * rpti_m_k for controls in a repeater
     */
    private Integer newrepeatSeqVal = 1;       //  i
    private Integer ctrlInNewrepeatSeqVal = 1; // m
    private Integer newrepeatTimesSeqVal = 1;  //k
    

	/**
     * Registers a widget within the Context
     *  
     * @param widget the widget to register
     * @return the field id used to identify this widget in the HTML Form
     */
    public String registerWidget(Widget widget) {
        if (fieldNames.containsKey(widget))
            throw new IllegalArgumentException("This widget is already registered");
        
        if(this.activeRptGroup == null){
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
        else{
        	/* we are in a new repeat tag now, 
             need to use the alternative naming system */
        	int thisRptVal = 0; 
        	int thisCtrlVal = 0;   
        	int thisRpttimeVal = 0;  
        	 
        	synchronized (newrepeatSeqVal) {
        		thisRptVal = newrepeatSeqVal;        
        	}
        	synchronized (ctrlInNewrepeatSeqVal) {
        		thisCtrlVal = ctrlInNewrepeatSeqVal;
        		ctrlInNewrepeatSeqVal = ctrlInNewrepeatSeqVal + 1;            
        	}
        	synchronized(this.newrepeatTimesSeqVal){
        		thisRpttimeVal = newrepeatTimesSeqVal;
        	}
        	/* i.e. rpt1_w1_1 stands for the first widget in the 1st repeat*/
        	String fieldName = "rpt" + thisRptVal+"_w"+thisCtrlVal+"_"+thisRpttimeVal;
        	fieldNames.put(widget, fieldName);
        	log.trace("Registered widget " + widget.getClass() + " as " + fieldName);
        	return fieldName;
        }
    }
    
    /**
     * Registers an error widget within the Context
     * 
     * @param widget the widget to associate this error widget with
     * @param errorWidget the error widget to register
     * @return the field id used to identify this widget in the HTML Form
     */
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

    /**
     * Gets the field id used to identify a specific widget within the HTML Form
     * 
     * @param widget the widget
     * @return the field id associated with the widget in the HTML Form
     */
    public String getFieldName(Widget widget) {
        String fieldName = fieldNames.get(widget);
        if (fieldName == null)
            throw new IllegalArgumentException("Widget not registered");
        else
            return fieldName;
    }
    
    /**
     * Gets the field id used to identify a specific error widget within the HTML Form
     * 
     * @param widget the widget
     * @return the field id associated with the error widget in the HTML Form
     */
    public String getErrorFieldId(Widget widget) {
        return getFieldName(errorWidgets.get(widget));
    }
    
    /**
     * Gets the fields ids for all currently registered error widgets
     * 
     * @return a set of all the field ids for all currently registered error widgets
     */
    public Collection<String> getErrorDivIds() {
        Set<String> ret = new HashSet<String>();
        for (ErrorWidget e : errorWidgets.values())
            ret.add(getFieldName(e));
        return ret;
    }
       
    /**
     * Marks the start of a new {@see ObsGroup} within current Context
     */
    public void beginObsGroup(Concept conceptSet) {
        currentObsGroupConcepts.push(conceptSet);
        activeObsGroup = new ObsGroup(conceptSet);
    }
    
    /**
     * Gets the {@see ObsGroup} that is currently active within the current Context
     * 
     * @return the currently active {@see ObsGroup}
     */
    public ObsGroup getActiveObsGroup() {
    	return activeObsGroup;
    }
    
    /**
     * Sets the active Obs group members to the Obs that are associated with the Obs passed as a parameter
     * 
     * @param group an Obs that should have group members
     */
    public void setObsGroup(Obs group) {
        if (group == null) {
            currentObsGroupMembers = null;
        } else {
            currentObsGroupMembers = new ArrayList<Obs>();
            for (Obs o : group.getGroupMembers())
                currentObsGroupMembers.add(o);
        }
    }
    
    /**
     * Closes the active {@see ObsGroup} and adds it to the Html Form Schema
     */
    public void endObsGroup() {
        currentObsGroupMembers = null;
        getSchema().addField(activeObsGroup);
        activeObsGroup = null;
        if (currentObsGroupConcepts.pop() == null)
            throw new RuntimeException("Trying to close an obs group where none is open");
    }
    
    /**
     * Returns the concepts associated with the active {@see ObsGroup}
     * 
     * @return a list of the concepts associated with the active {@see ObsGroup}
     */
    public List<Concept> getCurrentObsGroupConcepts() {
        return Collections.unmodifiableList(currentObsGroupConcepts);
    }
        
    /**
     * Returns (and removes) the Obs from the current {@see ObsGroup} with the specified concept and answer concept
     * 
     * @param concept the concept associated with the Obs we are looking for
     * @param answerConcept the concept associated with the coded value of the Obs we are looking for (may be null)
     * @return the Obs from the current {@see ObsGroup} with the specified concept and answer concept
     */
    public Obs getObsFromCurrentGroup(Concept concept, Concept answerConcept) {
        if (currentObsGroupMembers == null)
            return null;
        for (Iterator<Obs> iter = currentObsGroupMembers.iterator(); iter.hasNext(); ) {
            Obs obs = iter.next();
            if (!obs.isVoided() &&
                    concept.getConceptId().equals(obs.getConcept().getConceptId()) &&
                    (answerConcept == null || equalConcepts(answerConcept, obs.getValueCoded()))) {
                iter.remove();
                return obs;
            }
        }
        return null;
    }
    
    /**
     * Sets the Patient to associate with the context
     * 
     * @param patient patient to associate with the context
     */
    public void setupExistingData(Patient patient) {
    	existingPatient = patient;
    }
        
    /**
     * Sets the existing Encounter to associate with the context.
     * Also sets all the Obs associated with this Encounter as existing Obs
     * 
     * @param encounter encounter to associate with the context
     */
    public void setupExistingData(Encounter encounter) {
        existingEncounter = encounter;
        existingObs = new HashMap<Concept, List<Obs>>();
        Set <Obs> obsset = new HashSet<Obs>();
        if (encounter != null) {
        	
        	/* get all obs related with this encounter
			 will skip obsgroup cause it won't display, */
        	for(Obs obs:encounter.getObsAtTopLevel(false)){
        		if(!obs.hasGroupMembers()){
        			obsset.add(obs);
        		}//else{
        		//	obsset.addAll(obs.getGroupMembers());
        		//}
        	}
        	//the obs order will later be used to help the display
        	List <Obs> sortedObs = HtmlFormEntryUtil.SortObs(obsset);
            for (Obs obs : sortedObs) {
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
     * Removes (and returns) an Obs or ObsGroup associated with a specified Concept from existingObs. Use this version
     * for obs whose concept's datatype is not boolean.
     * 
     * @param question the concept associated with the Obs to remove
     * @param answer the concept that serves as the answer for Obs to remove (may be null)
     * @return
     */
    public Obs removeExistingObs(Concept question, Concept answer) {
        List<Obs> list = existingObs.get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (answer == null || equalConcepts(answer, test.getValueCoded())) {
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
     * This method exists because of the stupid bug where Concept.equals(Concept) doesn't always work.
     */
    private boolean equalConcepts(Concept c1, Concept c2) {
    	return OpenmrsUtil.nullSafeEquals(c1 == null ? null : c1.getConceptId(), c2 == null ? null : c2.getConceptId());
    }

	/**
     * Removes (and returns) an Obs or ObsGroup associated with a specified Concept from existingObs.
     * Use this version for obs whose concept's datatype is boolean that are checkbox-style.
     * 
     * param question the concept associated with the Obs to remove
     * @param parseBoolean the boolean value of the obs
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
     * Returns, and removes, the first {@see ObsGroup} that matches the specified 
     *  concept and question/answer list. (That is, if the concept of the parent Obs = groupConcept,
     *  and all Obs in the group also belong to questionsAndAnswers.)
     *  <p/>
     * <pre> 
     * For example you might have a group capable of holding (per questionsAndAnswers):
     *      Symptom Absent = Cough
     *      Symptom Present = Cough
     *      Symptom Duration = (null)
     * and if we have an obs group containing a subset of those
     *      Symptom Present = Cough
     *      Symptom Duration = 5
     *      
     * then we would return it, assuming the the concept associated
     * with the parent Obs = groupConcept
     * </pre>
     * 
     * @param groupConcept the grouping concept associated with the {@see ObsGroups}
     * @param requiredQuestionsAndAnswers the questions and answered associate with the {@see ObsGroup}
     * @return the first matching {@see ObsGroup}
     */
    public Obs findFirstMatchingObsGroup(Concept groupConcept, List<ObsGroupComponent> questionsAndAnswers) {
        for (Iterator<Obs> iter = existingObsInGroups.iterator(); iter.hasNext(); ) {
            Obs group = iter.next();
            if (group.getConcept() == groupConcept && ObsGroupComponent.supports(questionsAndAnswers, group)) {
                iter.remove();
                return group;
            }
        }
        return null;
    }
    
    /**
     * Returns the patient currently associated with the context
     */
    public Patient getExistingPatient() {
    	return existingPatient;
    }
     
    /**
     * Returns the encounter currently associated with the context
     */
    public Encounter getExistingEncounter() {
        return existingEncounter;
    }

    /** 
     * Returns the translator currently associated with the context
     * @return
     */
    public Translator getTranslator() {
    	return translator;
    }
    
    /**
     * Return the HTML Form schema currently associated with the context
     * @return
     */
    public HtmlFormSchema getSchema() {
    	return schema;
    }
    
    public RptGroup getActiveRptGroup() {
		return activeRptGroup;
	}

	public void setActiveRptGroup(RptGroup activeRptGroup) {
		this.activeRptGroup = activeRptGroup;
	}
	
    public Integer getNewrepeatSeqVal() {
    	int thisRptVal = 0; //rpt counter
    	 
    	synchronized (newrepeatSeqVal) {
    		thisRptVal = newrepeatSeqVal; 
    	}
		return thisRptVal;
	}
    
    
    
    public Integer getNewrepeatTimesSeqVal() {
    	int thisRptTimeVal = 0; //rpt times counter
   	 
    	synchronized (this.newrepeatTimesSeqVal) {
    		thisRptTimeVal = newrepeatTimesSeqVal;
    	}
		return thisRptTimeVal;
	}


	public List<RptGroup> getExistingRptGroups() {
		return exsistingRptGroups;
	}

	public void setExsistingRptGroups(List<RptGroup> exsistingRptGroups) {
		this.exsistingRptGroups = exsistingRptGroups;
	}

	/**
     * Modes associated with the HTML Form context
     */
    public enum Mode {
    	/** A new, unsaved form */
    	ENTER,
    	/** A saved form in edit mode */
        EDIT,
        /** A saved form in view-only mode */
        VIEW
    }

    /***
     * Notify we are starting a newrepeat group
     */
	public void beginNewRepeatGroup() {
		int index;
		synchronized (newrepeatSeqVal) {
    		index = newrepeatSeqVal;
    	}
    	/* we are in a repeat group */
		this.activeRptGroup = this.exsistingRptGroups.get(index -1);
	}

	 /***
     * Notify we are out of a newrepeat group
     */
	public void endNewRepeatGroup() {
		// TODO Auto-generated method stubs
		this.activeRptGroup = null;
		synchronized (newrepeatSeqVal) {
    		++newrepeatSeqVal;
    	}
		/* reset the m and k value */
		ResetCtrlInNewrepeatSeqVal();
		ResetNewrepeatTimesSeqVal();
	}

	/*** 
    *  Get the next newrepeatTimesSeqVal
	*/
	public void getnewrepeatTimesNextSeqVal(){
		synchronized(newrepeatTimesSeqVal){
			++newrepeatTimesSeqVal;
		}
	}
	
	/* reset the repeat counter
	 * */
	public void ResetNewrepeatSeqVal() {
		synchronized (newrepeatSeqVal) {
			this.newrepeatSeqVal = 1;
    	}	
	}
	
	/* reset the ctrl counter in repeat 
	 * */
	public void ResetCtrlInNewrepeatSeqVal() {
		synchronized(ctrlInNewrepeatSeqVal){
			this.ctrlInNewrepeatSeqVal =1;
		}
	}
	
	/* reset the repeat times counter in repeat 
	 * */
	public void ResetNewrepeatTimesSeqVal() {
		synchronized(newrepeatTimesSeqVal){
			this.newrepeatTimesSeqVal =1;
		}
	}
	
	/***
	 * set newrepeattimeseq = 0
	 */
	public void ZeroNewrepeatTimesSeqVal(){
		synchronized(newrepeatTimesSeqVal){
			this.newrepeatTimesSeqVal =0;
		}
	}
	
}
