package org.openmrs.module.htmlformentry;
/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p/>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing righ
 * <p/>ts and limitations
 * under the License.
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

import net.anotheria.webutils.servlet.request.HttpServletRequestMockImpl;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.serialization.OpenmrsSerializer;
import org.openmrs.serialization.SerializationException;
import org.openmrs.serialization.SimpleXStreamSerializer;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class SerializableFormObject implements Serializable{
    private static final long serialVersionUID = 1L;

    private String patientIdentifier;
    private String patientUuid;
    private String encounterUuid;
    private Map<String,String[]> parameterMap;
    private String xmlDefinition;
    private int htmlFormId;

    private transient FormEntrySession session = null;

    private class InnerHttpServletRequestMock extends HttpServletRequestMockImpl{
        private Map<String, String[]> paramMap;
        InnerHttpServletRequestMock(Map<String,String[]> parameterMap) {
            super();
            this.paramMap = parameterMap;
        }
        public Map getParamMap() {
            return paramMap;
        }

        public String getParameter(String parameter) {
            return paramMap.get(parameter)[0];
        }
    }

    public SerializableFormObject() {
    }

    public SerializableFormObject(String xmlDefinition, Map<String, String[]> parameterMap,int htmlFormId) {
        this.parameterMap = parameterMap;
        this.xmlDefinition = xmlDefinition;
        this.htmlFormId = htmlFormId;
    }

    public SerializableFormObject(String xmlDefinition, Map<String, String[]> parameterMap, String patientIdentifier,
                                  String patientUuid, String encounterUuid, int htmlFormId) {
        this.patientIdentifier = patientIdentifier;
        this.xmlDefinition = xmlDefinition;
        this.encounterUuid = encounterUuid;
        this.patientUuid = patientUuid;
        this.parameterMap = parameterMap;
        this.htmlFormId = htmlFormId;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getXmlDefinition() {
        return xmlDefinition;
    }

    public void setXmlDefinition(String xmlDefinition) {
        this.xmlDefinition = xmlDefinition;
    }

    public String getEncounterUuid() {
        return encounterUuid;
    }

    public void setEncounterUuid(String encounterUuid) {
        this.encounterUuid = encounterUuid;
    }

    public String getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, String[]> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public FormEntrySession getSession() throws Exception{
        if(session==null)createSession();
        return session;
    }

    /**
     * This method returns a FormEntrySession object using data in the instance object
     * @return FormEntrySession object
     * @throws Exception
     */
    private void createSession() throws Exception {
        //TODO: Check for null patientUuid and try to parse the xml to obtain the patient ID instead.
        Patient patient = Context.getPatientService().getPatientByUuid(getPatientUuid());
        session = new FormEntrySession(patient,getXmlDefinition(),null);
        HtmlForm htmlForm = HtmlFormEntryUtil.getService().getHtmlForm(htmlFormId);
        htmlForm.setXmlData(xmlDefinition);
        session.setHtmlForm(htmlForm);

        //getHtmlToDisplay() is called to generate necessary tag handlers and cache the form
        session.getHtmlToDisplay();

        //PrepareForSubmit is called to set patient and encounter if specified in tags
        session.prepareForSubmit();
    }

    /**
     *
     * @return
     */
    private HttpServletRequest createHttpServletRequest() throws Exception{
        if(getParameterMap()==null) {
            throw new Exception("Could not create Request without parameters");
        }
        HttpServletRequest request = new InnerHttpServletRequestMock(getParameterMap());
        return request;
    }

    public String getFileName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");
        String filename =  patientUuid + "-" + df.format(new Date());
        return filename;
    }

    /**
     *
     * @param directoryPath
     * @throws Exception
     */
    public void serializeToXml(String directoryPath) throws Exception {
        String filename;
        if(directoryPath.endsWith(File.separator)) {
            filename = directoryPath.concat(getFileName());
        } else {
            filename = directoryPath.concat(File.separator + getFileName());
        }

        //Use OpenMRS simpleXStreamSerializer
        OpenmrsSerializer serializer = Context.getSerializationService().getSerializer(SimpleXStreamSerializer.class);

        String xmlEquivalent = serializer.serialize(this);

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(filename+".xml"));
            bw.write(xmlEquivalent);
        }finally {
            if(bw != null) bw.close();
        }
    }
    /**
     * Given argument of the file this method tries to deserialize the contents of the file into SerializableFormObject
     * @param argument used to pass either a file path or a string representing the archivedData
     * @param isPath used to indicate whether the first argument is file path (true means it is)
     * @return  equivalent SerializableFormObject representation
     * @throws Exception
     */
    public static SerializableFormObject deserializeXml(String argument,boolean isPath) throws Exception {
        if(isPath) {
            //TODO:Check for existence of the file
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(argument));
                char[] buffer = new char[1024];
                StringBuilder xmlSb = new StringBuilder();
                int lengthRead;
                while ((lengthRead = br.read(buffer)) != -1) {
                    xmlSb.append(buffer, 0, lengthRead);
                }
                return deserializeXmlString(xmlSb.toString());
            } finally {
                if (br != null) br.close();
            }
        } else {
            return deserializeXmlString(argument);
        }
    }

    public static SerializableFormObject deserializeXml(String path) throws Exception {
        return deserializeXml(path,true);
    }

    public void handleSubmission() throws Exception{
        //Get the FormEntrySession & HttpServletRequest
        if(session==null)createSession();
        HttpServletRequest request = createHttpServletRequest();
        Map<Widget,String> fields = session.getContext().getFieldNames();

        //handle submission & save data
        session.getSubmissionController().handleFormSubmission(session, request);
    }

    private static SerializableFormObject deserializeXmlString(String xml) throws SerializationException{
        OpenmrsSerializer serializer = Context.getSerializationService().getSerializer(SimpleXStreamSerializer.class);
        return serializer.deserialize(xml, SerializableFormObject.class);
    }
}
