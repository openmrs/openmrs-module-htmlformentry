package org.openmrs.module.htmlformentry.schema;

import org.openmrs.CareSetting;

public class CareSettingAnswer {
	
	private String displayName;
	
	private CareSetting careSetting;
	
	public CareSettingAnswer() {
	}
	
	public CareSettingAnswer(CareSetting careSetting, String displayName) {
		this.careSetting = careSetting;
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public CareSetting getCareSetting() {
		return careSetting;
	}
	
	public void setCareSetting(CareSetting careSetting) {
		this.careSetting = careSetting;
	}
}
