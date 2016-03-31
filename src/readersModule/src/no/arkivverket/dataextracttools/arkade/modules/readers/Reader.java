/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

import no.arkivverket.dataextracttools.metadatastandards.addml.BasicElement;

/**
 * @version 0.08 2014-02-28
 * @author Riksarkivet
 *
 */
public interface Reader {
    public void init();
    public String getId();
    public BasicElement getElement();
    public void addReadListener(ReadListener listener);
    public void removeReadListener(ReadListener listener);
    
}
