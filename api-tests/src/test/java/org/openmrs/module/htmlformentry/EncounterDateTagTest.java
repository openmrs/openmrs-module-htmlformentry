package org.openmrs.module.htmlformentry;

import static java.util.Calendar.MILLISECOND;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class EncounterDateTagTest extends BaseHtmlFormEntryTest {
	
	@Test
	public void testSubmitEncounterDatetimeWithTimeZone() throws Exception {
		new RegressionTestHelper() {
			
			private void setGlobalProperty(String name, String value) {
				GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(name);
				if (gp == null) {
					gp = new GlobalProperty(name);
				}
				gp.setPropertyValue(value);
				Context.getAdministrationService().saveGlobalProperty(gp);
			}
			
			@Override
			public String getFormName() {
				return "";
			}
			
			@Override
			public String getFormXml() {
				return "<htmlform> Datetime: <encounterDate showTime=\"true\" /> </htmlform>";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Datetime" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				setGlobalProperty(HtmlFormEntryConstants.GP_HANDLE_TIMEZONES, "true");
				request.setParameter(widgets.get("Datetime"), "2020-11-16");
				request.setParameter("w1hours", "7");
				request.setParameter("w1minutes", "25");
				request.setParameter("w1seconds", "58");
				request.setParameter("w1timezone", "Pacific/Kiritimati");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Calendar cal = Calendar.getInstance();
				cal.set(2020, 11 - 1, 16, 7, 25, 58);
				cal.set(MILLISECOND, 0);
				cal.setTimeZone(TimeZone.getTimeZone("Pacific/Kiritimati"));
				Date expectedDateTime = cal.getTime();
				Date encounterDateTime = results.getEncounterCreated().getEncounterDatetime();
				Assert.assertEquals(expectedDateTime, encounterDateTime);
			}
			
		}.run();
		
		//Test global variable that handles timezones
		new RegressionTestHelper() {
			
			private void setGlobalProperty(String name, String value) {
				GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(name);
				if (gp == null) {
					gp = new GlobalProperty(name);
				}
				gp.setPropertyValue(value);
				Context.getAdministrationService().saveGlobalProperty(gp);
			}
			
			@Override
			public String getFormName() {
				return "";
			}
			
			@Override
			public String getFormXml() {
				return "<htmlform> Datetime: <encounterDate showTime=\"true\" /> </htmlform>";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Datetime" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				setGlobalProperty(HtmlFormEntryConstants.GP_HANDLE_TIMEZONES, "false");
				request.setParameter(widgets.get("Datetime"), "2020-11-16");
				request.setParameter("w1hours", "7");
				request.setParameter("w1minutes", "25");
				request.setParameter("w1seconds", "58");
				String serverTimezone = TimeZone.getDefault().getID();
				//to teste the handletimezone as false, we cannot have the same client and server tz.
				if (serverTimezone == "Pacific/Kiritimati") {
					request.setParameter("w1timezone", "America/Tijuana");
				} else {
					request.setParameter("w1timezone", "Pacific/Kiritimati");
				}
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				String formError = results.getValidationErrors().get(0).getError();
				String errorMessage = Context.getMessageSourceService().getMessage("htmlformentry.error.handleTimezones");
				Assert.assertEquals(formError, errorMessage);
			}
			
		}.run();
	}
	
	@Test
	public void testSubmitEncounterDatetimeWithTimezoneAndGlobalVarHandleTimezoneAsFalse() throws Exception {
		//Test global variable that handles timezones
		new RegressionTestHelper() {
			
			private void setGlobalProperty(String name, String value) {
				GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(name);
				if (gp == null) {
					gp = new GlobalProperty(name);
				}
				gp.setPropertyValue(value);
				Context.getAdministrationService().saveGlobalProperty(gp);
			}
			
			@Override
			public String getFormName() {
				return "";
			}
			
			@Override
			public String getFormXml() {
				return "<htmlform> Datetime: <encounterDate showTime=\"true\" /> </htmlform>";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Datetime" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				setGlobalProperty("GP_HANDLE_TIMEZONES", "false");
				request.setParameter(widgets.get("Datetime"), "2020-11-16");
				request.setParameter("w1hours", "7");
				request.setParameter("w1minutes", "25");
				request.setParameter("w1seconds", "58");
				String serverTimezone = TimeZone.getDefault().getID();
				//to teste the handletimezone as false, we cannot have the same client and server tz.
				if (serverTimezone == "Pacific/Kiritimati") {
					request.setParameter("w1timezone", "America/Tijuana");
				} else {
					request.setParameter("w1timezone", "Pacific/Kiritimati");
				}
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				String formError = results.getValidationErrors().get(0).getError();
				String errorMessage = Context.getMessageSourceService().getMessage("htmlformentry.error.handleTimezones");
				Assert.assertEquals(formError, errorMessage);
			}
			
		}.run();
	}
	
}
