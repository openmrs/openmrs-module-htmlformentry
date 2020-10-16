package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.widget.DrugOrdersWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.util.OpenmrsUtil;

/**
 * Holds the widgets used to represent drug orders for a configured set of drugs, and serves as both
 * the HtmlGeneratorElement and the FormSubmissionControllerAction for the associated drug orders.
 */
public class DrugOrdersSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrdersSubmissionElement.class);
	
	private DrugOrdersWidget drugOrdersWidget;
	
	private ErrorWidget drugOrdersErrorWidget;
	
	/**
	 * Instantiates a new Drug Order Submission Element, for the given Drug and Context
	 */
	public DrugOrdersSubmissionElement(FormEntryContext context, DrugOrdersWidget drugOrdersWidget) {
		this.drugOrdersWidget = drugOrdersWidget;
		DrugOrderField field = drugOrdersWidget.getDrugOrderField();
		drugOrdersWidget.setInitialValue(context.removeExistingDrugOrders(field));
		drugOrdersErrorWidget = new ErrorWidget();
		context.registerWidget(drugOrdersWidget);
		context.registerErrorWidget(drugOrdersWidget, drugOrdersErrorWidget);
		context.addFieldToActiveSection(field);
	}
	
	/**
	 * <strong>Should</strong> return HTML snippet
	 * 
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder html = new StringBuilder();
		html.append(drugOrdersWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			html.append(drugOrdersErrorWidget.generateHtml(context));
		}
		return html.toString();
	}
	
	/**
	 * For now, handle the following use cases: START: create new order STOP: stop order, create
	 * discontinue order EDIT: void / associate appropriate order and/or discontinue order
	 * 
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Encounter encounter = session.getSubmissionActions().getCurrentEncounter();
		List<DrugOrder> drugOrders = (List<DrugOrder>) drugOrdersWidget.getValue(session.getContext(), request);
		for (DrugOrder drugOrder : drugOrders) {
			if (drugOrder.getOrderer() == null) {
				drugOrder.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(encounter));
			}
			encounter.addOrder(drugOrder);
		}
	}
	
	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		List<DrugOrder> drugOrders = (List<DrugOrder>) drugOrdersWidget.getValue(context, submission);
		for (DrugOrder drugOrder : drugOrders) {
			Order.Action action = drugOrder.getAction();
			DrugOrder previousOrder = (DrugOrder) drugOrder.getPreviousOrder();
			if (action == Order.Action.NEW) {
				// TODO: Add any necessary validation for NEW orders here
			}
			else {
				// If not a new Order, then this must have a previousOrder to operate on
				if (previousOrder == null) {
					addError(ret, drugOrder, "htmlformentry.drugOrderError.invalidAction");
				}
				if (!isSameDrug(drugOrder, previousOrder) && BooleanUtils.isNotTrue(drugOrder.getVoided())) {
					addError(ret, drugOrder, "htmlformentry.drugOrderError.drugChangedForRevision");
				}
				if (action == Order.Action.RENEW) {
					if (dosingInstructionsChanged(drugOrder, previousOrder)) {
						addError(ret, drugOrder, "htmlformentry.drugOrderError.dosingChangedForRenew");
					}
				}
			}
			// TODO: Consider running the DrugOrderValidator here.  Need an Errors implementation to use with it.
		}
		return ret;
	}

	protected boolean isSameDrug(DrugOrder current, DrugOrder previous) {
		boolean ret = false;
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDrug(), previous.getDrug());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDrugNonCoded(), previous.getDrugNonCoded());
		return ret;
	}

	protected boolean dosingInstructionsChanged(DrugOrder current, DrugOrder previous) {
		boolean ret = false;
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDosingType(), previous.getDosingType());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDosingInstructions(), previous.getDosingInstructions());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDose(), previous.getDose());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDoseUnits(), previous.getDoseUnits());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getRoute(), previous.getRoute());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getFrequency(), previous.getFrequency());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getInstructions(), previous.getInstructions());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getAsNeeded(), previous.getAsNeeded());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getAsNeededCondition(), previous.getAsNeededCondition());
		return ret;
	}

	protected void addError(List<FormSubmissionError> errorList, DrugOrder drugOrder, String message) {
		String translatedMessage = Context.getMessageSourceService().getMessage(message);
		FormSubmissionError error = new FormSubmissionError(drugOrder.getDrug().getDisplayName(), translatedMessage);
		errorList.add(error);
	}

	public DrugOrdersWidget getDrugOrdersWidget() {
		return drugOrdersWidget;
	}
	
	public void setDrugOrdersWidget(DrugOrdersWidget drugOrdersWidget) {
		this.drugOrdersWidget = drugOrdersWidget;
	}
	
	public ErrorWidget getDrugOrdersErrorWidget() {
		return drugOrdersErrorWidget;
	}
	
	public void setDrugOrdersErrorWidget(ErrorWidget drugOrdersErrorWidget) {
		this.drugOrdersErrorWidget = drugOrdersErrorWidget;
	}
}
