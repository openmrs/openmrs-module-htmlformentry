package org.openmrs.module.htmlformentry.include;

import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.export.HtmlFormEntryExportUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryExportUtilTests extends BaseModuleContextSensitiveTest {

 protected final Log log = LogFactory.getLog(getClass());
    
    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
    
    protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data3.xml";
    
    @Before
    public void setupDatabase() throws Exception {
        initializeInMemoryDatabase();
        authenticate();
        executeDataSet(XML_DATASET_PACKAGE_PATH);
    }
    
    @Test
    @Verifies(value = "should recognize and return section tags in xml", method = "getSectionNodes(HtmlForm)")
    public void getSectionNodes_shouldReturnSectionNodesCorrectly() throws Exception {
        executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data.xml");
        
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestForm.xml"));
        Map<Integer, String> map = HtmlFormEntryExportUtil.getSectionIndex(htmlform);
        String st = "";
        for (Map.Entry<Integer, String> e : map.entrySet()){
            st += "|" +e.getKey() + " : " + e.getValue();
        }
        //System.out.println(st);
        TestUtil.assertFuzzyContains("Section One", st);
        TestUtil.assertFuzzyContains("Section One Inner One", st);
        TestUtil.assertFuzzyContains("Section One Inner Two", st);
        TestUtil.assertFuzzyContains("no name specified", st);
    }


    @Test
    @Verifies(value = "should return section as a new htmlform", method = "getSectionAsForm(HtmlForm)")
    public void getSectionAsForm_shouldReturnStringCorrectly() throws Exception {
        executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data.xml");
        
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestForm.xml"));
        String newXml = HtmlFormEntryExportUtil.getSectionAsFormXml(htmlform, 1);
        //System.out.println(newXML);
        htmlform.setXmlData(newXml);
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlform);
        String html = session.getHtmlToDisplay();
        //System.out.println(html);
        TestUtil.assertFuzzyContains("<span class=\"sectionHeader\">Section One Inner One</span>", html);
        TestUtil.assertFuzzyContains("ISONIAZID <select id=\"w2\" name=\"w2\"><option value=\"\" selected=\"true\"></option><option value=\"2474\">Susceptible</option><option value=\"3017\">Intermediate</option><option value=\"1441\">Resistant</option></select>", html);
    }
    

    @Test
    @Verifies(value = "should return trimmed encounter", method = "trimEncounterToMatchForm(Encounter e, HtmlForm htmlform)")
    public void trimEncounterToMatchForm_shouldReturnEncounterCorrectly() throws Exception {
        executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data.xml");
        
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
        String newXml = HtmlFormEntryExportUtil.getSectionAsFormXml(htmlform, 3);
        htmlform.setXmlData(newXml);
        

        Encounter e = new Encounter();
        e.setPatient(Context.getPatientService().getPatient(2));
        Date date = Context.getDateFormat().parse("01/02/2003");
        e.setDateCreated(new Date());
        e.setEncounterDatetime(date);
        e.setLocation(Context.getLocationService().getLocation(2));
        e.setProvider(Context.getPersonService().getPerson(502));
        
        
        TestUtil.addObs(e, 2474, Context.getConceptService().getConcept(656), date);
        TestUtil.addObs(e, 3017, Context.getConceptService().getConcept(767), date);
        TestUtil.addObs(e, 3032, new Date(), date);
        TestUtil.addObs(e, 1, 5000, date);
        TestUtil.addObs(e, 6, "blah blah", date);
            //1004 is ANOTHER ALLERGY CONSTRUCT, 1005 is HYPER-ALLERGY CODED, 1001 is PENICILLIN
        TestUtil.addObsGroup(e, 1004, new Date(), 1005, Context.getConceptService().getConcept(1001), new Date());
            //7 IS ALLERGY CONSTRUCT, 1000 IS ALLERGY CODED, 1003 IS OPENMRS
        TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date());
        
        Context.getEncounterService().saveEncounter(e);
        
        e = HtmlFormEntryExportUtil.trimEncounterToMatchForm(e, htmlform);
//        for (Obs otmp : e.getAllObs()){
//            System.out.println(otmp.getConcept());
//        }
        Assert.assertTrue(e.getAllObs().size() == 4);
        
    }
}
