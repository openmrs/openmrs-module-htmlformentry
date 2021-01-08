package org.openmrs.module.htmlformentry.schema;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Concept;

public class DrugOrderField implements HtmlFormField {
	
	private List<DrugOrderAnswer> drugOrderAnswers = new ArrayList<>();
	
	private List<CareSettingAnswer> careSettingAnswers = new ArrayList<>();
	
	private List<OrderTypeAnswer> orderTypeAnswers = new ArrayList<>();
	
	private List<ObsFieldAnswer> doseUnitAnswers = new ArrayList<>();
	
	private List<ObsFieldAnswer> routeAnswers = new ArrayList<>();
	
	private List<OrderFrequencyAnswer> frequencyAnswers = new ArrayList<>();
	
	private List<ObsFieldAnswer> durationUnitAnswers = new ArrayList<>();
	
	private List<ObsFieldAnswer> quantityUnitAnswers = new ArrayList<>();
	
	private Concept discontinuedReasonQuestion;
	
	private List<ObsFieldAnswer> discontinuedReasonAnswers = new ArrayList<>();
	
	public DrugOrderField() {
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
	
	public List<OrderTypeAnswer> getOrderTypeAnswers() {
		return orderTypeAnswers;
	}
	
	public void setOrderTypeAnswers(List<OrderTypeAnswer> orderTypeAnswers) {
		this.orderTypeAnswers = orderTypeAnswers;
	}
	
	public void addOrderTypeAnswer(OrderTypeAnswer a) {
		if (orderTypeAnswers == null) {
			orderTypeAnswers = new ArrayList<>();
		}
		orderTypeAnswers.add(a);
	}
	
	public List<ObsFieldAnswer> getDoseUnitAnswers() {
		return doseUnitAnswers;
	}
	
	public void setDoseUnitAnswers(List<ObsFieldAnswer> doseUnitAnswers) {
		this.doseUnitAnswers = doseUnitAnswers;
	}
	
	public void addDoseUnitAnswer(ObsFieldAnswer a) {
		if (doseUnitAnswers == null) {
			doseUnitAnswers = new ArrayList<>();
		}
		doseUnitAnswers.add(a);
	}
	
	public List<ObsFieldAnswer> getRouteAnswers() {
		return routeAnswers;
	}
	
	public void setRouteAnswers(List<ObsFieldAnswer> routeAnswers) {
		this.routeAnswers = routeAnswers;
	}
	
	public void addRouteAnswer(ObsFieldAnswer a) {
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
	
	public List<ObsFieldAnswer> getDurationUnitAnswers() {
		return durationUnitAnswers;
	}
	
	public void setDurationUnitAnswers(List<ObsFieldAnswer> durationUnitAnswers) {
		this.durationUnitAnswers = durationUnitAnswers;
	}
	
	public void addDurationUnitAnswer(ObsFieldAnswer a) {
		if (durationUnitAnswers == null) {
			durationUnitAnswers = new ArrayList<>();
		}
		durationUnitAnswers.add(a);
	}
	
	public List<ObsFieldAnswer> getQuantityUnitAnswers() {
		return quantityUnitAnswers;
	}
	
	public void setQuantityUnitAnswers(List<ObsFieldAnswer> quantityUnitAnswers) {
		this.quantityUnitAnswers = quantityUnitAnswers;
	}
	
	public void addQuantityUnitAnswer(ObsFieldAnswer a) {
		if (quantityUnitAnswers == null) {
			quantityUnitAnswers = new ArrayList<>();
		}
		quantityUnitAnswers.add(a);
	}
	
	public Concept getDiscontinuedReasonQuestion() {
		return discontinuedReasonQuestion;
	}
	
	public void setDiscontinuedReasonQuestion(Concept discontinuedReasonQuestion) {
		this.discontinuedReasonQuestion = discontinuedReasonQuestion;
	}
	
	public List<ObsFieldAnswer> getDiscontinuedReasonAnswers() {
		return discontinuedReasonAnswers;
	}
	
	public void setDiscontinuedReasonAnswers(List<ObsFieldAnswer> discontinuedReasonAnswers) {
		this.discontinuedReasonAnswers = discontinuedReasonAnswers;
	}
	
	public void addDiscontinuedReasonAnswer(ObsFieldAnswer ofa) {
		if (discontinuedReasonAnswers == null) {
			discontinuedReasonAnswers = new ArrayList<>();
		}
		discontinuedReasonAnswers.add(ofa);
	}
}
