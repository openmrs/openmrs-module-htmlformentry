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

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;

public class EncounterTypeTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void encounterTypeTag_shouldIncludeOnlyTheSpecifiedEncounterTypesOptions() throws Exception {
		String htmlform = "<htmlform><encounterType types=\"1\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertTrue(html.contains("<option value=\"\">Choose...</option>"));
		Assert.assertEquals(2, StringUtils.countMatches(session.getHtmlToDisplay(), "<option value=\""));
	}
	
	@Test
	public void encounterTypeTag_shouldIncludeAllEncounterTypesIfNoneAreSpecified() throws Exception {
		String htmlform = "<htmlform><encounterType/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertTrue(html.contains("<option value=\"\">Choose...</option>"));
		int numExpected = Context.getEncounterService().getAllEncounterTypes().size() + 1;
		Assert.assertEquals(numExpected, StringUtils.countMatches(session.getHtmlToDisplay(), "<option value=\""));
	}
	
	@Test
	public void encounterTypeTag_shouldAutoSelectTheDefaultEncounterTypeIfAny() throws Exception {
		String htmlform = "<htmlform><encounterType default=\"07000be2-26b6-4cce-8b40-866d8435b613\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertTrue(html.contains("<option value=\"\">Choose...</option>"));
		Assert.assertTrue(session.getHtmlToDisplay().contains("<option selected=\"true\" value=\"2\">Emergency</option>"));
	}
}
