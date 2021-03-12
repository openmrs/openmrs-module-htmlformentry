package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.OrderType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.OrderSubmissionElement;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;
import org.openmrs.module.htmlformentry.widget.OrderWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

/**
 * Tests that the DrugOrderTagHandler successfully parses the XML and sets the appropriate order
 * type
 */
public class DrugOrderTagHandlerTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagHandlerTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void shouldAddOrderWidgetWithImpliedOrderType() {
		List<OrderWidget> widgets = getDrugOrderWidgets("orderTestForm.xml");
		assertThat(widgets.size(), is(1));
		OrderWidget widget = widgets.get(0);
		assertThat(widget.getOrderField().getOrderType().getUuid(), is("131168f4-15f5-102d-96e4-000c29c2a5d7"));
	}
	
	@Test
	public void shouldAddOrderSubmissionElement() {
		assertThat(getOrderSubmissionElements("orderTestForm.xml").size(), is(1));
	}
	
	@Test
	public void shouldFailIfNoOrderTypeIsSpecifiedAndMultipleDrugOrderTypesExist() {
		OrderType newDrugOrderType = new OrderType();
		newDrugOrderType.setName("Another drug order type");
		newDrugOrderType.setJavaClassName("org.openmrs.DrugOrder");
		Context.getOrderService().saveOrderType(newDrugOrderType);
		FormTester formTester = FormTester.buildForm("orderTestForm.xml");
		FormSessionTester fst = formTester.openNewForm(6);
		fst.assertHtmlContains("org.openmrs.module.htmlformentry.BadFormDesignException");
	}
	
	@Test
	public void shouldAllowExplicitOrderTypeToBeSpecified() {
		OrderType newDrugOrderType = new OrderType();
		newDrugOrderType.setName("Another drug order type");
		newDrugOrderType.setJavaClassName("org.openmrs.DrugOrder");
		Context.getOrderService().saveOrderType(newDrugOrderType);
		List<OrderWidget> widgets = getDrugOrderWidgets("orderTestFormAllDefaults.xml");
		assertThat(widgets.size(), is(1));
		OrderWidget widget = widgets.get(0);
		assertThat(widget.getOrderField().getOrderType().getUuid(), is("131168f4-15f5-102d-96e4-000c29c2a5d7"));
	}
	
	private List<OrderWidget> getDrugOrderWidgets(String form) {
		List<OrderWidget> ret = new ArrayList<>();
		FormTester formTester = FormTester.buildForm(form);
		FormSessionTester fst = formTester.openNewForm(6);
		assertNoBadFormDesignException(fst);
		for (Widget widget : fst.getFormEntrySession().getContext().getFieldNames().keySet()) {
			if (widget instanceof OrderWidget) {
				ret.add((OrderWidget) widget);
			}
		}
		return ret;
	}
	
	private List<OrderSubmissionElement> getOrderSubmissionElements(String form) {
		List<OrderSubmissionElement> ret = new ArrayList<>();
		FormTester formTester = FormTester.buildForm(form);
		FormSessionTester fst = formTester.openNewForm(6);
		assertNoBadFormDesignException(fst);
		for (FormSubmissionControllerAction action : fst.getFormEntrySession().getSubmissionController().getActions()) {
			if (action instanceof OrderSubmissionElement) {
				ret.add((OrderSubmissionElement) action);
			}
		}
		return ret;
	}
	
	private void assertAttribute(OrderWidget widget, String property, String attributeName, String value) {
		assertThat(widget.getWidgetConfig().getAttributes(property).get(attributeName), is(value));
	}
	
	private void assertNoBadFormDesignException(FormSessionTester fst) {
		fst.assertHtmlDoesNotContain("org.openmrs.module.htmlformentry.BadFormDesignException");
	}
}
