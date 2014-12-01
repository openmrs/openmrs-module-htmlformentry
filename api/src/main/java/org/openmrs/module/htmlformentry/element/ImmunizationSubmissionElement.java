package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.InvalidActionException;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.util.OpenmrsUtil;

/**
 * Submission element for immunizations
 */
public class ImmunizationSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	public static final String CIEL_IMMNUNIZATION_HISTORY = "CIEL:1421";
	
	public static final String CIEL_IMMUNIZATIONS = "CIEL:984";
	
	public static final String CIEL_IMMUNIZATION_SEQUENCE_NUMBER = "CIEL:1418";
	
	public static final String CIEL_VACCINATION_DATE = "CIEL:1410";
	
	private String id;
	
	private Concept vaccineConcept;
	
	private Integer sequenceNumber;
	
	private String label;
	
	private CheckboxWidget checkboxWidget;
	
	private ErrorWidget errorWidget;
	
	private DateWidget dateWidget;
	
	private Obs existingObsGroup;
	
	// Question concepts
	private Concept immunizationGroupConcept = HtmlFormEntryUtil.getConcept(CIEL_IMMNUNIZATION_HISTORY);
	
	private Concept vaccineQuestionConcept = HtmlFormEntryUtil.getConcept(CIEL_IMMUNIZATIONS);
	
	private Concept sequenceNumberConcept = HtmlFormEntryUtil.getConcept(CIEL_IMMUNIZATION_SEQUENCE_NUMBER);
	
	private Concept vaccinationDateConcept = HtmlFormEntryUtil.getConcept(CIEL_VACCINATION_DATE);
	
	/**
	 * Constructs a new immunization submission element
	 * 
	 * @param context the form entry context
	 * @param parameters the tag parameters
	 */
	public ImmunizationSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		
		if (immunizationGroupConcept == null || vaccineQuestionConcept == null || sequenceNumberConcept == null
		        || vaccinationDateConcept == null) {
			throw new IllegalStateException(
			        "To use the immunizations tag, you must either have the CIEL dictionary install, or "
			                + "have concepts in your dictionary mapped to ");
		}
		
		String vaccineConceptId = parameters.get("vaccineConceptId");
		if (StringUtils.isEmpty(vaccineConceptId)) {
			throw new RuntimeException("vaccineConceptId attribute required");
		}
		
		id = parameters.get("id");
		vaccineConcept = HtmlFormEntryUtil.getConcept(vaccineConceptId);
		sequenceNumber = parameters.containsKey("sequenceNumber") ? Integer.parseInt(parameters.get("sequenceNumber"))
		        : null;
		label = parameters.get("label");
		
		existingObsGroup = matchExistingObsGroup(context);
		
		errorWidget = new ErrorWidget();
		
		Boolean showDate = Boolean.valueOf(parameters.get("showDate"));
		if (showDate) {
			dateWidget = new DateWidget();
			dateWidget.setInitialValue(existingObsGroup != null ? existingObsGroup.getObsDatetime() : null);
			
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, errorWidget);
		} else {
			checkboxWidget = new CheckboxWidget(getLabel(), "true");
			
			// Checkbox is checked if we can find a matching obs group
			checkboxWidget.setValue("true");
			checkboxWidget.setInitialValue(existingObsGroup != null ? Boolean.TRUE : null);
			
			context.registerWidget(checkboxWidget);
			context.registerErrorWidget(checkboxWidget, errorWidget);
		}
		
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		boolean viewMode = context.getMode().equals(FormEntryContext.Mode.VIEW);
		StringBuilder sb = new StringBuilder();
		
		if (id != null) {
			sb.append("<span id=\"" + id + "\">");
			if (checkboxWidget == null) {
				context.registerPropertyAccessorInfo(id + ".date", context.getFieldNameIfRegistered(dateWidget),
				    "dateFieldGetterFunction", null, "dateSetterFunction");
			} else {
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(checkboxWidget), null,
				    null, null);
			}
			context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(errorWidget), null, null,
			    null);
		}
		
		if (checkboxWidget == null) {
			sb.append(getLabel()).append(" ").append(dateWidget.generateHtml(context));
		} else {
			sb.append(checkboxWidget.generateHtml(context));
		}
		
		if (!viewMode) {
			sb.append(errorWidget.generateHtml(context));
		}
		
		if (id != null) {
			sb.append("</span>");
		}
		
		return sb.toString();
	}
	
	/**
	 * Gets the label to use for this control
	 * 
	 * @return the label
	 */
	protected String getLabel() {
		if (label != null) {
			return label;
		}
		
		String ret = vaccineConcept.getName().getName();
		if (sequenceNumber != null) {
			ret += "-" + sequenceNumber;
		}
		return ret;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		try {
			if (dateWidget != null) { 
				Date date = dateWidget.getValue(context, submission);
				
				if (date != null) {
					if (OpenmrsUtil.compare(date, new Date()) > 0) {
						ret.add(new FormSubmissionError(dateWidget, Context.getMessageSourceService().getMessage(
						    "htmlformentry.error.cannotBeInFuture")));
					}
				}
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(dateWidget, ex.getMessage()));
		}
		
		return ret;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		final boolean checked;
		final Date date;
		if (checkboxWidget != null) {
			checked = checkboxWidget.getValue(session.getContext(), request) != null;
			date = null;
		} else {
			date = dateWidget.getValue(session.getContext(), request);
			checked = date != null;
		}
		
		if (!checked && existingObsGroup != null) {
			session.getSubmissionActions().getObsToVoid().add(existingObsGroup);
		} else if (checked) {
			if (existingObsGroup != null && OpenmrsUtil.compareWithNullAsEarliest(existingObsGroup.getObsDatetime(), date) == 0) {
				return; //nothing changed
			}
			
			try {
				if (existingObsGroup != null) {
					session.getSubmissionActions().getObsToVoid().add(existingObsGroup);
				}
				
				Obs group = new Obs();
				group.setConcept(immunizationGroupConcept);
				if (dateWidget != null) {
					group.setObsDatetime(date);
				}
				
				session.getSubmissionActions().beginObsGroup(group);
				session.getSubmissionActions().createObs(vaccineQuestionConcept, vaccineConcept, date, null, null);
				session.getSubmissionActions().createObs(sequenceNumberConcept, sequenceNumber, date, null, null);
				if (dateWidget != null) {
					session.getSubmissionActions().createObs(vaccinationDateConcept, date, date, null, null);
				}
				session.getSubmissionActions().endObsGroup();
			}
			catch (InvalidActionException e) {
				e.printStackTrace();
			}
		} 
	}
	
	/**
	 * Matches an existing obs group
	 * 
	 * @param context the form entry context
	 * @return the obs or null
	 */
	protected Obs matchExistingObsGroup(FormEntryContext context) {
		for (Map.Entry<Obs, Set<Obs>> entry : context.getExistingObsInGroups().entrySet()) {
			Obs group = entry.getKey();
			
			// Skip if obs group isn't an immunization obs grouping
			if (!group.getConcept().equals(immunizationGroupConcept)) {
				continue;
			}
			
			Concept vaccineAnswer = null;
			Integer sequenceNumberAnswer = null;
			
			// Look through obs group members to find vaccine and sequence number
			for (Obs memberObs : entry.getValue()) {
				if (memberObs.getConcept().equals(vaccineQuestionConcept)) {
					vaccineAnswer = memberObs.getValueCoded();
				} else if (memberObs.getConcept().equals(sequenceNumberConcept)) {
					sequenceNumberAnswer = memberObs.getValueNumeric().intValue();
				}
			}
			
			// Remove and return and group if both vaccine and sequence number match
			if (OpenmrsUtil.nullSafeEquals(vaccineAnswer, vaccineConcept)
			        && OpenmrsUtil.nullSafeEquals(sequenceNumberAnswer, sequenceNumber)) {
				context.getExistingObsInGroups().remove(group);
				return group;
			}
		}
		
		return null;
	}
}
