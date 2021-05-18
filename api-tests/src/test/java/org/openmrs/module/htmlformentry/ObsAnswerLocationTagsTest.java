package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;

public class ObsAnswerLocationTagsTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void obsAnswerLocationTags_shouldRestrictToTaggedLocations() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"60000\" style=\"location\" answerLocationTags=\"Some Tag,1002\"  /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("Kigali", htmlToDisplay);
		TestUtil.assertFuzzyContains("Mirebalais", htmlToDisplay);
		
		// this location has been retired, so it should not be displayed
		TestUtil.assertFuzzyDoesNotContain("Indianapolis", htmlToDisplay);
		
		// should *not* contain Lacolline
		TestUtil.assertFuzzyDoesNotContain("Lacolline", session.getHtmlToDisplay());
		
		// should *not* contain any of the options from the standard test dataset (as they are not tagged)
		TestUtil.assertFuzzyDoesNotContain("Unknown Location", session.getHtmlToDisplay());
		TestUtil.assertFuzzyDoesNotContain("Xanadu", session.getHtmlToDisplay());
		TestUtil.assertFuzzyDoesNotContain("Never Never Land", session.getHtmlToDisplay());
		
	}
	
	@Test
	public void obsAnswerLocationTags_shouldRestrictToVisitLocationsAndChildren() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"60000\" style=\"location\" restrictToCurrentVisitLocation=\"true\"  /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		Visit visit = new Visit();
		visit.setLocation(Context.getLocationService().getLocation("Boston"));
		session.getContext().setVisit(visit);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		TestUtil.assertFuzzyContains("Boston", htmlToDisplay);
		TestUtil.assertFuzzyContains("Jamaica Plain", htmlToDisplay);
		
		TestUtil.assertFuzzyDoesNotContain("Indianapolis", htmlToDisplay);
		TestUtil.assertFuzzyDoesNotContain("Mirebaials", htmlToDisplay);
		TestUtil.assertFuzzyDoesNotContain("Scituate", htmlToDisplay);
		TestUtil.assertFuzzyDoesNotContain("Lacolline", htmlToDisplay);
		
	}
	
	@Test
	public void obsAnswerLocationTags_shouldDisplayAllLocations() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"60000\" style=\"location\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		assertContainsAllLocations(htmlToDisplay);
	}
	
	@Test
	public void obsAnswerLocationTags0_shouldDisplayAllLocations() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"60000\" style=\"location\" answerLocationTags=\"\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		assertContainsAllLocations(htmlToDisplay);
	}
	
	@Test
	public void obsAnswerLocationTagsEmpty_shouldDisplayAllLocations() throws Exception {
		String htmlform = "<htmlform><obs conceptId=\"60000\" style=\"location\" answerLocationTags=\",,\" /></htmlform>";
		FormEntrySession session = new FormEntrySession(null, htmlform, null);
		
		String htmlToDisplay = session.getHtmlToDisplay();
		assertContainsAllLocations(htmlToDisplay);
	}
	
	private void assertContainsAllLocations(String htmlToDisplay) {
		TestUtil.assertFuzzyContains("Kigali", htmlToDisplay);
		TestUtil.assertFuzzyContains("Mirebalais", htmlToDisplay);
		TestUtil.assertFuzzyContains("Indianapolis", htmlToDisplay);
		TestUtil.assertFuzzyContains("Boston", htmlToDisplay);
		TestUtil.assertFuzzyContains("Scituate", htmlToDisplay);
		TestUtil.assertFuzzyContains("Lacolline", htmlToDisplay);
	}
}
