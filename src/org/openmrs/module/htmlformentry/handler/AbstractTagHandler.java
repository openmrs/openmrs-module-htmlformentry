package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.List;

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

	/** Name of the tag, to be used in the designer **/
	private String name = createName();
	
	/** Description of the tag, to be used in the designer **/
	private String description = createDescription();
	
	/** Classes that extend this class can override this method to specify attribute descriptors */
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		return null;
	}
	
	public List<AttributeDescriptor> getAttributeDescriptors() {
		return attributeDescriptors;
	}
	
	/** Subclasses can override this method to provide a custom name for the designer **/
	protected String createName() {
		String str = this.getClass().getSimpleName();
		int firstChar = str.lastIndexOf ('.') + 1;
		int lastChar = str.lastIndexOf("Tag");
		if (lastChar == -1)
			lastChar = str.lastIndexOf("Handler");
		
		return str.substring(firstChar, lastChar);
	}
	
	/** Subclasses can override this method to provide a custom description for the designer **/
	protected String createDescription() {
		return "No description for this tag.";
	}
	
    /**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	@Override
	public boolean allowsChildren() {
		return false;
	}

	abstract public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node);

    abstract public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node);
}
