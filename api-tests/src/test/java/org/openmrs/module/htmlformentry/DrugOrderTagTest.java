package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class DrugOrderTagTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagTest.class);
	
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
}
