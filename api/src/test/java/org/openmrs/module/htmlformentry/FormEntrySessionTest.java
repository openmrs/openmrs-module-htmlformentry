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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class FormEntrySessionTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	private Patient patient = null;
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
	}
	
	/**
	 * @see {@link FormEntrySession#createForm(String)}
	 */
	@Test
	@Verifies(value = "should return correct xml with a greater than character in an excludeIf tag", method = "createForm(String)")
	public void createForm_shouldReturnCorrectXmlWithAGreaterThanCharacterInAnExcludeIfTag() throws Exception {
		Assert.assertEquals(37, patient.getAge().intValue());
		String expectedText = "Patient is atleast 37";
		String htmlform = "<htmlform><includeIf velocityTest=\"$patient.age >= 37\">" + expectedText
		        + "</includeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">Patient is atleast 37</div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link FormEntrySession#createForm(String)}
	 */
	@Test
	@Verifies(value = "should return correct xml with a greater than character in an includeIf tag", method = "createForm(String)")
	public void createForm_shouldReturnCorrectXmlWithAGreaterThanCharacterInAnIncludeIfTag() throws Exception {
		Assert.assertEquals(37, patient.getAge().intValue());
		String expectedText = "Patient is atleast 37";
		String htmlform = "<htmlform><excludeIf velocityTest=\"$patient.age >= 37\">" + expectedText
		        + "</excludeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\"></div>", session.getHtmlToDisplay());
	}
}
