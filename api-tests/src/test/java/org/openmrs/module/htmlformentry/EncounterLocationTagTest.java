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
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncounterLocationTagTest extends BaseHtmlFormEntryTest {
	
	private static Log log = LogFactory.getLog(EncounterLocationTagTest.class);
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputIfTypeIsNotSpecified() throws Exception {
		String htmlform = "<htmlform><encounterLocation /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">Choose a Location...</option>"));
		Assert.assertFalse(session.getHtmlToDisplay().contains("placeholder=\"Enter......\""));
	}
	
	@Test
	public void encounterLocationTag_shouldDisplayEnterOptionIfTypeIsSetToAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertFalse(
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">Choose a Location...</option>"));
		Assert.assertTrue(session.getHtmlToDisplay().contains("placeholder=\"Enter......\""));
	}
	
	@Test
	public void encounterLocationTag_shouldDisplaySelectInputByDefaultIfAnIvalidTypeValueIsEntered() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"invalid\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(
		    session.getHtmlToDisplay().contains("<option value=\"\" selected=\"true\">Choose a Location...</option>"));
		Assert.assertFalse(session.getHtmlToDisplay().contains("placeholder=\"Enter......\""));
	}
	
	@Test
	public void encounterLocationTag_shouldSupportDefaultFieldWithAutocomplete() throws Exception {
		String htmlform = "<htmlform><encounterLocation type=\"autocomplete\" default=\"1\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Assert.assertTrue(session.getHtmlToDisplay().contains("<input type=\"text\" id=\"w1\" value=\"Unknown Location\""));
	}
	
	@Test
	public void encounterLocationTag_shouldNotSelectAnythingByDefaultIfNothingIsSpecified() throws Exception {
		String htmlform = "<htmlform><encounterLocation /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>")
		        .matcher(session.getHtmlToDisplay());
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
		
		Matcher matcher = Pattern.compile("<option.+?value=\"(.+?)\".+?selected=\"true\".*?>")
		        .matcher(session.getHtmlToDisplay());
		Assert.assertTrue(matcher.find());
		Assert.assertTrue(session.getHtmlToDisplay().contains("<option value=\"2\" selected=\"true\">Xanadu</option>"));
	}
	
	@Test
	public void encounterLocationTag_shouldSupportDefaultSelectByDefaultLocation() throws Exception {
		
		String htmlform = "<htmlform><encounterLocation default=\"SystemDefault\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		log.debug(session.getHtmlToDisplay());
		TestUtil.assertFuzzyContains("<option value=\"1\" selected=\"true\">Unknown Location</option>",
		    session.getHtmlToDisplay());
		
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
		htmlform.setXmlData("<htmlform><encounterLocation /></htmlform>");
		FormEntrySession session = new FormEntrySession(patient, encounter, FormEntryContext.Mode.EDIT, htmlform, null);
		TestUtil.assertFuzzyContains("<option value=\"3\" selected=\"true\">Never Never Land</option>",
		    session.getHtmlToDisplay());
		
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
	
	@Test
	public void shouldRestrictToVisitLocationAndChildrenIfRestrictToVisitLocationEnabledAndVisitInContext()
	        throws Exception {
		
		// create mock visit with Location = Boston
		Visit visit = new Visit();
		visit.setLocation(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4588-8f2b-2361b3446eb8"));
		
		String htmlform = "<htmlform><encounterLocation restrictToCurrentVisitLocation=\"true\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		session.getContext().setVisit(visit);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		
		TestUtil.assertFuzzyContains("Boston", htmlToDisplay);
		TestUtil.assertFuzzyContains("Jamaica Plain", htmlToDisplay);
		
		TestUtil.assertFuzzyDoesNotContain("Kigali", htmlToDisplay);
		TestUtil.assertFuzzyDoesNotContain("Mirebalais", htmlToDisplay);
		TestUtil.assertFuzzyDoesNotContain("Scituate", htmlToDisplay);
	}
	
	@Test
	public void shouldRestrictToVisitLocationAndChildrenIfRestrictToVisitLocationEnabledButNoVisitInContext()
	        throws Exception {
		
		String htmlform = "<htmlform><encounterLocation restrictToCurrentVisitLocation=\"true\"/></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		
		TestUtil.assertFuzzyContains("Kigali", htmlToDisplay);
		TestUtil.assertFuzzyContains("Mirebalais", htmlToDisplay);
		TestUtil.assertFuzzyContains("Boston", htmlToDisplay);
		TestUtil.assertFuzzyContains("Scituate", htmlToDisplay);
		TestUtil.assertFuzzyContains("Jamaica Plain", htmlToDisplay);
	}
	
	@Test
	public void getAllVisitsAndChildLocations_shouldReturnAllVisitsAndTheirChildLocations() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/encounterLocationTest.xml");
		Set<Location> traversedLocations = new HashSet<Location>();
		Set<Location> allVisitLocations = new HashSet<Location>();
		LocationTag visitLocationTag = HtmlFormEntryUtil.getLocationTag("Visit Location");
		List<Location> visitLocations = Context.getLocationService().getLocationsByTag(visitLocationTag);
		allVisitLocations.addAll(visitLocations);
		assertThat(
		    EncounterDetailSubmissionElement.getAllVisitsAndChildLocations(allVisitLocations, traversedLocations).size(),
		    is(10));
		
	}
	
}
