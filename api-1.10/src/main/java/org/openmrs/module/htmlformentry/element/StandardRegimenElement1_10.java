package org.openmrs.module.htmlformentry.element;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.regimen.RegimenUtil1_10;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.util.OpenmrsUtil;

public class StandardRegimenElement1_10 extends StandardRegimenElement {

	public StandardRegimenElement1_10(FormEntryContext context, Map<String, String> parameters) {
	    super(context, parameters);
    }

	@Override
	protected void matchStandardRegimenInExistingOrders(FormEntryContext context) {
		Map<RegimenSuggestion, List<DrugOrder>> map =  RegimenUtil1_10.findStrongestStandardRegimenInDrugOrders(possibleRegimens, context.getRemainingExistingOrders());
		if (map.size() == 1){
			existingStandardRegimen = map.keySet().iterator().next();
			for (DrugOrder dor : map.get(existingStandardRegimen)){
				regDrugOrders.add(context.removeExistingDrugOrder(dor.getDrug()));
				regWidget.setInitialValue(existingStandardRegimen.getCodeName());
			}
			 discontinuedDateWidget.setInitialValue(getCommonDiscontinueDate(regDrugOrders));
			 Order discontinuationOrder = Context.getOrderService().getDiscontinuationOrder(regDrugOrders.get(0));
			    if (discontinuedReasonWidget != null && discontinuationOrder != null)
			        discontinuedReasonWidget.setInitialValue(discontinuationOrder.getOrderReason().getConceptId());
		}
	}
	
	@Override
	protected Date getCommonDiscontinueDate(List<DrugOrder> orders) {
		Date candidate = null;
		if (orders != null & orders.size() > 0)
				candidate = orders.get(0).getDateStopped();
		for (Order o : orders){
			if (!OpenmrsUtil.nullSafeEquals(o.getDateStopped(), candidate))
				return null;
		}
		return candidate;
	}
	
	@Override
	protected void enterStandardRegimen(FormEntrySession session, String regCode, Date startDate, Date discontinuedDate,
	        String discontinuedReasonStr) {
		RegimenSuggestion rs = RegimenUtil1_10.getStandardRegimenByCode(possibleRegimens, regCode);
		//create new drugOrders
		Set<Order> ords = RegimenUtil1_10.standardRegimenToDrugOrders(rs, startDate, session.getPatient());	
		for (Order o: ords){
			if (o.getDateCreated() == null)
	    	    o.setDateCreated(new Date());
	    	if (o.getCreator() == null)
	    	    o.setCreator(Context.getAuthenticatedUser());
	    	if (o.getUuid() == null)
	    	    o.setUuid(UUID.randomUUID().toString());
	    	Order discontinuationOrder = null;
			if (discontinuedDate != null){
				discontinuationOrder = newDiscontinuationOrder(discontinuedDate, discontinuedReasonStr, o);
	    	}
			if (o.getOrderer() == null) {
				setOrderer(o, session);
			}
			session.getSubmissionActions().getCurrentEncounter().addOrder(o);
			if (discontinuationOrder != null) {
				session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
			}
		}	
	}

	private void setOrderer(Order o, FormEntrySession session) {
	    Set<EncounterProvider> encounterProviders = session.getSubmissionActions().getCurrentEncounter().getEncounterProviders();
	    for (EncounterProvider encounterProvider : encounterProviders) {
	        if (!encounterProvider.isVoided()) {
	        	o.setOrderer(encounterProvider.getProvider());
	        	break;
	        }
	    }
    }
	
	@Override
	protected void editStandardRegimen(FormEntrySession session, String regCode, Date startDate, Date discontinuedDate,
	        String discontinuedReasonStr) {
		if (existingStandardRegimen != null && regCode.equals(existingStandardRegimen.getCodeName())){
			//the drug orders are already there and attached to the encounter.
			for (Order o : regDrugOrders){
				o.setStartDate(startDate);
				Order discontinuationOrder = null;
				if (discontinuedDate != null){
					discontinuationOrder = newDiscontinuationOrder(discontinuedDate, discontinuedReasonStr, o);
    	    	}  
				if (discontinuationOrder != null) {
					session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
				}
				
			}
		} else {
			//standard regimen changed in the drop-down...  I'm going to have this void the old DrugOrders, and create new ones.
			 voidDrugOrders(regDrugOrders, session);
			 RegimenSuggestion rs = RegimenUtil1_10.getStandardRegimenByCode(possibleRegimens, regCode);
			 Set<Order> ords = RegimenUtil1_10.standardRegimenToDrugOrders(rs, startDate, session.getPatient());	
				for (Order o: ords){
					if (o.getDateCreated() == null)
	    	    	    o.setDateCreated(new Date());
	    	    	if (o.getCreator() == null)
	    	    	    o.setCreator(Context.getAuthenticatedUser());
	    	    	if (o.getUuid() == null)
	    	    	    o.setUuid(UUID.randomUUID().toString());
	    	    	Order discontinuationOrder = null;
					if (discontinuedDate != null){
						discontinuationOrder = newDiscontinuationOrder(discontinuedDate, discontinuedReasonStr, o);
	    	    	}  
					session.getSubmissionActions().getCurrentEncounter().addOrder(o);
					if (discontinuationOrder != null) {
						session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
					}
				}	
		}
	}
	
	private Order newDiscontinuationOrder(Date discontinuedDate, String discontinuedReasonStr, Order o) {
	    Order discontinuationOrder;
	    discontinuationOrder = o.cloneForDiscontinuing();
	    discontinuationOrder.setStartDate(discontinuedDate);
	    if (!StringUtils.isEmpty(discontinuedReasonStr)) {
	    	discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
	    }
	    return discontinuationOrder;
	}
}
