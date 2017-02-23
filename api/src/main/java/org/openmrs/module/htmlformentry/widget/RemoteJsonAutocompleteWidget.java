package org.openmrs.module.htmlformentry.widget;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.htmlformentry.FormEntryContext;

import javax.servlet.http.HttpServletRequest;

/**
 * Uses jQuery-ui, and uses handlebars for custom display and value templates
 */
public class RemoteJsonAutocompleteWidget implements Widget {

    private Option initialValue;
    private String remoteUrl;
    private String valueTemplate = "{{value}}";
    private String displayTemplate = "{{label}}";

    public RemoteJsonAutocompleteWidget(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public void setValueTemplate(String valueTemplate) {
        this.valueTemplate = valueTemplate;
    }

    public void setDisplayTemplate(String displayTemplate) {
        this.displayTemplate = displayTemplate;
    }

    @Override
    public void setInitialValue(Object initialValue) {
        if (initialValue instanceof Drug) {
            Drug asDrug = (Drug) initialValue;
            this.initialValue = new Option(asDrug.getName(), "Drug:" + asDrug.getId(), true);
        }
        else if (initialValue instanceof Concept) {
            Concept asConcept = (Concept) initialValue;
            this.initialValue = new Option((asConcept.getName().getName()), asConcept.getConceptId().toString(), true);
        }
        else {
            this.initialValue = (Option) initialValue;
        }
    }

    @Override
    public String generateHtml(FormEntryContext context) {
        if (FormEntryContext.Mode.VIEW == context.getMode()) {
            return initialValue == null ? "" : initialValue.getLabel();
        }
        String formFieldName = context.getFieldName(this);
        StringBuilder sb = new StringBuilder();
        sb.append("<input id=\"" + formFieldName + "-display\"");
        if (initialValue != null) {
            sb.append(" value=\"" + initialValue.getLabel() + "\"");
        }
        sb.append("/>\n");
        sb.append("<input id=\"" + formFieldName + "-value\" type=\"hidden\" name=\"" + formFieldName + "\"");
        if (initialValue != null) {
            sb.append(" value=\"" + initialValue.getValue() + "\"");
        }
        sb.append("/>\n");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("$j(function() {\n");
        sb.append("  var displayTemplate" + formFieldName + " = htmlForm.compileMustacheTemplate('" + escapeJs(displayTemplate) + "');\n");
        sb.append("  var valueTemplate" + formFieldName + " = htmlForm.compileMustacheTemplate('" + escapeJs(valueTemplate) + "');\n");
        sb.append("  $j('#" + formFieldName + "-display').autocomplete({\n");
        sb.append("    source: '" + remoteUrl + "',\n");
        sb.append("    minLength: 2,\n");
        sb.append("    focus: function(event, ui) {\n");
        sb.append("      if (ui.item.name != 'No results') {\n");
        sb.append("         $j('#" + formFieldName + "-display').val(displayTemplate" + formFieldName + "(ui.item));\n");
        sb.append("      } else {\n");
        sb.append("         $j('#" + formFieldName + "-value').val('');\n");
        sb.append("      }\n");
        sb.append("      return false;\n");
        sb.append("    },\n");
        sb.append("    select: function(event, ui) {\n");
        sb.append("      if (ui.item.name != 'No results') {\n");
        sb.append("         $j('#" + formFieldName + "-display').val(displayTemplate" + formFieldName + "(ui.item));\n");
        sb.append("         $j('#" + formFieldName + "-value').val(valueTemplate" + formFieldName + "(ui.item));\n");
        sb.append("      } else {\n");
        sb.append("         $j('#" + formFieldName + "-value').val('');\n");
        sb.append("      }\n");
        sb.append("      return false;");
        sb.append("    },\n");
        sb.append("    change: function(event, ui) {\n");
        sb.append("      if (!ui.item) {\n");
        sb.append("         $j('#" + formFieldName + "-value').val('');\n");
        sb.append("      }\n");
        sb.append("    },\n");
        sb.append("    response: function(event, ui) {\n");
        sb.append("      var results = ui.content;\n");
        sb.append("      if (results.length === 0) {\n");
        sb.append("         results.push({ value: '', name: 'No results', label: '' });\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  })\n");
        sb.append("  .data('ui-autocomplete')._renderItem = function(ul, item) {\n");
        sb.append("    return $j('<li>').data('autocomplete-item', item).append('<a>' + displayTemplate" + formFieldName + "(item) + '</a>').appendTo(ul);\n");
        sb.append("  };\n");
        sb.append("});\n");
        sb.append("</script>\n");
        return sb.toString();
    }

    private String escapeJs(String input) {
        input = input.replaceAll("\n", "\\\\n");
        input = input.replaceAll("'", "\\\\'");
        input = input.replaceAll("\"", "\\\\\"");
        return input;
    }

    /**
     * @see Widget#getValue(FormEntryContext, HttpServletRequest)
     */
    @Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        return request.getParameter(context.getFieldName(this));
    }

}
