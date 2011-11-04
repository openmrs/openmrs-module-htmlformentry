package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that implements radio buttons, like a bunch of {@code <input type="radio"/>}.
 */
public class RadioButtonsWidget extends SingleOptionWidget {

	/**
	 * Default Constructor
	 */
    public RadioButtonsWidget() {
    	super();
    }
    
    /**
     * @see Widget#generateHtml(FormEntryContext)
     */
    public String generateHtml(FormEntryContext context) {
        String id = context.getFieldName(this);
        StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            for (Option opt : getOptions()) {
                boolean selected = getInitialValue() == null ?
                    "".equals(opt.getValue()) :
                    getInitialValue().equals(opt.getValue());
                if (selected) {
                    sb.append(WidgetFactory.displayValue("[X]&nbsp;" + opt.getLabel() + "&nbsp;"));
                } else {
                    sb.append(WidgetFactory.displayEmptyValue("[&nbsp;&nbsp;]&nbsp;" + opt.getLabel()));
                }
            }
        } else {
            for (int i = 0; i < getOptions().size(); ++i) {
                Option option = getOptions().get(i);
                boolean selected = option.isSelected();
                if (!selected)
                    selected = getInitialValue() == null ? option.getValue().equals("") : getInitialValue().equals(option.getValue());
                sb.append("<input type=\"radio\" id=\"").append(id + "_" + i)
                        .append("\" name=\"").append(id)
                        .append("\" value=\"").append(option.getValue()).append("\"");
                if (selected)
                    sb.append(" checked=\"true\"");
                sb.append(" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"");
                sb.append("/>");
                sb.append(option.getLabel());
                if (i < getOptions().size() - 1)
                    sb.append("&nbsp;");
            }
        }
        return sb.toString();
    }
}
