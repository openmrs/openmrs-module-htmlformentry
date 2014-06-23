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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class EncounterProviderTagTest extends BaseModuleContextSensitiveTest {

    public static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	public static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    @Before
	public void before() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		LogicUtil.registerDefaultRules();
	}

    @Test
	public void encounterProviderTag_shouldDisplaySelectInputIfTypeIsNotSpecified() throws Exception {
		String htmlform = "<htmlform><encounterProvider/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseAProvider</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}

    @Test
	public void encounterProviderTag_shouldDisplayEnterOptionIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterProvider type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseAProvider</option>") == -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") > -1);
	}

    @Test
	public void encounterProviderTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterProvider type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseAProvider</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}

    @Test
    public void encounterProviderTag_shouldSupportDefaultFieldWithAutocomplete() throws Exception {
        String htmlform = "<htmlform><encounterProvider type=\"autocomplete\" default=\"502\" /></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        TestUtil.assertFuzzyContains("<input type=\"text\" id=\"w1\" value=\"Hippocrates of Cos\"",session.getHtmlToDisplay());

    }

    @Test
	public void encounterProviderTag_shouldNotSelectAnythingByDefaultIfNothingIsSpecified() throws Exception {
		String htmlform = "<htmlform><encounterProvider/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);

		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>").matcher(session.getHtmlToDisplay());
		Assert.assertFalse(matcher.find());
	}

}

