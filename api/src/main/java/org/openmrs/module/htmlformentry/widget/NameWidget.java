package org.openmrs.module.htmlformentry.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.compatibility.NameSupportCompatibility;

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

	@Override
    public String generateHtml(FormEntryContext context) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		NameSupportCompatibility nameSupport = Context.getRegisteredComponent("htmlformentry.NameSupportCompatibility", NameSupportCompatibility.class);

		TextFieldWidget textFieldWidget;
		Map<String, String> fieldMap;

		if (!isRegistered) {
			registerWidgets(context);
			isRegistered = true;
		}

		// have the date and time widgets generate their HTML
		StringBuilder sb = new StringBuilder();

		sb.append("<table>");

		List<List<Map<String, String>>> fieldLines = nameSupport.getLines();

		for (List<Map<String, String>> line : fieldLines) {
			sb.append("<tr>");
			int colIndex = 0;
			for (Iterator<Map<String, String>> iterator = line.iterator(); iterator.hasNext(); colIndex++) {

				fieldMap = iterator.next();

				if (fieldMap.get("isToken").equals(nameSupport.getLayoutToken())) {

					String label = messageSourceService.getMessage(fieldMap.get("displayText"));
					textFieldWidget = widgetMap.get(fieldMap.get("codeName"));
					textFieldWidget.setTextFieldSize(Integer.parseInt(fieldMap.get("displaySize")));
					sb.append("<td>").append(label).append("</td>");
					if (!iterator.hasNext() && colIndex < nameSupport.getMaxTokens()) {
						sb.append("<td colspan='").append(nameSupport.getMaxTokens() - colIndex).append("'>");
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

	@Override
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
		
		if (context.getExistingPatient() != null) {
			PersonName originalPreferedName = context.getExistingPatient().getPersonName();
			
			if (originalPreferedName != null && isPersonNameEqual(originalPreferedName, returnPersonName)) {
				returnPersonName = originalPreferedName;
			}
		}
		
		return returnPersonName;
	}
	
	private boolean isPersonNameEqual(PersonName personName1, PersonName personName2) {
		
		boolean returnValue = true;
		
		// these are the methods to compare. All are expected to be Strings
		String[] methods = { "getGivenName", "getMiddleName", "getFamilyName" };
		
		Class<? extends PersonName> nameClass = personName1.getClass();
		
		// loop over all of the selected methods and compare this and other
		for (String methodName : methods) {
			try {
				Method method = nameClass.getMethod(methodName, new Class[] {});
				
				String person1Value = (String) method.invoke(personName1);
				String person2Value = (String) method.invoke(personName2);
				
				if (person2Value != null && person2Value.length() > 0)
					returnValue &= person2Value.equals(person1Value);
				else if (person1Value != null && person1Value.length() > 0)
					returnValue &= false;
				
			}
			catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return returnValue;
	}

	@Override
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
