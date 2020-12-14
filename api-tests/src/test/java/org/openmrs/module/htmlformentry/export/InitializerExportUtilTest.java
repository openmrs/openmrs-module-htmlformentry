package org.openmrs.module.htmlformentry.export;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class InitializerExportUtilTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void testNoSubstitutions() throws Exception {
		HtmlForm testForm = FormTester.buildForm("htmlFormWithComments.xml").saveForm();
		String xml = InitializerExportUtil.getXmlForInitializer(testForm, false);
		assertThat(xml, containsString("<htmlform "));
		assertThat(xml, containsString("<!-- Inline comments -->"));
		assertThat(xml, containsString("htmlformUuid=\"" + testForm.getUuid() + "\""));
		assertThat(xml, containsString("formName=\"Test form\""));
		assertThat(xml, containsString("formUuid=\"d9218f76-6c39-45f4-8efa-4c5c6c199f50\""));
		assertThat(xml, containsString("formDescription=\"A form for testing\""));
		assertThat(xml, containsString("formVersion=\"1.0\""));
		assertThat(xml, containsString("formEncounterType=\"61ae96f4-6afe-4351-b6f8-cd4fc383cce1\""));
		assertThat(xml, containsString("formPublished=\"true\""));
		assertThat(xml, containsString("formRetired=\"false\""));
	}
	
}
