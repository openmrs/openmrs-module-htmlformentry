package org.openmrs.module.htmlformentry.element;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.regimen.RegimenUtil1_10;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.util.OpenmrsUtil;

public class StandardRegimenElement1_10 extends StandardRegimenElement {
	
	private DropdownWidget careSettingWidget;
	
	public StandardRegimenElement1_10(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	@Override
	protected void createAdditionalWidgets(FormEntryContext context, Map<String, String> parameters) {
		//We support inpatient care settings only since numberOfRefills and quantity cannot
		//be specified for DrugSuggestion
		careSettingWidget = DrugOrderSubmissionElement1_10.createCareSettingWidget(context, true);
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		MessageSourceService mss = Context.getMessageSourceService();
		String html = super.generateHtml(context);
		html += DrugOrderSubmissionElement.generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.careSetting") + " ",
		    careSettingWidget, null);
		return html;
	}
	
	@Override
	protected void matchStandardRegimenInExistingOrders(FormEntryContext context) {
		Map<RegimenSuggestion, List<DrugOrder>> map = RegimenUtil1_10.findStrongestStandardRegimenInDrugOrders(
		    possibleRegimens, context.getRemainingExistingOrders());
		if (map.size() == 1) {
			existingStandardRegimen = map.keySet().iterator().next();
			for (DrugOrder dor : map.get(existingStandardRegimen)) {
				regDrugOrders.add(context.removeExistingDrugOrder(dor.getDrug()));
				regWidget.setInitialValue(existingStandardRegimen.getCodeName());
				careSettingWidget.setInitialValue(dor.getCareSetting());
			}
			discontinuedDateWidget.setInitialValue(getCommonDiscontinueDate(regDrugOrders));
			Order discontinuationOrder = Context.getOrderService().getDiscontinuationOrder(regDrugOrders.get(0));
			if (discontinuedReasonWidget != null && discontinuationOrder != null)
				discontinuedReasonWidget.setInitialValue(discontinuationOrder.getOrderReason().getConceptId());
		}
	}
	
	@Override
	protected Date getCommonDiscontinueDate(List<DrugOrder> orders) {
		Date candidate = null;
		if (orders != null & orders.size() > 0)
			candidate = orders.get(0).getDateStopped();
		for (Order o : orders) {
			if (!OpenmrsUtil.nullSafeEquals(o.getDateStopped(), candidate))
				return null;
		}
		return candidate;
	}
	
	@Override
	protected void enterStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr) {
		RegimenSuggestion rs = RegimenUtil1_10.getStandardRegimenByCode(possibleRegimens, regCode);
		//create new drugOrders
		Set<Order> ords = RegimenUtil1_10.standardRegimenToDrugOrders(rs, startDate, session.getPatient());
		for (Order o : ords) {
			if (o.getDateCreated() == null)
				o.setDateCreated(new Date());
			if (o.getCreator() == null)
				o.setCreator(Context.getAuthenticatedUser());
			if (o.getUuid() == null)
				o.setUuid(UUID.randomUUID().toString());
			Order discontinuationOrder = null;
			if (discontinuedDate != null) {
				discontinuationOrder = newDiscontinuationOrder(discontinuedDate, discontinuedReasonStr, o);
			}
			if (o.getOrderer() == null) {
				setOrderer(o, session);
			}
			if (o.getCareSetting() == null) {
				CareSetting careSetting = getCareSetting(session, submission);
				o.setCareSetting(careSetting);
			}
			session.getSubmissionActions().getCurrentEncounter().addOrder(o);
			if (discontinuationOrder != null) {
				session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
			}
		}
	}
	
	private CareSetting getCareSetting(FormEntrySession session, HttpServletRequest submission) {
		Integer careSettingId = Integer.valueOf((String) careSettingWidget.getValue(session.getContext(), submission));
		CareSetting careSetting = Context.getOrderService().getCareSetting(careSettingId);
		return careSetting;
	}
	
	private void setOrderer(Order o, FormEntrySession session) {
		Set<EncounterProvider> encounterProviders = session.getSubmissionActions().getCurrentEncounter()
		        .getEncounterProviders();
		for (EncounterProvider encounterProvider : encounterProviders) {
			if (!encounterProvider.isVoided()) {
				o.setOrderer(encounterProvider.getProvider());
				break;
			}
		}
	}
	
	@Override
	protected void editStandardRegimen(FormEntrySession session, HttpServletRequest submission, String regCode,
	        Date startDate, Date discontinuedDate, String discontinuedReasonStr) {
		//TODO: change this to act more as DrugOrderSubmissionElement1_10 and instead of
		//voiding drug orders revise them or discontinue.
		voidDrugOrders(regDrugOrders, session);
		RegimenSuggestion rs = RegimenUtil1_10.getStandardRegimenByCode(possibleRegimens, regCode);
		Set<Order> ords = RegimenUtil1_10.standardRegimenToDrugOrders(rs, startDate, session.getPatient());
		for (Order o : ords) {
			if (o.getDateCreated() == null)
				o.setDateCreated(new Date());
			if (o.getCreator() == null)
				o.setCreator(Context.getAuthenticatedUser());
			if (o.getUuid() == null)
				o.setUuid(UUID.randomUUID().toString());
			if (o.getOrderer() == null) {
				setOrderer(o, session);
			}
			if (o.getCareSetting() == null) {
				CareSetting careSetting = getCareSetting(session, submission);
				o.setCareSetting(careSetting);
			}
			
			Order discontinuationOrder = null;
			if (discontinuedDate != null) {
				discontinuationOrder = newDiscontinuationOrder(discontinuedDate, discontinuedReasonStr, o);
			}
			session.getSubmissionActions().getCurrentEncounter().addOrder(o);
			if (discontinuationOrder != null) {
				session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
			}
		}
	}
	
	private Order newDiscontinuationOrder(Date discontinuedDate, String discontinuedReasonStr, Order o) {
		Order discontinuationOrder;
		discontinuationOrder = o.cloneForDiscontinuing();
		discontinuationOrder.setDateActivated(discontinuedDate);
		if (!StringUtils.isEmpty(discontinuedReasonStr)) {
			discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
		}
		return discontinuationOrder;
	}
}
