package org.openmrs.module.htmlformentry.comparator;

import org.openmrs.module.htmlformentry.widget.Option;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *  Used to sort the options in a drop down menu in alphabetical order
 */
public class OptionComparator{

    public OptionComparator(List<Option> options) {
        sortList(options);

    }

    private void sortList(List<Option> options) {
        Collections.sort(options, new Comparator<Option>() {

            @Override
            public int compare(Option left, Option right) {
                return left.getLabel().compareTo(right.getLabel());
            }
        });
    }


}
