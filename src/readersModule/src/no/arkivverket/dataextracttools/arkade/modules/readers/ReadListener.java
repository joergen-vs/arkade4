/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

/**
 * @version 0.05 2014-02-28
 * @author Riksarkivet
 *
 */
public interface ReadListener {

    public void itemRead(ReadEvent readEvent);
}
