package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * An abstract widget implementation that represents a widget that can be opened in edit mode (a field on a form)
 */
public abstract class EditableWidget implements Widget {

    private boolean viewOnly = false;

    /**
     * @return true if the widget should be rendered in VIEW mode
     */
    public boolean renderInViewMode(FormEntryContext context) {
        return viewOnly || context.getMode() == Mode.VIEW;
    }

    public boolean isViewOnly() {
        return viewOnly;
    }

    public void setViewOnly(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }
}
