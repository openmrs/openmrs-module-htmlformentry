package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tag represents a specific xml node in an htmlform
 */
public class HtmlFormEntryTag {
	
	private String name;
	
	private boolean registeredTag = false;
	
	private Map<String, String> attributes = new HashMap<>();
	
	private List<HtmlFormEntryTag> childTags = new ArrayList<>();
	
	public HtmlFormEntryTag() {
	}
	
	public boolean hasAttribute(String name, String value) {
		String v = attributes.get(name);
		return v != null && v.equalsIgnoreCase(value);
	}
	
	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}
	
	@Override
	public String toString() {
		return getName() + getAttributes();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isRegisteredTag() {
		return registeredTag;
	}
	
	public void setRegisteredTag(boolean registeredTag) {
		this.registeredTag = registeredTag;
	}
	
	public Map<String, String> getAttributes() {
		if (attributes == null) {
			attributes = new HashMap<>();
		}
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	public List<HtmlFormEntryTag> getChildTags() {
		return childTags;
	}
	
	public void setChildTags(List<HtmlFormEntryTag> childTags) {
		this.childTags = childTags;
	}
}
