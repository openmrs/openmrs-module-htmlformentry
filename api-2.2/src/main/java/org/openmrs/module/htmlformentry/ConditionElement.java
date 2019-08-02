package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private MessageSourceService mss;
	private ConditionService conditionService;
	private boolean required;
    private final String CONDITION_LIST_CONCEPT_CLASS_NAME = "Diagnosis";
    
	// widgets
	private Widget conditionSearchWidget;
	private DateWidget onSetDateWidget;
	private DateWidget endDateWidget;
	private RadioButtonsWidget conditionStatusesWidget;
	private ErrorWidget endDateErrorWidget;
	private ErrorWidget conditionSearchErrorWidget;
	private ErrorWidget conditionStatusesErrorWidget;
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		if (context.getMode() != Mode.VIEW) {
			Condition condition = new Condition();
			CodedOrFreeText conditionConcept = new CodedOrFreeText();
			try {
				int conceptId = Integer.parseInt((String) conditionSearchWidget.getValue(session.getContext(), submission));
				conditionConcept.setCoded(new Concept(conceptId));
				
			} catch(NumberFormatException e) {
				String nonCodedConcept = submission.getParameter(context.getFieldName(conditionSearchWidget));
				if (StringUtils.isBlank(nonCodedConcept) && !required) {
					// ignore silently
					return;
				}
				conditionConcept.setNonCoded(nonCodedConcept);
			}
			condition.setCondition(conditionConcept);
			condition.setClinicalStatus(getStatus(context, submission));
			condition.setOnsetDate(onSetDateWidget.getValue(context, submission));
			if (ConditionClinicalStatus.INACTIVE == getStatus(context, submission)) {
				condition.setEndDate(endDateWidget.getValue(context, submission));
			}
			condition.setPatient(session.getPatient());
			conditionService = Context.getConditionService();
			conditionService.saveCondition(condition);
		}
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {		
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Date givenOnsetDate = onSetDateWidget.getValue(context, submission);
		Date givenEndDate = endDateWidget.getValue(context, submission);
		String condition = StringUtils.isNotBlank((String) conditionSearchWidget.getValue(context, submission)) ? 
				(String) conditionSearchWidget.getValue(context, submission) : submission.getParameter(context.getFieldName(conditionSearchWidget));
		ConditionClinicalStatus status = getStatus(context, submission);
		
		if (context.getMode() != Mode.VIEW) {
			if (StringUtils.isBlank(condition) && required) {
				ret.add(new FormSubmissionError(context.getFieldName(conditionSearchWidget), 
		                    Context.getMessageSourceService().getMessage("htmlformentry.conditionui.condition.required")));
			}	
			if (givenOnsetDate != null && givenEndDate != null) {
				if (givenOnsetDate.after(givenEndDate)) {
					ret.add(new FormSubmissionError(context.getFieldName(endDateWidget), 
							Context.getMessageSourceService().getMessage("htmlformentry.conditionui.endDate.before.onsetDate.error")));
				}
			} 
			if (status == null && required) {
				ret.add(new FormSubmissionError(context.getFieldName(conditionStatusesWidget), 
						Context.getMessageSourceService().getMessage("htmlformentry.conditionui.status.required")));
			}
		}
		return ret;
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=\"htmlformentry-condition\">");
		ret.append(htmlForConditionSearchWidget(context));
		ret.append(htmlForConditionStatusesWidgets(context));		
		ret.append(htmlForConditionDatesWidget(context));
		ret.append("</div>");
		return ret.toString();
	}
		
	private String htmlForConditionSearchWidget(FormEntryContext context) {
		Set<Concept> initialConcepts = new HashSet<Concept>();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		ConceptClass conceptClass = Context.getConceptService().getConceptClassByName(CONDITION_LIST_CONCEPT_CLASS_NAME);
		initialConcepts.addAll(Context.getConceptService().getConceptsByClass(conceptClass));
		conditionSearchWidget = new ConceptSearchAutocompleteWidget(new ArrayList<Concept>(initialConcepts), Arrays.asList(conceptClass));
		String conditionNameTextInputId = context.registerWidget(conditionSearchWidget);
		conditionSearchErrorWidget = new ErrorWidget();
		context.registerErrorWidget(conditionSearchWidget, conditionSearchErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append(conditionSearchWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			ret.append(conditionSearchErrorWidget.generateHtml(context));
		}
		ret.append("\n<script>jq('#" + conditionNameTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.conditionui.condition") + "');\n");
		ret.append(" jq('#" + conditionNameTextInputId + "').css('min-width', '46.4%');\n");

		// Add support for non-coded concept values.
		// This a hack to let the autocomplete widget accept values that aren't part of the concept list.
		ret.append("jq('#" + conditionNameTextInputId + "').blur(function(e){\n");
		ret.append("     var valueAttr = jq('#" + conditionNameTextInputId + "_hid" + "').attr('value');\n");
		ret.append("     if(valueAttr === \"ERROR\"){\n");
		ret.append("        jq('#" + conditionNameTextInputId + "_hid" + "').attr('value', '');\n");
		ret.append("     }\n");
		ret.append("});\n");				
		ret.append("</script>\n");
		return ret.toString();
	}
	
	private String htmlForConditionStatusesWidgets(FormEntryContext context) {
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		Option active = new Option(mss.getMessage("coreapps.conditionui.active.label"), "active", false);
		Option inactive = new Option(mss.getMessage("coreapps.conditionui.inactive.label"), "inactive", false);
		Option historyOf = new Option(mss.getMessage("htmlformentry.conditionui.historyOf.label"), "history-of", false);
		conditionStatusesWidget = new RadioButtonsWidget();
		conditionStatusesErrorWidget = new ErrorWidget();
		conditionStatusesWidget.addOption(active);
		conditionStatusesWidget.addOption(inactive);
		conditionStatusesWidget.addOption(historyOf);
		String radioGroupName = context.registerWidget(conditionStatusesWidget);
		context.registerErrorWidget(conditionStatusesWidget, conditionStatusesErrorWidget);

		StringBuilder sb = new StringBuilder();
		sb.append("<div id=\"condition-status\">");
		sb.append(conditionStatusesErrorWidget.generateHtml(context));
		sb.append(conditionStatusesWidget.generateHtml(context));
		sb.append("<script>");		
		sb.append("jq(\"input[name='" + radioGroupName + "']\").change(function(e){\n" +
				"    if($(this).val() == 'active') {\n" + 
				"		document.getElementById('endDatePicker').style.visibility=\"hidden\"; \n" +
				"    } else {\n" + 
				"		document.getElementById('endDatePicker').style.visibility=\"visible\";\n" +
				"    }\n" + 
				"\n" + 
				"});");
		sb.append("</script>");
		sb.append("<style>");
		sb.append("#condition-status input {\n" + 
				"    display: inline;\n" + 
				"    float: none;\n" + 
				"}\n" + 
				"#condition-status label {\n" + 
				"    display: inline;\n" + 
				"}");
		sb.append("#condition-status {\n" +
				 "   padding:10px 0px;\n" +
				 " }");
		sb.append("</style>");
		sb.append("</div>");
		return sb.toString();	
	}
	
	private String htmlForConditionDatesWidget(FormEntryContext context) {
		onSetDateWidget = new DateWidget();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		String onsetDateTextInputId = context.registerWidget(onSetDateWidget) + "-display";
		endDateWidget = new DateWidget();
		endDateErrorWidget = new ErrorWidget();
		String endDateTextInputId = context.registerWidget(endDateWidget) + "-display";
		context.registerErrorWidget(endDateWidget, endDateErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append("<ul>");
		ret.append("<li>");
		ret.append(onSetDateWidget.generateHtml(context));
		ret.append("</li> <li>");
		ret.append("<span id=\"endDatePicker\">");
		ret.append(endDateWidget.generateHtml(context));
		ret.append("</span>");
		ret.append("</li>");
		if (context.getMode() != Mode.VIEW) {
			ret.append(endDateErrorWidget.generateHtml(context));
		}
		ret.append("</ul> <br/>");
		ret.append("<script> jq('#" + onsetDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.conditionui.onsetdate") + "');");
		ret.append("jq('#" + endDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.stopDate.label") + "');");
		ret.append("</script>");
		ret.append("<style>");
		ret.append("#htmlformentry-condition li {\n" + 
				"	width:30%;\n" + 
				"   float: left;\n" + 
				"}");
		ret.append("#htmlformentry-condition ul {\n" + 
				"	display:flow-root;\n" + 
				"   width:85%" +
				"}");
		ret.append("</style>");
		return ret.toString();
	}

	private ConditionClinicalStatus getStatus(FormEntryContext context, HttpServletRequest request) {
		if (conditionStatusesWidget == null) {
			return null;
		}
		Object status = conditionStatusesWidget.getValue(context, request);
		if (status != null) {			
			if (((String)status).equals("active")) {
				return ConditionClinicalStatus.ACTIVE;
			}
			if (((String)status).equals("inactive")) {
				return ConditionClinicalStatus.INACTIVE;
			}
			if (((String)status).equals("history-of")) {
				return ConditionClinicalStatus.HISTORY_OF;
			}
		}
		return null;
	}

	public void setConditionSearchWidget(Widget conditionSearchWidget) {
		this.conditionSearchWidget = conditionSearchWidget;
	}

	public void setOnSetDateWidget(DateWidget onSetDateWidget) {
		this.onSetDateWidget = onSetDateWidget;
	}

	public void setEndDateWidget(DateWidget endDateWidget) {
		this.endDateWidget = endDateWidget;
	}

	public void setConditionStatusesWidget(RadioButtonsWidget conditionStatusesWidget) {
		this.conditionStatusesWidget = conditionStatusesWidget;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

}
