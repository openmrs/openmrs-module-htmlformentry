package org.openmrs.module.htmlformentry.element;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.RptGroup;

/***
 * serves asthe HtmlGeneratorElement for the newrepeat tag
 * 
 */
public class NewRepeatElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {

	/***
	 * The html to generate when see a starttag <newrepeat>
	 * 
	 * @param context
	 * @return the generated string
	 */
	public String generateStartHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();

		/* the jquery function to generate the template */
		sb.append("\n<tr><td> <script type=\"text/javascript\" charset=\"utf-8\">\n");
		sb.append("$(document).ready(function() {\n");
		sb.append("var kCount   = parseInt($('#kCount"
				+ context.getNewrepeatSeqVal() + "').val());\n");
		sb.append("var $newRow = cloneAndInsertBefore('newRepeat"
				+ context.getNewrepeatSeqVal() + "_1', $('#newRepeat"
				+ context.getNewrepeatSeqVal() + "_1'));\n");
		sb.append("$newRow.attr('id', 'newRepeatTemplate"
				+ context.getNewrepeatSeqVal() + "');\n");
		//sb.append("$newRow.prepend('<br/>');\n");
		sb.append("var newRowChildren = $newRow.children();\n");

		sb.append("var stack = new Array();\n");
		sb.append("for(var i = 0; i< newRowChildren.length; ++i){stack.push(newRowChildren[i]);}\n");
		sb.append("while(stack.length>0){\n");
		sb.append("	var child = stack.pop();\n");
		sb.append("for(var i = 0; i< child.children.length; ++i){\n");
		sb.append("	stack.push(child.children[i]);\n");
		sb.append("	}\n");
		sb.append("  if (child.id == 'removeRowButton') { child.style.display = ''; }\n");
		sb.append("else if(child.id ==\"defaultFieldlistObjAddButton"+context.getNewrepeatSeqVal()+"\"){child.id = \"defaultFieldlistObjAddButton0\";" +
				"child.style.display = '';}");
		sb.append("else if(child.id.length>4 && child.id.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = child.id.indexOf(\"_\");\n");
		sb.append("	var pos2 = child.id.indexOf(\"_\", pos1+1);\n");
		sb.append("if(child.id.indexOf(\".\")==-1){");
		sb.append("	child.id = child.id.substring(0,pos2+1) + '0';\n");
		sb.append("	child.name = child.id.substring(0,pos2+1) + '0';\n");
		sb.append("}\n");
		sb.append("else{");
		sb.append("child.id = child.id.substring(0, pos2 + 1) + '0'+child.id.substring(child.id.indexOf(\".\"),child.id.length);\n");
		sb.append("child.name = child.id;\n");
		sb.append("}");
		
		sb.append("}\n");
		/*
		sb.append("else if(child.name!== undefined&&child.name.length>4 && child.name.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = child.name.indexOf(\"_\");\n");
		sb.append("	var pos2 = child.name.indexOf(\"_\", pos1+1);\n");
		sb.append("	child.name = child.name.substring(0,pos2+1) + '0';\n");
		sb.append("}\n");
		*/
		
		sb.append("}\n");

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
		
		sb.append("var stack = new Array();");
		sb.append("for(var i = 0; i< newRowChildren.length; ++i){");
		sb.append("stack.push(newRowChildren[i]);");
		sb.append("}");
		
		sb.append("while(stack.length> 0){\n");
		sb.append("var child = stack.pop();\n");
		sb.append("for(var i = 0; i< child.children.length; ++i){\n");
		sb.append("	stack.push(child.children[i]);\n");
		sb.append("}\n");
		
		sb.append("if (child.id == 'removeRowButton') { child.style.display = ''; }	\n");
		sb.append("else if(child.id ==\"defaultFieldlistObjAddButton0\"){child.style.display = 'none';}");
		sb.append("if (child.className == 'error') \n");
		sb.append("{ child.style.display = 'none'; }	\n");

		sb.append("if(child.id.length>4 && child.id.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = child.id.indexOf(\"_\");\n");
		sb.append("	var pos2 = child.id.indexOf(\"_\", pos1+1);\n");
		
		sb.append("if(child.id.indexOf(\".\")==-1){");
		sb.append("	child.id = child.id.substring(0,pos2+1) + kCount;\n");
		sb.append("	child.name = child.id;\n");
		sb.append("}\n");
		sb.append("else{");
		sb.append("child.id = child.id.substring(0, pos2 + 1) + kCount +child.id.substring(child.id.indexOf(\".\"),child.id.length);\n");
		sb.append("child.name = child.id;\n");
		sb.append("}");
		
		
		sb.append("if(child.attributes[\"onBlur\"]!== undefined){");
		sb.append("var onblur =child.attributes[\"onBlur\"].value;");
		sb.append("pos1 = onblur.indexOf(\"'\");");
		sb.append("pos2 = onblur.indexOf(\"'\", pos1+1);");
		sb.append("var temp =  onblur.substring(pos1+1,pos2);");
		sb.append("temp = onblur.replace(temp, GetNewRptTimeId(temp, $('#kCount"
						+ context.getNewrepeatSeqVal() + "').val()-1));");
		sb.append("child.attributes[\"onBlur\"].value=temp;}");
		sb.append("	}\n");
		/*
		sb.append("else if(child.name!== undefined&&child.name.length>4 && child.name.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = child.name.indexOf(\"_\");\n");
		sb.append("	var pos2 = child.name.indexOf(\"_\", pos1+1);\n");
		sb.append("	child.name = child.name.substring(0,pos2+1) + '0';\n");
		sb.append("}\n");
		*/
		sb.append("	}});	\n");
		sb.append("});\n");
		sb.append("</script>\n");

		if(context.getMode() == Mode.EDIT||context.getMode() == Mode.VIEW){
			sb.append("<#reservenewrepeat" + context.getNewrepeatSeqVal());
		}
		sb.append("<span id=\"newRepeat" + context.getNewrepeatSeqVal() + "_1"
				+ "\" class=\"newRepeat" + context.getNewrepeatSeqVal()
				+ "\" style=\"display:block\" ><table style=\"display:inline\"> \n");
		
		return sb.toString();
	}

	/***
	 * generate html when see a </newrepeat> tag
	 * 
	 * @param context
	 * @return html
	 */
	public String generateEndHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();

		/* the remove button */
		sb.append("</table><input type=\"button\" id=\"removeRowButton"
						+ "\" value=\"X\" size=\"1\" style=\"display:none\";  onclick=\"removeParentWithClass(this,'newRepeat"
						+ context.getNewrepeatSeqVal() + "');\" />\n");

		/* the add button */
		sb.append("<input id=\"defaultFieldlistObjAddButton"
				+ context.getNewrepeatSeqVal()
				+ "\" type=\"button\" value=\"+\" size=\"1\" /></span>"); 
		
		
		sb.append("</td></tr>\n");

		if(context.getMode() != Mode.EDIT){
		sb.append("<input id=\"kCount"
				+ context.getNewrepeatSeqVal() + "\""
				+ " name=\"kCount" + context.getNewrepeatSeqVal()
				+ "\"" + " style=\"display: none;\" value=\""
				+ context.getNewrepeatTimesSeqVal() + "\"/> \n");
		}else{
			sb.append("<input id=\"kCount"
					+ context.getNewrepeatSeqVal() + "\""
					+ " name=\"kCount" + context.getNewrepeatSeqVal()
					+ "\"" + " style=\"display: none;\" value=\""
					+ context.getExistingRptGroups().get(context.getNewrepeatSeqVal()-1).getRepeattime() + "\"/> \n");
		}
		
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
