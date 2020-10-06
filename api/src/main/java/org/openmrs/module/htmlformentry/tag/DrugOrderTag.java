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
 * Defines the configuration attributes available in the encounterProviderAndRole tag, and provides
 * parsing and validation
 */
public class DrugOrderTag {
	
	public static final String DRUG_NAMES = "drugNames";
	
	public static final String DRUG_LABELS = "drugLabels";
	
	public static final String CHECKBOX = "checkbox";
	
	public static final String TOGGLE = "toggle";
	
	public static final String CARE_SETTING = "careSetting";
	
	public static final String AS_NEEDED_LABEL = "asNeededLabel";
	
	public static final String SHOW_ORDER_DURATION = "showOrderDuration";
	
	public static final String HIDE_DOSE_AND_FREQUENCY = "hideDoseAndFrequency";
	
	public static final String DEFAULT_DOSE = "defaultDose";
	
	public static final String VALIDATE_DOSE = "validateDose";
	
	public static final String INSTRUCTIONS_LABEL = "instructionsLabel";
	
	public static final String DISCONTINUE_CONCEPT_ID = "discontinuedReasonConceptId";
	
	public static final String DISCONTINUED_ANSWERS = "discontinueReasonAnswers";
	
	public static final String DISCONTINUED_ANSWER_LABELS = "discontinueReasonAnswerLabels";
	
	private List<Drug> drugs; // The drug list to limit this to
	
	private List<String> drugLabels; // The drug labels to associate with the configured drugs
	
	private boolean checkbox;
	
	private String toggle;
	
	private CareSetting careSetting;
	
	private String asNeededLabel;
	
	private boolean showOrderDuration; // If true, show duration and duration units, else hide
	
	private boolean hideDoseAndFrequency; // If true, use FreeTextDosingInstructions, else use SimpleDosingInstructions
	
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
		drugs = TagUtil.parseListParameter(parameters, DRUG_NAMES, Drug.class);
		drugLabels = TagUtil.parseListParameter(parameters, DRUG_LABELS, String.class);
		checkbox = TagUtil.parseParameter(parameters, CHECKBOX, Boolean.class, false);
		toggle = TagUtil.parseParameter(parameters, TOGGLE, String.class, "");
		careSetting = TagUtil.parseParameter(parameters, CARE_SETTING, CareSetting.class, null);
		asNeededLabel = TagUtil.parseParameter(parameters, AS_NEEDED_LABEL, String.class, "DrugOrder.asNeeded");
		showOrderDuration = TagUtil.parseParameter(parameters, SHOW_ORDER_DURATION, Boolean.class, false);
		hideDoseAndFrequency = TagUtil.parseParameter(parameters, HIDE_DOSE_AND_FREQUENCY, Boolean.class, false);
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
		if (drugs.isEmpty()) {
			throw new IllegalArgumentException(DRUG_NAMES + " must contain at least one valid drug reference");
		}
		if (drugLabels.size() > 0 && drugLabels.size() != drugs.size()) {
			throw new IllegalArgumentException("There are a different number of drugLabels (" + drugLabels.size()
			        + ") than drugs (" + drugs.size() + ").");
		}
	}
	
	public DrugOrderField getDrugOrderField() {
		DrugOrderField dof = new DrugOrderField();
		for (int i = 0; i < drugs.size(); i++) {
			Drug drug = drugs.get(i);
			String label = drug.getDisplayName();
			if (getDrugLabels().size() >= (i - 1)) {
				label = getDrugLabels().get(i);
			}
			dof.addDrugOrderAnswer(new DrugOrderAnswer(drug, label));
		}
		return dof;
	}
	
	public Class<? extends DosingInstructions> getDosingInstructionsType() {
		if (isHideDoseAndFrequency()) {
			return FreeTextDosingInstructions.class;
		}
		return SimpleDosingInstructions.class;
	}
	
	public List<Drug> getDrugs() {
		return drugs;
	}
	
	public void setDrugs(List<Drug> drugs) {
		this.drugs = drugs;
	}
	
	public List<String> getDrugLabels() {
		return drugLabels;
	}
	
	public void setDrugLabels(List<String> drugLabels) {
		this.drugLabels = drugLabels;
	}
	
	public boolean isCheckbox() {
		return checkbox;
	}
	
	public void setCheckbox(boolean checkbox) {
		this.checkbox = checkbox;
	}
	
	public String getToggle() {
		return toggle;
	}
	
	public void setToggle(String toggle) {
		this.toggle = toggle;
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
	
	public void setAsNeededLabel(String asNeededLabel) {
		this.asNeededLabel = asNeededLabel;
	}
	
	public boolean isShowOrderDuration() {
		return showOrderDuration;
	}
	
	public void setShowOrderDuration(boolean showOrderDuration) {
		this.showOrderDuration = showOrderDuration;
	}
	
	public boolean isHideDoseAndFrequency() {
		return hideDoseAndFrequency;
	}
	
	public void setHideDoseAndFrequency(boolean hideDoseAndFrequency) {
		this.hideDoseAndFrequency = hideDoseAndFrequency;
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
