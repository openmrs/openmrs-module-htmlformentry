package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.export.HtmlFormEntryExportUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryExportUtilTest extends BaseModuleContextSensitiveTest {

    protected final Log log = LogFactory.getLog(getClass());
    
    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
    
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
    
    @Before
    public void setupDatabase() throws Exception {
    	executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
    }
    
    @Test
    @Verifies(value = "should recognize and return section tags in xml", method = "getSectionNodes(HtmlForm)")
    public void getSectionNodes_shouldReturnSectionNodesCorrectly() throws Exception {      
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestForm.xml"));
        Map<Integer, String> map = HtmlFormEntryExportUtil.getSectionIndex(htmlform);
        String st = "";
        for (Map.Entry<Integer, String> e : map.entrySet()){
            st += "|" + e.getKey() + " : " + e.getValue();
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
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestForm.xml"));
        String newXml = HtmlFormEntryExportUtil.getSectionAsFormXml(htmlform, 1);
        //System.out.println(newXML);
        htmlform.setXmlData(newXml);
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlform, null);
        String html = session.getHtmlToDisplay();
        System.out.println(html);
        TestUtil.assertFuzzyContains("<span class=\"sectionHeader\">Section One Inner One</span>", html);
        TestUtil.assertFuzzyContains("ISONIAZID <select id=\"w2\" name=\"w2\" size=1><option value=\"\" selected=\"true\"></option><option value=\"2474\">Susceptible</option><option value=\"3017\">Intermediate</option><option value=\"1441\">Resistant</option></select>", html);
    }
    

    @Test
    @Verifies(value = "should return trimmed encounter", method = "trimEncounterToMatchForm(Encounter e, HtmlForm htmlform)")
    public void trimEncounterToMatchForm_shouldReturnEncounterCorrectly() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
        String newXml = HtmlFormEntryExportUtil.getSectionAsFormXml(htmlform, 0);
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
        TestUtil.addObs(e, 2, 5000, date); //not in form schema, should not be included after trimEncounter
        TestUtil.addObs(e, 3, 5000, date); //not in form schema, should not be included after trimEncounter
        TestUtil.addObs(e, 6, "blah blah", date); 
            //1004 is ANOTHER ALLERGY CONSTRUCT, 1005 is HYPER-ALLERGY CODED, 1001 is PENICILLIN
        TestUtil.addObsGroup(e, 1004, new Date(), 1005, Context.getConceptService().getConcept(1001), new Date()); 
            //7 IS ALLERGY CONSTRUCT, 1000 IS ALLERGY CODED, 1003 IS OPENMRS
        TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date()); 
        Context.getEncounterService().saveEncounter(e);
        e = HtmlFormEntryExportUtil.trimEncounterToMatchForm(e, htmlform);
        if (log.isDebugEnabled()){
            for (Obs otmp : e.getAllObs()){
                log.debug("trimEncounterToMatchForm returned an obs with concept: " + otmp.getConcept());
            }
        }
        //Note, this assertion corresponds to section index 0 in the form, so 5 obs should be returned
            // the form has 6 obs under section 0, but we didn't create the obs for conceptId = 9
        Assert.assertEquals(5, e.getAllObs().size());
        
    }
    
    @Test
    @Verifies(value = "should return form schema", method = "generateColumnHeadersFromHtmlForm(HtmlForm htmlform, String[] extraCols, StringBuffer, List<PatientIdenitifierType> pitList)")
    public void generateColumnHeadersFromHtmlForm_shouldReturnSchemaCorrectly() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "returnSectionsAndConceptsInSectionsTestFormWithGroups.xml"));
        List<String> extraCols = new ArrayList<String>();
        extraCols.add("valueModifier");
        extraCols.add("accessionNumber");
        extraCols.add("comment");
        PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
        String ret = HtmlFormEntryExportUtil.generateColumnHeadersFromHtmlForm(htmlform, extraCols, new StringBuffer(""), Collections.singletonList(pit));
        //System.out.println("TESTHEADER " + ret);
        
        Assert.assertTrue(ret.contains("\"ENCOUNTER_ID\""));
        Assert.assertTrue(ret.contains("\"ENCOUNTER_DATE\""));
        Assert.assertTrue(ret.contains("\"ENCOUNTER_LOCATION\""));
        Assert.assertTrue(ret.contains("\"ENCOUNTER_PROVIDER\""));
        Assert.assertTrue(ret.contains("\"INTERNAL_PATIENT_ID\""));
        Assert.assertTrue(ret.contains("\"Old Identification Number\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID_DATE\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID_PARENT\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"ISONIAZID_COMMENT\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED_DATE\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED_PARENT\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"HYPER_ALLERGY_CODED_COMMENT\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE_DATE\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE_PARENT\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_DATE_COMMENT\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN_DATE\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN_PARENT\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"RIFAMPICIN_COMMENT\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE_DATE\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE_PARENT\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"DST_START_DATE_COMMENT\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT_DATE\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT_PARENT\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"CD4_COUNT_COMMENT\""));
        Assert.assertTrue(ret.contains("\"MARRIED\""));
        Assert.assertTrue(ret.contains("\"MARRIED_DATE\""));
        Assert.assertTrue(ret.contains("\"MARRIED_PARENT\""));
        Assert.assertTrue(ret.contains("\"MARRIED_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"MARRIED_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"MARRIED_COMMENT\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED_DATE\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED_PARENT\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED_VALUE_MOD\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED_ACCESSION_NUM\""));
        Assert.assertTrue(ret.contains("\"ALLERGY_CODED_COMMENT\""));
   
    }    
    
    @Test
    @Verifies(value = "should return encounter rows", method = "generateColumnDataFromHtmlForm(List<Encounter> encounters, HtmlForm form, List<String> extraCols, StringBuffer sb, Locale locale),List<PatientIdentifierType> pitList")
    public void generateColumnDataFromHtmlForm_shouldReturnRowsCorrectly() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "obsGroupDataExportTest.xml"));
        List<String> extraCols = new ArrayList<String>();
        extraCols.add("valueModifier");
        extraCols.add("accessionNumber");
        extraCols.add("comment");
        PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
        String header = HtmlFormEntryExportUtil.generateColumnHeadersFromHtmlForm(htmlform, extraCols, new StringBuffer(""), Collections.singletonList(pit));
        //Build a couple of encounters for the form
        List<Encounter> encounters = new ArrayList<Encounter>();
        
        
        //encounter1
        Encounter e = new Encounter();
        e.setPatient(Context.getPatientService().getPatient(2));
        Date date = Context.getDateFormat().parse("01/02/2003");
        e.setDateCreated(new Date());
        e.setEncounterDatetime(date);
        e.setLocation(Context.getLocationService().getLocation(2));
        e.setProvider(Context.getPersonService().getPerson(502));
        //top of form
        TestUtil.addObs(e, 3032, date, date);
        TestUtil.addObs(e, 1441, Context.getConceptService().getConcept(656), date);
        TestUtil.addObsGroup(e, 1004, date, 1005, Context.getConceptService().getConcept(1001), new Date());
        TestUtil.addObs(e, 1119, new Date(), date);
        TestUtil.addObs(e, 2474, Context.getConceptService().getConcept(767), date);
        //DST RESULT
        Obs dstParent = TestUtil.createObs(e, 3040, null, date);
        e.addObs(dstParent);
        Obs resultParent = TestUtil.createObs(e, 3025, null, date);
        dstParent.addGroupMember(resultParent);
        Obs drugResult = TestUtil.createObs(e, 3017, Context.getConceptService().getConcept(656), date);
        resultParent.addGroupMember(drugResult);
        Obs colonies1 = TestUtil.createObs(e, 3016, 200, date);
        resultParent.addGroupMember(colonies1);

        //saving the enconter in order to see obsGroupId in export
        Context.getEncounterService().saveEncounter(e);
        encounters.add(e);

        String ret = HtmlFormEntryExportUtil.generateColumnDataFromHtmlForm(encounters, htmlform, extraCols, new StringBuffer(""), new Locale("en"), Collections.singletonList(pit));
        System.out.println(ret);
        ArrayList<String> splitheader = new ArrayList<String>();
        for (StringTokenizer st = new StringTokenizer(header, ","); st.hasMoreTokens(); )
            splitheader.add(st.nextToken().trim());
        ArrayList<String> splitret = new ArrayList<String>();
        for (StringTokenizer st = new StringTokenizer(ret, ","); st.hasMoreTokens(); ) 
            splitret.add(st.nextToken().trim());
        Assert.assertTrue(splitret.size() == splitheader.size());
