/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.arkivverket.dataextracttools.metadatastandards.addml.BasicElement;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescriptionElement;

/**
 * @version 0.06 2014-03-21
 * @author Riksarkivet
 *
 */
public abstract class BasicReader implements Reader, ReadListener {
    private final String id;
    private final DatasetDescriptionElement dde;
    protected ArrayList<ReadListener> readListeners;
    protected String loggerName;

    /**
     * 
     * @param id
     * @param element
     * @param datasetDescription 
     */
    public BasicReader(String id, BasicElement element, DatasetDescription datasetDescription) {
        this.id = id;
        this.dde = new DatasetDescriptionElement(element, "element",
                    datasetDescription);
        readListeners = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public BasicElement getElement() {
        return (dde != null) ? dde.getElement() : null;
    }

    public DatasetDescription getDatasetDescription() {
        return (dde != null) ? dde.getDatasetDescription() : null;
    }

    public DatasetDescriptionElement getDatasetDescriptionElement() {
        return dde;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }
    
    public void log(String message) {
        log(message, Level.INFO);
    }
    
    public void log(String message, Level level) {
        Logger logger;
        Level l;
                
        if (loggerName != null) {
            logger = Logger.getLogger(loggerName);
        } else {
            logger = Logger.getGlobal();
        }
        if (level != null) {
            l = level;
        } else {
            l = Level.INFO;
        }
        if (message != null) {
            logger.log(l, message);
        } else {
            logger.log(l, logger.getName());
        }
    }
    
    public abstract void read();
    
    public abstract void read(Item item);
    
    @Override
    public synchronized void addReadListener(ReadListener listener) {
        readListeners.add(listener);
    }

    @Override
    public synchronized void removeReadListener(ReadListener listener) {
        readListeners.remove(listener);
    }

    @Override
    public String toString() {
        return dde != null ? dde.toString() : super.toString();
    }    
}
