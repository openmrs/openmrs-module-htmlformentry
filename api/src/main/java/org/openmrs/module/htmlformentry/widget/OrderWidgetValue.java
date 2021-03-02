package org.openmrs.module.htmlformentry.widget;

import org.openmrs.Order;

/**
 * Encapsulates a given widget value that has a reference to previous and new orders, and other
 * information. This is what is constructed out of the request and passed to the submission element
 * for processing
 */
public class OrderWidgetValue {
	
	private String fieldSuffix;
	
	private Order previousOrder;
	
	private Order newOrder;
	
	public OrderWidgetValue() {
	}
	
	public String getFieldSuffix() {
		return fieldSuffix;
	}
	
	public void setFieldSuffix(String fieldSuffix) {
		this.fieldSuffix = fieldSuffix;
	}
	
	public Order getPreviousOrder() {
		return previousOrder;
	}
	
	public void setPreviousOrder(Order previousOrder) {
		this.previousOrder = previousOrder;
	}
	
	public Order getNewOrder() {
		return newOrder;
	}
	
	public void setNewOrder(Order newOrder) {
		this.newOrder = newOrder;
	}
}
