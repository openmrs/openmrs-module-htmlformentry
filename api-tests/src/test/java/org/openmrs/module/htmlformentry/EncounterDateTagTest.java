package org.openmrs.module.htmlformentry;

import static java.util.Calendar.MILLISECOND;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.messagesource.MessageSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class EncounterDateTagTest extends BaseHtmlFormEntryTest {
	
	@Autowired
	private AdministrationService adminService;
	
	@Autowired
	private MessageSourceService messageSourceService;
	
	private TimeZone systemTimezone;
	
	@Before
	public void before() {
		systemTimezone = TimeZone.getDefault();
	}
	
	@After
	public void after() {
		TimeZone.setDefault(systemTimezone);
	}
	
	/**
	 * To test the encounter date tag submissions with the <pre>showTime</pre> attribute set to true.
	 */
	public class TimezonesEncounterDateTestHelper extends RegressionTestHelper {
		
		public TimezonesEncounterDateTestHelper(boolean convertTimezones) {
			if (convertTimezones) {
				adminService.saveGlobalProperty(new GlobalProperty(GP_TIMEZONE_CONVERSIONS, "true"));
			} else {
				adminService.saveGlobalProperty(new GlobalProperty(GP_TIMEZONE_CONVERSIONS, "false"));
			}
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Zurich"));
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
			request.setParameter(widgets.get("Datetime"), "2020-11-02");
			request.setParameter("w1hours", "03");
			request.setParameter("w1minutes", "30");
			request.setParameter("w1seconds", "00");
			request.setParameter("w1timezone", "Pacific/Kiritimati");
		}
		
	}
	
	@Test
	public void submitEncounterDateWithTimezone_shouldConvertDateToServerTimezoneWhenTimezonesSupport() throws Exception {
		
		new TimezonesEncounterDateTestHelper(true) {
			
			@Override
			public void testResults(SubmissionResults results) {
				Calendar cal = Calendar.getInstance();
				cal.set(2020, 11 - 1, 02, 3, 30, 00);
				cal.set(MILLISECOND, 0);
				cal.setTimeZone(TimeZone.getTimeZone("Pacific/Kiritimati"));
				results.assertEncounterDatetime(cal.getTime()); // this converts the Kiritimati datetime to Zurich
			}
			
		}.run();
		
	}
	
	@Test
	public void submitEncounterDateWithoutTimezone_shouldError() throws Exception {
		
		new TimezonesEncounterDateTestHelper(true) { // false would do the same
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				super.setupRequest(request, widgets);
				request.removeParameter("w1timezone"); // not really possible when using ZonedDateTimeWidget
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				String formError = results.getValidationErrors().get(0).getError();
				Assert.assertEquals(messageSourceService.getMessage("htmlformentry.error.noClientTimezone"), formError);
			}
			
		}.run();
		
	}
	
	@Test
	public void submitEncounterDateWithTimezone_shouldErrorWhenTimezonesSupportDisabledAndTimezonesDiffer()
	        throws Exception {
		
		new TimezonesEncounterDateTestHelper(false) {
			
			@Override
			public void testResults(SubmissionResults results) {
				String formError = results.getValidationErrors().get(0).getError();
				Assert.assertEquals(messageSourceService.getMessage("htmlformentry.error.handleTimezones"), formError);
			}
			
		}.run();
		
	}
	
	@Test
	public void submitEncounterDateWithTimezone_shouldNotConvertDateWhenTimezonesSupportDisabledAndSameTimezones()
	        throws Exception {
		
		new TimezonesEncounterDateTestHelper(false) {
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Datetime"), "2020-11-02");
				request.setParameter("w1hours", "03");
				request.setParameter("w1minutes", "30");
				request.setParameter("w1seconds", "00");
				request.setParameter("w1timezone", "Europe/Zurich");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Calendar cal = Calendar.getInstance();
				cal.set(2020, 11 - 1, 02, 3, 30, 00);
				cal.set(MILLISECOND, 0);
				results.assertEncounterDatetime(cal.getTime());
			}
			
		}.run();
		
	}
	
}
