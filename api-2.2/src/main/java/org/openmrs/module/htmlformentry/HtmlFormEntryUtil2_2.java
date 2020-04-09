package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.context.Context;

import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;


/**
 * HTML Form Entry utility methods for the 2.2 Platform profile
 */
public class HtmlFormEntryUtil2_2 {

	public static Log log = LogFactory.getLog(HtmlFormEntryUtil2_2.class);

	/***
	 * Get the program attribute type by: 1)an integer id like 5090 or 2) uuid like
	 * "a3e12268-74bf-11df-9768-17cfc9833272"
	 *
	 * @param id
	 * @return the program attribute type if exist, else null
	 * <strong>Should</strong> find a program attribute type by its id
	 * <strong>Should</strong> find a program attribute type by its uuid
	 * <strong>Should</strong> return null otherwise
	 */
	public static ProgramAttributeType getProgramAttributeType(String id) {

		ProgramAttributeType programAttributeType = null;

		if (id != null) {

            id = id.trim();

			// see if this is parseable int; if so, try looking up by id
			try {//handle integer: id
				int programAttributeTypeId = Integer.parseInt(id);
				programAttributeType = Context.getProgramWorkflowService().getProgramAttributeType(programAttributeTypeId);

				if (programAttributeType != null) {
					return programAttributeType;
				}
			}
			catch (Exception ex) {
				//do nothing
			}

			//handle uuid id: "a3e1302b-74bf-11df-9768-17cfc9833272", if id matches uuid format
			if (HtmlFormEntryUtil.isValidUuidFormat(id)) {
				programAttributeType = Context.getProgramWorkflowService().getProgramAttributeTypeByUuid(id);

				if (programAttributeType != null) {
					return programAttributeType;
				}
			}

		}
		return programAttributeType;
	}
}
