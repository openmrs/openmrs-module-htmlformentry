package org.openmrs.module.htmlformentry.support;

import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.htmlformentry.impl.EncounterSaveSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
@OpenmrsProfile(openmrsVersion = "1.10")
public class EncounterSaveSupport1_10 implements EncounterSaveSupport {

	@Autowired
	@Qualifier("orderService")
	OrderService orderService;
	
	@Autowired
	@Qualifier("encounterService")
	EncounterService encounterService;
	
	@Override
    public void saveEncounter(Encounter encounter) {
		encounterService.saveEncounter(encounter);
		
		//Saving orders is no longer cascaded in 1.10		
	    if (encounter.getOrders() != null) {
	    	for (Order order : encounter.getOrders()) {
	            orderService.saveOrder(order, null);
            }
	    }
    }
	
}
