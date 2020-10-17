package org.openmrs.module.htmlformentry.widget;

import org.openmrs.DrugOrder;

/**
 * Encapsulates a given widget value that has a reference to previous and new orders, and other
 * information
 */
public class DrugOrderWidgetValue {
	
	private DrugOrder previousDrugOrder;
	
	private DrugOrder newDrugOrder;
	
	private boolean voidPreviousOrder;
	
	public DrugOrderWidgetValue() {
	}
	
	public DrugOrder getPreviousDrugOrder() {
		return previousDrugOrder;
	}
	
	public void setPreviousDrugOrder(DrugOrder previousDrugOrder) {
		this.previousDrugOrder = previousDrugOrder;
	}
	
	public DrugOrder getNewDrugOrder() {
		return newDrugOrder;
	}
	
	public void setNewDrugOrder(DrugOrder newDrugOrder) {
		this.newDrugOrder = newDrugOrder;
	}
	
	public boolean isVoidPreviousOrder() {
		return voidPreviousOrder;
	}
	
	public void setVoidPreviousOrder(boolean voidPreviousOrder) {
		this.voidPreviousOrder = voidPreviousOrder;
	}
}
