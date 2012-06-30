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
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
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
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplayEnterOptionIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") == -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") > -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}
	
	@Test
	public void encounterLocationTag_shouldNotSelectAnythingByDefaultIfNothingIsSpecified() throws Exception {
		String htmlform = "<htmlform><encounterLocation /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>").matcher(session.getHtmlToDisplay());
		Assert.assertFalse(matcher.find());
	}
	@Test

	public void encounterLocationTag_shouldSupportDefaultSelectyByGlobalProperty() throws Exception {
		String GP_NAME = "kenyaemr.defaultLocation";
		String GP_VALUE = "2";
		Assert.assertNotNull(Context.getLocationService().getLocation(Integer.valueOf(GP_VALUE)));
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(GP_NAME, GP_VALUE));
		
		String htmlform = "<htmlform><encounterLocation default=\"GlobalProperty:" + GP_NAME + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>").matcher(session.getHtmlToDisplay());
		Assert.assertTrue(matcher.find());
        Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"2\" selected=\"true\">Xanadu</option>") > -1);

		//String selectedId = matcher.group(1);
		//Assert.assertEquals("2", selectedId);
	}
	
	public void encounterLocationTag_shouldSupportDefaultSelectyByUserProperty() throws Exception {
		String UP_NAME = "kenyaemr.defaultLocation";
		String UP_VALUE = "2";
		Assert.assertNotNull(Context.getLocationService().getLocation(Integer.valueOf(UP_VALUE)));
		Context.getAuthenticatedUser().setUserProperty(UP_NAME, UP_VALUE);
		
		String htmlform = "<htmlform><encounterLocation default=\"UserProperty:" + UP_NAME + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform);
		
		Matcher matcher = Pattern.compile("<option.+?selected=\"true\".+?value=\"(.+?)\".*?>").matcher(session.getHtmlToDisplay());
		Assert.assertTrue(matcher.find());
		String selectedId = matcher.group(1);
		Assert.assertEquals("2", selectedId);
	}

}
