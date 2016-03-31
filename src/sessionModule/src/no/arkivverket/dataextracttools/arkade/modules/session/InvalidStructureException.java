/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.session;

/**
 * 
 * @version 0.05 2014-02-28
 * @author Riksarkivet
 */
public class InvalidStructureException extends Exception {

    /**
     * Creates a new instance of <code>IllegalStructureException</code> without
     * detail message.
     */
    public InvalidStructureException() {
    }

    /**
     * Constructs an instance of <code>IllegalStructureException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public InvalidStructureException(String msg) {
        super(msg);
    }
}
