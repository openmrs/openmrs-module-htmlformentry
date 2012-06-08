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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;

public class EncounterLocationTagTest extends BaseModuleContextSensitiveTest {
	
	public static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	public static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		LogicUtil.registerDefaultRules();
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputIfTypeIsNotSpecified() throws Exception {
		String htmlform = "<htmlform><encounterLocation /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("$j('input#display_w1').autocomplete(") == -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplayInputWithAutocompleteIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\">htmlformentry.chooseALocation</option>") == -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("$j('input#display_w1').autocomplete(") > -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("$j('input#display_w1').autocomplete(") == -1);
	}
	
}
