package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.matching.ObsGroupEntity;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
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
    private Map<String, String> javascriptFieldAccessorInfo = new LinkedHashMap<String, String>();
    private Translator translator = new Translator();
    private HtmlFormSchema schema = new HtmlFormSchema();
    private Stack<Map<ObsGroup, List<Obs>>> obsGroupStack = new Stack<Map<ObsGroup, List<Obs>>>();
    private ObsGroup activeObsGroup;
    
    private Patient existingPatient;
    private Encounter existingEncounter;
    private Map<Concept, List<Obs>> existingObs;
    private Map<Concept, List<Order>> existingOrders;
    private Map<Obs, Set<Obs>> existingObsInGroups;
    
    private Stack<Concept> currentObsGroupConcepts = new Stack<Concept>();
    private List<Obs> currentObsGroupMembers;
    private Location defaultLocation;
    
    private Date previousEncounterDate;  // if the encounter has been edited on a form, this stores the prior encounter date
    
    private List<ObsGroupEntity> unmatchedObsGroupEntities = null;
    private boolean unmatchedMode = false;
    
    private boolean guessingInd = false;
    
    public FormEntryContext(Mode mode) {
        this.mode = mode;
        setupExistingData((Encounter) null);
        schema.addSection(new HtmlFormSection());
    }
    
    /**
     * Gets the {@see Mode} associated with this Context
     * @return the {@see Mode} associatd with this Context
     */
    public Mode getMode() {
        return mode;
    }
    
    private Integer sequenceNextVal = 1;
    
    /**
     * Registers a widget within the Context
     *  
     * @param widget the widget to register
     * @return the field id used to identify this widget in the HTML Form
     */
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
        if (log.isTraceEnabled())
        	log.trace("Registered widget " + widget.getClass() + " as " + fieldName);
        return fieldName;
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
     * @throws IllegalArgumentException if the given widget is not registered
     */
    public String getFieldName(Widget widget) {
        String fieldName = fieldNames.get(widget);
        if (fieldName == null)
            throw new IllegalArgumentException("Widget not registered");
        else
            return fieldName;
    }
    
    /**
     * Like {@link #getFieldName(Widget)} but returns null if the widget is not registered (instead
     * of throwing an exception).
     * @param widget
     * @return
     */
    public String getFieldNameIfRegistered(Widget widget) {
        return fieldNames.get(widget);
    }
    
    /**
     * @return the widget that is registered for the given field name, or null if there is none
     */
	public Widget getWidgetByFieldName(String fieldName) {
		for (Map.Entry<Widget, String> e : fieldNames.entrySet()) {
			if (e.getValue().equals(fieldName))
				return e.getKey();
		}
		return null;
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
    public void beginObsGroup(Concept conceptSet, Obs thisGroup, ObsGroup obsGroupSchemaObj) {
        setObsGroup(thisGroup);
        currentObsGroupConcepts.push(conceptSet);
        activeObsGroup = obsGroupSchemaObj;
        Map<ObsGroup, List<Obs>> map = new HashMap<ObsGroup, List<Obs>>();
        map.put(this.getActiveObsGroup(), currentObsGroupMembers);
        obsGroupStack.push(map);
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
        //remove itself
    	
        if (!obsGroupStack.isEmpty()){
            obsGroupStack.pop();
            currentObsGroupConcepts.pop();
        }

        //set the activeObsGroup back to parent, if there is one.
        if (!obsGroupStack.isEmpty()){
            Map<ObsGroup, List<Obs>> map = obsGroupStack.peek();
            for (Map.Entry<ObsGroup, List<Obs>> e : map.entrySet()){
                e.getKey().addChild(activeObsGroup);
                currentObsGroupMembers = e.getValue();
                activeObsGroup = e.getKey();
                break;
            }
        } else {
            currentObsGroupMembers = null;
            getSchema().addField(activeObsGroup);
            activeObsGroup = null;
        }
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
            if (!obs.isVoided() && (concept == null || concept.getConceptId().equals(obs.getConcept().getConceptId())) &&
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
     * Also sets all the Orders associated with this Encounter as existing Orders
     * 
     * @param encounter encounter to associate with the context
     */
	public void setupExistingData(Encounter encounter) {
		existingEncounter = encounter;
		existingObs = new HashMap<Concept, List<Obs>>();
		existingOrders = new HashMap<Concept, List<Order>>();
		if (encounter != null) {
			for (Obs obs : encounter.getObsAtTopLevel(false)) {
				List<Obs> list = existingObs.get(obs.getConcept());
				if (list == null) {
					list = new LinkedList<Obs>();
					existingObs.put(obs.getConcept(), list);
				}
				list.add(obs);
			}
			for (Order order : encounter.getOrders()) {
			    if (!order.isVoided()){
    			  //load DrugOrders for later retrieval as DrugOrders
                    if (order.isDrugOrder()){
                        order = (Order) Context.getOrderService().getOrder(order.getOrderId(), DrugOrder.class);
                    }
    			    List<Order> list = existingOrders.get(order.getConcept());
    				if (list == null) {
    					list = new LinkedList<Order>();
    					existingOrders.put(order.getConcept(), list);
    				}
    				list.add(order);
			    }
			}
		}
		guessingInd = false;
		existingObsInGroups = new LinkedHashMap<Obs, Set<Obs>>();
		if (encounter != null)
			setupExistingObsInGroups(encounter.getObsAtTopLevel(false));
	}
    
    /**
     * 
     * Sets obs associated with an obs groups in existin obs groups.
     * 
     * @param oSet the obsGroup to add to existingObsInGroups
     */     
    public void setupExistingObsInGroups(Set<Obs> oSet){
        for (Obs parent : oSet)       
            if (parent.isObsGrouping()){
                existingObsInGroups.put(parent, parent.getGroupMembers());
                setupExistingObsInGroups(parent.getGroupMembers());
            }    
    }
            
     /**
      * Removes an Obs or ObsGroup of the relevant Concept from existingObs, and returns it. Use this version
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
     * Removes an Obs or ObsGroup of the relevant Concept from existingObs, and returns it. Use this version
     * for ConceptSelect obs tags.
    * 
    * @param questions the concepts associated with the Obs to remove
    * @param answer the concept that serves as the answer for Obs to remove (may NOT be null)
    * @return
    */
    public Obs removeExistingObs(List<Concept> questions, Concept answer) {
        for (Concept question:questions){
            Obs ret = removeExistingObs(question, answer);
            if (ret != null)
                return ret;
        }
        return null;
    }
    
	/**
	 * Removes an Order of the relevant Concept from existingOrders, and returns it.
	 * 
	 * @param question the concept associated with the Obs to remove
	 * @return
	 */
	public Order removeExistingOrder(Concept concept) {
		List<Order> list = existingOrders.get(concept);
		if (list != null) {
			for (Iterator<Order> iter = list.iterator(); iter.hasNext();) {
				Order test = iter.next();
				if (equalConcepts(concept, test.getConcept())) {
					iter.remove();
					if (list.size() == 0)
						existingOrders.remove(concept);
					return test;
				}
			}
		}
		return null;
	}
	
	/**
	 * checks the existing orders property and return a list of all as-of-yet unmatched orders
	 * @return the list of orders
	 */
	public List<Order> getRemainingExistingOrders(){
		List<Order> ret = new ArrayList<Order>();
		if (this.getExistingOrders() != null){
			for (Map.Entry<Concept, List<Order>> e : this.getExistingOrders().entrySet()){
				List<Order> ords = e.getValue();
				for (Order o : ords)
					ret.add(o);
			}
		}
		return ret;
	}
	
	
	/**
     * Removes a DrugOrder of the relevant Drug.Concept from existingOrders, and returns it.
     * 
     * @param question the concept associated with the Obs to remove
     * @return
     */
    public DrugOrder removeExistingDrugOrder(Drug drug) {
        if (drug != null){
            Concept concept = drug.getConcept();
            List<Order> list = existingOrders.get(concept);
            if (list != null) {
                for (Iterator<Order> iter = list.iterator(); iter.hasNext();) {
                    Order test = iter.next();
                    if (test.isDrugOrder()){
                        DrugOrder testDrugOrder = (DrugOrder) test;
                        if (equalDrug(testDrugOrder.getDrug(), drug)) {
                            iter.remove();
                            if (list.size() == 0)
                                existingOrders.remove(concept);
                            return testDrugOrder;
                        }
                    }
                    
                    
                }
            }
        }
        return null;
    }
    
    /**
     * This method exists because of the stupid bug where Concept.equals(Concept) doesn't always work.
     */
    private boolean equalDrug(Drug c1, Drug c2) {
        return OpenmrsUtil.nullSafeEquals(c1 == null ? null : c1.getDrugId(), c2 == null ? null : c2.getDrugId());
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
    public Obs removeExistingObs(Concept question, Boolean answer) {
        List<Obs> list = existingObs.get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (test.getValueAsBoolean() == null) {
                	throw new RuntimeException("Invalid boolean value for concept " + question + "; possibly caused by TRUNK-3150");
                }
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

    public Obs getNextUnmatchedObsGroup(String path) {
        Obs ret = null;
    	int unmatchedContenterCount = 0;
        for (Map.Entry<Obs, Set<Obs>> e : existingObsInGroups.entrySet() ) {
    		if (path.equals(ObsGroupComponent.getObsGroupPath(e.getKey()))) {
    			if (ret == null) ret = e.getKey();
    			unmatchedContenterCount++;
    		}
    	}
        if (ret != null){
        	if (unmatchedContenterCount > 1) guessingInd = true;
        	
            existingObsInGroups.remove(ret);
            existingObs.remove(ret);
            return ret;
        }
        return null;
    }
    
    public int getExistingObsInGroupsCount() {
    	if (existingObsInGroups != null) return existingObsInGroups.size();
    	return 0;
    }
    
    /**
     * Finds the best matching obsGroup at the right obsGroup hierarchy level
     *  <p/>
     * 
     * @param groupConcept the grouping concept associated with the {@see ObsGroups}
     * @param requiredQuestionsAndAnswers the questions and answered associate with the {@see ObsGroup}
     * @param obsGroupDepth  the depth level of the obsGroup in the xml
     * @return the first matching {@see ObsGroup}
     */
    public Obs findBestMatchingObsGroup(List<ObsGroupComponent> questionsAndAnswers, String xmlObsGroupConcept, String path) {
        Set<Obs> contenders = new HashSet<Obs>();
        // first all obsGroups matching parentObs.concept at the right obsGroup hierarchy level in the encounter are 
        // saved as contenders
        for (Map.Entry<Obs, Set<Obs>> e : existingObsInGroups.entrySet() ) {
//            log.debug("Comparing obsVal " + ObsGroupComponent.getObsGroupPath(e.getKey()) + " to xmlval " + path);
            if (path.equals(ObsGroupComponent.getObsGroupPath(e.getKey())) )
                contenders.add(e.getKey());
         }
        Obs ret = null;
        
        if (contenders.size() > 0){
            List<Obs> rankTable = new ArrayList<Obs>();
            int topRanking = 0;
            
            for (Obs parentObs:contenders){
                int rank = ObsGroupComponent.supportingRank(questionsAndAnswers, parentObs, existingObsInGroups.get(parentObs));

                if (rank > 0) {
                    if (rank > topRanking) {
                        topRanking = rank;
                        rankTable.clear();
                        rankTable.add(parentObs);
                    } else if (rank == topRanking) {
                    	rankTable.add(parentObs);
                    }
                }
            } 
            
            if (rankTable.size() == 0) {
                /* No matching obsGroup found; returning null obsGroup.  This will 
                 * trigger the creation of an <unmatched id={} /> tag which will be replaced on 
                 * a subsequent form scan.
                 */
                log.debug("No matching obsGroup found; returning null obsGroup.");
            } else if (rankTable.size() == 1) {
                ret = rankTable.get(0);
                log.debug("Found exactly one matching obsGroup; returning that obsGroup.");
            } else if (rankTable.size() > 1) {
                /* Multiple obsgroups support obs set, returning null obsGroup.  This will 
                 * trigger the creation of an <unmatched id={} /> tag which will be replaced on 
                 * a subsequent form scan.
                 */
                log.debug("Multiple obsgroups support obs set; returning null obsGroup");
            }
        }
        
        if (ret != null){
            existingObsInGroups.remove(ret);
            existingObs.remove(ret);
            return ret;
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
    
    public Map<Widget, String> getFieldNames() {
        return fieldNames;
    }
        
    public Map<Concept, List<Obs>> getExistingObs() {
        return existingObs;
    }
        
    public Map<Obs, Set<Obs>> getExistingObsInGroups() {
        return existingObsInGroups;
    }
    
    public Map<Concept, List<Order>> getExistingOrders() {
        return existingOrders;
    }
    
    /**
     * Sets up the necessary information so that the javascript getField, getValue and setValue
     * functions can work.
     * @param property the user-facing name of this property, e.g. weight.value
     * @param widgetId the HTML DOM id of the widget, to be passed to the property accessor methods (if this parameter is null, the method call has no effect)
     * @param fieldFunctionName the name of the javascript function used to access the field itself (or null to use the default function
     * @param getterFunctionName the name of the javascript function used to get the value of the field (or null to use the default function
     * @param setterFunctionName the name of the javascript function used to set the value of the field (or null to use the default function 
     */
	public void registerPropertyAccessorInfo(String property, String widgetId,
	                                         String fieldFunctionName, String getterFunctionName, String setterFunctionName) {
		if (widgetId == null)
			return;		
		StringBuilder val = new StringBuilder("{ id: \"" + widgetId + "\" ");
		if (fieldFunctionName != null)
			val.append(", field: " + fieldFunctionName);
		if (getterFunctionName != null)
			val.append(", getter: " + getterFunctionName);
		if (setterFunctionName != null)
			val.append(", setter: " + setterFunctionName);
		val.append(" };");
		if (javascriptFieldAccessorInfo.containsKey(property)) {
			// if this key has already been registered, this probably means we're inside a repeat tag.
			// set it up as follows (assuming property="weight.value")
			//   weight.value = X
			//   weight.value_1 = X
			//   weight.value_2 = Y
			//   weight.value_3 = Z
			// ...
			int i = 1;
			while (javascriptFieldAccessorInfo.containsKey(property + "_" + i))
				++i;
			if (i == 1) {
				// this is the second time we hit this key (i.e. property is registered, but property_1 is not)
				// we copy key to key_1
				javascriptFieldAccessorInfo.put(property + "_1", javascriptFieldAccessorInfo.get(property));
				i = 2;
			}
			property = property + "_" + i;
		}
		javascriptFieldAccessorInfo.put(property, val.toString());
    }
	
    /**
     * @return the javascriptFieldAccessors
     */
    public Map<String, String> getJavascriptFieldAccessorInfo() {
    	return javascriptFieldAccessorInfo;
    }

	/**
	 * @return the defaultLocation
	 */
	public Location getDefaultLocation() {
		return defaultLocation;
	}

	/**
	 * @param defaultLocation the defaultLocation to set
	 */
	public void setDefaultLocation(Location defaultLocation) {
		this.defaultLocation = defaultLocation;
	}

	public boolean isGuessingInd() {
		return guessingInd;
	}

	public void setGuessingInd(boolean guessingInd) {
		this.guessingInd = guessingInd;
	}
	    
	public String getGuessingInd() {
		return guessingInd ? "true" : "false";
	}

	public Date getPreviousEncounterDate() {
	    return previousEncounterDate;
    }

	public void setPreviousEncounterDate(Date previousEncounterDate) {
	    this.previousEncounterDate = previousEncounterDate;
    }
	
	public boolean hasUnmatchedObsGroupEntities() {
		return unmatchedObsGroupEntities != null && unmatchedObsGroupEntities.size() > 0 ? true : false;
	}
	
    public int addUnmatchedObsGroupEntities(ObsGroupEntity obsGroupEntity) {
    	if (unmatchedObsGroupEntities == null) unmatchedObsGroupEntities = new ArrayList<ObsGroupEntity>();
    	int id = unmatchedObsGroupEntities.size();
    	obsGroupEntity.setId(id);
    	unmatchedObsGroupEntities.add(obsGroupEntity);
    	return id;
    }

	public List<ObsGroupEntity> getUnmatchedObsGroupEntities() {
		return unmatchedObsGroupEntities;
	}

	public void setUnmatchedObsGroupEntities(
			List<ObsGroupEntity> unmatchedObsGroupEntities) {
		this.unmatchedObsGroupEntities = unmatchedObsGroupEntities;
	}

	public boolean isUnmatchedMode() {
		return unmatchedMode;
	}

	public void setUnmatchedMode(boolean unmatchedMode) {
		this.unmatchedMode = unmatchedMode;
	}

}
