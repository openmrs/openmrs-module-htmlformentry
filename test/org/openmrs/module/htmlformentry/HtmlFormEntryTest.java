package org.openmrs.module.htmlformentry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.dataset.xml.XmlDataSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.mock.web.MockHttpServletRequest;

/** Basic test cases for HTML Form Entry module */

@SkipBaseSetup
public class HtmlFormEntryTest extends BaseModuleContextSensitiveTest {

    protected final Log log = LogFactory.getLog(getClass());
    
    // This is for trunk
    protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data.xml";
    
    // This is for sync
    //protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data.forsync.xml";
    
    protected static final String XML_DATASET_PACKAGE_PATH2 = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data2.xml";
    protected static final String HTML_FORM_PATH = "test/org/openmrs/module/htmlformentry/include/htmlForm.xml";
    protected static final String HTML_FORM2_PATH = "test/org/openmrs/module/htmlformentry/include/htmlForm2.xml";
    
    @Before
    public void setupDatabase() throws Exception {
        initializeInMemoryDatabase();
        authenticate();
        executeDataSet(XML_DATASET_PACKAGE_PATH);
        executeXmlDataSet(XML_DATASET_PACKAGE_PATH2);
    }
    
    /*
    @Test
    public void shouldCreateFormViewFromMetadata() throws Exception {
        Form form = Context.getFormService().getForm(1);
        Assert.assertNotNull(form);
        System.out.println("Looking at form id=" + form + " name=" + form.getName());
        TreeMap<Integer, TreeSet<FormField>> structure = FormUtil.getFormStructure(form);
        
        FormView fv = new FormView();
        createElementHelper(fv, structure.get(0), structure);

        FormEntryContext context = new FormEntryContext();
        fv.initializeWidgets(context);
        
        System.out.println("Html:\n" + fv.generateHtml(context));
    }
    
    private void createElementHelper(FormEntryContainerElement container, Collection<FormField> fields, TreeMap<Integer, TreeSet<FormField>> structure) {
        if (fields != null)
            for (FormField field : fields)
                createElementHelper(container, field, structure);
    }

    private void createElementHelper(FormEntryContainerElement container, FormField ff, TreeMap<Integer, TreeSet<FormField>> structure) {
        HtmlFormEntryService service = HtmlFormEntryUtil.getService();
        Map<String, String> map = service.getAttributeMap(ff);
        String className = map.get("htmlformentry.class");
        if (className == null) {
            // recurse to children of form field, if any
            createElementHelper(container, structure.get(ff.getFormFieldId()), structure);
            
        } else { // className != null
            try {
                if (!className.contains("."))
                    className = "org.openmrs.module.htmlformentry.element." + className;
                Class<FormEntryElement> clazz = (Class<FormEntryElement>) Class.forName(className);
                FormEntryElement obj = clazz.newInstance();
                // if this class takes a a formField property, set it
                try {
                    PropertyDescriptor pd = new PropertyDescriptor("formField", clazz);
                    Method meth = pd.getWriteMethod();
                    meth.invoke(obj, ff);
                } catch (Exception ex) {
                    // can't set a formField property. not a problem.
                }
                // set any properties on this object whose names match attributes on this form field
                for (Map.Entry<String, String> e : map.entrySet()) {
                    try {
                        PropertyDescriptor pd = new PropertyDescriptor(e.getKey(), clazz);
                        Method meth = pd.getWriteMethod();
                        meth.invoke(obj, HtmlFormEntryUtil.convertToType(e.getValue(), pd.getPropertyType()));
                    } catch (Exception ex) {
                        // can't set field property. not a problem
                    }
                }
                
                // if this type of Element can have children, then recurse.
                if (obj instanceof FormEntryContainerElement) {
                    // look for children of this form field
                    createElementHelper((FormEntryContainerElement) obj, structure.get(ff.getFormFieldId()), structure);
                }

                container.addElement(obj);
                
            } catch (Exception ex) {
                log.error("Exception trying to construct form entry element", ex);
            }
        }
    }
    */
    
