/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

import java.util.EventObject;

/**
 * @version 0.05 2014-02-28
 * @author Riksarkivet
 *
 */
public class ReadEvent extends EventObject {

    private final Item item;

    public ReadEvent(Reader reader, Item item) {
        super(reader);
        this.item = item;
    }

    public Reader getReader() {
        return (Reader) getSource();
    }

    public Item getItem() {
        return item;
    }
}
