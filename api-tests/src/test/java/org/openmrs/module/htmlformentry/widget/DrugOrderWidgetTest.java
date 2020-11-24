package org.openmrs.module.htmlformentry.widget;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class DrugOrderWidgetTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderWidgetTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void testDrugOrdersTag_htmlShouldRenderCorrectlyWithDefaultFormValues() {
		FormTester formTester = FormTester.buildForm("drugOrderTestForm.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(2);
		formSessionTester.setEncounterFields("2020-03-30", "2", "502");
		formSessionTester.assertHtmlContains("drugorders-element");
		formSessionTester.assertHtmlContains("drugorders-order-section");
		formSessionTester.assertHtmlContains("drugorders-selector-section");
		formSessionTester.assertHtmlContains("drugorders-order-form");
		formSessionTester.assertStartingFormValue("order-field-label orderType", "1");
		log.trace(formSessionTester.getHtmlToDisplay());
	}
	
	/*
	TODO:
	
	  - Should test that fieldName, fieldName_header, fieldName_orders, fieldName_template are created, with appropriate classes
	- Should ensure that all order properties are part of the template, even if not explicitly configured in the template
	 - Should have a default template?
	 - Should append missing properties to hidden or non-hidden sections depending on requirement
	 - Should have order-field, order-field-label, order-field-widget classes for all properties as appropriate
	 - Should have an appropriate javascript method call with appropriate javascript object
	
	 */
}
