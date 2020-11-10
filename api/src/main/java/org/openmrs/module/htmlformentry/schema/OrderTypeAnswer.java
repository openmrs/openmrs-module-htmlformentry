package org.openmrs.module.htmlformentry.schema;

import org.openmrs.OrderType;

public class OrderTypeAnswer {
	
	private String displayName;
	
	private OrderType orderType;
	
	public OrderTypeAnswer() {
	}
	
	public OrderTypeAnswer(OrderType orderType, String displayName) {
		this.orderType = orderType;
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public OrderType getOrderType() {
		return orderType;
	}
	
	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}
}
