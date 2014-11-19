package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.openmrs.api.context.Context;
import org.openmrs.util.LocaleUtility;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Encapsulates Translator functionality (see {@see HtmlFormEntryGenerator#applyTranslations(String, FormEntryContext)})
 */
public class Translator {

	private String defaultLocaleStr = "en";
	private Map<String, Map<String, String>> translations = new HashMap<String, Map<String, String>>();
	
	/**
	 * Allowed formats for {@see translate(String, String, Format)}
	 */
	public static enum Format {
		/** Set all characters to lower-case */
		LOWERCASE, 
		/** Set all characters to upper-case */
		UPPERCASE, 
		/** Capitalize the first word (set the first letter of the first word to upper-case */
		CAPITALIZE, 
		/** Capitalize all words (set first letter of each word to upper-case)*/
		CAPITALIZE_ALL
	}
	
	/**
	 * Default Constructor
	 */
	public Translator() {}

	/**
	 * Returns the translation mappings associated with this Translator
	 * 
	 * @return the translations
	 */
	public Map<String, Map<String, String>> getTranslations() {
		return translations;
	}

	/**
	 * Sets the translation mappings associated with this Translator
	 * 
	 * @param translations the translations to set
	 */
	public void setTranslations(Map<String, Map<String, String>> translations) {
		this.translations = translations;
	}
	
	/**
	 * Return the default locale used by this Translator
	 * 
	 * @return the defaultLocaleStr
	 */
	public String getDefaultLocaleStr() {
		return defaultLocaleStr;
	}

	/**
	 * Sets the default locale used by this Translator
	 * 
	 * @param defaultLocaleStr the defaultLocaleStr to set
	 */
	public void setDefaultLocaleStr(String defaultLocaleStr) {
		this.defaultLocaleStr = defaultLocaleStr;
	}

	/**
	 * Adds a translation for the given code and locale
	 * <p>
	 * (see {@see HtmlFormEntryGenerator#applyTranslations(String, FormEntryContext)})
	 * 
	 * @param localeStr the name of the locale that this translation is for
	 * @param code the code that we are translating
	 * @param translation the translation for the specified code for the specified locale
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
	 * Gets the translation map associated with a particular locale
	 * 
	 * @param locale the name of the locale
	 * @return the translation map for that locale
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
	 * Gets the translation map associated with a particular locale
	 * 
	 * @param locale the locale
	 * @return the translation map for that locale
	 */
	public Map<String, String> getTranslations(Locale locale) {
		return getTranslations(locale == null ? null : locale.toString());
	}
	
	/**
	 * Gets the translation for a specific code and locale
	 * 
	 * @param localeStr the name of the locale
	 * @param key the code 
	 * @return the translation associated with that code and locale
	 */
	public String translate(String localeStr, String key) {
		Map<String, String> translations = getTranslations(localeStr);
		String ret = translations.get(key);
		if (StringUtils.isEmpty(ret)) {
			ret = Context.getMessageSourceService().getMessage(key, null, LocaleUtility.fromSpecification(localeStr));
		}
		return (ret == null ? key : ret);
	}
	
	/**
	 * Gets the translation for a specific code and locale, returning the
	 * translation in a specified format
	 * 
	 * @param localeStr the name of the locale
	 * @param key the code 
	 * @param format to return the translation in (see {@see Format})
	 * @return the translation associated with that code and locale
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
