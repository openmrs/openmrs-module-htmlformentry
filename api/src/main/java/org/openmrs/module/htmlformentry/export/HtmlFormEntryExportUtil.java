package org.openmrs.module.htmlformentry.export;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.Translator;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HtmlFormEntryExportUtil {
    
    protected final static Log log = LogFactory.getLog(HtmlFormEntryExportUtil.class);
    
    private static DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy");
    
    private static final String DEFAULT_QUOTE = "\"";

    private static final String DEFAULT_COLUMN_SEPARATOR = ",";

    private static final String DEFAULT_LINE_SEPARATOR = "\n";
    
    private static final String EMPTY = "";
    
    /**
     * returns a Map<Integer, String> of all form sections, in order, 
     * where the String value is either the specified name, or an arbitrary one if none was given.
     * and the integer is the numeric index of the sections, starting with 0.
     * 
     * @param HtmlForm htmlForm
     * @return
     */
    public static Map<Integer, String> getSectionIndex(HtmlForm htmlForm) throws Exception{
        Document doc = HtmlFormEntryUtil.stringToDocument(htmlForm.getXmlData());
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlForm, null); // session gets a null HttpSession
        NodeList nl = doc.getElementsByTagName("section");
        Map<Integer, String> ret = new LinkedHashMap<Integer, String>();
        for (int i = 0; i < nl.getLength(); i++){
            Node sectionNode = nl.item(i);
            NamedNodeMap map = sectionNode.getAttributes();
            String headerLabel = "no name specified";
            for (int j = 0; j < map.getLength(); ++j) {
                Node attribute = map.item(j);
                if (attribute.getNodeName().equals("headerLabel")) {
                    headerLabel = attribute.getNodeValue();
                }
                if (attribute.getNodeName().equals("headerCode")) {
                    Translator trans = session.getContext().getTranslator();
                    headerLabel = trans.translate(Context.getLocale().toString(), attribute.getNodeValue());
                }
            }
            ret.put(i, headerLabel);
        }
        return ret;
    }
    
    /**
     * 
     * Returns a section as a complete htmlform, including the three required encounter tags.
     * 
     * @param htmlForm
     * @param sectionIndex
     * @return
     * @throws Exception
     */
    public static String getSectionAsFormXml(HtmlForm htmlForm, Integer sectionIndex) throws Exception{
        Document doc = HtmlFormEntryUtil.stringToDocument(htmlForm.getXmlData());
        NodeList nl = doc.getElementsByTagName("section");
        Node sectionNode = null;
        try {
            sectionNode = nl.item(sectionIndex);
        } catch (Exception ex){
            throw new RuntimeException("The section index that you've passed in is out of range.  There are only " + nl.getLength() + " section tags in the document and you requested section tag " + sectionIndex);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc2 = db.newDocument();
        Node formRoot = doc2.createElement("htmlform");
        doc2.appendChild(formRoot);
        formRoot.appendChild(doc2.importNode(sectionNode, true));
        if (doc2.getElementsByTagName("encounterLocation").getLength() == 0){
            Node encLoc = doc2.createElement("encounterLocation");
            formRoot.appendChild(encLoc);
        }
        if (doc2.getElementsByTagName("encounterDate").getLength() == 0){
            Node encDate = doc2.createElement("encounterDate");
            formRoot.appendChild(encDate);
        }
        if (doc2.getElementsByTagName("encounterProvider").getLength() == 0){
            Node encDate = doc2.createElement("encounterProvider");
            Element encDateElement = (Element) encDate;
            encDateElement.setAttribute("role","Provider");
            formRoot.appendChild(encDate);
           
        }
        doc2.normalize();
            
        try
        {
           DOMSource domSource = new DOMSource(doc2);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }
    
    /**
     * 
     * Returns the encounter with the obs that aren't used when populating form are removed.  
     * This *doesn't* save the encounter.
     * TODO: handle Orders?
     * 
     * @param e
     * @param htmlform
     * @return
     * @throws Exception
     */
    public static Encounter trimEncounterToMatchForm(Encounter e, HtmlForm htmlform) throws Exception {
        
       //this should move existing obs from session to tag handlers.
        FormEntrySession session = new FormEntrySession(e.getPatient(), e, FormEntryContext.Mode.VIEW, htmlform, null); // session gets a null HttpSession
        session.getHtmlToDisplay();
        
        if (log.isDebugEnabled()){
            Map<Concept, List<Obs>>  map = session.getContext().getExistingObs();
            if (map != null){
                for (Map.Entry<Concept, List<Obs>> existingObs : map.entrySet()){
                    List<Obs> oList = existingObs.getValue();
                    for (Obs oInner : oList)
                        log.debug("Obs in existingObs " + existingObs.getKey() + " " + oInner.getConcept());
                }
            }
            Map<Obs, Set<Obs>> map2 = session.getContext().getExistingObsInGroups();
            if (map2 != null){
                for (Map.Entry<Obs, Set<Obs>> existingObsInGroups : map2.entrySet()){
                    Set<Obs> oList = existingObsInGroups.getValue();
                    for (Obs oInner : oList)
                        log.debug("Obs in existingObsInGroups " + existingObsInGroups.getKey().getConcept() + " " + oInner.getConcept());
                }
            }
        }
        Encounter ret = new Encounter();
        ret.setCreator(e.getCreator());
        ret.setEncounterDatetime(e.getEncounterDatetime());
        ret.setProvider(e.getProvider());
        ret.setLocation(e.getLocation());
        ret.setDateCreated(e.getDateCreated());
        ret.setPatient(e.getPatient());
        //renders new encounter unsave-able:
        ret.setEncounterId(e.getEncounterId());
        
        for (Obs oTest : e.getAllObs()){
            boolean found = false;
            if (session.getContext().getExistingObs() != null && !oTest.isObsGrouping()){
                List<Obs> obsList = session.getContext().getExistingObs().get(oTest.getConcept());
                if (obsList != null && obsList.size() > 0){
                    for (Obs o : obsList){
                        if (o.getObsId().equals(oTest.getObsId())){
                            found = true;
                            continue;
                        }
                    }   
                }
            } 
            if (!found && session.getContext().getExistingObsInGroups() != null){
                for (Map.Entry<Obs, Set<Obs>> mapEntry : session.getContext().getExistingObsInGroups().entrySet()){
                    if (mapEntry.getKey().equals(oTest)){
                        found = true;
                        continue;
                    } else {
                        Set<Obs> oSet = mapEntry.getValue();
                        //note: oSet.contains fails for some reason
                        for (Obs o:oSet){
                              if (o.getObsId().equals(oTest.getObsId())){
                                  found = true; 
                                  continue;
                              }    
                        }  
                    }
                }
            }
            if (!found)
                ret.addObs(oTest);
        }
        session = null;
        return ret;
    }  
    
    /**
     * 
     * Generate the header row for the csv file.
     * 
     * @param form
     * @param extraCols
     * @return
     * @throws Exception
     */ 
    public static String generateColumnHeadersFromHtmlForm(HtmlForm form, List<String> extraCols, StringBuffer sb, List<PatientIdentifierType> pitList) throws Exception {
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), form, null); // session gets a null HttpSession
        session.getHtmlToDisplay();
        HtmlFormSchema hfs = session.getContext().getSchema();
        
        sb.
        append(DEFAULT_QUOTE).append("ENCOUNTER_ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_DATE").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_LOCATION").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_PROVIDER").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("INTERNAL_PATIENT_ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
        int index = 1;
        for (PatientIdentifierType pit :  pitList){
            sb.append(DEFAULT_QUOTE).append(pit.getName()).append(DEFAULT_QUOTE);
            if (index < pitList.size())
                sb.append(DEFAULT_COLUMN_SEPARATOR);
            index ++;
        }    
        
        for (HtmlFormField hfsec : hfs.getAllFields())
                sb = generateColumnHeadersFromHtmlFormHelper(hfsec, extraCols, sb);

        session = null;
        sb.append(DEFAULT_LINE_SEPARATOR);
        return sb.toString();
    }
    
    private static StringBuffer generateColumnHeadersFromHtmlFormHelper(HtmlFormField hff, List<String> extraCols, StringBuffer sb){
        if (hff instanceof ObsField){
            ObsField of = (ObsField) hff;      
            sb = buildHeadersForObsField(of, extraCols, sb);
        } else if (hff instanceof ObsGroup){
                ObsGroup og = (ObsGroup) hff;
                for (HtmlFormField of : og.getChildren()){
                    sb = generateColumnHeadersFromHtmlFormHelper(of, extraCols, sb);
                }
        }
        return sb;
    }
    
    /**
     * 
     * Builds the root column name for the concept from the conceptID
     * 
     * @param of
     * @return
     */
    private static String buildColumnHeader(ObsField of){
        StringBuilder sb = new StringBuilder(EMPTY);
        Locale loc = Context.getLocale();
        if (of.getQuestion() != null){
            //TODO: add fieldId, fieldPart, Page???
            sb.append(of.getQuestion().getBestShortName(loc));
        } else if (of.getAnswers().size() == 1){
            sb.append(of.getAnswers().get(0).getConcept().getBestShortName(loc));
        } else {
            throw new RuntimeException("Obs Field has no conceptId, and multiple answers -- this isn't yet supported.");
        }
        return sb.toString().replaceAll("\\s", "_").replaceAll("-", "_").toUpperCase();
    }
    
    /**
     * 
     * Adds all of the columns for an Obs Field.
     * 
     * @param of
     * @param extraCols
     * @param sb
     * @return
     */
    private static StringBuffer buildHeadersForObsField(ObsField of, List<String> extraCols, StringBuffer sb){
        
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);
        String columnHeader = buildColumnHeader(of);
        sb.append(columnHeader);
        sb.append(DEFAULT_QUOTE);
    
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);     
        sb.append(columnHeader + "_DATE");
        sb.append(DEFAULT_QUOTE);
        
      //always export obsGroupId
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);     
        sb.append(columnHeader + "_PARENT");
        sb.append(DEFAULT_QUOTE);
        
        if (extraCols != null){
            for (String st : extraCols){
                if (st.equals("valueModifier")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_VALUE_MOD");                                   
                    sb.append(DEFAULT_QUOTE);
                } else if (st.equals("accessionNumber")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_ACCESSION_NUM");                                   
                    sb.append(DEFAULT_QUOTE);
                } else if (st.equals("comment")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_COMMENT");                                   
                    sb.append(DEFAULT_QUOTE);
                }
            }
        }
        
        return sb;
    }
    
    /**
     * 
     * Generates all of the data rows
     * 
     * @param form
     * @param extraCols
     * @param sb
     * @return
     * @throws Exception
     */
    public static String generateColumnDataFromHtmlForm(List<Encounter> encounters, HtmlForm form, List<String> extraCols, StringBuffer sb, Locale locale,List<PatientIdentifierType> pitList) throws Exception {
        for (Encounter e: encounters){
            
            sb.append(DEFAULT_QUOTE).append(e.getEncounterId()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);         
            sb.append(DEFAULT_QUOTE).append(DATE_FORMATTER.format(e.getEncounterDatetime())).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append(e.getLocation().getName()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append(e.getProvider().getGivenName()+ " " + e.getProvider().getFamilyName()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append((e.getPatient() != null ? e.getPatient().getPatientId() : EMPTY)).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);       
            int index = 1;
            for (PatientIdentifierType pit :  pitList){
                sb.append(DEFAULT_QUOTE).append(e.getPatient().getPatientIdentifier(pit)).append(DEFAULT_QUOTE);
                if (index < pitList.size())
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                index ++;
            }
            
            FormEntrySession session = new FormEntrySession(e.getPatient(), e, Mode.VIEW, form, null); // session doesn't get HttpSession
            session.getHtmlToDisplay();
            FormSubmissionController  fsa = session.getSubmissionController();
            List<FormSubmissionControllerAction> actions = fsa.getActions();
            for (FormSubmissionControllerAction fsca : actions){
                if (fsca instanceof ObsSubmissionElement){
                    ObsSubmissionElement ose = (ObsSubmissionElement) fsca;
                    sb = appendObsToRow(ose, sb, extraCols, locale);   
                } else {
                    //TODO: add programs, orders, logic, etc...
                    // just make sure these are in the headers too...
                }
            }
            session = null;
            sb.append(DEFAULT_LINE_SEPARATOR);
        }
        return sb.toString();
    }
    
    /**
     * 
     * Writes the row entries for the Obs
     * 
     * @param o
     * @param sb
     * @param extraCols
     * @param rowStarted
     * @return
     */
    private static StringBuffer appendObsToRow(ObsSubmissionElement ose, StringBuffer sb, List<String> extraCols, Locale locale){
            Obs o = ose.getExistingObs();       

            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);
            if (ose.getConcept() != null)
                sb.append((o != null) ? getObsValueAsString(Context.getLocale(), o):EMPTY);
            else 
                sb.append((o != null) ? o.getConcept().getBestName(locale):EMPTY);
            sb.append(DEFAULT_QUOTE);
            
            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);     
            sb.append((o!=null)? Context.getDateFormat().format(o.getObsDatetime()):EMPTY);                                   
            sb.append(DEFAULT_QUOTE); 
            
            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);     
            sb.append(getObsGroupPath(o));                                   
            sb.append(DEFAULT_QUOTE);
            
            if (extraCols != null){
                for (String st : extraCols){
                    if (st.equals("valueModifier")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getValueModifier() != null) ? o.getValueModifier():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    } else if (st.equals("accessionNumber")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getAccessionNumber() != null) ? o.getAccessionNumber():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    } else if (st.equals("comment")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getComment() != null) ? o.getComment():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    }
                }
            }
        return sb;
    }
    
    public static String getObsGroupPath(Obs o){
        StringBuilder st = new StringBuilder(EMPTY);
        if (o != null)
            while (o.getObsGroup() != null){
                o = o.getObsGroup();
                st.insert(0, o.getObsId() + "|");
            }
        return st.toString();
    }
    
    /**
     * The main method for exporting an htmlform to a csv.
     * 
     * @param encounters
     * @param form
     * @param extraCols
     * @param sb
     * @param locale
     * @param pitList
     * @return the complete StringBuffer, ready for export
     */
    public static StringBuffer buildHtmlFormExport(List<Encounter> encounters, HtmlForm htmlForm, List<String> extraCols, StringBuffer sb, Locale locale,List<PatientIdentifierType> pitList){
        try {
            HtmlFormEntryExportUtil.generateColumnHeadersFromHtmlForm(htmlForm, extraCols, sb, pitList);
            HtmlFormEntryExportUtil.generateColumnDataFromHtmlForm(encounters, htmlForm, extraCols, sb, Context.getLocale(), pitList);
        } catch (Exception ex){
            ex.printStackTrace();
            throw new RuntimeException("Unable to export form.  Check the log for details.  Underlying error was: " + ex.getMessage());
        }
        return sb;
    }
    
    /**
     * 
     * format the obs value
     * 
     * @param locale
     * @param o
     * @return
     */
    public static String getObsValueAsString(Locale locale, Obs o){
        String ret = "";
        if (o.getConcept() != null){
            String abbrev = o.getConcept().getDatatype().getHl7Abbreviation();
            if (abbrev.equals("DT")){
                return (o.getValueDatetime() == null ? "" : Context.getDateFormat().format(o.getValueDatetime()));
            } else if (abbrev.equals("TS") && o.getValueDatetime() != null){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return sdf.format(o.getValueDatetime());
            } else {
                ret = o.getValueAsString(locale);
            }    
        }
        return ret;
    }
    
}
