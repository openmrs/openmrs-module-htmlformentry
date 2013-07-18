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
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplayEnterOptionIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") == -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") > -1);
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"\" selected=\"true\">htmlformentry.chooseALocation</option>") > -1);
		Assert.assertTrue(session.getHtmlToDisplay().indexOf("placeholder=\"htmlformentry.form.value.placeholder\"") == -1);
	}

    @Test
    public void encounterLocationTag_shouldSupportDefaultFieldWithAutocomplete() throws Exception {
        String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" default=\"1\" /></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertTrue(session.getHtmlToDisplay().indexOf("<input type=\"text\" id=\"w1\" value=\"Unknown Location\"") > -1);
    }


    @Test
	public void encounterLocationTag_shouldNotSelectAnythingByDefaultIfNothingIsSpecified() throws Exception {
		String htmlform = "<htmlform><encounterLocation /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
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
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>").matcher(session.getHtmlToDisplay());
		Assert.assertTrue(matcher.find());
        Assert.assertTrue(session.getHtmlToDisplay().indexOf("<option value=\"2\" selected=\"true\">Xanadu</option>") > -1);

		//String selectedId = matcher.group(1);
		//Assert.assertEquals("2", selectedId);
	}

    @Test
	public void encounterLocationTag_shouldSupportDefaultSelectByUserProperty() throws Exception {
		String UP_NAME = "kenyaemr.defaultLocation";
		String UP_VALUE = "2";
		Assert.assertNotNull(Context.getLocationService().getLocation(Integer.valueOf(UP_VALUE)));
		Context.getAuthenticatedUser().setUserProperty(UP_NAME, UP_VALUE);
		
		String htmlform = "<htmlform><encounterLocation default=\"UserProperty:" + UP_NAME + "\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);


        TestUtil.assertFuzzyContains("<option value=\"2\" selected=\"true\">", session.getHtmlToDisplay());
	}

    @Test
    public void encounterLocationTag_shouldNotShowRetiredLocations() throws Exception {
        String htmlform = "<htmlform><encounterLocation /></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        TestUtil.assertFuzzyDoesNotContain("<option value=\"3\">Never Never Land</option>", session.getHtmlToDisplay());
    }


    @Test
    public void encounterLocationTag_shouldShowRetiredLocationIfPreviouslySelected() throws Exception {

        // create an encounter associated with a retired location
        Location location = Context.getLocationService().getLocation(3);
        Patient patient = Context.getPatientService().getPatient(2);
        Encounter encounter = Context.getEncounterService().getEncounter(101);

        // (sanity check)
        Assert.assertTrue(location.isRetired());

        // set the location on this encounter to the retired location
        encounter.setLocation(location);

        // now render a form using that encounter
        HtmlForm htmlform = new HtmlForm();
        htmlform.setXmlData( "<htmlform><encounterLocation /></htmlform>");
        FormEntrySession session = new FormEntrySession(patient, encounter, FormEntryContext.Mode.EDIT, htmlform, null);
        TestUtil.assertFuzzyContains("<option value=\"3\" selected=\"true\">Never Never Land</option>", session.getHtmlToDisplay());

    }

    @Test
    public void encounterLocationTag_shouldRestrictToTaggedLocations() throws Exception {
        String htmlform = "<htmlform><encounterLocation tags=\"Some Tag,1002\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);

        TestUtil.assertFuzzyContains("Kigali", session.getHtmlToDisplay());
        TestUtil.assertFuzzyContains("Mirebalais", session.getHtmlToDisplay());
        TestUtil.assertFuzzyContains("Boston", session.getHtmlToDisplay());
        TestUtil.assertFuzzyContains("Scituate", session.getHtmlToDisplay());

        // this location has been retired, so it should not be displayed
        TestUtil.assertFuzzyDoesNotContain("Indianapolis", session.getHtmlToDisplay());

        // should *not* contain any of the options from the standard test dataset (as they are not tagged)
        TestUtil.assertFuzzyDoesNotContain("Unknown Location", session.getHtmlToDisplay());
        TestUtil.assertFuzzyDoesNotContain("Xanadu", session.getHtmlToDisplay());
        TestUtil.assertFuzzyDoesNotContain("Never Never Land", session.getHtmlToDisplay());
    }

    @Test
    public void encounterLocationTag_shouldSortLocationsByDefault() throws Exception {
        String htmlform = "<htmlform><encounterLocation/></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);

        String htmlToDisplay = session.getHtmlToDisplay();

        Assert.assertTrue(htmlToDisplay.indexOf("Boston") < htmlToDisplay.indexOf("Kigali"));
        Assert.assertTrue(htmlToDisplay.indexOf("Kigali") < htmlToDisplay.indexOf("Mirebalais"));
        Assert.assertTrue(htmlToDisplay.indexOf("Mirebalais") < htmlToDisplay.indexOf("Scituate"));
        Assert.assertTrue(htmlToDisplay.indexOf("Scituate") < htmlToDisplay.indexOf("Unknown Location"));
        Assert.assertTrue(htmlToDisplay.indexOf("Unknown Location") < htmlToDisplay.indexOf("Xanadu"));

    }


    @Test
    public void shouldListLocationsInSpecifiedOrderIfOrderAttributePresent() throws Exception {
        String htmlform = "<htmlform><encounterLocation order=\"1002,1003,1004\"/></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);

        String htmlToDisplay = session.getHtmlToDisplay();

        Assert.assertTrue(htmlToDisplay.indexOf("Mirebalais") < htmlToDisplay.indexOf("Indianapolis"));
        Assert.assertTrue(htmlToDisplay.indexOf("Indianapolis") < htmlToDisplay.indexOf("Boston"));
    }

}
