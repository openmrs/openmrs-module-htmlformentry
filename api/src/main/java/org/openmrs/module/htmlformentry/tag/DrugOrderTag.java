/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.tag;

import java.util.List;
import java.util.Map;

import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

/**
 * Defines the configuration attributes available in the drugOrder tag, and provides parsing and
 * validation
 */
public class DrugOrderTag {
	
	public static final String DRUG = "drug";
	
	public static final String DRUG_LABEL = "drugLabel";
	
	public static final String DOSING_TYPE = "dosingType";
	
	public static final String CARE_SETTING = "careSetting";
	
	public static final String SHOW_DURATION = "showDuration";
	
	public static final String AS_NEEDED_LABEL = "asNeededLabel";
	
	public static final String DEFAULT_DOSE = "defaultDose";
	
	public static final String VALIDATE_DOSE = "validateDose";
	
	public static final String INSTRUCTIONS_LABEL = "instructionsLabel";
	
	public static final String DISCONTINUE_CONCEPT_ID = "discontinuedReasonConceptId";
	
	public static final String DISCONTINUED_ANSWERS = "discontinueReasonAnswers";
	
	public static final String DISCONTINUED_ANSWER_LABELS = "discontinueReasonAnswerLabels";
	
	private Drug drug;
	
	private String drugLabel;
	
	private String dosingType; // freeText or simple
	
	private CareSetting careSetting;
	
	private boolean showDuration;
	
	private String asNeededLabel;
	
	private Double defaultDose;
	
	private boolean validateDose;
	
	private String instructionsLabel;
	
	private Concept discontinueQuestion;
	
	private List<Concept> discontinueAnswers;
	
	private List<String> discontinueAnswerLabels;
	
	/**
	 * @param parameters - the parameters passed in from the parsed XML of the htmlform
	 */
	public DrugOrderTag(Map<String, String> parameters) {
		drug = TagUtil.parseParameter(parameters, DRUG, Drug.class);
		drugLabel = TagUtil.parseParameter(parameters, DRUG_LABEL, String.class);
		dosingType = TagUtil.parseParameter(parameters, DOSING_TYPE, String.class);
		careSetting = TagUtil.parseParameter(parameters, CARE_SETTING, CareSetting.class, null);
		showDuration = TagUtil.parseParameter(parameters, SHOW_DURATION, Boolean.class, true);
		asNeededLabel = TagUtil.parseParameter(parameters, AS_NEEDED_LABEL, String.class, "DrugOrder.asNeeded");
		defaultDose = TagUtil.parseParameter(parameters, DEFAULT_DOSE, Double.class, null);
		validateDose = TagUtil.parseParameter(parameters, VALIDATE_DOSE, Boolean.class, false);
		instructionsLabel = TagUtil.parseParameter(parameters, INSTRUCTIONS_LABEL, String.class, "");
		discontinueQuestion = TagUtil.parseParameter(parameters, DISCONTINUE_CONCEPT_ID, Concept.class, null);
		discontinueAnswers = TagUtil.parseListParameter(parameters, DISCONTINUED_ANSWERS, Concept.class);
		discontinueAnswerLabels = TagUtil.parseListParameter(parameters, DISCONTINUED_ANSWER_LABELS, String.class);
		populateTagDefaults();
		validate();
	}
	
	public void populateTagDefaults() {
		if (getCareSetting() == null) {
			setCareSetting(HtmlFormEntryUtil.getCareSetting("OUTPATIENT"));
		}
	}
	
	public void validate() {
		if (drug == null) {
			throw new IllegalArgumentException(DRUG + " is required");
		}
	}
	
	public DrugOrderField getDrugOrderField() {
		DrugOrderField dof = new DrugOrderField();
		dof.addDrugOrderAnswer(new DrugOrderAnswer(drug, drugLabel));
		return dof;
	}
	
	public String getDrugDisplayName() {
		return drugLabel == null ? drug.getDisplayName() : drugLabel;
	}
	
	public Class<? extends DosingInstructions> getDosingInstructionsType() {
		if ("freeText".equalsIgnoreCase(dosingType)) {
			return FreeTextDosingInstructions.class;
		}
		return SimpleDosingInstructions.class;
	}
	
	public Drug getDrug() {
		return drug;
	}
	
	public void setDrug(Drug drug) {
		this.drug = drug;
	}
	
	public String getDrugLabel() {
		return drugLabel;
	}
	
	public void setDrugLabel(String drugLabel) {
		this.drugLabel = drugLabel;
	}
	
	public String getDosingType() {
		return dosingType;
	}
	
	public void setDosingType(String dosingType) {
		this.dosingType = dosingType;
	}
	
	public CareSetting getCareSetting() {
		return careSetting;
	}
	
	public void setCareSetting(CareSetting careSetting) {
		this.careSetting = careSetting;
	}
	
	public String getAsNeededLabel() {
		return asNeededLabel;
	}
	
	public boolean isShowDuration() {
		return showDuration;
	}
	
	public void setShowDuration(boolean showDuration) {
		this.showDuration = showDuration;
	}
	
	public void setAsNeededLabel(String asNeededLabel) {
		this.asNeededLabel = asNeededLabel;
	}
	
	public Double getDefaultDose() {
		return defaultDose;
	}
	
	public void setDefaultDose(Double defaultDose) {
		this.defaultDose = defaultDose;
	}
	
	public boolean isValidateDose() {
		return validateDose;
	}
	
	public void setValidateDose(boolean validateDose) {
		this.validateDose = validateDose;
	}
	
	public String getInstructionsLabel() {
		return instructionsLabel;
	}
	
	public void setInstructionsLabel(String instructionsLabel) {
		this.instructionsLabel = instructionsLabel;
	}
	
	public Concept getDiscontinueQuestion() {
		return discontinueQuestion;
	}
	
	public void setDiscontinueQuestion(Concept discontinueQuestion) {
		this.discontinueQuestion = discontinueQuestion;
	}
	
	public List<Concept> getDiscontinueAnswers() {
		return discontinueAnswers;
	}
	
	public void setDiscontinueAnswers(List<Concept> discontinueAnswers) {
		this.discontinueAnswers = discontinueAnswers;
	}
	
	public List<String> getDiscontinueAnswerLabels() {
		return discontinueAnswerLabels;
	}
	
	public void setDiscontinueAnswerLabels(List<String> discontinueAnswerLabels) {
		this.discontinueAnswerLabels = discontinueAnswerLabels;
	}
}
