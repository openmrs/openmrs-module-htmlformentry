package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.OrderType;

public class OrderField implements HtmlFormField {
	
	private OrderType orderType;
	
	private List<ConceptOption> conceptOptions = new ArrayList<>();
	
	private List<DrugOrderAnswer> drugOrderAnswers = new ArrayList<>();
	
	private List<CareSettingAnswer> careSettingAnswers = new ArrayList<>();
	
	private List<ConceptOption> orderReasonAnswers = new ArrayList<>();
	
	private List<ConceptOption> discontinueReasonAnswers = new ArrayList<>();
	
	private List<ConceptOption> doseUnitAnswers = new ArrayList<>();
	
	private List<ConceptOption> routeAnswers = new ArrayList<>();
	
	private List<OrderFrequencyAnswer> frequencyAnswers = new ArrayList<>();
	
	private List<ConceptOption> durationUnitAnswers = new ArrayList<>();
	
	private List<ConceptOption> quantityUnitAnswers = new ArrayList<>();
	
	private Map<String, List<ConceptOptionGroup>> conceptOptionGroups = new HashMap<>();
	
	public OrderField() {
	}
	
	public OrderType getOrderType() {
		return orderType;
	}
	
	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}
	
	public List<ConceptOption> getConceptOptions() {
		return conceptOptions;
	}
	
	public void setConceptOptions(List<ConceptOption> conceptOptions) {
		this.conceptOptions = conceptOptions;
	}
	
	public void addConceptOption(ConceptOption a) {
		if (conceptOptions == null) {
			conceptOptions = new ArrayList<>();
		}
		conceptOptions.add(a);
	}
	
	public List<DrugOrderAnswer> getDrugOrderAnswers() {
		return drugOrderAnswers;
	}
	
	public void setDrugOrderAnswers(List<DrugOrderAnswer> drugOrderAnswers) {
		this.drugOrderAnswers = drugOrderAnswers;
	}
	
	public void addDrugOrderAnswer(DrugOrderAnswer doa) {
		if (drugOrderAnswers == null) {
			drugOrderAnswers = new ArrayList<>();
		}
		drugOrderAnswers.add(doa);
	}
	
	public List<CareSettingAnswer> getCareSettingAnswers() {
		return careSettingAnswers;
	}
	
	public void setCareSettingAnswers(List<CareSettingAnswer> careSettingAnswers) {
		this.careSettingAnswers = careSettingAnswers;
	}
	
	public void addCareSettingAnswer(CareSettingAnswer a) {
		if (careSettingAnswers == null) {
			careSettingAnswers = new ArrayList<>();
		}
		careSettingAnswers.add(a);
	}
	
	public List<ConceptOption> getOrderReasonAnswers() {
		return orderReasonAnswers;
	}
	
	public void setOrderReasonAnswers(List<ConceptOption> orderReasonAnswers) {
		this.orderReasonAnswers = orderReasonAnswers;
	}
	
	public void addOrderReasonAnswer(ConceptOption a) {
		if (orderReasonAnswers == null) {
			orderReasonAnswers = new ArrayList<>();
		}
		orderReasonAnswers.add(a);
	}
	
	public List<ConceptOption> getDiscontinueReasonAnswers() {
		return discontinueReasonAnswers;
	}
	
	public void setDiscontinueReasonAnswers(List<ConceptOption> discontinueReasonAnswers) {
		this.discontinueReasonAnswers = discontinueReasonAnswers;
	}
	
	public void addDiscontinueReasonAnswer(ConceptOption a) {
		if (discontinueReasonAnswers == null) {
			discontinueReasonAnswers = new ArrayList<>();
		}
		discontinueReasonAnswers.add(a);
	}
	
	public List<ConceptOption> getDoseUnitAnswers() {
		return doseUnitAnswers;
	}
	
	public void setDoseUnitAnswers(List<ConceptOption> doseUnitAnswers) {
		this.doseUnitAnswers = doseUnitAnswers;
	}
	
	public void addDoseUnitAnswer(ConceptOption a) {
		if (doseUnitAnswers == null) {
			doseUnitAnswers = new ArrayList<>();
		}
		doseUnitAnswers.add(a);
	}
	
	public List<ConceptOption> getRouteAnswers() {
		return routeAnswers;
	}
	
	public void setRouteAnswers(List<ConceptOption> routeAnswers) {
		this.routeAnswers = routeAnswers;
	}
	
	public void addRouteAnswer(ConceptOption a) {
		if (routeAnswers == null) {
			routeAnswers = new ArrayList<>();
		}
		routeAnswers.add(a);
	}
	
	public List<OrderFrequencyAnswer> getFrequencyAnswers() {
		return frequencyAnswers;
	}
	
	public void setFrequencyAnswers(List<OrderFrequencyAnswer> frequencyAnswers) {
		this.frequencyAnswers = frequencyAnswers;
	}
	
	public void addOrderFrequencyAnswer(OrderFrequencyAnswer a) {
		if (frequencyAnswers == null) {
			frequencyAnswers = new ArrayList<>();
		}
		frequencyAnswers.add(a);
	}
	
	public List<ConceptOption> getDurationUnitAnswers() {
		return durationUnitAnswers;
	}
	
	public void setDurationUnitAnswers(List<ConceptOption> durationUnitAnswers) {
		this.durationUnitAnswers = durationUnitAnswers;
	}
	
	public void addDurationUnitAnswer(ConceptOption a) {
		if (durationUnitAnswers == null) {
			durationUnitAnswers = new ArrayList<>();
		}
		durationUnitAnswers.add(a);
	}
	
	public List<ConceptOption> getQuantityUnitAnswers() {
		return quantityUnitAnswers;
	}
	
	public void setQuantityUnitAnswers(List<ConceptOption> quantityUnitAnswers) {
		this.quantityUnitAnswers = quantityUnitAnswers;
	}
	
	public void addQuantityUnitAnswer(ConceptOption a) {
		if (quantityUnitAnswers == null) {
			quantityUnitAnswers = new ArrayList<>();
		}
		quantityUnitAnswers.add(a);
	}
	
	public Map<String, List<ConceptOptionGroup>> getConceptOptionGroups() {
		if (conceptOptionGroups == null) {
			conceptOptionGroups = new LinkedHashMap<>();
		}
		return conceptOptionGroups;
	}
	
	public void addConceptOptionGroup(String propertyName, ConceptOptionGroup optionSet) {
		List<ConceptOptionGroup> l = getConceptOptionGroups().get(propertyName);
		if (l == null) {
			l = new ArrayList<>();
			getConceptOptionGroups().put(propertyName, l);
		}
		l.add(optionSet);
	}
	
	public void setConceptOptionGroups(Map<String, List<ConceptOptionGroup>> conceptOptionGroups) {
		this.conceptOptionGroups = conceptOptionGroups;
	}
}
