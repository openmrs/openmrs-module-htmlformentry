package org.openmrs.module.htmlformentry.widget;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that implements radio buttons, like a bunch of {@code <input type="radio"/>}.
 */
public class RadioButtonsWidget extends SingleOptionWidget {
	
	private String answerSeparator = null;
	
	/**
	 * Default Constructor
	 */
	public RadioButtonsWidget() {
		super();
	}
	
	/**
	 * @see Widget#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		String id = context.getFieldName(this);
		StringBuilder sb = new StringBuilder();
		if (context.getMode() == Mode.VIEW) {
            for (int i = 0; i < getOptions().size(); ++i) {
                Option opt = getOptions().get(i);
				boolean selected = getInitialValue() == null ? "".equals(opt.getValue()) : getInitialValue().equals(
				    opt.getValue());
				if (selected) {
					sb.append(WidgetFactory.displayValue("[X]&#160;" + opt.getLabel()));
				} else {
					sb.append(WidgetFactory.displayEmptyValue("[&#160;&#160;]&#160;" + opt.getLabel()));
				}
                if (i < getOptions().size() - 1) {
                    sb.append(getAnswerSeparator());
                }
			}
		} else {
			for (int i = 0; i < getOptions().size(); ++i) {
				Option option = getOptions().get(i);
				boolean selected = option.isSelected();
				if (!selected)
					selected = getInitialValue() == null ? option.getValue().equals("") : getInitialValue().equals(
					    option.getValue());
				sb.append("<input type=\"radio\" id=\"").append(id + "_" + i).append("\" name=\"")
				        .append(id).append("\" value=\"").append(option.getValue()).append("\"");
				if (selected)
					sb.append(" checked=\"true\"");
				sb.append(" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"");
				sb.append("/>");
				sb.append("<label for=\"").append(id + "_" + i).append("\">").append(option.getLabel()).append("</label>");
				if (i < getOptions().size() - 1) {
					sb.append(getAnswerSeparator());
                }
			}
		}
		return sb.toString();
	}
	
	/**
	 * @return the answerSeparator
	 */
	public String getAnswerSeparator() {
		if (answerSeparator == null)
			answerSeparator = "&#160;";
		return answerSeparator;
	}
	
	/**
	 * @param answerSeparator the answerSeparator to set
	 */
	public void setAnswerSeparator(String answerSeparator) {
		this.answerSeparator = answerSeparator;
	}
}
