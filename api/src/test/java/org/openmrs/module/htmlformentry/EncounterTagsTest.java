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
        String htmlform = "<htmlform><encounterDate/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterDate.value'\\]", session.getFieldAccessorJavascript());
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterDate.error'\\]", session.getFieldAccessorJavascript());
    }

    @Test
    public void shouldDisplayProviderValuePropertyAccessor() throws Exception {
        String htmlform = "<htmlform><encounterProvider/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterProvider.value'\\]", session.getFieldAccessorJavascript());
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterProvider.error'\\]", session.getFieldAccessorJavascript());
    }

    @Test
    public void shouldDisplayLocationValuePropertyAccessor() throws Exception {
        String htmlform = "<htmlform><encounterLocation/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterLocation.value'\\]", session.getFieldAccessorJavascript());
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterLocation.error'\\]", session.getFieldAccessorJavascript());
    }

    @Test
    public void shouldDisplayEncounterTypeValue() throws Exception {
        String htmlform = "<htmlform><encounterType/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterType.value'\\]", session.getFieldAccessorJavascript());
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['encounterType.error'\\]", session.getFieldAccessorJavascript());
    }

	@Test
	public void shouldDisplayDatePropertyAccessorUsingCustomName() throws Exception {
	String htmlform = "<htmlform><encounterDate id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}

	@Test
	public void shouldDisplayProviderValuePropertyAccessorUsingCustomName() throws Exception {
	String htmlform = "<htmlform><encounterProvider id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}

	@Test
	public void shouldDisplayLocationValuePropertyAccessorUsingCustomName() throws Exception {
	String htmlform = "<htmlform><encounterLocation id=\"myEncounter\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
		TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
	}

    @Test
    public void shouldDisplayEncounterTypeValuePropertyAccessorUsingCustomName() throws Exception {
        String htmlform = "<htmlform><encounterType id=\"myEncounter\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform, null);
        session.getHtmlToDisplay();
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.value'\\]", session.getFieldAccessorJavascript());
        TestUtil.assertFuzzyContains("propertyAccessorInfo\\['myEncounter.error'\\]", session.getFieldAccessorJavascript());
    }
	
}
