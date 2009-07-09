package org.openmrs.module.htmlformentry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.openmrs.api.context.Context;

/**
 * Encapsulates Translator functionality
 */
public class Translator {

	private String defaultLocaleStr = "en";
	private Map<String, Map<String, String>> translations = new HashMap<String, Map<String, String>>();
	
	public static enum Format {
		LOWERCASE, UPPERCASE, CAPITALIZE, CAPITALIZE_ALL
	}
	
	/**
	 * Default Constructor
	 */
	public Translator() {}

	/**
	 * @return the translations
	 */
	public Map<String, Map<String, String>> getTranslations() {
		return translations;
	}

	/**
	 * @param translations the translations to set
	 */
	public void setTranslations(Map<String, Map<String, String>> translations) {
		this.translations = translations;
	}
	
	/**
	 * @return the defaultLocaleStr
	 */
	public String getDefaultLocaleStr() {
		return defaultLocaleStr;
	}

	/**
	 * @param defaultLocaleStr the defaultLocaleStr to set
	 */
	public void setDefaultLocaleStr(String defaultLocaleStr) {
		this.defaultLocaleStr = defaultLocaleStr;
	}

	/**
	 * Adds a translation for the given code and {@link String}
	 * @param localeStr
	 * @param code 
	 * @param translation
	 */
	public void addTranslation(String localeStr, String code, String translation) {
		if (translations == null) {
			translations = new HashMap<String, Map<String, String>>();
		}
		if (localeStr == null) {
			localeStr = defaultLocaleStr;
		}
		Map<String, String> localeMap = translations.get(localeStr);
		if (localeMap == null) {
			localeMap = new HashMap<String, String>();
			translations.put(localeStr, localeMap);
		}
		localeMap.put(code, translation);
	}
	
	/**
	 * @param locale
	 * @return
	 */
	public Map<String, String> getTranslations(String localeStr) {
	
		Map<String, String> ret = new HashMap<String, String>();
		
		// First add all translations from the default locale.  Then gradually build up variants
		if (translations.get(defaultLocaleStr) != null) {
			ret.putAll(translations.get(defaultLocaleStr));
		}
		
		if (localeStr != null) {
			String[] split = localeStr.split(Pattern.quote("_"));
			String currLocaleStr = "";
			for (int i=0; i<split.length; i++) {
				currLocaleStr += (i > 0 ? "_" : "") + split[i];
				Map<String, String> m = translations.get(currLocaleStr);
				if (m != null) {
					ret.putAll(m);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * @param locale
	 * @return
	 */
	public Map<String, String> getTranslations(Locale locale) {
		return getTranslations(locale == null ? null : locale.toString());
	}
	
	/**
	 * @param localeStr
	 * @param key
	 * @return
	 */
	public String translate(String localeStr, String key) {
		Map<String, String> translations = getTranslations(localeStr);
		String ret = translations.get(key);
		if (StringUtils.isEmpty(ret)) {
			ret = Context.getMessageSourceService().getMessage(key);
		}
		return (ret == null ? key : ret);
	}
	
	/**
	 * @param localeStr
	 * @param key
	 * @param format
	 * @return
	 */
	public String translate(String localeStr, String key, Format format) {
		String text = translate(localeStr, key);
		if (format == Format.UPPERCASE) {
			return text.toUpperCase();
		}
		text = text.toLowerCase();
		if (format == Format.CAPITALIZE) {
			return StringUtils.capitalize(text);
		}
		if (format == Format.CAPITALIZE_ALL) {
			return WordUtils.capitalizeFully(text);
		}
		return text;
	}
}
