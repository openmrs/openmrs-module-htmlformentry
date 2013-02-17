package org.openmrs.module.htmlformentry.comparator;

import java.util.Comparator;

import org.openmrs.module.htmlformentry.widget.Option;

/**
 *  Used to sort the options in a drop down menu in alphabetical order
 */

public class OptionComparator implements Comparator<Option>{

    public OptionComparator() {

    }

    @Override
    public int compare(Option left, Option right) {
        return left.getLabel().compareTo(right.getLabel());
    }
}
