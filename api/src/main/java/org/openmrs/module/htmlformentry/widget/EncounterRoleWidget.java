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

import org.openmrs.EncounterRole;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Widget that lets you choose an {@link EncounterRole} from a dropdown.
 */
public class EncounterRoleWidget implements Widget {
	
	private EncounterRole initialValue;
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (EncounterRole) initialValue;
	}

	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
            if (initialValue != null)
                return WidgetFactory.displayValue(initialValue.getName());
            else
                return "";
        }

		List<EncounterRole> roles = Context.getEncounterService().getAllEncounterRoles(true);
        Collections.sort(roles, new Comparator<EncounterRole>() {
			@Override
            public int compare(EncounterRole left, EncounterRole right) {
	            return left.getName().compareTo(right.getName());
            }
        });
		
		StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"" + context.getFieldName(this) + "\">");
        sb.append("\n<option value=\"\">");
        sb.append(Context.getMessageSourceService().getMessage("htmlformentry.chooseAnEncounterRole"));
        sb.append("</option>");
        
        for (EncounterRole role : roles) {
        	sb.append("\n<option ");
        	if (initialValue != null && initialValue.equals(role))
        		sb.append("selected=\"true\" ");
        	sb.append("value=\"" + role.getId() + "\">").append(role.getName()).append("</option>");
        }
        sb.append("</select>");
		return sb.toString();
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val)) {
        	return Context.getEncounterService().getEncounterRole(Integer.valueOf(val));
        }
        return null;
	}
	
}
