package org.openmrs.module.htmlformentry.widget;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlFormEntryUtilTest {

    @Test
    public void testEmptyNodeDescendant() throws Exception {
        String xml = "<htmlform><obs conceptId=\"2\" /></htmlform>";
        Document document = HtmlFormEntryUtil.stringToDocument(xml);
        Node obsNode = HtmlFormEntryUtil.findDescendant(document, "obs");
        Assert.assertNotNull(obsNode);
    }
}