package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;


public class EncounterTagsTest extends BaseModuleContextSensitiveTest {
	
	public static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	public static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	private Patient patient;
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
		LogicUtil.registerDefaultRules();
	}
	
	@Test
	public void shouldDisplayDatePropertyAccessor() throws Exception {
	String htmlform = "<htmlform><encounterDate id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}
	
	@Test
	public void shouldDisplayProviderValuePropertyAccessor() throws Exception {
	String htmlform = "<htmlform><encounterProvider id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}
	
	@Test
	public void shouldDisplayLocationValuePropertyAccessor() throws Exception {
	String htmlform = "<htmlform><encounterLocation id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}
	
}
