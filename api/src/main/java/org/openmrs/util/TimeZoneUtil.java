/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.joda.time.DateTimeZone.UTC;

/**
 * Helps provide tools to support recommended OpenMRS time zones conventions.
 * 
 * @see https://wiki.openmrs.org/display/docs/Time+Zones+Conventions
 */
public class TimeZoneUtil {
	
	/**
	 * Formats a date as its RFC 3339 string representation.
	 * 
	 * @param date The date.
	 * @return The date formated as RFC 3339.
	 */
	public static String toRFC3339(Date date) {
		return ISODateTimeFormat.dateTime().print(new DateTime(date.getTime(), UTC));
	}
	
	/**
	 * Gets the Calendar instance for the date set in UTC. This always returns a GregorianCalendar
	 * subclass.
	 * 
	 * @param date The date.
	 * @return The GregorianCalendar set in UTC for the date.
	 */
	public static Calendar toUTCCalendar(Date date) {
		return new DateTime(date.getTime(), UTC).toGregorianCalendar();
	}
	
	/**
	 * Get a Date out of its ISO 8601 string representation.
	 *
	 * @param isoDateString A date formatted as ISO 8601.
	 * @return The Date object.
	 * @Throws IllegalArgumentException – if string parameter does not conform to lexical value space
	 */
	public static Date fromISO8601(String isoDateString) throws IllegalArgumentException {
		DateTimeFormatter parser = ISODateTimeFormat.dateTime();
		return parser.parseDateTime(isoDateString).toDate();
	}
	
}
