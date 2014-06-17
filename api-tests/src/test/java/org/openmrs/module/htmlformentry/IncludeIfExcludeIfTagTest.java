package org.openmrs.module.htmlformentry;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class IncludeIfExcludeIfTagTest extends BaseModuleContextSensitiveTest {

    @Test
    public void singleIncludeIf_shouldExcludeText() throws Exception {
        String htmlform = "<htmlform><includeIf velocityTest=\"false\">excluded</includeIf></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\"></div>", session.getHtmlToDisplay());
    }

    @Test
    public void singleIncludeIf_shouldIncludeText() throws Exception {
        String htmlform = "<htmlform><includeIf velocityTest=\"true\">included</includeIf></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\">included</div>", session.getHtmlToDisplay());
    }

    @Test
    public void singleExcludeIf_shouldExcludeText() throws Exception {
        String htmlform = "<htmlform><excludeIf velocityTest=\"true\">excluded</excludeIf></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\"></div>", session.getHtmlToDisplay());
    }

    @Test
    public void doubleNestedIncludeIf_shouldBeProcessedCorrectly() throws Exception {
        String htmlform = "<htmlform>" +
                "<includeIf velocityTest=\"false\">" +
                "<includeIf velocityTest=\"true\">" +
                "included" +
                "</includeIf>" +
                "</includeIf>" +
                "</htmlform>";

        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\"></div>", session.getHtmlToDisplay());
    }

    @Test
    public void tripleNestedIncludeIf_shouldBeProcessedCorrectly() throws Exception {
        String htmlform = "<htmlform>" +
                "<includeIf velocityTest=\"false\">" +
                "<includeIf velocityTest=\"true\">" +
                "<includeIf velocityTest=\"true\">" +
                "included" +
                "</includeIf>" +
                "</includeIf>" +
                "</includeIf>" +
                "</htmlform>";

        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\"></div>", session.getHtmlToDisplay());
    }

    @Test
    public void testIncludeSpecialCharacters() throws Exception {
        String htmlform = "<htmlform><includeIf velocityTest=\"true && true && 1 < 2 && 2 > 1 || false\">included</includeIf></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\">included</div>", session.getHtmlToDisplay());
    }

    @Test
    public void testIncludeSpecialCharactersAlreadyConverted() throws Exception {
        String htmlform = "<htmlform><includeIf velocityTest=\"true &amp;&amp; true &amp;&amp; 1 &lt; 2 &amp;&amp; 2&gt; 1\">included</includeIf></htmlform>";
        FormEntrySession session = new FormEntrySession(null, htmlform, null);
        Assert.assertEquals("<div class=\"htmlform\">included</div>", session.getHtmlToDisplay());
    }

}
