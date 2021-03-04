package org.openmrs.module.htmlformentry.widget;

/**
 * Represents a widget Option
 */
public class Option {
	
	private String label;
	
	private String value;
	
	private String groupLabel; // Optional to provide information to widget as to how to group with other options
	
	private String groupCssClass; // Optional to add a particular css class to the grouping element
	
	private String cssClass; // Optional to enable setting particular class attributes on the rendered option
	
	private boolean selected = false;
	
	private boolean retired = false;
	
	public Option() {
		value = "";
		label = "";
	}
	
	public Option(String label, String value) {
		this(label, value, false);
	}
	
	public Option(String label, String value, boolean selected) {
		this.label = label;
		this.value = value;
		this.selected = selected;
	}
	
	public Option(String label, String value, boolean selected, boolean retired) {
		this.label = label;
		this.value = value;
		this.selected = selected;
		this.retired = retired;
	}
	
	/**
	 * Gets the label to use with the Option
	 * 
	 * @return label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Sets the label to use with the Option
	 * 
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * Gets the value for the Option
	 * 
	 * @return value
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Sets the value for the Option
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Returns true/false whether or not the Option is selected
	 * 
	 * @return true/false is selected
	 */
	public boolean isSelected() {
		return selected;
	}
	
	/**
	 * Sets whether or not the Option is selected
	 * 
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	/**
	 * Returns true/false whether or not the underlying OpenMRS entity representing the item is retired
	 *
	 * @return true/false
	 */
	public boolean isRetired() {
		return retired;
	}
	
	public void setRetired(boolean retired) {
		this.retired = retired;
	}
	
	/**
	 * If the Option has a value, returns the value, otherwise returns the label
	 * 
	 * @return value or label
	 */
	public String getValueOrLabel() {
		if (value != null)
			return value;
		else
			return label;
	}
	
	public String getGroupLabel() {
		return groupLabel;
	}
	
	public void setGroupLabel(String groupLabel) {
		this.groupLabel = groupLabel;
	}
	
	public String getGroupCssClass() {
		return groupCssClass;
	}
	
	public void setGroupCssClass(String groupCssClass) {
		this.groupCssClass = groupCssClass;
	}
	
	public String getCssClass() {
		return cssClass;
	}
	
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}
}