    private void executeXmlDataSet(String datasetFilename) throws Exception {
        File file = new File(datasetFilename);
        
        InputStream fileInInputStreamFormat = null;
        
        // try to load the file if its a straight up path to the file or
        // if its a classpath path to the file
        if (file.exists())
            fileInInputStreamFormat = new FileInputStream(datasetFilename);
        else {
            fileInInputStreamFormat = getClass().getClassLoader().getResourceAsStream(datasetFilename);
            if (fileInInputStreamFormat == null)
                throw new FileNotFoundException("Unable to find '" + datasetFilename + "' in the classpath");
        }
        
        XmlDataSet xmlDataSetToRun = null;
        try {
            xmlDataSetToRun = new XmlDataSet(fileInInputStreamFormat);
        }
        catch (Exception ex) {
            System.out.println("got here " + ex);
        }
        finally {
            fileInInputStreamFormat.close();
        }
        
        executeDataSet(xmlDataSetToRun);
    }

    @Test
    public void testApplyMacros() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<htmlform>");
        sb.append("<macros>");
        sb.append("count=1, 2, 3");
        sb.append("</macros>");
        sb.append("You can count like $count");
        sb.append("</htmlform>");
        
        HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();
        
        String result = generator.applyMacros(sb.toString()).trim();
        System.out.println(result);
        Assert.assertEquals("<htmlform>You can count like 1, 2, 3</htmlform>", result);
    }
    
    @Test
    public void testApplyMacrosFromDatabase() throws Exception {
        setupDatabase();
        
        HtmlForm htmlForm = HtmlFormEntryUtil.getService().getHtmlForm(1);
        
        HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();

        String result = generator.applyMacros(htmlForm.getXmlData()).trim();
        System.out.println(result);
        Assert.assertTrue(equalsIgnoreSpaces("<htmlform>You can count like 1, 2, 3</htmlform>", result));
    }
    
    private boolean equalsIgnoreSpaces(String a, String b) {
        a = a.replace(" ", "");
        a = a.replace("\t", "");
        a = a.replace("\n", "");
        b = b.replace(" ", "");
        b = b.replace("\t", "");
        b = b.replace("\n", "");
        return a.equals(b);
    }

    @Test
    public void testGenerateHtmlFormFromXml() throws Exception {
        BufferedReader r = new BufferedReader(new FileReader(HTML_FORM_PATH));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null)
                break;
            sb.append(line).append("\n");
        }
        
        FormEntrySession session = new FormEntrySession(new Patient(2), sb.toString());
        System.out.println("html = " + session.getHtmlToDisplay());
    }
    
    @Test
    public void testGenerateAnotherHtmlFormFromXml() throws Exception {
        BufferedReader r = new BufferedReader(new FileReader(HTML_FORM2_PATH));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null)
                break;
            sb.append(line).append("\n");
        }
        
        FormEntrySession session = new FormEntrySession(new Patient(2), sb.toString());
        System.out.println("html = " + session.getHtmlToDisplay());
    }
    
    @Test
    public void testGenerateHtmlViewFromXml() throws Exception {
        File file = new File(HTML_FORM_PATH);
        System.out.println(file.getAbsolutePath());
        
        BufferedReader r = new BufferedReader(new FileReader(HTML_FORM_PATH));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null)
                break;
            sb.append(line).append("\n");
        }
        
        Patient pat = new Patient(2);
        Encounter enc = new Encounter();
        enc.setPatient(pat);
        enc.setEncounterDatetime(new Date());
        enc.setLocation(Context.getLocationService().getLocation(1));
        Obs obs = HtmlFormEntryUtil.createObs(Context.getConceptService().getConcept(1), 123, new Date(), null);
        enc.addObs(obs);
        Context.getEncounterService().saveEncounter(enc);
        
        HtmlForm fakeForm = new HtmlForm();
        fakeForm.setXmlData(sb.toString());
        FormEntrySession session = new FormEntrySession(pat, enc, FormEntryContext.Mode.VIEW, fakeForm);
        String html = session.getHtmlToDisplay();
        
        System.out.println("html = " + html);
    }
    
    @Test
    public void testGenerateAndSubmitHtmlFormFromXml() throws Exception {
        File file = new File(HTML_FORM_PATH);
        System.out.println(file.getAbsolutePath());
        
        BufferedReader r = new BufferedReader(new FileReader(HTML_FORM_PATH));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null)
                break;
            sb.append(line).append("\n");
        }

        FormEntrySession session = new FormEntrySession(new Patient(2), sb.toString());
        String html = session.getHtmlToDisplay();
        
        System.out.println("html = " + html);
        
        MockHttpServletRequest submission = new MockHttpServletRequest();

        List<FormSubmissionError> errors = session.getSubmissionController().validateSubmission(session.getContext(), submission);
        Assert.assertEquals(3, errors.size()); // encounter date, location, and provider are required
        
        submission.addParameter("w2", "illegal");

        errors = session.getSubmissionController().validateSubmission(session.getContext(), submission);
        for (FormSubmissionError error : errors)
            System.out.println(error.getId() + " -> " + error.getError());
        Assert.assertEquals(4, errors.size()); // now one more
        
        submission.setParameter("w2", "321");

        errors = session.getSubmissionController().validateSubmission(session.getContext(), submission);
        Assert.assertEquals(3, errors.size()); // that one has been fixed
        
        session.prepareForSubmit();
        session.getSubmissionController().handleFormSubmission(session, submission);
    }

    @Test
    public void testGenerateHtmlFormFromDatabase() throws Exception {
        setupDatabase();
        HtmlForm form = HtmlFormEntryUtil.getService().getHtmlForm(2);
        FormEntrySession session = new FormEntrySession(Context.getPatientService().getPatient(2), form.getXmlData());
        System.out.println("html = " + session.getHtmlToDisplay());
        File file = new File("/home/djazayeri/testForm.html");
        if (file.exists())
            file.delete();
        BufferedWriter wr = new BufferedWriter(new FileWriter(file));
        wr.write("<html><body><form>");
        wr.write(session.getHtmlToDisplay());
        wr.write("</form></body></html>");
        wr.close();
        Runtime.getRuntime().exec("firefox /home/djazayeri/testForm.html");
    }
    
    @Test
    public void testEvaluateVelocityExpression() throws Exception {
        setupDatabase();
        Patient patient = Context.getPatientService().getPatient(2);
        FormEntrySession session = new FormEntrySession(patient, "<htmlform></htmlform>");
        Assert.assertEquals("2", session.evaluateVelocityExpression("$patient.patientId"));
        Assert.assertEquals("M", session.evaluateVelocityExpression("$patient.gender"));
        Assert.assertEquals("John", session.evaluateVelocityExpression("$patient.personName.givenName"));
    }
    
    @Test
    public void testObsGroupTag() throws Exception {
        Context.openSession();
        
        File file = new File(HTML_FORM_PATH);
        System.out.println(file.getAbsolutePath());
        
        BufferedReader r = new BufferedReader(new FileReader(HTML_FORM_PATH));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null)
                break;
            sb.append(line).append("\n");
        }

        Context.addProxyPrivilege("View Relationships");
        FormEntrySession session = new FormEntrySession(new Patient(2), sb.toString());
        String html = session.getHtmlToDisplay();
        
        System.out.println("html = " + html);
        
        MockHttpServletRequest submission = new MockHttpServletRequest();
                
        submission.setParameter("w2", "321"); // cd4
        submission.setParameter("w5", "65"); // weight
        submission.setParameter("w6", "02/03/2004"); // encounter date
        submission.setParameter("w8", "1"); // encounter location
        submission.setParameter("w10", "1"); // encounter provider

        List<FormSubmissionError> errors = session.getSubmissionController().validateSubmission(session.getContext(), submission);
        if (errors.size() != 0) {
            for (FormSubmissionError err : errors)
                System.out.println("Error: " + err.getId() + " -> " + err.getError());
        }
        Assert.assertEquals(0, errors.size());
        
        session.prepareForSubmit();
        session.getSubmissionController().handleFormSubmission(session, submission);
        session.applyActions();

        List<Obs> obsCreated = Context.getObsService().getObservationsByPerson(new Patient(2));
        Assert.assertEquals(3, obsCreated.size()); // should have weight, cd4, and a group
        Set<Integer> idsCreated = new HashSet<Integer>();
        for (Obs o : obsCreated)
            idsCreated.add(o.getConcept().getConceptId());
        Assert.assertEquals(3, idsCreated.size()); // should all be distinct concept ids
        
        Context.closeSession();
    }
    
}
