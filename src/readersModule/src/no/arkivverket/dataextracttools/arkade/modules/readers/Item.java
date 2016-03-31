/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

import java.util.TreeMap;

/**
 * @version 0.05 2014-02-28
 * @author Riksarkivet
 *
 */
public class Item {

    private final Object value;
    private final TreeMap<String, Object> parameters;

    public Item(Object value, TreeMap<String, Object> parameters) {
        this.value = value;
        this.parameters = parameters;
    }

    public Object getValue() {
        return value;
    }

    public TreeMap<String, Object> getParameters() {
        return parameters;
    }
}
