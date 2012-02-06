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
	
	private final String style;
	
	private final String label;
	
	private final String enrollInProgram;
	
	private final Set<String> allowedStyles;
	
	/**
	 * @param parameters
	 */
	public WorkflowStateTag(Map<String, String> parameters) {
		if (StringUtils.isBlank(parameters.get("workflowId"))) {
			throw new IllegalArgumentException("workflowId is missing");
		} else {
			workflowId = parameters.get("workflowId");
		}
		
		if (!StringUtils.isBlank(parameters.get("stateId"))) {
			if (!StringUtils.isBlank(parameters.get("stateIds"))) {
				throw new IllegalArgumentException("stateId and stateIds must not be used together");
			}
			stateIds = Collections.unmodifiableList(Arrays.asList(parameters.get("stateId")));
			allowedStyles = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("checkbox", "hidden")));
		} else {
			if (!StringUtils.isBlank(parameters.get("stateIds"))) {
				String[] ids = parameters.get("stateIds").split(",");
				stateIds = Collections.unmodifiableList(Arrays.asList(ids));
			} else {
				stateIds = null;
			}
			allowedStyles = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("radio", "dropdown")));
		}
		
		if (StringUtils.isBlank(parameters.get("style"))) {
			style = "dropdown";
		} else {
			style = parameters.get("style");
		}
		
		if (!allowedStyles.contains(style)) {
			throw new IllegalArgumentException("Invalid style: " + style + ". Allowed styles: "
			        + StringUtils.join(allowedStyles, ","));
		}
		
		if (StringUtils.isBlank(parameters.get("stateLabels"))) {
			if (StringUtils.isBlank(parameters.get("stateLabel"))) {
				stateLabels = null;
			} else {
				if (!StringUtils.isBlank(parameters.get("stateId"))) {
					throw new IllegalArgumentException("stateId must be specfified if stateLabel used");
				}
				stateLabels = Collections.unmodifiableList(Arrays.asList(parameters.get("stateLabel")));
			}
		} else {
			if (stateIds == null) {
				throw new IllegalArgumentException("stateIds must be specfified if stateLabels used");
			}
			String[] labels = parameters.get("stateLabels").split(",");
			if (labels.length != stateIds.size()) {
				throw new IllegalArgumentException("stateLabels length " + labels.length + " must match stateIds length "
				        + stateIds.size());
			}
			stateLabels = Collections.unmodifiableList(Arrays.asList(labels));
		}
		
		if (!StringUtils.isBlank(parameters.get("label"))) {
			label = parameters.get("label");
		} else {
			label = null;
		}
		
		if (StringUtils.isBlank(parameters.get("enrollInProgram"))) {
			enrollInProgram = "true";
		} else {
			enrollInProgram = parameters.get("enrollInProgram");
			if (!enrollInProgram.equalsIgnoreCase("true") || !enrollInProgram.equalsIgnoreCase("false")) {
				throw new IllegalArgumentException("enrollInProgram invalid: " + enrollInProgram
				        + ". Allowed values: true or false.");
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
	 * @return the style
	 */
	public String getStyle() {
		return style;
	}
	
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @return the enrollInProgram
	 */
	public String getEnrollInProgram() {
		return enrollInProgram;
	}
	
	/**
	 * @return the allowedStyles
	 */
	public Set<String> getAllowedStyles() {
		return allowedStyles;
	}
	
}
