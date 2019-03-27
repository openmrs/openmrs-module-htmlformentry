package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.module.htmlformentry.matching.ObsGroupEntity;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.LocaleUtility;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * This class holds the context data around generating html widgets from tags in an HtmlForm.
 * <p/>
 * It allows you to register widgets, which assigns them an id/name in the generated html, and allows you to
 * look up those id/names later. It allows you specify which error widget goes to which widget, so that error
 * messages get displayed in the right place.
 * <p/>
 * It also holds existing data for an encounter in View mode, so that widgets can be set with
 * their appropriate values.
 * <p/>
 * TODO rename this class: it's really more of the html generation context than a form entry context
 * </p>
 * TODO move Mode class up to FormEntrySession instead?
 * <p>
 * 'automaticClientSideValidation' means that elements and widgets should generate HTML that does things like numeric range
 * checking when you blur a text field.
 * 'clientSideValidationHints' means that elements and widgets should generate HTML where inputs have classes like "required"
 * and "numeric-range", and attributes like "min" and "max".
 * </p>
 */
public class FormEntryContext {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());
    
    private Mode mode;

    private Map<String, String> javascriptFieldAccessorInfo = new LinkedHashMap<String, String>();
    private Translator translator = new Translator();
    private HtmlFormSchema schema = new HtmlFormSchema();
    private Stack<HtmlFormSection> sectionsStack = new Stack<HtmlFormSection>();
    private WidgetRegister widgetRegister = new WidgetRegister();

    private Stack<Map<ObsGroup, List<Obs>>> obsGroupStack = new Stack<Map<ObsGroup, List<Obs>>>();
    private ObsGroup activeObsGroup;
    
    private Patient existingPatient;
    private EncounterDataHolder currentEncounterData;
    private Set<EncounterDataHolder> currentVisitData = new HashSet<EncounterDataHolder>();

    private Stack<Concept> currentObsGroupConcepts = new Stack<Concept>();
    private List<Obs> currentObsGroupMembers;
    private Location defaultLocation;
    
    private List<ObsGroupEntity> unmatchedObsGroupEntities = null;
    private boolean unmatchedMode = false;

    private HttpSession httpSession;

    private boolean automaticClientSideValidation = true;
    private boolean clientSideValidationHints = false;

    private Stack<Object> stack = new Stack<Object>();

    // TODO once Html Form Entry no longer supports older core versions that don't have visits, we should:
    // TODO 1) change the type of this variable to visit
    // TODO 2) change HtmlFormEntryController so that it correctly populates the context with the relevant visit (if available)
    private Object visit;

    public FormEntryContext(Mode mode) {
        this.mode = mode;
        setupExistingData((Encounter) null);
        translator.setDefaultLocaleStr(LocaleUtility.getDefaultLocale().toString());
    }

    public String registerWidget(Widget widget) {
        return getWidgetRegister().registerWidget(widget);
    }

    public String registerErrorWidget(Widget widget, ErrorWidget errorWidget) {
        return getWidgetRegister().registerErrorWidget(widget, errorWidget);
    }

    public String getFieldName(Widget widget) {
        return getWidgetRegister().getFieldName(widget);
    }

    public String getFieldNameIfRegistered(Widget widget) {
        return getWidgetRegister().getFieldNameIfRegistered(widget);
    }

    public Widget getWidgetByFieldName(String fieldName) {
        return getWidgetRegister().getWidgetByFieldName(fieldName);
    }

    public String getErrorFieldId(Widget widget) {
        return getWidgetRegister().getErrorFieldId(widget);
    }

    public Collection<String> getErrorDivIds() {
        return getWidgetRegister().getErrorDivIds();
    }

    public WidgetRegister getWidgetRegister() {
        if (widgetRegister == null) {
            widgetRegister = new WidgetRegister();
        }
        return widgetRegister;
    }
    
    /**
     * Gets the {@see Mode} associated with this Context
     * @return the {@see Mode} associated with this Context
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Adds a new section
     */
    public void beginSection(HtmlFormSection section) {

        // is this a top-level section or it is a child of the existing section
        if (sectionsStack.size() > 0) {
            sectionsStack.peek().addChildSection(section);
        }
        else {
            schema.getSections().add(section);
        }

        sectionsStack.push(section);
    }

    public void beginSection() {
        beginSection(new HtmlFormSection());
    }

    public HtmlFormSection getActiveSection() {
        if (sectionsStack.size() > 0) {
            return sectionsStack.peek();
        }
        else {
            return null;
        }
    }

    /**
     * Adds an HTML Form Field to the schema
     *
     * @param field the field to add
     */
    public void addFieldToActiveSection(HtmlFormField field) {
        if (sectionsStack.size() > 0) {
            sectionsStack.peek().addField(field);
        }
        else {
            schema.getFields().add(field);
        }
    }

    public void endSection(){
        sectionsStack.pop();
    }
       
    /**
     * Marks the start of a new {@see ObsGroup} within current Context
     */
    public void beginObsGroup(Concept conceptSet, Obs thisGroup, ObsGroup obsGroupSchemaObj) {
        if (thisGroup == null) {
            currentObsGroupMembers = null;
        }
        else {
            currentObsGroupMembers = new ArrayList<Obs>();
            for (Obs o : thisGroup.getGroupMembers()) {
                currentObsGroupMembers.add(o);
            }
        }
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

    public void addFieldToActiveObsGroup(HtmlFormField field) {
        getActiveObsGroup().getChildren().add(field);
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
            addFieldToActiveSection(activeObsGroup);
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
                    (answerConcept == null || HtmlFormEntryUtil.areEqual(answerConcept, obs.getValueCoded()))) {
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
     * 
     * @param encounter encounter to associate with the context
     */
	public void setupExistingData(Encounter encounter) {
        currentEncounterData = new EncounterDataHolder(encounter);
        if (encounter != null && encounter.getVisit() != null) {
            visit = encounter.getVisit();
            for (Encounter e : encounter.getVisit().getEncounters()) {
                if (!e.equals(currentEncounterData)) {
                    currentVisitData.add(new EncounterDataHolder(e));
                }
            }
        }
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
    public EncounterDataHolder getCurrentEncounterData() {
        return currentEncounterData;
    }

    public Set<EncounterDataHolder> getCurrentVisitData() {
        return currentVisitData;
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

    public void setHttpSession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public void pushToStack(Object object) {
        stack.push(object);
    }

    public Object popFromStack() {
        return stack.pop();
    }

    public <T> T getHighestOnStack(Class<T> clazz) {
        for (int i = stack.size() - 1; i >= 0; --i) {
            Object candidate = stack.get(i);
            if (clazz.isAssignableFrom(candidate.getClass())) {
                return (T) candidate;
            }
        }
        return null;
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
        return getWidgetRegister().getFieldNames();
    }

    public Map<Concept, List<Obs>> getExistingObs() {
        return getCurrentEncounterData().getObsByConcept();
    }

    public Map<Obs, Set<Obs>> getExistingObsInGroups() {
        return getCurrentEncounterData().getObsInGroups();
    }

    public Map<Concept, List<Order>> getExistingOrders() {
        return getCurrentEncounterData().getOrdersByConcept();
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
		return getCurrentEncounterData().isGuessingInd();
	}

	public void setGuessingInd(boolean guessingInd) {
		getCurrentEncounterData().setGuessingInd(guessingInd);
	}

	public String getGuessingInd() {
		return isGuessingInd() ? "true" : "false";
	}

    public Date getPreviousEncounterDate() {
        return getCurrentEncounterData().getPreviousEncounterDate();
    }

    public void setPreviousEncounterDate(Date previousEncounterDate) {
        getCurrentEncounterData().setPreviousEncounterDate(previousEncounterDate);
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

    public boolean isAutomaticClientSideValidation() {
        return automaticClientSideValidation;
    }

    public void setAutomaticClientSideValidation(boolean automaticClientSideValidation) {
        this.automaticClientSideValidation = automaticClientSideValidation;
    }

    public boolean isClientSideValidationHints() {
        return clientSideValidationHints;
    }

    public void setClientSideValidationHints(boolean clientSideValidationHints) {
        this.clientSideValidationHints = clientSideValidationHints;
    }

    public Object getVisit() {
        return visit;
    }

    public void setVisit(Object visit) {
        this.visit = visit;
    }

}
