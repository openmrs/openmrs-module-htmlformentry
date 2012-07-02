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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.EncounterType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows for the selection of an {@link EncounterType}. Implemented using a drop-down
 * selection list.
 */
public class EncounterTypeWidget implements Widget {
	
	private EncounterType encounterType;
	
	private List<EncounterType> options;
	
	public EncounterTypeWidget() {
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			if (encounterType != null)
				return WidgetFactory.displayValue(encounterType.getName());
			else
				return "";
		}
		
		List<EncounterType> encounterTypes;
		if (options != null) {
			encounterTypes = options;
		} else {
			encounterTypes = Context.getEncounterService().getAllEncounterTypes();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<select id=\"" + context.getFieldName(this) + "\" name=\"" + context.getFieldName(this) + "\">");
		sb.append("\n<option value=\"\">");
		sb.append(Context.getMessageSourceService().getMessage("htmlformentry.chooseEncounterType"));
		sb.append("</option>");
		for (EncounterType type : encounterTypes) {
			sb.append("\n<option");
			if (encounterType != null && encounterType.equals(type))
				sb.append(" selected=\"true\"");
			sb.append(" value=\"" + type.getEncounterTypeId() + "\">").append(type.getName()).append("</option>");
		}
		sb.append("</select>");
		
		return sb.toString();
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	@Override
	public void setInitialValue(Object initialValue) {
		encounterType = (EncounterType) initialValue;
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String val = request.getParameter(context.getFieldName(this));
		if (StringUtils.hasText(val))
			return HtmlFormEntryUtil.convertToType(val, EncounterType.class);
		return null;
	}
	
	/**
	 * @param options the options to set
	 */
	public void setOptions(List<EncounterType> options) {
		this.options = options;
	}
}
