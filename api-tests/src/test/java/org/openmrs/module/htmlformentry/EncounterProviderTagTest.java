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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncounterProviderTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void encounterProviderTag_shouldDisplaySelectInputIfTypeIsNotSpecified() throws Exception {
		String htmlform = "<htmlform><encounterProvider/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		Assert.assertTrue(htmlToDisplay.contains("<option value=\"\" selected=\"true\">Choose a Provider</option>"));
		Assert.assertFalse(htmlToDisplay.contains("placeholder=\"htmlformentry.form.value.placeholder\""));
	}
	
	@Test
	public void encounterProviderTag_shouldDisplayEnterOptionIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterProvider type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		Assert.assertFalse(htmlToDisplay.contains("<option value=\"\" selected=\"true\">Choose a Provider</option>"));
		Assert.assertTrue(htmlToDisplay.contains("placeholder=\"Enter......\""));
	}
	
	@Test
	public void encounterProviderTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterProvider type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		String htmlToDisplay = session.getHtmlToDisplay();
		Assert.assertTrue(htmlToDisplay.contains("<option value=\"\" selected=\"true\">Choose a Provider</option>"));
		Assert.assertFalse(htmlToDisplay.contains("placeholder=\"htmlformentry.form.value.placeholder\""));
	}
	
	@Test
	public void encounterProviderTag_shouldSupportDefaultFieldWithAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterProvider type=\"autocomplete\" default=\"502\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		TestUtil.assertFuzzyContains("<input type=\"text\" id=\"w1\" value=\"Hippocrates of Cos\"",
		    session.getHtmlToDisplay());
		
	}
	
	@Test
	public void encounterProviderTag_shouldNotSelectAnythingByDefaultIfNothingIsSpecified() throws Exception {
		String htmlform = "<htmlform><encounterProvider/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>")
		        .matcher(session.getHtmlToDisplay());
		Assert.assertFalse(matcher.find());
	}
	
}
