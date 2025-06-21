package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Provides a register of all handled tags in a html form
 */
public class TagRegister {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private final List<HtmlFormEntryTag> tags = new ArrayList<>();
	
	private final Stack<HtmlFormEntryTag> tagStack = new Stack<>();
	
	public List<HtmlFormEntryTag> getTags() {
		return tags;
	}
	
	public void startTag(Node node, boolean registeredTag) {
		HtmlFormEntryTag tag = new HtmlFormEntryTag();
		tag.setName(node.getNodeName());
		tag.setRegisteredTag(registeredTag);
		NamedNodeMap attrs = node.getAttributes();
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				tag.getAttributes().put(attr.getNodeName(), attr.getNodeValue());
			}
		}
		if (tagStack.isEmpty()) {
			tags.add(tag);
		} else {
			tagStack.peek().getChildTags().add(tag);
		}
		tagStack.push(tag);
		log.trace("startTag: " + tag.getName());
	}
	
	public void endTag(Node node, boolean registeredTag) {
		HtmlFormEntryTag tag = tagStack.pop();
		log.trace("endTag:" + tag.getName());
	}
}
