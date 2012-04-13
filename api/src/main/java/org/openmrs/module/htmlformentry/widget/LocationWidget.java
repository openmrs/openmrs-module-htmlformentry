package org.openmrs.module.htmlformentry.widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.WebConstants;
import org.springframework.util.StringUtils;

/**
 * A widget that allows for the selection of a Location. Implemented using a drop-down selection
 * list.
 */
public class LocationWidget implements Widget {
	
	private Location location;
	
	private List<Location> options;
	
	private String type;
	
	public LocationWidget() {
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			if (location != null)
				return WidgetFactory.displayValue(location.getName());
			else
				return "";
		}
		
		List<Location> useLocations;
		if (options != null) {
			useLocations = options;
		} else {
			useLocations = Context.getLocationService().getAllLocations();
			Collections.sort(useLocations, new Comparator<Location>() {
				
				@Override
				public int compare(Location left, Location right) {
					return left.getName().compareTo(right.getName());
				}
			});
		}
		
		StringBuilder sb = new StringBuilder();
		if ("autocomplete".equalsIgnoreCase(type)) {
			sb.append("<input type=\"text\" id=\"display_" + context.getFieldName(this) + "\" value=\""
			        + ((location != null) ? location.getName() : "")
			        + "\" onchange=\"updateFormField(this)\" placeholder=\""
			        + Context.getMessageSourceService().getMessage("htmlformentry.form.location.placeholder") + "\" />");
			sb.append("\n<input type=\"hidden\" id=\"" + context.getFieldName(this) + "\" name=\""
			        + context.getFieldName(this) + "\" value=\"" + ((location != null) ? location.getLocationId() : "")
			        + "\" />");
			sb.append("\n<script src=\"/" + WebConstants.WEBAPP_NAME
			        + "/moduleResources/htmlformentry/jquery.ui.autocomplete.autoSelect.js");
			sb.append("?v=").append(OpenmrsConstants.OPENMRS_VERSION_SHORT + "\" type=\"text/javascript\">");
			sb.append("\n</script>");
			sb.append("\n<script>");
			sb.append("\nvar locationNameIdMap = new Object();");
			for (Location location : useLocations) {
				sb.append("\nlocationNameIdMap['" + location.getName() + "'] = " + location.getLocationId() + ";");
			}
			sb.append("\n");
			//clear the form field when user clears the field
			sb.append("\nfunction updateFormField(displayField){");
			sb.append("\n	if($j.trim($j(displayField).val()) == '')");
			sb.append("\n		$j(\"#" + context.getFieldName(this) + "\").val('');");
			sb.append("\n}");
			sb.append("\n");
			sb.append("\n$j('input#display_" + context.getFieldName(this) + "').autocomplete({");
			sb.append("\n	source:[" + StringUtils.collectionToDelimitedString(useLocations, ",", "\"", "\"") + "],");
			sb.append("\n	select: function(event, ui) {");
			sb.append("\n				$j(\"#" + context.getFieldName(this) + "\").val(locationNameIdMap[ui.item.value]);");
			sb.append("\n			}");
			sb.append("\n});");
			sb.append("</script>");
			
		} else {
			sb.append("<select id=\"" + context.getFieldName(this) + "\" name=\"" + context.getFieldName(this) + "\">");
			sb.append("\n<option value=\"\">");
			sb.append(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation"));
			sb.append("</option>");
			for (Location l : useLocations) {
				sb.append("\n<option");
				if (location != null && location.equals(l))
					sb.append(" selected=\"true\"");
				sb.append(" value=\"" + l.getLocationId() + "\">").append(l.getName()).append("</option>");
			}
			sb.append("</select>");
		}
		
		return sb.toString();
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String val = request.getParameter(context.getFieldName(this));
		if (StringUtils.hasText(val))
			return HtmlFormEntryUtil.convertToType(val, Location.class);
		return null;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		location = (Location) initialValue;
	}
	
	/**
	 * Sets the Locations to use as options for this widget
	 */
	public void setOptions(List<Location> options) {
		this.options = options;
	}
	
	/**
	 * Sets the type of input element to generate
	 * 
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
}
