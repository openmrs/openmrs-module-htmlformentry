package org.openmrs.module.htmlformentry.extender;

import org.openmrs.module.htmlformentry.FormEntrySession;

public interface FormActionsExtender {

    public void applyActions(FormEntrySession session);

    public FormActionsExtenderType getType();

}
