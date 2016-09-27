package org.openmrs.module.htmlformentry.util;

import java.util.Collection;

public interface Transformer<T,V> {
    Collection<V>  transform(Collection<T> collection,Predicate<T> predicate);
}
