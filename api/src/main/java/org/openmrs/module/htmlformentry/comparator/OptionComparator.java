package org.openmrs.module.htmlformentry.comparator;

import org.openmrs.module.htmlformentry.widget.Option;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: isha
 * Date: 6/8/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
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
