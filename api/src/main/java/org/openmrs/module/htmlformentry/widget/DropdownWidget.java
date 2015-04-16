package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A dropdown widget, like {@code <select name="..."><option value="...">...</option></select>}
 */
public class DropdownWidget extends SingleOptionWidget {

    private Integer size;
	/**
	 * Default Constructor
	 */
    public DropdownWidget() { }

    public DropdownWidget(Integer size) {
           this.size = size;
    }

    /**
     * 
     * @see Widget#generateHtml(FormEntryContext)
     */
    @Override
    public String generateHtml(FormEntryContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            String toPrint = "";
            if (getInitialValue() != null) {
                // lookup the label for the selected value
                boolean found = false;
                for (Option o : getOptions()) {
                    if (getInitialValue().equals(o.getValue())) {
                        toPrint = o.getLabel();
                        found = true;
                        break;
                    }
                }
                if (!found)
                    toPrint = getInitialValue();
                return WidgetFactory.displayValue(toPrint);
            } else {
                return WidgetFactory.displayDefaultEmptyValue();
            }
        } else {
            String id = context.getFieldName(this);
            sb.append("<select id=\"" + id + "\" name=\"" + id +"\"");
            if(size !=null){
                if (size == 999) {
                    size = getOptions().size()+1; // Add one to make sure all elements show up without scrollbar
                }
                sb.append(" size=").append("\"" + size.intValue() + "\"");
            }
            sb.append(">");
            for (int i = 0; i < getOptions().size(); ++i) {
                Option option = getOptions().get(i);
                boolean selected = option.isSelected();
                if (!selected)
                    selected = getInitialValue() == null ? option.getValue().equals("") : getInitialValue().equals(option.getValue());
                sb.append("<option value=\"").append(option.getValue()).append("\"");
                if (selected)
                    sb.append(" selected=\"true\"");
                sb.append(">");
                sb.append(option.getLabel());
                sb.append("</option>");
            }
            sb.append("</select>");
            return sb.toString();
        }
    }
}
