package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class DynamicRepeatTagHandler extends AbstractTagHandler {
	int count=0;
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	    throws BadFormDesignException {
		// TODO Auto-generated method stub
		StringWriter outer = new StringWriter();
		PrintWriter pout=new PrintWriter(outer);
		if(count==0){
		pout.print("<div>");
		}
		count++;
		NodeList nchilds= node.getChildNodes();
		for (int i=0;i<nchilds.getLength();i++)
		{
			String name = nchilds.item(i).getNodeName();
			 TagHandler handler = HtmlFormEntryUtil.getService().getHandlerByTagName(name);
			if(handler!=null){
				boolean check;
			check= handler.doStartTag(session, pout, node, nchilds.item(i));
			if(check)
			{
				this.doStartTag(session, pout,node, nchilds.item(i));
				this.doEndTag(session, pout, node, nchilds.item(i));
			}
			handler.doEndTag(session, pout, node, nchilds.item(i));
			}
		}
		if(count==1){
		pout.print("<input value='add' type='button' class='dynamicRepeat' onClick='cloneFunction($j(this).parent())'>");
		pout.print("</div>");
		}
		out.print(outer.toString());
		
		return false;
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
		// TODO Auto-generated method stub
		count--;
	}
	
}
