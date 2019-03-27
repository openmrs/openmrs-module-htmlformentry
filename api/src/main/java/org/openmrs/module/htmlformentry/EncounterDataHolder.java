package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class holds existing data for an encounter
 */
public class EncounterDataHolder {

    protected final Log log = LogFactory.getLog(getClass());

    private Date previousEncounterDate;
    private Encounter encounter;
    private Map<Concept, List<Obs>> obsByConcept;
    private Map<Concept, List<Order>> ordersByConcept;
    private Map<Obs, Set<Obs>> obsInGroups;
    private boolean guessingInd;

    /**
     * Sets the existing Encounter to associate with the context.
     * Also sets all the Obs associated with this Encounter as existing Obs
     * Also sets all the Orders associated with this Encounter as existing Orders
     * 
     * @param encounter encounter to associate with the context
     */
	public EncounterDataHolder(Encounter encounter) {
		this.encounter = encounter;
        this.guessingInd = false;
        obsByConcept = new HashMap<Concept, List<Obs>>();
        ordersByConcept = new HashMap<Concept, List<Order>>();
		if (encounter != null) {
		    this.previousEncounterDate = encounter.getEncounterDatetime();
			for (Obs obs : encounter.getObsAtTopLevel(false)) {
				List<Obs> list = obsByConcept.get(obs.getConcept());
				if (list == null) {
					list = new LinkedList<Obs>();
                    obsByConcept.put(obs.getConcept(), list);
				}
				list.add(obs);
			}
			for (Order order : encounter.getOrders()) {
			    if (!order.isVoided()){
                    order = Context.getOrderService().getOrder(order.getOrderId()); // load subclasses for later retrieval
    			    List<Order> list = ordersByConcept.get(order.getConcept());
    				if (list == null) {
    					list = new LinkedList<Order>();
                        ordersByConcept.put(order.getConcept(), list);
    				}
    				list.add(order);
			    }
			}
		}
        obsInGroups = new LinkedHashMap<Obs, Set<Obs>>();
		if (encounter != null) {
            setupExistingObsInGroups(obsInGroups, encounter.getObsAtTopLevel(false));
        }
	}
    
    /**
     * Sets obs associated with an obs groups in existing obs groups
     * @param oSet the obsGroup to add to existingObsInGroups
     */     
    private void setupExistingObsInGroups(Map<Obs, Set<Obs>> obsInGroups, Set<Obs> oSet){
        for (Obs parent : oSet) {
            if (parent.isObsGrouping()) {
                obsInGroups.put(parent, parent.getGroupMembers());
                setupExistingObsInGroups(obsInGroups, parent.getGroupMembers());
            }
        }
    }

    /**
     * Finds the best matching obsGroup at the right obsGroup hierarchy level
     *  <p/>
     *
     * @param xmlObsGroupConcept the grouping concept associated with the {@see ObsGroups}
     * @param questionsAndAnswers the questions and answered associate with the {@see ObsGroup}
     * @param path  the depth level of the obsGroup in the xml
     * @return the first matching {@see ObsGroup}
     */
    public Obs removeBestMatchingObsGroup(List<ObsGroupComponent> questionsAndAnswers, String xmlObsGroupConcept, String path) {
        Obs ret = null;

        // first all obsGroups matching parentObs.concept at the right obsGroup hierarchy level in the encounter are saved as contenders
        Set<Obs> contenders = new HashSet<Obs>();
        for (Map.Entry<Obs, Set<Obs>> e : getObsInGroups().entrySet() ) {
            log.debug("Comparing obsVal " + ObsGroupComponent.getObsGroupPath(e.getKey()) + " to xmlval " + path);
            if (path.equals(ObsGroupComponent.getObsGroupPath(e.getKey())) ) {
                contenders.add(e.getKey());
            }
        }

        if (contenders.size() > 0) {
            List<Obs> rankTable = new ArrayList<Obs>();
            int topRanking = 0;
            for (Obs parentObs : contenders){
                int rank = ObsGroupComponent.supportingRank(questionsAndAnswers, parentObs, getObsInGroups().get(parentObs));
                if (rank > 0) {
                    if (rank > topRanking) {
                        topRanking = rank;
                        rankTable.clear();
                        rankTable.add(parentObs);
                    }
                    else if (rank == topRanking) {
                        rankTable.add(parentObs);
                    }
                }
            }

            if (rankTable.size() == 0 || rankTable.size() > 1) {
                /* No unique matching obsGroup found; returning null obsGroup.  This will
                 * trigger the creation of an <unmatched id={} /> tag which will be replaced on
                 * a subsequent form scan.
                 */
                ret = null;
            }
            else {
                // exactly one matching obs group
                ret = rankTable.get(0);
            }
        }

        if (ret != null){
            getObsInGroups().remove(ret);
            getObsByConcept().remove(ret);
            return ret;
        }
        else {
            return null;
        }
    }

