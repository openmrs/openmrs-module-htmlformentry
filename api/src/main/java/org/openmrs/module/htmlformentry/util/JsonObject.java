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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
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
