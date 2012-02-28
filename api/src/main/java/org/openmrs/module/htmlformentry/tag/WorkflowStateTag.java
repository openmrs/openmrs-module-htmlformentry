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
package org.openmrs.module.htmlformentry.tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class WorkflowStateTag {
	
	private final String workflowId;
	
	private final List<String> stateIds;
	
	private final List<String> stateLabels;
	
	private final String type;
	
	private final String labelText;
	
	private final String labelCode;
	
	private final Set<String> allowedTypes;
	
	/**
	 * @param parameters
	 */
	public WorkflowStateTag(Map<String, String> parameters) {
		if (StringUtils.isBlank(parameters.get("workflowId"))) {
			throw new IllegalArgumentException("workflowId is missing");
		} else {
			workflowId = parameters.get("workflowId").trim();
		}
		
		if (!StringUtils.isBlank(parameters.get("stateId"))) {
			if (!StringUtils.isBlank(parameters.get("stateIds"))) {
				throw new IllegalArgumentException("stateId and stateIds must not be used together");
			}
			stateIds = Collections.unmodifiableList(Arrays.asList(parameters.get("stateId").trim()));
			allowedTypes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("checkbox", "hidden")));
		} else {
			if (!StringUtils.isBlank(parameters.get("stateIds"))) {
				String[] ids = parameters.get("stateIds").split(",");
				for (int i = 0; i < ids.length; i++) {
					ids[i] = ids[i].trim();
				}
				stateIds = Collections.unmodifiableList(Arrays.asList(ids));
			} else {
				stateIds = null;
			}
			allowedTypes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("radio", "dropdown")));
		}
		
		if (StringUtils.isBlank(parameters.get("type"))) {
			if (!StringUtils.isBlank(parameters.get("stateId"))) {
				type = "checkbox";
			} else {
				type = "dropdown";
			}
		} else {
			type = parameters.get("type").trim();
		}
		
		if (!allowedTypes.contains(type)) {
			throw new IllegalArgumentException("Invalid type: " + type + ". Allowed type: "
			        + StringUtils.join(allowedTypes, ","));
		}
		
		if (StringUtils.isBlank(parameters.get("stateLabels"))) {
			if (StringUtils.isBlank(parameters.get("stateLabel"))) {
				stateLabels = null;
			} else {
				if (StringUtils.isBlank(parameters.get("stateId"))) {
					throw new IllegalArgumentException("stateId must be specfified if stateLabel used");
				}
				stateLabels = Collections.unmodifiableList(Arrays.asList(parameters.get("stateLabel").trim()));
			}
		} else {
			if (StringUtils.isBlank(parameters.get("stateIds"))) {
				throw new IllegalArgumentException("stateIds must be specfified if stateLabels used");
			}
			String[] labels = parameters.get("stateLabels").split(",");
			for (int i = 0; i < labels.length; i++) {
				labels[i] = labels[i].trim();
			}
			if (labels.length != stateIds.size()) {
				throw new IllegalArgumentException("stateLabels length " + labels.length + " must match stateIds length "
				        + stateIds.size());
			}
			stateLabels = Collections.unmodifiableList(Arrays.asList(labels));
		}
		
		if (!StringUtils.isBlank(parameters.get("labelText"))) {
			labelText = parameters.get("labelText").trim();
		} else {
			labelText = null;
		}
		
		if (!StringUtils.isBlank(parameters.get("labelCode"))) {
			labelCode = parameters.get("labelCode").trim();
		} else {
			labelCode = null;
		}
		
		if (type.equals("hidden")) {
			if (labelText != null || labelCode != null) {
				throw new IllegalArgumentException("labelText and labelCode are not allowed for the hidden type");
			}
		}
	}
	
	/**
	 * @return the workflowId
	 */
	public String getWorkflowId() {
		return workflowId;
	}
	
	/**
	 * @return the stateIds
	 */
	public List<String> getStateIds() {
		return stateIds;
	}
	
	/**
	 * @return the stateLabels
	 */
	public List<String> getStateLabels() {
		return stateLabels;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return the labelText
	 */
	public String getLabelText() {
		return labelText;
	}
	
	/**
	 * @return the labelCode
	 */
	public String getLabelCode() {
		return labelCode;
	}
	
	/**
	 * @return the allowedStyles
	 */
	public Set<String> getAllowedTypes() {
		return allowedTypes;
	}
	
}
