package org.openmrs.module.htmlformentry;

import org.openmrs.Encounter;
import org.openmrs.Patient;

import javax.servlet.http.Cookie;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

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

public class SerializableFormObject implements Serializable{
    private static final long serialVersionUID = 1L;

    private String patientIdentifier;
    private String patientUuid;
    private String encounterUuid;
    private Map<String,String[]> parameterMap;
    private String xmlDefinition;


    public SerializableFormObject() {
    }

    public SerializableFormObject(String xmlDefinition, Map<String, String[]> parameterMap) {
        this.parameterMap = parameterMap;
        this.xmlDefinition = xmlDefinition;
    }

    public SerializableFormObject(String xmlDefinition, Map<String, String[]> parameterMap, String patientIdentifier,
                                  String patientUuid, String encounterUuid) {
        this.patientIdentifier = patientIdentifier;
        this.xmlDefinition = xmlDefinition;
        this.encounterUuid = encounterUuid;
        this.patientUuid = patientUuid;
        this.parameterMap = parameterMap;
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

    public String getFileName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");
        String filename =  patientUuid + "-" + df.format(new Date());
        return filename;
    }
}
