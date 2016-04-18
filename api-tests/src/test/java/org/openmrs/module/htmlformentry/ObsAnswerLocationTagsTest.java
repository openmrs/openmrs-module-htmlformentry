package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class ObsAnswerLocationTagsTest extends BaseModuleContextSensitiveTest {

    public static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

    public static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    @Before
    public void before() throws Exception {
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
        LogicUtil.registerDefaultRules();
    }

    @Test
    public void obsAnswerLocationTags_shouldRestrictToTaggedLocations() throws Exception {
        String htmlform = "<htmlform><obs conceptId=\"6\" style=\"location\" answerLocationTags=\"Some Tag,1002\" /></htmlform>";
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
    public void obsAnswerLocationTags_shouldDisplayAllLocations() throws Exception {
        String htmlform = "<htmlform><obs conceptId=\"6\" style=\"location\" /></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);

        String htmlToDisplay = session.getHtmlToDisplay();
        assertContainsAllLocations(htmlToDisplay);
    }

    @Test
    public void obsAnswerLocationTags0_shouldDisplayAllLocations() throws Exception {
        String htmlform = "<htmlform><obs conceptId=\"6\" style=\"location\" answerLocationTags=\"\" /></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);

        String htmlToDisplay = session.getHtmlToDisplay();
        assertContainsAllLocations(htmlToDisplay);
    }

    @Test
    public void obsAnswerLocationTagsEmpty_shouldDisplayAllLocations() throws Exception {
        String htmlform = "<htmlform><obs conceptId=\"6\" style=\"location\" answerLocationTags=\",,\" /></htmlform>";
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
