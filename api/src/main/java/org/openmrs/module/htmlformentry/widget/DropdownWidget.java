package org.openmrs.module.htmlformentry.widget;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

/**
 * A dropdown widget, like {@code <select name="..."><option value="...">...</option></select>}
 */
public class DropdownWidget extends SingleOptionWidget {
	
	private Integer size;
	
	/**
	 * Default Constructor
	 */
	public DropdownWidget() {
	}
	
	public DropdownWidget(Integer size) {
		this.size = size;
	}
	
	/**
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
			sb.append("<select id=\"" + id + "\" name=\"" + id + "\"");
			if (size != null) {
				if (size == 999) {
					size = getOptions().size() + 1; // Add one to make sure all elements show up without scrollbar
				}
				sb.append(" size=").append("\"" + size.intValue() + "\"");
			}
			sb.append(">");
			String currentOptionGroup = "";
			for (int i = 0; i < getOptions().size(); ++i) {
				Option option = getOptions().get(i);
				
				String optionGroup = (option.getGroupLabel() == null ? "" : option.getGroupLabel().trim());
				if (!optionGroup.equalsIgnoreCase(currentOptionGroup)) {
					if (StringUtils.isNotBlank(optionGroup)) {
						if (StringUtils.isNotBlank(currentOptionGroup)) {
							sb.append("</optgroup>");
						}
						sb.append("<optgroup label=\"").append(HtmlFormEntryUtil.translate(optionGroup)).append("\"");
						if (StringUtils.isNotBlank(option.getGroupCssClass())) {
							sb.append(" class=\"").append(option.getGroupCssClass()).append("\"");
						}
						sb.append(">");
					}
				}
				currentOptionGroup = optionGroup;
				
				boolean selected = option.isSelected();
				if (!selected)
					selected = getInitialValue() == null ? option.getValue().equals("")
					        : getInitialValue().equals(option.getValue());
				sb.append("<option value=\"").append(option.getValue()).append("\"");
				if (selected) {
					sb.append(" selected=\"true\"");
				}
				if (StringUtils.isNotBlank(option.getCssClass())) {
					sb.append(" class=\"").append(option.getCssClass()).append("\"");
				}
				sb.append(">");
				sb.append(option.getLabel());
				sb.append("</option>");
			}
			if (StringUtils.isNotBlank(currentOptionGroup)) {
				sb.append("</optgroup>");
			}
			sb.append("</select>");
			return sb.toString();
		}
	}
	
	public DropdownWidget clone() {
		DropdownWidget clone = new DropdownWidget(this.size);
		clone.setInitialValue(this.getInitialValue());
		clone.setOptions(this.getOptions());
		return clone;
	}
}
