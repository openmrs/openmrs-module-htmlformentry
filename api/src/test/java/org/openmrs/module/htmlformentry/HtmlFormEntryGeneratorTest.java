package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryGeneratorTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	private Patient patient = null;
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyExcludes(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return correct xml after apply excludeIf tag", method = "applyExcludes(FormEntrySession,String)")
	public void applyExcludes_shouldReturnCorrectXmlAfterApplyExcludeIfTag() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><excludeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</excludeIf><excludeIf logicTest=\"GENDER = M\">This shows a logic test for a man</excludeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a woman</div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyIncludes(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return correct xml after apply include tag", method = "applyIncludes(FormEntrySession,String)")
	public void applyIncludes_shouldReturnCorrectXmlAfterApplyIncludeTag() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><includeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</includeIf><includeIf logicTest=\"GENDER = M\">This shows a logic test for a man</includeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a man</div>", session.getHtmlToDisplay());
	}

    /**
	 * @see {@link HtmlFormEntryGenerator#applyRoleRestrictions(String)}
     * @throws Exception
	 */
	@Test
	@Verifies(value = "should return correct xml after apply restrictByRole tag", method = "applyRoleRestrictions(FormEntrySession,String)")
	public void applyRoleRestrictions_shouldReturnCorrectXmlAfterApplyRestrictByRoleTag() throws Exception {

        /* check the restriction with a single role in include/exclude */
        String htmlform = "<htmlform><restrictByRole include=\"System Developer\">This is shown to admin as as content is included</restrictByRole><restrictByRole include=\"Data Manager\">This is not shown to admin as it doesn't contain this role</restrictByRole></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin as as content is included</div>", session.getHtmlToDisplay());

        String htmlform2 = "<htmlform><restrictByRole exclude=\"System Developer\">This is not shown to admin as content is excluded</restrictByRole><restrictByRole exclude=\"Data Manager\">This is shown to admin as content is not excluded</restrictByRole></htmlform>";
		FormEntrySession session2 = new FormEntrySession(patient, htmlform2);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin as content is not excluded</div>", session2.getHtmlToDisplay());

        /* check the restriction with multiple roles in include/exclude */
        String htmlform3 = "<htmlform><restrictByRole include=\"System Developer,Data Manager\">This is shown to admin with multiple roles in include field</restrictByRole><restrictByRole include=\"Provider,Data Manager\">This is not shown to admin with multiple roles in include field</restrictByRole></htmlform>";
		FormEntrySession session3 = new FormEntrySession(patient, htmlform3);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles in include field</div>", session3.getHtmlToDisplay());

        String htmlform4 = "<htmlform><restrictByRole exclude=\"System Developer,Data Manager\">This is not shown to admin with multiple roles in exclude field</restrictByRole><restrictByRole exclude=\"Provider,Data Manager\">This is shown to admin with multiple roles in exclude field</restrictByRole></htmlform>";
        FormEntrySession session4 = new FormEntrySession(patient, htmlform4);
        Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles in exclude field</div>", session4.getHtmlToDisplay());

        /* check the restriction for a single user with multiple roles */
        Context.getAuthenticatedUser().addRole(new Role("Test Role", "A temporary role for the test"));

        String htmlform5 = "<htmlform><restrictByRole include=\"System Developer,Test Role\">This is shown to admin with multiple roles to single user in include field</restrictByRole><restrictByRole include=\"Provider,Data Manager\">This is not shown to admin with multiple roles to single user in include field</restrictByRole></htmlform>";
        FormEntrySession session5 = new FormEntrySession(patient, htmlform5);
        Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles to single user in include field</div>", session5.getHtmlToDisplay());

        String htmlform6 = "<htmlform><restrictByRole exclude=\"System Developer,Test Role\">This is not shown to admin with multiple roles to single user in exclude field</restrictByRole><restrictByRole exclude=\"Provider,Data Manager\">This is shown to admin with multiple roles to single user in exclude field</restrictByRole></htmlform>";
        FormEntrySession session6 = new FormEntrySession(patient, htmlform6);
        Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles to single user in exclude field</div>", session6.getHtmlToDisplay());

    }


	/**
	 * @see {@link HtmlFormEntryGenerator#getTestStr(String)}
	 */
	@Test
	@Verifies(value = "should extract the correct expression from teststr", method = "getTestStr(String)")
	public void getTestStr_shouldExtractTheCorrectExpressionFromTeststr() throws Exception {
		String includeStr = "logicTest = \"GENDER = F\">";// <includeIf test =
		// "FEMALE">
		Assert.assertEquals("GENDER = F", HtmlFormEntryGenerator.getTestStr(includeStr));
		includeStr = "velocityTest = \"$patient.gender == 'F'\">";
		Assert.assertEquals("$patient.gender == 'F'", HtmlFormEntryGenerator.getTestStr(includeStr));
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#processIncludeLogic(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return a correct boolean value for logic test string", method = "processIncludeLogic(FormEntrySession,String)")
	public void processIncludeLogic_shouldReturnACorrectBooleanValueForLogicTestString() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><includeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</includeIf><includeIf logicTest=\"GENDER = M\">This shows a logic test for a man</includeIf></htmlform>";
		String testStr = "logicTest=\"GENDER = M\">";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals(true, HtmlFormEntryGenerator.processIncludeLogic(session, testStr));
	}
	
	/**
	 * @see HtmlFormEntryGenerator#wrapInDiv(String)
	 * @verifies remove htmlform tag and wrap form in div
	 */
	@Test
	public void wrapInDiv_shouldRemoveHtmlformTagAndWrapFormInDiv() throws Exception {
		String htmlform = "<htmlform>\rsomeContent</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		TestUtil.assertFuzzyContains("(?s)<div class=\"htmlform\">(.*)someContent</div>", session.getHtmlToDisplay());
	}
	
	  /**
     * @see {@link HtmlFormEntryGenerator#stripComments(String)}
     * @verifies  filters out all the comments in the input string
     */
    @Test
    public void stripComments_shouldStripOutComments() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><!--<repeat><template></template><render/></repeat>--><repeat><template></template><render/></repeat></section></htmlform>";
        HtmlFormEntryGenerator htmlFormEntryGenerator = new HtmlFormEntryGenerator();
        String returnedXml = htmlFormEntryGenerator.stripComments(htmlform);

        Assert.assertEquals("<htmlform><section><repeat><template></template><render/></repeat></section></htmlform>", returnedXml);
    }

    /**
     * @see {@link HtmlFormEntryGenerator#stripComments(String)}
     */
    @Test
    @Verifies(value = "should return correct xml to display after filtering out comments", method = "stripComments(String)")
    public void stripComments_shouldReturnCorrectHtmlAfterFilteringOutComments() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><!--some comment that should not be displayed--></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        Assert.assertEquals("<div class=\"htmlform\"><div class=\"section\"></div></div>", session.getHtmlToDisplay());
    }
    
    /**
     * @see {@link HtmlFormEntryGenerator#doStartTag(FormEntrySession,PrintWriter,Node,Node)}
     * @see {@link HtmlFormEntryGenerator#doEndTag(FormEntrySession,PrintWriter,Node,Node)}
     */
    @Test
    @Verifies(value = "should close br tags", method = "doStartTag(FormEntrySession,PrintWriter,Node,Node)")
    public void doStartTag_shouldCloseBrTags() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><span></span><br/><h1></h1></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        String html = session.getHtmlToDisplay();
        Assert.assertTrue("<br/> should be closed with one tag", html.contains("<br/>"));
        Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
        Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
    }
    
    /**
     * Similar to doStartTag test, but with capitalized BR tags.
     * 
     * @see {@link HtmlFormEntryGenerator#doStartTag(FormEntrySession,PrintWriter,Node,Node)}
     * @see {@link HtmlFormEntryGenerator#doEndTag(FormEntrySession,PrintWriter,Node,Node)}
     */
    @Test
    @Verifies(value = "should skip br tags", method = "doEndTag(FormEntrySession,PrintWriter,Node,Node)")
    public void doEndTag_shouldCloseBrTags() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><span></span><BR/><h1></h1></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        String html = session.getHtmlToDisplay();
        Assert.assertTrue("<BR/> should be closed with one tag", html.contains("<BR/>"));
        Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
        Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
    }
    
}
