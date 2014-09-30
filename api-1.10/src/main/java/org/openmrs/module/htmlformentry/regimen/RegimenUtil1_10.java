package org.openmrs.module.htmlformentry.regimen;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement1_10;
import org.openmrs.order.DrugSuggestion;
import org.openmrs.order.RegimenSuggestion;

/**
 * Helper for standardRegimen tag.
 * 
 * NOTE:  matching regimens to existing DrugOrders does NOT take dose or frequency into account.  All matching is by drug, start date, discontinued date.
 * 
 * @author dthomas
 *
 */
public class RegimenUtil1_10 {

	
	/**
	 * Finds 'strongest' RegimenSuggestion match, and returns map with size 0 or 1.  If 1, map contains the RegimenSuggestion and the list of DrugOrders.
	 * 
	 * 'strongest' = if you have standard regimen A containing 1 drug, and you have standard regimen B containing the same drug plus one other drug,
	 * 		then if both standard regimens pass the 'match' test, this method will return standard regimen B because of the greater number of matches.
	 * 
	 * In the event of a tie, the first match with the greatest 'strength' gets returned.
	 * 
	 * NOTE:  supports drugId in standard regimen XML being a uuid (matches on drug.uuid).
	 * @param regimenCandidates
	 * @param drugOrders
	 * @return Map<RegimenSuggestion, List<DrugOrder>>
	 */
	public static Map<RegimenSuggestion, List<DrugOrder>> findStrongestStandardRegimenInDrugOrders(List<RegimenSuggestion> regimenCandidates, List<Order> drugOrders){
		Map<RegimenSuggestion, List<DrugOrder>> ret = new HashMap<RegimenSuggestion, List<DrugOrder>>();
		int numComponents = 0;
		if (regimenCandidates != null && drugOrders != null){
			for (RegimenSuggestion rs :  regimenCandidates){
				List<DrugOrder> matchHolder = new ArrayList<DrugOrder>(); //collects drug matches between encounter and RegimenSuggestion
				boolean allFound = true; //true until a DrugSuggestion from the RegimenSuggestion isn't matched.
				Date startDate = null;  //if all DrugSuggestions are matched but have different start dates, we can't match.
				for (DrugSuggestion dc : rs.getDrugComponents()){
					DrugOrder dorToInspect = drugIdFoundInDrugSet(dc, drugOrders); //assumes patient will never have multiple drug orders of same drug in same encounter...
					if (dorToInspect != null){ // there was a match by drug
						if (startDate == null) // setup standard regimen start date
							startDate = dorToInspect.getDateActivated();
						if (startDate.equals(dorToInspect.getDateActivated())) //this will always be true for first drug matched.
							matchHolder.add(drugIdFoundInDrugSet(dc, drugOrders));
						else {
							//the drug was matched on drug type, but the start date was different.  This tag assumes equal start and stop dates to be a standard regimen.
							allFound = false;
							break;
						}
						
					} else {
						allFound = false;
						break;
					}
				}
				if (allFound && numComponents < matchHolder.size()){
					numComponents = matchHolder.size();
					ret.clear();
					ret.put(rs, matchHolder);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Finds a drug in a set of drugs by id or uuid; excludes voided DrugOrders from consideration
	 * 
	 * 
	 */
	private static DrugOrder drugIdFoundInDrugSet(DrugSuggestion ds, List<Order> dors){
		for (Order or : dors){
			if (or instanceof DrugOrder){
				DrugOrder dor = (DrugOrder) or;
				if (dor.getDrug() != null && !dor.isVoided() && (dor.getDrug().getDrugId().toString().equals(ds.getDrugId()) || dor.getDrug().getUuid().toString().equals(ds.getDrugId())))
					return dor;
			}
		}
		return null;
	}
	
	/**
	 * Creates DrugOrders from a given RegimenSuggestion
	 * @param rs  the Regimen Suggestion
	 * @param startDate  the Regimen start date
	 * @param patient the Patient
	 * @return
	 */
	public static Set<Order> standardRegimenToDrugOrders(RegimenSuggestion rs, Date startDate, Patient patient){
		Set<Order> ret = new HashSet<Order>();
		if (rs != null){
			OrderService orderService = Context.getOrderService();
			ConceptService conceptService = Context.getConceptService();
			for (DrugSuggestion ds : rs.getDrugComponents()){
				DrugOrder dor = new DrugOrder();
				Drug drug = Context.getConceptService().getDrugByNameOrId(ds.getDrugId());
				if (drug == null)
					drug = Context.getConceptService().getDrugByUuid(ds.getDrugId());
				if (drug == null)
					throw new RuntimeException("Your standard regimen xml file constains a drugId that can't be found, for regimen " + rs.getCodeName() + ", DrugComponent.id = " + ds.getDrugId());
				dor.setDrug(drug);
				
				OrderFrequency frequency = findFrequency(orderService, ds.getFrequency());
				dor.setFrequency(frequency);
				dor.setInstructions(ds.getInstructions());
				dor.setDose(Double.valueOf(ds.getDose()));
				dor.setDoseUnits(conceptService.getConceptByName(ds.getUnits()));
				dor.setRoute(Context.getConceptService().getConceptByName("UNKNOWN"));
				dor.setDateActivated(startDate);
				dor.setPatient(patient);
				dor.setDateChanged(new Date());
				dor.setCreator(Context.getAuthenticatedUser());
				dor.setOrderType(Context.getOrderService().getOrderTypeByUuid(DrugOrderSubmissionElement1_10.DRUG_ORDER_TYPE_UUID));
				ret.add(dor);
			}
		}	
		return ret;
	}

	private static OrderFrequency findFrequency(OrderService orderService, String frequency) {
	    OrderFrequency orderFrequency = null;
	    List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(frequency, null, false, false);
	    if (!orderFrequencies.isEmpty()) {
	    	orderFrequency = orderFrequencies.get(0);
	    }
	    return orderFrequency;
    }
	
	/**
	 * finds the RegimenSuggestion by its code.
	 */
	public static RegimenSuggestion getStandardRegimenByCode(List<RegimenSuggestion> regimens, String code){
		for (RegimenSuggestion rs : regimens){
			if (rs.getCodeName().equals(code))
				return rs;
		}
		return null;
	}
}
