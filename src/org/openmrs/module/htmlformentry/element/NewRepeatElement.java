package org.openmrs.module.htmlformentry.element;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.RptGroup;

/***
 *  serves asthe HtmlGeneratorElement for the newrepeat tag
 * 
 */
public class NewRepeatElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {


	/***
	 * The html to generate when see a starttag <newrepeat>
	 * @param context
	 * @return the generated string 
	 */
	public String generateStartHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		
		/* the jquery function to generate the template */
		// sb.append("<script src=\"../../moduleResources/htmlformentry/jquery-1.4.2.min.js\" type=\"text/javascript\"></script>");
		// sb.append("<script src=\"../../moduleResources/htmlformentry/htmlwidgets.js\" type=\"text/javascript\"></script> ");
		sb.append("\n <script type=\"text/javascript\" charset=\"utf-8\">\n");
		sb.append("$(document).ready(function() {\n");
		sb.append("var kCount   = parseInt($('#kCount"
				+ context.getNewrepeatSeqVal() + "').val());\n");
		sb.append("var $newRow = cloneAndInsertBefore('newRepeat"
				+ context.getNewrepeatSeqVal() + "_1', $('.newRepeat"
				+ context.getNewrepeatSeqVal() + "'));\n");
		sb.append("$newRow.attr('id', 'newRepeatTemplate"
				+ context.getNewrepeatSeqVal() + "');\n");
		sb.append("$newRow.prepend('<br/>');\n");
		sb.append("var newRowChildren = $newRow.children();\n");
		sb.append("for (var i=0; i<newRowChildren.length; i++) {\n");
		sb.append("if (newRowChildren[i].id == 'removeRowButton') { newRowChildren[i].style.display = ''; }\n");
		sb.append("if(newRowChildren[i].id.length>4 && newRowChildren[i].id.substring(0,3)== 'rpt'){\n");
		sb.append("var pos1 = newRowChildren[i].id.indexOf(\"_\");\n");
		sb.append("var pos2 = newRowChildren[i].id.indexOf(\"_\", pos1+1);\n");
		sb.append("newRowChildren[i].id = newRowChildren[i].id.substring(0,pos2+1) + '0';\n");
		sb.append("newRowChildren[i].name = newRowChildren[i].id.substring(0,pos2+1) + '0';\n");
		sb.append("	}}\n");
		sb.append("$newRow.hide();\n");
		sb.append("	}); \n </script>\n");

		/* the jquery function to add fields */
		sb.append("<script type=\"text/javascript\" charset=\"utf-8\">\n");
		sb.append("$(document).ready(function() {	\n");
		sb.append("$(\"#defaultFieldlistObjAddButton"
				+ context.getNewrepeatSeqVal()
				+ "\").click(function(event){	\n");
		sb.append("var kCount = parseInt($('#kCount"
				+ context.getNewrepeatSeqVal() + "').val()) + 1; \n");
		sb.append("$('#kCount" + context.getNewrepeatSeqVal()
				+ "').val(kCount);\n");
		sb.append("var $newRow = cloneAndInsertBefore('newRepeatTemplate"
				+ context.getNewrepeatSeqVal() + "', this);	\n");
		sb.append("$newRow.attr('id', 'newRepeat"
				+ context.getNewrepeatSeqVal() + "_'" + "+kCount); \n");
		sb.append("$newRow.prepend('<br/>'); \n");
		sb.append("var newRowChildren = $newRow.children();	\n");
		sb.append("for (var i=0; i<newRowChildren.length; i++) {	\n");
		sb.append("if (newRowChildren[i].id == 'removeRowButton') { newRowChildren[i].style.display = ''; }	\n");
		sb.append("if (newRowChildren[i].className == 'error') \n"); 
		sb.append("{ newRowChildren[i].style.display = 'none'; }	\n");

		sb.append("if(newRowChildren[i].id.length>4 && newRowChildren[i].id.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = newRowChildren[i].id.indexOf(\"_\");\n");
		sb.append("	var pos2 = newRowChildren[i].id.indexOf(\"_\", pos1+1);\n");
		sb.append("	newRowChildren[i].id = newRowChildren[i].id.substring(0,pos2+1) + kCount;\n");
		sb.append("newRowChildren[i].name = newRowChildren[i].id.substring(0,pos2+1) + kCount;\n");
		sb.append("if(newRowChildren[i].attributes[\"onBlur\"]!== undefined){");
		sb.append("var onblur =newRowChildren[i].attributes[\"onBlur\"].value;");
		sb.append("pos1 = onblur.indexOf(\"'\");");
		sb.append("pos2 = onblur.indexOf(\"'\", pos1+1);");
		sb.append("var temp =  onblur.substring(pos1+1,pos2);");
		sb.append("temp = onblur.replace(temp, GetNewRptTimeId(temp, $('#kCount"+context.getNewrepeatSeqVal()+"').val()-1));");
		sb.append("newRowChildren[i].attributes[\"onBlur\"].value=temp;};");
		sb.append("	}			\n");
		sb.append("	}});	\n");
		sb.append("});\n");
		sb.append("</script>\n");	
		
		
		sb.append("<span id=\"newRepeat" + context.getNewrepeatSeqVal()
				+"_1" 
				+"\" class=\"newRepeat" + context.getNewrepeatSeqVal()
				+ "\"> \n");

		return sb.toString();
	}

	/***
	 * generate html when see a </newrepeat> tag
	 * @param context
	 * @return html
	 */
	public String generateEndHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		
		/*the remove button */
		sb.append("<input type=\"button\" id=\"removeRowButton"
				+ "\" value=\"X\" size=\"1\" style=\"display:none\";  onclick=\"removeParentWithClass(this,'newRepeat"+context.getNewrepeatSeqVal()+"');\" />\n");
		
		/*the add button */
		sb.append("</span><input id=\"defaultFieldlistObjAddButton"
				+ context.getNewrepeatSeqVal()
				+ "\" type=\"button\" value=\"+\" size=\"1\" />\n");

		return sb.toString();
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void handleSubmission(FormEntrySession session,
			HttpServletRequest submission) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(
			FormEntryContext context, HttpServletRequest submission) {
		// TODO Auto-generated method stub
		return null;
	}

}
