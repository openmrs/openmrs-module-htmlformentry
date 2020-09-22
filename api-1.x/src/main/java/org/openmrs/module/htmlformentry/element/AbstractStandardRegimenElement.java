package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.DrugOrder;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.schema.StandardRegimenAnswer;
import org.openmrs.module.htmlformentry.schema.StandardRegimenField;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.order.DrugOrderSupport;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.util.OpenmrsConstants;

/**
 * @author dthomas NOTE: the routines here WON'T change regimens if you change the definitions of a
 *         regimen in the XML automatically.
 */
public abstract class AbstractStandardRegimenElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(AbstractStandardRegimenElement.class);
	
	public static final String STANDARD_REGIMEN_CODES = "regimenCodes";
	
	public static final String FIELD_DISCONTINUED_REASON = "discontinuedReasonConceptId";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWERS = "discontinueReasonAnswers";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWER_LABELS = "discontinueReasonAnswerLabels";
	
	public static final String STANDARD_REGIMEN_GLOBAL_PROPERTY = "dashboard.regimen.standardRegimens";
	
	protected Widget regWidget;
	
	protected ErrorWidget regErrorWidget;
	
	protected DateWidget startDateWidget;
	
	protected ErrorWidget startDateErrorWidget;
	
	protected DateWidget discontinuedDateWidget;
	
	protected ErrorWidget discontinuedDateErrorWidget;
	
	protected DropdownWidget discontinuedReasonWidget;
	
	protected ErrorWidget discontinuedReasonErrorWidget;
	
	protected List<DrugOrder> regDrugOrders = new ArrayList<DrugOrder>();
	
	protected RegimenSuggestion existingStandardRegimen;
	
	//helpers:
	protected List<RegimenSuggestion> allSystemStandardRegimens = new ArrayList<RegimenSuggestion>();
	
	protected List<RegimenSuggestion> possibleRegimens = new ArrayList<RegimenSuggestion>();
	
	public AbstractStandardRegimenElement(FormEntryContext context, Map<String, String> parameters) {
		
		String regimenCodes = parameters.get(STANDARD_REGIMEN_CODES);
		if (regimenCodes == null || regimenCodes.length() < 1)
			throw new IllegalArgumentException(
			        "You must provide a valid regimenCode from your standard regimen XML (see global property "
			                + STANDARD_REGIMEN_GLOBAL_PROPERTY + " for these codes) " + parameters);
		
		List<Option> options = new ArrayList<Option>();
		options.add(new Option("", "", false));
		
		StringTokenizer tokenizer = new StringTokenizer(regimenCodes, ",");
		allSystemStandardRegimens = DrugOrderSupport.getInstance().getStandardRegimens();
		StandardRegimenField srf = new StandardRegimenField();
		while (tokenizer.hasMoreElements()) {
			String regCode = (String) tokenizer.nextElement();
			RegimenSuggestion rs = getRegimenSuggestionByCode(regCode, allSystemStandardRegimens);
			if (rs != null) {
				options.add(new Option(rs.getDisplayName(), rs.getCodeName(), false));
				srf.addStandardRegimenAnswer(new StandardRegimenAnswer(rs));
				possibleRegimens.add(rs);
			} else {
				throw new IllegalArgumentException("standardRegimen tag can't find regimen code " + regCode
				        + " found in regimenCodes attribute in global property " + STANDARD_REGIMEN_GLOBAL_PROPERTY);
			}
		}
		DropdownWidget dw = new DropdownWidget();
		dw.setOptions(options);
		regWidget = dw;
		context.registerWidget(regWidget);
		regErrorWidget = new ErrorWidget();
		context.registerErrorWidget(regWidget, regErrorWidget);
		
		//start date
		startDateWidget = new DateWidget();
		startDateErrorWidget = new ErrorWidget();
		context.registerWidget(startDateWidget);
		context.registerErrorWidget(startDateWidget, startDateErrorWidget);
		
		//end date
		discontinuedDateWidget = new DateWidget();
		discontinuedDateErrorWidget = new ErrorWidget();
		context.registerWidget(discontinuedDateWidget);
		context.registerErrorWidget(discontinuedDateWidget, discontinuedDateErrorWidget);
		
		//discontinue reasons
		if (parameters.get(FIELD_DISCONTINUED_REASON) != null) {
			String discReasonConceptStr = (String) parameters.get(FIELD_DISCONTINUED_REASON);
			Concept discontineReasonConcept = HtmlFormEntryUtil.getConcept(discReasonConceptStr);
			if (discontineReasonConcept == null)
				throw new IllegalArgumentException(
				        "discontinuedReasonConceptId is not set to a valid conceptId or concept UUID");
			srf.setDiscontinuedReasonQuestion(discontineReasonConcept);
			
			discontinuedReasonWidget = new DropdownWidget();
			discontinuedReasonErrorWidget = new ErrorWidget();
			
			List<Option> discOptions = new ArrayList<Option>();
			discOptions.add(new Option("", "", false));
			
			if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS) != null) {
				//setup a list of the reason concepts
				List<Concept> discReasons = new ArrayList<Concept>();
				String discAnswersString = (String) parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS);
				String[] strDiscAnswers = discAnswersString.split(",");
				for (int i = 0; i < strDiscAnswers.length; i++) {
					String thisAnswer = strDiscAnswers[i];
					Concept answer = HtmlFormEntryUtil.getConcept(thisAnswer,
					    "discontinueReasonAnswers includes a value that is not a valid conceptId or concept UUID");
					discReasons.add(answer);
				}
				
				if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS) != null) {
					// use the listed discontinueReasons, and use labels:
					String discLabelsString = parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS);
					String[] strDiscAnswerLabels = discLabelsString.split(",");
					//a little validation:
					if (strDiscAnswerLabels.length != discReasons.size())
						throw new RuntimeException(
						        "discontinueReasonAnswers and discontinueReasonAnswerLabels must contain the same number of members.");
					for (int i = 0; i < strDiscAnswerLabels.length; i++) {
						discOptions.add(
						    new Option(strDiscAnswerLabels[i], discReasons.get(i).getConceptId().toString(), false));
						srf.addDiscontinuedReasonAnswer(
						    new ObsFieldAnswer(strDiscAnswerLabels[i].trim(), discReasons.get(i)));
					}
				} else {
					// use the listed discontinueReasons, and use their ConceptNames.
					for (Concept c : discReasons) {
						discOptions.add(
						    new Option(c.getBestName(Context.getLocale()).getName(), c.getConceptId().toString(), false));
						srf.addDiscontinuedReasonAnswer(new ObsFieldAnswer(c.getBestName(Context.getLocale()).getName(), c));
					}
				}
			} else {
				//just use the conceptAnswers
				for (ConceptAnswer ca : discontineReasonConcept.getAnswers()) {
					discOptions.add(new Option(ca.getAnswerConcept().getBestName(Context.getLocale()).getName(),
					        ca.getAnswerConcept().getConceptId().toString(), false));
					srf.addDiscontinuedReasonAnswer(new ObsFieldAnswer(
					        ca.getAnswerConcept().getBestName(Context.getLocale()).getName(), ca.getAnswerConcept()));
				}
			}
			if (discOptions.size() == 1)
				throw new IllegalArgumentException("discontinue reason Concept doesn't have any ConceptAnswers");
			
			discontinuedReasonWidget.setOptions(discOptions);
			context.registerWidget(discontinuedReasonWidget);
			context.registerErrorWidget(discontinuedReasonWidget, discontinuedReasonErrorWidget);
		}
		
		createAdditionalWidgets(context, parameters);
		
		if (context.getMode() != Mode.ENTER && context.getExistingOrders() != null) {
			matchStandardRegimenInExistingOrders(context);
		}
		if (regDrugOrders != null && regDrugOrders.size() > 0) {
			String startDateFieldName = (ModuleUtil.compareVersion(OpenmrsConstants.OPENMRS_VERSION_SHORT, "1.10") > -1)
			        ? "dateActivated"
			        : "startDate";
			try {
				startDateWidget.setInitialValue(PropertyUtils.getProperty(regDrugOrders.get(0), startDateFieldName));
			}
			catch (Exception e) {
				throw new APIException(e);
			}
		}
		context.addFieldToActiveSection(srf);
	}
	
	protected abstract void createAdditionalWidgets(FormEntryContext context, Map<String, String> parameters);
	
	protected abstract void matchStandardRegimenInExistingOrders(FormEntryContext context);
	
	protected abstract Date getCommonDiscontinueDate(List<DrugOrder> orders);
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		//if no drug specified, then don't do anything.
		if (regWidget != null && regWidget.getValue(context, submission) != null
		        && !((String) regWidget.getValue(context, submission)).trim().equals("")
		        && !((String) regWidget.getValue(context, submission)).trim().equals("~")) {
			
			try {
				if (startDateWidget != null) {
					Date dateCreated = startDateWidget.getValue(context, submission);
					if (dateCreated == null)
						throw new Exception("htmlformentry.error.required");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(startDateErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (startDateWidget != null && discontinuedDateWidget != null) {
					Date startDate = startDateWidget.getValue(context, submission);
					Date endDate = discontinuedDateWidget.getValue(context, submission);
					if (startDate != null && endDate != null && startDate.getTime() > endDate.getTime())
						throw new Exception("htmlformentry.error.discontinuedDateBeforeStartDate");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(discontinuedDateErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (discontinuedReasonWidget != null && discontinuedDateWidget != null) {
					String discReason = (String) discontinuedReasonWidget.getValue(context, submission);
					Date endDate = discontinuedDateWidget.getValue(context, submission);
					if (endDate == null && !StringUtils.isEmpty(discReason))
						throw new Exception("htmlformentry.error.discontinuedReasonEnteredWithoutDate");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(discontinuedReasonErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
		}
		
		return ret;
	}
	
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		MessageSourceService mss = Context.getMessageSourceService();
		
		if (regWidget != null) {
			ret.append(mss.getMessage("htmlformentry.standardRegimen") + " ");
			ret.append(regWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(regErrorWidget.generateHtml(context));
		}
		
		if (startDateWidget != null) {
			ret.append(" | ");
			ret.append(mss.getMessage("general.dateStart") + " ");
			ret.append(startDateWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(startDateErrorWidget.generateHtml(context));
		}
		//duration and discontinuedDate are now mutually exclusive
		if (discontinuedDateWidget != null) {
			ret.append(mss.getMessage("general.dateDiscontinued") + " ");
			ret.append(discontinuedDateWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(discontinuedDateErrorWidget.generateHtml(context));
		}
		if (discontinuedReasonWidget != null) {
			ret.append(" | " + mss.getMessage("general.discontinuedReason") + " ");
			ret.append(discontinuedReasonWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(discontinuedReasonErrorWidget.generateHtml(context));
		}
		
		return ret.toString();
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		String regCode = null;
		if (regWidget.getValue(session.getContext(), submission) != null)
			regCode = ((String) regWidget.getValue(session.getContext(), submission));
		Date startDate = startDateWidget.getValue(session.getContext(), submission);
		Date discontinuedDate = null;
		if (discontinuedDateWidget != null) {
			discontinuedDate = discontinuedDateWidget.getValue(session.getContext(), submission);
		}
		String discontinuedReasonStr = null;
		if (discontinuedReasonWidget != null) {
			discontinuedReasonStr = (String) discontinuedReasonWidget.getValue(session.getContext(), submission);
		}
		if (!StringUtils.isEmpty(regCode)) {
			if (session.getContext().getMode() == Mode.ENTER
			        || (session.getContext().getMode() == Mode.EDIT && regDrugOrders == null)) {
				enterStandardRegimen(session, submission, regCode, startDate, discontinuedDate, discontinuedReasonStr);
			} else if (session.getContext().getMode() == Mode.EDIT) {
				editStandardRegimen(session, submission, regCode, startDate, discontinuedDate, discontinuedReasonStr);
			}
		} else if (regDrugOrders != null) {
			//void all existing orders in standard regimen -- this is if you un-select an existing standardRegimen
			//these are already part of the encounter, so will be updated when encounter is saved.
			voidDrugOrders(regDrugOrders, session);
		}
	}
	
	protected abstract void editStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr);
	
	protected abstract void enterStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr);
	
	private RegimenSuggestion getRegimenSuggestionByCode(String code, List<RegimenSuggestion> rs) {
		for (RegimenSuggestion r : rs) {
			if (r.getCodeName() != null && r.getCodeName().equals(code))
				return r;
		}
		return null;
	}
	
	protected void voidDrugOrders(List<DrugOrder> dos, FormEntrySession session) {
		for (DrugOrder dor : dos) {
			dor.setVoided(true);
			dor.setVoidedBy(Context.getAuthenticatedUser());
			dor.setVoidReason("Drug De-selected in " + session.getForm().getName());
		}
	}
}
