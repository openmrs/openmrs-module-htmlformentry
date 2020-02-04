package org.openmrs.module.htmlformentry;

import org.openmrs.ProgramAttributeType;
import org.openmrs.api.context.Context;

/**
 * HTML Form Entry utility methods for Platform 2.2
 */
public class HTMLFormEntryUtil2_2 {

    /***
     * Get the program attribute by: 1)an integer id like 5090 or 2) uuid like
     * "a3e12268-74bf-11df-9768-17cfc9833272" or 3) name
     *
     * @param id
     * @return the program attribute if exist, else null
     * @should find a program attribute by its id
     * @should find a program by its uuid
     * @should return null otherwise
     *
     *
     * TODO: find a program attribute by name
     *
     */
    public static ProgramAttributeType getProgramAttributeType (String id) {

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

            //get Program by mapping
            programAttributeType = HtmlFormEntryUtil.getMetadataByMapping(ProgramAttributeType.class, id);
            if(programAttributeType != null){
                return programAttributeType;
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
