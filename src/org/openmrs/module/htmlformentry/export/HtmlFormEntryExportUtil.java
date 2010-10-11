package org.openmrs.module.htmlformentry.export;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.Translator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HtmlFormEntryExportUtil {
    
    protected final static Log log = LogFactory.getLog(HtmlFormEntryExportUtil.class);
    
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
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlForm);
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
     * 
     * @param e
     * @param htmlform
     * @return
     * @throws Exception
     */
    public static Encounter trimEncounterToMatchForm(Encounter e, HtmlForm htmlform) throws Exception {
        
       //this should move existing obs from session to tag handlers.
        FormEntrySession session = new FormEntrySession(e.getPatient(), e, FormEntryContext.Mode.VIEW, htmlform);
        
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
            if (session.getContext().getExistingObsInGroups() != null){
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
    
}
