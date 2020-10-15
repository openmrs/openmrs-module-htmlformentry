/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.OpenmrsMetadata;
import org.openmrs.module.htmlformentry.FormEntryContext;

/**
 * Widget that lets you choose a {@link OpenmrsMetadata} from a dropdown
 */
public class MetadataDropdownWidget<T extends OpenmrsMetadata> extends DropdownWidget {
	
	private Map<String, T> metadataVals = new LinkedHashMap<>();
	
	public MetadataDropdownWidget(List<T> metadataOptions, String emptyLabel) {
		super();
		List<Option> options = new ArrayList<Option>();
		if (emptyLabel != null) {
			options.add(new Option(emptyLabel, "", false));
		}
		for (T m : metadataOptions) {
			options.add(new Option(m.getName(), m.getId().toString(), false));
			metadataVals.put(m.getId().toString(), m);
		}
		setOptions(options);
	}
	
	public void setInitialMetadataValue(T m) {
		if (m == null) {
			setInitialValue(null);
		} else {
			setInitialValue(m.getId().toString());
		}
	}

	public T getMetadataValue(FormEntryContext context, HttpServletRequest request) {
		Object value = getValue(context, request);
		return value == null ? null : metadataVals.get(value.toString());
	}
}
