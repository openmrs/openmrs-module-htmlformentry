package org.openmrs.module.htmlformentry.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.layout.web.address.AddressSupport;
import org.openmrs.layout.web.address.AddressTemplate;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that allows the input of a Person address. Implemented using text fields
 * that accept all name properties. The fields will display based on the 
 * layout and templet defined in the global property.
 */
public class AddressWidget extends Gadget {

	private PersonAddress initialValue;
	private Map<String, TextFieldWidget> widgetMap = new HashMap<String, TextFieldWidget>();

	public AddressWidget() {
		widgetMap.put("address1", new TextFieldWidget());
		widgetMap.put("address2", new TextFieldWidget());
		widgetMap.put("cityVillage", new TextFieldWidget());
		widgetMap.put("stateProvince", new TextFieldWidget());
		widgetMap.put("postalCode", new TextFieldWidget());
		widgetMap.put("country", new TextFieldWidget());
		widgetMap.put("latitude", new TextFieldWidget());
		widgetMap.put("longitude", new TextFieldWidget());
		widgetMap.put("countyDistrict", new TextFieldWidget());
		widgetMap.put("neighborhoodCell", new TextFieldWidget());
		widgetMap.put("townshipDivision", new TextFieldWidget());
		widgetMap.put("subregion", new TextFieldWidget());
		widgetMap.put("region", new TextFieldWidget());
	}

	public AddressWidget(PersonAddress personAddress) {
		this();
		setInitialValue(personAddress);
	}

	public String generateHtml(FormEntryContext context) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		AddressTemplate defaultLayoutTemplate = AddressSupport.getInstance().getDefaultLayoutTemplate();
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
			for (Iterator<Map<String, String>> iterator = line.iterator(); iterator.hasNext();colIndex++ ) {				
				
				fieldMap = iterator.next();				
				
				if (fieldMap.get("isToken").equals(defaultLayoutTemplate.getLayoutToken())) {
					
					String label = messageSourceService.getMessage(fieldMap.get("displayText"));
					textFieldWidget = widgetMap.get(fieldMap.get("codeName"));
					textFieldWidget.setTextFieldSize(Integer.parseInt(fieldMap.get("displaySize")));
					sb.append("<td>").append(label).append("</td>");
					if(!iterator.hasNext() && colIndex < defaultLayoutTemplate.getMaxTokens()){
						sb.append("<td colspan='").append(defaultLayoutTemplate.getMaxTokens()-colIndex).append("'>");
					}else{
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

	public PersonAddress getValue(FormEntryContext context, HttpServletRequest request) {

		PersonAddress returnPersonAddress = new PersonAddress();

		returnPersonAddress.setAddress1(getWidgetValue("address1", context, request));
		returnPersonAddress.setAddress2(getWidgetValue("address2", context, request));
		returnPersonAddress.setCityVillage(getWidgetValue("cityVillage", context, request));
		returnPersonAddress.setStateProvince(getWidgetValue("stateProvince", context, request));
		returnPersonAddress.setPostalCode(getWidgetValue("postalCode", context, request));
		returnPersonAddress.setCountry(getWidgetValue("country", context, request));
		returnPersonAddress.setLatitude(getWidgetValue("latitude", context, request));
		returnPersonAddress.setLongitude(getWidgetValue("longitude", context, request));
		returnPersonAddress.setCountyDistrict(getWidgetValue("countyDistrict", context, request));
		returnPersonAddress.setNeighborhoodCell(getWidgetValue("neighborhoodCell", context, request));
		returnPersonAddress.setTownshipDivision(getWidgetValue("townshipDivision", context, request));
		returnPersonAddress.setSubregion(getWidgetValue("subregion", context, request));
		returnPersonAddress.setRegion(getWidgetValue("region", context, request));

		if (context.getMode() == Mode.EDIT) {
			PersonAddress preferedAddress = context.getExistingPatient().getPersonAddress();
			if (preferedAddress != null && returnPersonAddress.equalsContent(preferedAddress)) {
				returnPersonAddress = preferedAddress;
			}
		}

		return returnPersonAddress;
	}

	private String getWidgetValue(String fieldName, FormEntryContext context, HttpServletRequest request) {
		return widgetMap.get(fieldName).getValue(context, request);
	}

	private void setWidgetValue(String fieldName, String value) {
		widgetMap.get(fieldName).setInitialValue(value);
	}

	public void setInitialValue(Object value) {
		if (value != null) {
			initialValue = (PersonAddress) value;

			setWidgetValue("address1", initialValue.getAddress1());
			setWidgetValue("address2", initialValue.getAddress2());
			setWidgetValue("cityVillage", initialValue.getCityVillage());
			setWidgetValue("stateProvince", initialValue.getStateProvince());
			setWidgetValue("postalCode", initialValue.getPostalCode());
			setWidgetValue("country", initialValue.getCountry());
			setWidgetValue("latitude", initialValue.getLatitude());
			setWidgetValue("longitude", initialValue.getLongitude());
			setWidgetValue("countyDistrict", initialValue.getCountyDistrict());
			setWidgetValue("neighborhoodCell", initialValue.getNeighborhoodCell());
			setWidgetValue("townshipDivision", initialValue.getTownshipDivision());
			setWidgetValue("subregion", initialValue.getSubregion());
			setWidgetValue("region", initialValue.getRegion());
		}
	}

	@Override
	protected void registerWidgets(FormEntryContext context) {
		for (String key : widgetMap.keySet()) {
			context.registerWidget(widgetMap.get(key));
		}
	}
}