//        for (int i = 0; i < splitheader.size(); i++){
//            System.out.println(splitheader.get(i) + " = " + splitret.get(i));
//        }
        
    }
    
    @Test
    @Verifies(value = "should handle multiple identifier types correctly", method = "buildHtmlFormExport(List<Encounter> encounters, HtmlForm htmlForm, List<String> extraCols, StringBuffer sb, Locale locale,List<PatientIdentifierType> pitList)")
    public void generateColumnDataFromHtmlForm_shouldReturnRowsCorrectlyWithMultipleIDTypes() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "obsGroupDataExportTest.xml"));
        List<String> extraCols = new ArrayList<String>();
        extraCols.add("valueModifier");
        extraCols.add("accessionNumber");
        extraCols.add("comment");
        PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
        PatientIdentifierType pitTwo = Context.getPatientService().getPatientIdentifierType(1);
        List<PatientIdentifierType> pitList = new ArrayList<PatientIdentifierType>();
        pitList.add(pit);
        pitList.add(pitTwo);
        //Build a couple of encounters for the form
        List<Encounter> encounters = new ArrayList<Encounter>();
        
        
        //encounter1
        Encounter e = new Encounter();
        e.setPatient(Context.getPatientService().getPatient(2));
        Date date = Context.getDateFormat().parse("01/02/2003");
        e.setDateCreated(new Date());
        e.setEncounterDatetime(date);
        e.setLocation(Context.getLocationService().getLocation(2));
        e.setProvider(Context.getPersonService().getPerson(502));
        
        Context.getEncounterService().saveEncounter(e);
        encounters.add(e);

        StringBuffer ret = HtmlFormEntryExportUtil.buildHtmlFormExport(encounters, htmlform, extraCols, new StringBuffer(""), new Locale("en"),pitList);
        
        //System.out.println("HEADER " + ret.toString());
        Assert.assertTrue(ret.toString().contains(",\"Old Identification Number\",\"Test Identifier Type\","));
        Assert.assertTrue(ret.toString().contains(",\"101\",\"a-D\","));
        
    }     
    
    
    
    @Test
    @Verifies(value = "should render timestamps and datetimes correctly", method = "getObsValueAsString")
    public void getObsValueAsString_shouldRenderCorrectly() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "datatypeRenderingTest.xml"));
        List<String> extraCols = new ArrayList<String>();
        extraCols.add("valueModifier");
        extraCols.add("accessionNumber");
        extraCols.add("comment");
        PatientIdentifierType pit = Context.getPatientService().getPatientIdentifierType(2);
        PatientIdentifierType pitTwo = Context.getPatientService().getPatientIdentifierType(1);
        List<PatientIdentifierType> pitList = new ArrayList<PatientIdentifierType>();
        pitList.add(pit);
        pitList.add(pitTwo);
        //Build a couple of encounters for the form
        List<Encounter> encounters = new ArrayList<Encounter>();
        
        
        //encounter1
        Encounter e = new Encounter();
        e.setPatient(Context.getPatientService().getPatient(2));
        Date date = Context.getDateFormat().parse("01/02/2003");
        e.setDateCreated(new Date());
        e.setEncounterDatetime(date);
        e.setLocation(Context.getLocationService().getLocation(2));
        e.setProvider(Context.getPersonService().getPerson(502));
        
        
        TestUtil.addObs(e, 1119, date, date);
        TestUtil.addObs(e, 1007, date, date);
        
        Context.getEncounterService().saveEncounter(e);
        encounters.add(e);

        StringBuffer ret = HtmlFormEntryExportUtil.buildHtmlFormExport(encounters, htmlform, extraCols, new StringBuffer(""), new Locale("en"),pitList);
        
        Assert.assertTrue(ret.toString().contains("\"2003-02-01 00:00:00\",\"01/02/2003\",\"\",\"\",\"\",\"\",\"01/02/2003\",\"01/02/2003\","));
        
        
    }
    
    
    /**
     * calls session.createForm on a form that has both conceptIds with labels, and conceptIds without labels in obs tags.
     * Verifies that the dropdown options for selecting a concept are correctly labeled.
     * @throws Exception
     */
    @Test
    @Verifies(value = "should test labels generation for concept selects on dropdown options", method = "")
    public void getSectionNodes_shouldReturnDropdownForConceptSelects() throws Exception {
        Form form = new Form();
        HtmlForm htmlform = new HtmlForm();
        htmlform.setForm(form);
        form.setEncounterType(new EncounterType());
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "obsGroupDataExportTest.xml"));
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlform, null);
        String xml = session.createForm(htmlform.getXmlData());
//        System.out.println(xml);
        Assert.assertTrue(xml.contains("<option value=\"\" selected=\"true\"></option><option value=\"2474\">Susc</option><option value=\"3017\">Interm</option>"));
        Assert.assertTrue(xml.contains("<option value=\"\" selected=\"true\"></option><option value=\"2474\">Susceptible</option><option value=\"3017\">Intermediate</option><option value=\"1441\">Resistant</option>"));
    }
    
    
    
}
