package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.widget.DrugOrdersWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;

/**
 * Holds the widgets used to represent drug orders for a configured set of drugs, and serves as both
 * the HtmlGeneratorElement and the FormSubmissionControllerAction for the associated drug orders.
 */
public class DrugOrdersSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrdersSubmissionElement.class);
	
	private DrugOrdersWidget drugOrderWidget;
	
	private ErrorWidget drugOrderErrorWidget;
	
	/**
	 * Instantiates a new Drug Order Submission Element, for the given Drug and Context
	 */
	public DrugOrdersSubmissionElement(FormEntryContext context, Map<String, String> parameters, DrugOrderField field) {
		drugOrderWidget = new DrugOrdersWidget(parameters, field);
		drugOrderErrorWidget = new ErrorWidget();
		context.registerWidget(drugOrderWidget);
		context.registerErrorWidget(drugOrderWidget, drugOrderErrorWidget);
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
		html.append(drugOrderWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			html.append(drugOrderErrorWidget.generateHtml(context));
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
		DrugOrder drugOrder = (DrugOrder) drugOrderWidget.getValue(session.getContext(), request);
		// TODO: Handle submission
	}
	
	/**
	 * <strong>Should</strong> return validation errors if any data is invalid
	 * 
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		// TODO: Compare with master
		return ret;
	}
}
