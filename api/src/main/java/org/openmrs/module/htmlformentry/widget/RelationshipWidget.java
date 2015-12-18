package org.openmrs.module.htmlformentry.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

/**
 * A widget that allows for the selection of a Person.  Implemented uses a pop-up to display person 
 * search.
 */
public class RelationshipWidget implements Widget {
	
	private List<RelationshipType> relationshipsToCreate = new ArrayList<RelationshipType>();
	private List<String> roleInRelationship = new ArrayList<String>();
	private boolean allRelationshipsFullfilled = true;
    private String parentId;

	public RelationshipWidget() { }

	@Override
    public void setInitialValue(Object initialValue) {
	 
    }
	
	
	@Override
    public String generateHtml(FormEntryContext context) {
     
        StringBuilder sb = new StringBuilder();
        
        //this check is needed or else the code falls over when generating preview for editing form
		//TODO: probably need a new mode for previewing to better deal with this sort of stuff.
		if(context.getExistingPatient() != null && context.getExistingPatient().getId() != null)
		{
			sb.append("<table border=1 style=\"border-collapse:collapse;border:1px solid black\"><tr><th colspan=2>");
			sb.append("<strong>");
			sb.append(Context.getMessageSourceService().getMessage("htmlformentry.existingRelationshipsLabel"));
			sb.append(" </strong>");
			sb.append("</th></tr>");
			//okay we need to first display any existing relationships
			List<Relationship> existingRelationships = Context.getPersonService().getRelationshipsByPerson(context.getExistingPatient());
			for(int i=0; i < relationshipsToCreate.size(); i++)
			{
				if(i > 0)
				{
					sb.append("<br />");
				}
				
				RelationshipType rt = relationshipsToCreate.get(i);
				String side = roleInRelationship.get(i);
				sb.append("<tr><td>" +" " );
				String rl;
				StringBuilder val = new StringBuilder();
				if(side.equals("A"))
				{
					rl=rt.getbIsToA();
				}
				else
				{
					rl=rt.getaIsToB();
				}
				sb.append(rl);
				sb.append(": ");
				sb.append("</td>");
				sb.append("<td>" + " ");
				boolean addComma = false;
				
				for(Relationship r: existingRelationships)
				{
					if(r.getRelationshipType().getId().equals(rt.getId()))
					{
						if(side.equals("A"))
	    				{
	    					if(r.getPersonA().equals(context.getExistingPatient()))
	    					{
	    						if(addComma)
	    						{
	    							sb.append(",");
	    							val.append(",");
	    						}
	    						else
	    						{
	    							addComma = true;
	    						}
	    						String s = r.getPersonB().getGivenName()+" "+r.getPersonB().getFamilyName();	    						
	    						sb.append(s);	    	
	    						val.append(s);
	    						if (context.getMode() == Mode.VIEW) {
	    							sb.append(" ");
	    							sb.append(Context.getMessageSourceService().getMessage("htmlformentry.existingRelationshipsAdded"));
	    							sb.append(" - ");
	    							sb.append(new SimpleDateFormat("yyyy-MM-dd").format(r.getDateCreated()));
	    						}
	    						
	    					}
	    				}
	    				if(side.equals("B"))
	    				{
	    					if(r.getPersonB().equals(context.getExistingPatient()))
	    					{
	    						if(addComma)
	    						{
	    							sb.append(",");
	    							val.append(",");
	    						}
	    						else
	    						{
	    							addComma = true;
	    						}
	    						String s =r.getPersonA().getGivenName()+" "+r.getPersonA().getFamilyName();
	    						sb.append(s);
	    						val.append(s);
	    						if (context.getMode() == Mode.VIEW) {
	    							sb.append(" ");
	    							sb.append(Context.getMessageSourceService().getMessage("htmlformentry.existingRelationshipsAdded"));
	    							sb.append(" - ");
	    							sb.append(new SimpleDateFormat("yyyy-MM-dd").format(r.getDateCreated()));
	    						}
	    					}
	    				}
					}
				}

                // store the current relationships in hidden fields and register them as property accessors
                // these hidden fields *are not* used at all during form submittal; they are just used to provide
                // access to existing relationships for javascript validation (for instance, to validate on submittal
                // that a patient as at least one relationship of type x)

				sb.append("<input name='");
		    	sb.append(context.getFieldName(this));
		    	sb.append("' id='");
		    	sb.append(parentId+"."+HtmlFormEntryUtil.toCamelCase(rl));
		    	sb.append("' value='");
		    	sb.append(val);
		    	sb.append("' type='hidden'>");
		    	context.registerPropertyAccessorInfo(parentId+"."+HtmlFormEntryUtil.toCamelCase(rl) + ".value", context.getFieldNameIfRegistered(this), null,null, null);
				sb.append("</td>");
				sb.append("</tr>");
				if(!addComma)
				{
					allRelationshipsFullfilled = false;
				}
			}
			sb.append("</table>");
		}
    	
        return sb.toString();
    }

	@Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
		
        return null;
    }

	
    /**
     * @return the relationshipsToCreate
     */
    public List<RelationshipType> getRelationshipsToCreate() {
    	return relationshipsToCreate;
    }

	
    /**
     * @param relationshipsToCreate the relationshipsToCreate to set
     */
    public void setRelationshipsToCreate(List<RelationshipType> relationshipsToCreate) {
    	this.relationshipsToCreate = relationshipsToCreate;
    }

	
    /**
     * @return the roleInRelationship
     */
    public List<String> getRoleInRelationship() {
    	return roleInRelationship;
    }

	
    /**
     * @param roleInRelationship the roleInRelationship to set
     */
    public void setRoleInRelationship(List<String> roleInRelationship) {
    	this.roleInRelationship = roleInRelationship;
    }

	
    /**
     * @return the allRelationshipsFullfilled
     */
    public boolean isAllRelationshipsFullfilled() {
    	return allRelationshipsFullfilled;
    }

	
    /**
     * @param allRelationshipsFullfilled the allRelationshipsFullfilled to set
     */
    public void setAllRelationshipsFullfilled(boolean allRelationshipsFullfilled) {
    	this.allRelationshipsFullfilled = allRelationshipsFullfilled;
    }

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
}
