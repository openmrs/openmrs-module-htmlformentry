package org.openmrs.module.htmlformentry.schema;

import org.openmrs.OrderFrequency;

public class OrderFrequencyAnswer {
	
	private String displayName;
	
	private OrderFrequency orderFrequency;
	
	public OrderFrequencyAnswer() {
	}
	
	public OrderFrequencyAnswer(OrderFrequency orderFrequency, String displayName) {
		this.orderFrequency = orderFrequency;
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public OrderFrequency getOrderFrequency() {
		return orderFrequency;
	}
	
	public void setOrderFrequency(OrderFrequency orderFrequency) {
		this.orderFrequency = orderFrequency;
	}
}
