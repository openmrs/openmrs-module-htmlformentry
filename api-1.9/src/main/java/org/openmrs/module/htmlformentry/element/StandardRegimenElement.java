package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.compatibility.DrugOrderCompatibility;
import org.openmrs.module.htmlformentry.regimen.StandardRegimenUtil;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.util.OpenmrsUtil;

/**
 * @author dthomas NOTE: the routines here WON'T change regimens if you change the definitions of a
 *         regimen in the XML automatically.
 */
public class StandardRegimenElement extends AbstractStandardRegimenElement {
	
	public StandardRegimenElement(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	@Override
	protected void createAdditionalWidgets(FormEntryContext context, Map<String, String> parameters) {
	}
	
	@Override
	protected void matchStandardRegimenInExistingOrders(FormEntryContext context) {
		DrugOrderCompatibility compat = Context.getRegisteredComponents(DrugOrderCompatibility.class).get(0);
		Map<RegimenSuggestion, List<DrugOrder>> map = StandardRegimenUtil
		        .findStrongestStandardRegimenInDrugOrders(possibleRegimens, context.getRemainingExistingOrders());
		if (map.size() == 1) {
			existingStandardRegimen = map.keySet().iterator().next();
			for (DrugOrder dor : map.get(existingStandardRegimen)) {
				regDrugOrders.add(context.removeExistingDrugOrder(dor.getDrug()));
				regWidget.setInitialValue(existingStandardRegimen.getCodeName());
			}
			discontinuedDateWidget.setInitialValue(getCommonDiscontinueDate(regDrugOrders));
			if (discontinuedReasonWidget != null && regDrugOrders.get(0).getDiscontinuedReason() != null)
				discontinuedReasonWidget.setInitialValue(regDrugOrders.get(0).getDiscontinuedReason().getConceptId());
		}
	}
	
	protected Date getCommonDiscontinueDate(List<DrugOrder> orders) {
		Date candidate = null;
		if (orders != null & orders.size() > 0)
			candidate = orders.get(0).getDiscontinuedDate();
		for (Order o : orders) {
			if (!OpenmrsUtil.nullSafeEquals(o.getDiscontinuedDate(), candidate))
				return null;
		}
		return candidate;
	}
	
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
	
	@Override
	protected void editStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr) {
		if (existingStandardRegimen != null && regCode.equals(existingStandardRegimen.getCodeName())) {
			//the drug orders are already there and attached to the encounter.
			for (Order o : regDrugOrders) {
				
				if (!StringUtils.isEmpty(discontinuedReasonStr))
					o.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
				if (discontinuedDate != null) {
					o.setDiscontinuedDate(discontinuedDate);
					o.setDiscontinued(true);
				}
				o.setStartDate(startDate);
			}
		} else {
			//standard regimen changed in the drop-down...  I'm going to have this void the old DrugOrders, and create new ones.
			voidDrugOrders(regDrugOrders, session);
			enterStandardRegimen(session, submission, regCode, startDate, discontinuedDate, discontinuedReasonStr);
		}
	}
	
	@Override
	protected void enterStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr) {
		RegimenSuggestion rs = StandardRegimenUtil.getStandardRegimenByCode(possibleRegimens, regCode);
		//create new drugOrders
		Set<Order> ords = StandardRegimenUtil.standardRegimenToDrugOrders(rs, startDate, session.getPatient());
		for (Order o : ords) {
			if (o.getDateCreated() == null)
				o.setDateCreated(new Date());
			if (o.getCreator() == null)
				o.setCreator(Context.getAuthenticatedUser());
			if (o.getUuid() == null)
				o.setUuid(UUID.randomUUID().toString());
			if (!StringUtils.isEmpty(discontinuedReasonStr))
				o.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
			if (discontinuedDate != null) {
				o.setDiscontinuedDate(discontinuedDate);
				o.setDiscontinued(true);
				o.setDiscontinuedBy(Context.getAuthenticatedUser());
			}
			session.getSubmissionActions().getCurrentEncounter().addOrder(o);
		}
	}
}
