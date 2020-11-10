/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.htmlformentry.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.OpenmrsData;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.api.context.Context;

/**
 * Simple Object class that facilitates constructing a json message
 */
public class JsonObject extends LinkedHashMap<String, Object> {
	
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	public JsonObject() {
	}
	
	public static JsonObject fromJson(String json) {
		try {
			return objectMapper.readValue(json, JsonObject.class);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to parse json", e);
		}
	}
	
	public void addString(String key, String value) {
		put(key, value);
	}
	
	public void addIdAndLabel(String key, String idName, String labelName, Object val) {
		JsonObject o = addObject(key);
		String id = "";
		String label = "";
		if (val != null) {
			if (val instanceof OpenmrsMetadata) {
				OpenmrsMetadata metadata = (OpenmrsMetadata) val;
				id = metadata.getId().toString();
				label = metadata.getName();
			} else if (val instanceof OpenmrsData) {
				OpenmrsData data = (OpenmrsData) val;
				id = data.getId().toString();
				label = data.getId().toString();
			} else if (val instanceof Concept) {
				Concept conceptVal = (Concept) val;
				id = conceptVal.getId().toString();
				label = conceptVal.getDisplayString();
			} else if (val instanceof Date) {
				Date dateVal = (Date) val;
				id = new SimpleDateFormat("yyyy-MM-dd").format(dateVal);
				label = Context.getDateFormat().format(dateVal);
			} else if (val instanceof Class) {
				Class classValue = (Class) val;
				id = classValue.getName();
				label = classValue.getSimpleName();
			} else if (val instanceof Enum) {
				Enum enumVal = (Enum) val;
				id = enumVal.name();
				label = enumVal.name();
			} else {
				id = val.toString();
				label = val.toString();
			}
		}
		if (idName != null) {
			o.addString(idName, id);
		}
		if (labelName != null) {
			o.addString(labelName, label);
		}
	}
	
	public void addTranslation(String prefix, String key) {
		addString(key, Context.getMessageSourceService().getMessage(prefix + key));
	}
	
	public JsonObject addObjectToArray(String key) {
		List<JsonObject> l = getObjectArray(key);
		JsonObject o = new JsonObject();
		l.add(o);
		return o;
	}
	
	public JsonObject addObject(String key) {
		JsonObject o = new JsonObject();
		put(key, o);
		return o;
	}
	
	public String getString(String key) {
		return (String) get(key);
	}
	
	public JsonObject getObject(String key) {
		return (JsonObject) get(key);
	}
	
	public List<JsonObject> getObjectArray(String key) {
		List<JsonObject> l = (List<JsonObject>) get(key);
		if (l == null) {
			l = new ArrayList<>();
			put(key, l);
		}
		return l;
	}
	
	public String toJson() {
		try {
			return objectMapper.writeValueAsString(this);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to write to json", e);
		}
	}
}
