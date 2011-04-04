package org.openmrs.module.htmlformentry.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.layout.web.name.NameSupport;
import org.openmrs.layout.web.name.NameTemplate;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that allows the input of a Person name. Implemented using text fields
 * that accept all name properties. The fields will display based on the 
 * layout and templet defined in the global property.
 */
public class NameWidget extends Gadget {

	private Map<String, TextFieldWidget> widgetMap = new HashMap<String, TextFieldWidget>();

	public NameWidget() {
		widgetMap.put("prefix", new TextFieldWidget());
		widgetMap.put("givenName", new TextFieldWidget());
		widgetMap.put("middleName", new TextFieldWidget());
		widgetMap.put("familyNamePrefix", new TextFieldWidget());
		widgetMap.put("familyNameSuffix", new TextFieldWidget());
		widgetMap.put("familyName", new TextFieldWidget());
		widgetMap.put("familyName2", new TextFieldWidget());
		widgetMap.put("degree", new TextFieldWidget());
	}

	public NameWidget(PersonName personName) {
		this();
		setInitialValue(personName);
	}

	public String generateHtml(FormEntryContext context) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		NameTemplate defaultLayoutTemplate = NameSupport.getInstance().getDefaultLayoutTemplate();
		TextFieldWidget textFieldWidget;
		Map<String, String> fieldMap;

		if (!isRegistered) {
			registerWidgets(context);
			isRegistered = true;
		}

		// have the date and time widgets generate their HTML
		StringBuilder sb = new StringBuilder();

		sb.append("<table>");

		List<List<Map<String, String>>> fieldLines = defaultLayoutTemplate.getLines();

		for (List<Map<String, String>> line : fieldLines) {
			sb.append("<tr>");
			int colIndex = 0;
			for (Iterator<Map<String, String>> iterator = line.iterator(); iterator.hasNext(); colIndex++) {

				fieldMap = iterator.next();

				if (fieldMap.get("isToken").equals(defaultLayoutTemplate.getLayoutToken())) {

					String label = messageSourceService.getMessage(fieldMap.get("displayText"));
					textFieldWidget = widgetMap.get(fieldMap.get("codeName"));
					textFieldWidget.setTextFieldSize(Integer.parseInt(fieldMap.get("displaySize")));
					sb.append("<td>").append(label).append("</td>");
					if (!iterator.hasNext() && colIndex < defaultLayoutTemplate.getMaxTokens()) {
						sb.append("<td colspan='").append(defaultLayoutTemplate.getMaxTokens() - colIndex).append("'>");
					}
					else {
						sb.append("<td>");
					}
					sb.append(textFieldWidget.generateHtml(context)).append("</td>");
				}
			}
			sb.append("</tr>");
		}
		sb.append("</table> \n");
		return sb.toString();
	}

	public PersonName getValue(FormEntryContext context, HttpServletRequest request) {

		PersonName returnPersonName = new PersonName();

		returnPersonName.setPrefix(getWidgetValue("prefix", context, request));
		returnPersonName.setGivenName(getWidgetValue("givenName", context, request));
		returnPersonName.setMiddleName(getWidgetValue("middleName", context, request));
		returnPersonName.setFamilyName(getWidgetValue("familyName", context, request));
		returnPersonName.setFamilyName2(getWidgetValue("familyName2", context, request));
		returnPersonName.setFamilyNamePrefix(getWidgetValue("familyNamePrefix", context, request));
		returnPersonName.setFamilyNameSuffix(getWidgetValue("familyNameSuffix", context, request));
		returnPersonName.setDegree(getWidgetValue("degree", context, request));

		if (context.getMode() == Mode.EDIT) {
			PersonName preferedName = context.getExistingPatient().getPersonName();
			if (preferedName != null && returnPersonName.equalsContent(preferedName)) {
				returnPersonName = preferedName;
			}
		}

		return returnPersonName;
	}

	public void setInitialValue(Object value) {
		if (value != null) {
			PersonName initialValue = (PersonName) value;
			setWidgetValue("prefix", initialValue.getPrefix());
			setWidgetValue("givenName", initialValue.getGivenName());
			setWidgetValue("middleName", initialValue.getMiddleName());
			setWidgetValue("familyNamePrefix", initialValue.getFamilyNamePrefix());
			setWidgetValue("familyNameSuffix", initialValue.getFamilyNameSuffix());
			setWidgetValue("familyName", initialValue.getFamilyName());
			setWidgetValue("familyName2", initialValue.getFamilyName2());
			setWidgetValue("degree", initialValue.getDegree());
		}
	}

	@Override
	protected void registerWidgets(FormEntryContext context) {
		for (TextFieldWidget textWidget : widgetMap.values()) {
			context.registerWidget(textWidget);
		}
	}

	private String getWidgetValue(String fieldName, FormEntryContext context, HttpServletRequest request) {
		return widgetMap.get(fieldName).getValue(context, request);
	}

	private void setWidgetValue(String fieldName, String value) {
		widgetMap.get(fieldName).setInitialValue(value);
	}
}
