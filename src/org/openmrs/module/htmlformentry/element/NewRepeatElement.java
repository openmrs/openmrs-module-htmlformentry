package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.RptGroup;
import org.openmrs.util.OpenmrsUtil;

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

		if (!context.getExistingRptGroups().get(
				context.getNewrepeatSeqVal() - 1).isIntd()) {
			sb.append("\n<tr><td> ");
		}

		
		/* the jquery function to add fields */
		sb.append("<script type=\"text/javascript\" charset=\"utf-8\">\n");
		sb.append("$(document).ready(function() {	\n");
		sb.append("$(\"#defaultFieldlistObjAddButton"
				+ context.getNewrepeatSeqVal()
				+ "\").click(function(event){	\n");
		sb.append("var kCount = parseInt($('#kCount"
				+ context.getNewrepeatSeqVal() + "').val()) + 1; \n");

		sb.append("var maxRpt = parseInt($('#maxRpt"
				+ context.getNewrepeatSeqVal() + "').val()); \n");
		sb
				.append("if(kCount>maxRpt){alert('Multiple Limit reached, please contact the form designer!');return;}");

		sb.append("$('#kCount" + context.getNewrepeatSeqVal()
				+ "').val(kCount);\n");
		sb.append("var $newRow = cloneAndInsertBefore('newRepeatTemplate"
				+ context.getNewrepeatSeqVal() + "', this);	\n");
		sb.append("$('#newRepeat" + context.getNewrepeatSeqVal() + "_'"
				+ "+(kCount-1)).css(\"display\", \"block\");");
		sb.append("$newRow.attr('id', 'newRepeat"
				+ context.getNewrepeatSeqVal() + "_'" + "+kCount); \n");
		sb.append("$newRow.css(\"display\", \"block\");");
		// sb.append("$newRow.prepend('<br/>'); \n");

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

		sb
				.append("if (child.id == 'removeRowButton') { child.style.display = ''; }	\n");
		sb
				.append("else if(child.id ==\"defaultFieldlistObjAddButton0\"){child.style.display = 'none';}");
		sb.append("if (child.className == 'error') \n");
		sb.append("{ child.style.display = 'none'; }	\n");

		sb
				.append("if(child.id.length>4 && child.id.substring(0,3)== 'rpt'){\n");
		sb.append("	var pos1 = child.id.indexOf(\"_\");\n");
		sb.append("	var pos2 = child.id.indexOf(\"_\", pos1+1);\n");

		sb.append("if(child.id.indexOf(\".\")==-1){");
		sb.append("	child.id = child.id.substring(0,pos2+1) + kCount;\n");
		sb.append("	child.name = child.id;\n");
		sb.append("}\n");
		sb.append("else{");
		sb
				.append("child.id = child.id.substring(0, pos2 + 1) + kCount +child.id.substring(child.id.indexOf(\".\"),child.id.length);\n");
		sb.append("child.name = child.id;\n");
		sb.append("}");

		sb.append("if(child.attributes[\"onBlur\"]!== undefined){");
		sb.append("var onblur =child.attributes[\"onBlur\"].value;");
		sb.append("pos1 = onblur.indexOf(\"'\");");
		sb.append("pos2 = onblur.indexOf(\"'\", pos1+1);");
		sb.append("var temp =  onblur.substring(pos1+1,pos2);");
		sb
				.append("temp = onblur.replace(temp, GetNewRptTimeId(temp, $('#kCount"
						+ context.getNewrepeatSeqVal() + "').val()));");
		sb.append("child.attributes[\"onBlur\"].value=temp;}");
		sb.append("	}\n");

		sb.append("	}});	\n");
		sb.append("});\n");
		sb.append("</script>\n");

		if (context.getMode() == Mode.ENTER) {
			/* the jquery to pre generate the min number of multiple */
			sb.append("<script type=\"text/javascript\" charset=\"utf-8\">\n");
			sb.append("$(document).ready(function() {\n");
			sb.append("var minRpt   = parseInt($('#minRpt"
					+ context.getNewrepeatSeqVal() + "').val());\n");
			sb.append("var kCount   = parseInt($('#kCount"
					+ context.getNewrepeatSeqVal() + "').val());\n");
			sb.append("for(var i = kCount; i< minRpt;++i){");
			sb.append("$(\"#defaultFieldlistObjAddButton"
					+ context.getNewrepeatSeqVal() + "\").click();\n");
			sb.append("}});");
			sb.append("</script>\n");
		}
		
		sb.append("<span id=\"newRepeatTemplate"
						+ context.getNewrepeatSeqVal()
						+ "\" class=\"newRepeat"
						+ context.getNewrepeatSeqVal()
						+ "\" style=\"display:none\" ><table style=\"display:inline\"> \n");

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

		if (context.getMode() != Mode.VIEW) {
			/* the remove button */// enter and edit
			sb.append("</table><input type=\"button\" id=\"removeRowButton"
							+ "\" value=\""
					+ context.getExistingRptGroups().get(
							context.getNewrepeatSeqVal() - 1).getDellabel()
					+ "\" size=\"1\"   onclick=\"removeParentWithClass(this,'newRepeat"
							+ context.getNewrepeatSeqVal() + "');\" />\n");
			
			sb.append("</span>");
			if (context.getMode() == Mode.EDIT) {
				sb.append("<#reservenewrepeat" + context.getNewrepeatSeqVal());
			} 
			
			sb.append("<input id=\"defaultFieldlistObjAddButton"
					+ context.getNewrepeatSeqVal()
					+ "\" type=\"button\" value=\""
					+ context.getExistingRptGroups().get(
							context.getNewrepeatSeqVal() - 1).getLabel()
					+ "\" size=\"1\" />");
			
		} else {// view
			sb.append("</table></span>");
			sb.append("<#reservenewrepeat" + context.getNewrepeatSeqVal());
		}

		if (!context.getExistingRptGroups().get(
				context.getNewrepeatSeqVal() - 1).isIntd()) {
			sb.append("</td></tr>\n");
		}

		if (context.getMode() != Mode.VIEW) {
			if (context.getMode() != Mode.EDIT) {
				sb.append("<input id=\"kCount" + context.getNewrepeatSeqVal()
						+ "\"" + " name=\"kCount"
						+ context.getNewrepeatSeqVal() + "\""
						+ " style=\"display: none;\" value=\""
						+ context.getNewrepeatTimesSeqVal() + "\"/> \n");
			} else {
				sb.append("<input id=\"kCount"
						+ context.getNewrepeatSeqVal()
						+ "\""
						+ " name=\"kCount"
						+ context.getNewrepeatSeqVal()
						+ "\""
						+ " style=\"display: none;\" value=\""
						+ context.getExistingRptGroups().get(
								context.getNewrepeatSeqVal() - 1)
								.getRepeattime() + "\"/> \n");
			}
		}

		sb.append("<input id=\"minRpt"
				+ context.getNewrepeatSeqVal()
				+ "\""
				+ " name=\"minRpt"
				+ context.getNewrepeatSeqVal()
				+ "\""
				+ " style=\"display: none;\" value=\""
				+ context.getExistingRptGroups().get(
						context.getNewrepeatSeqVal() - 1).getMinrpt()
				+ "\"/> \n");

		sb.append("<input id=\"maxRpt"
				+ context.getNewrepeatSeqVal()
				+ "\""
				+ " name=\"maxRpt"
				+ context.getNewrepeatSeqVal()
				+ "\""
				+ " style=\"display: none;\" value=\""
				+ context.getExistingRptGroups().get(
						context.getNewrepeatSeqVal() - 1).getMaxrpt()
				+ "\"/> \n");

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
		return null;
	}

}
