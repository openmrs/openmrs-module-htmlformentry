/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests the obs tag.
 */
public class ObsTagTest extends BaseModuleContextSensitiveTest {
	
	private Patient patient;
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
	}
	
	@Test
	public void shouldSetDefaultNumericValue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"2\" defaultValue=\"60\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"60.0\""));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultNumericValueIsInvalid() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"2\" defaultValue=\"invalidNumber\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultTextValue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"8\" defaultValue=\"sometext\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("value=\"sometext\""));
	}
	
	@Test
	public void shouldSetDefaultCodedValue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"1001\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"1001\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsInvalid() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"invalidValue\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultCodedValueIsNotAllowedAnswer() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1000\" defaultValue=\"2\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToTrue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"true\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"true\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToFalse() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"false\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"false\" selected=\"true\">"));
	}
	
	@Test
	public void shouldSetDefaultBooleanValueToNone() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"\" style=\"no_yes_dropdown\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(),
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultBooleanValueIsInvalid() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"4\" defaultValue=\"yes\" style=\"no_yes_dropdown\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
	@Test
	public void shouldSetDefaultDateValue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test
	public void shouldSetDefaultDatetimeValue() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertTrue("Result: " + session.getHtmlToDisplay(), session.getHtmlToDisplay().contains("2011-02-02"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfDefaultDateAndDefaultDatetimeSet() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><obs conceptId=\"1119\" defaultValue=\"2011-02-02-00-00\" defaultDatetime=\"2011-02-02-00-00\"/></htmlform>";
		new FormEntrySession(patient, htmlform);
	}
	
}
