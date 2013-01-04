package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.List;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

/**
 * This abstract handler provides a default implementation of getAttributeDescriptors that returns null
 * Tag handlers that extend this class can override createAttributeDescriptors if they need
 * to specify attribute descriptors
 */
public abstract class AbstractTagHandler implements TagHandler {

    /** Holds the attribute descriptors for this class **/
	private List<AttributeDescriptor>  attributeDescriptors = createAttributeDescriptors();

	/** Classes that extend this class can override this method to specify attribute descriptors */
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		return null;
	}
	
	@Override
    public List<AttributeDescriptor> getAttributeDescriptors() {
		return attributeDescriptors;
	}
	
    @Override
    abstract public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException;

    @Override
    abstract public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException;

    /**
     * Helper method for getting an attribute value, with a default.
     * @param node
     * @param attributeName
     * @param defaultValue
     * @return
     */
    public String getAttribute(Node node, String attributeName, String defaultValue) {
        Node item = node.getAttributes().getNamedItem(attributeName);
        String ret = null;
        if (item != null) {
            ret = item.getNodeValue();
        }
        return ret != null ? ret : defaultValue;
    }
    
}