    public Obs getNextUnmatchedObsGroup(String path) {
        Obs ret = null;
        int unmatchedContenterCount = 0;
        for (Map.Entry<Obs, Set<Obs>> e : getObsInGroups().entrySet() ) {
            if (path.equals(ObsGroupComponent.getObsGroupPath(e.getKey()))) {
                if (ret == null) ret = e.getKey();
                unmatchedContenterCount++;
            }
        }
        if (ret != null){
            if (unmatchedContenterCount > 1) {
                guessingInd = true;
            }
            getObsInGroups().remove(ret);
            getObsByConcept().remove(ret);
            return ret;
        }
        return null;
    }

    public int getExistingObsInGroupsCount() {
        if (getObsInGroups() != null) {
            return getObsInGroups().size();
        }
        return 0;
    }

    /**
     * Removes an Obs or ObsGroup of the relevant Concept from existingObs, and returns the list for
     * the question. Use this version for obtaining the whole list of obs saved for the single
     * Question concept.Presently used for dynamic lists.
     *
     * @param question concept associated with the Obs to remove
     * @return the list of obs associated with it
     */
    public List<Obs> removeExistingObs(Concept question) {
        return getObsByConcept().remove(question);
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
        List<Obs> list = getObsByConcept().get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (answer == null || HtmlFormEntryUtil.areEqual(answer, test.getValueCoded())) {
                    iter.remove();
                    if (list.size() == 0) {
                        getObsByConcept().remove(question);
                    }
                    return test;
                }
            }
        }
        return null;
    }

    /**
     * Removes (and returns) an Obs or ObsGroup associated with a specified Concept from existingObs.
     * Use this version for obs whose concept's datatype is boolean that are checkbox-style.
     *
     * @param question - the concept associated with the Obs to remove
     * @param answer - the boolean value of the Obs
     * @return
     */
    public Obs removeExistingObs(Concept question, Boolean answer) {
        List<Obs> list = getObsByConcept().get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (test.getValueAsBoolean() == null) {
                    throw new RuntimeException("Invalid boolean value for concept " + question + "; possibly caused by TRUNK-3150");
                }
                if (answer == test.getValueAsBoolean()) {
                    iter.remove();
                    if (list.size() == 0) {
                        removeExistingObs(question);
                    }
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
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * Finds whether there is existing obs created for the question concept and numeric answer,
     * returns that <obs> if any, Use this only when datatype is numeric and style="checkbox"
     *
     * @param question - the concept associated with the Obs to acquire
     * @param numericAns - numeric answer given with <obs/> declaration
     * @return the matching Obs, if any
     */
    public Obs removeExistingObs(Concept question, String numericAns) {
        Number numVal = Double.valueOf(numericAns);
        List<Obs> list = getObsByConcept().get(question);
        if (list != null) {
            for (Iterator<Obs> iter = list.iterator(); iter.hasNext(); ) {
                Obs test = iter.next();
                if (test.getValueNumeric().equals(numVal)) {
                    iter.remove();
                    if (list.size() == 0) {
                        getObsByConcept().remove(question);
                    }
                    return test;
                }
            }
        }

        return null;
    }

    //************* ORDER METHODS *************

    /**
     * Removes a DrugOrder of the relevant Drug.Concept from existingOrders, and returns it.
     *
     * @param drug- the drug associated with the DrugOrder to remove
     * @return
     */
    public DrugOrder removeExistingDrugOrder(Drug drug) {
        if (drug != null) {
            Concept concept = drug.getConcept();
            List<Order> list = getOrdersByConcept().get(concept);
            if (list != null) {
                for (Iterator<Order> iter = list.iterator(); iter.hasNext();) {
                    Order test = iter.next();
                    if (test instanceof DrugOrder) {
                        DrugOrder testDrugOrder = (DrugOrder) test;
                        if (HtmlFormEntryUtil.areEqual(testDrugOrder.getDrug(), drug)) {
                            iter.remove();
                            if (list.size() == 0) {
                                getOrdersByConcept().remove(concept);
                            }
                            return testDrugOrder;
                        }
                    }
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
        if (getOrdersByConcept() != null) {
            for (Map.Entry<Concept, List<Order>> e : getOrdersByConcept().entrySet()){
                List<Order> ords = e.getValue();
                for (Order o : ords) {
                    ret.add(o);
                }
            }
        }
        return ret;
    }

    public Date getPreviousEncounterDate() {
        return previousEncounterDate;
    }

    public void setPreviousEncounterDate(Date previousEncounterDate) {
        this.previousEncounterDate = previousEncounterDate;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public Map<Concept, List<Obs>> getObsByConcept() {
        return obsByConcept;
    }

    public Map<Concept, List<Order>> getOrdersByConcept() {
        return ordersByConcept;
    }

    public Map<Obs, Set<Obs>> getObsInGroups() {
        return obsInGroups;
    }

    public boolean isGuessingInd() {
        return guessingInd;
    }

    public void setGuessingInd(boolean guessingInd) {
        this.guessingInd = guessingInd;
    }
}
