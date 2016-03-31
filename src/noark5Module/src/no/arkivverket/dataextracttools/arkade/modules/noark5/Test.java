/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.noark5;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.logging.Level;
import javax.xml.datatype.DatatypeConfigurationException;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.utils.Utils;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

/**
 * Class for executing Noark 5-processes
 *
 * @version 0.12 2014-04-24
 * @author Riksarkivet
 *
 */
public class Test extends BasicDataExtractProcess {

    private boolean start = true;
    private final String queryName;
    private final Collection metadataCollection;
    private final Collection dataCollection;
    private final File rootDirectory;
    private final String documentDirectory;
    private String separatorChar;

    /**
     * 
     * @param id
     * @param testName
     * @param longName
     * @param queryName
     * @param orderKey
     * @param metadataCollection
     * @param dataCollection
     * @param maxNumberOfResults
     * @param rootDirectory
     * @param documentDirectory 
     */
    public Test(String id, String testName, String longName, String queryName,
            String orderKey,
            Collection metadataCollection, Collection dataCollection,
            int maxNumberOfResults,
            File rootDirectory,
            String documentDirectory) {
        super((id + "_" + testName).replace(' ', '_'), testName, longName);
        this.queryName = queryName;
        this.orderKey = orderKey;
        this.metadataCollection = metadataCollection;
        this.dataCollection = dataCollection;
        this.maxNumberOfResults = maxNumberOfResults;
        this.rootDirectory = rootDirectory;
        this.documentDirectory = documentDirectory;
    }

    /**
     * 
     * @param id
     * @param testName
     * @param longName
     * @param queryName
     * @param orderKey
     * @param metadataCollection
     * @param dataCollection
     * @param maxNumberOfResults
     * @param rootDirectory
     * @param documentDirectory
     * @param separatorChar 
     */
    public Test(String id, String testName, String longName, String queryName,
            String orderKey,
            Collection metadataCollection, Collection dataCollection,
            int maxNumberOfResults,
            File rootDirectory,
            String documentDirectory, String separatorChar) {
        this(id, testName, longName, queryName, orderKey, metadataCollection, 
                dataCollection, maxNumberOfResults, rootDirectory, documentDirectory);
        this.separatorChar = separatorChar;
        activity.setOrderKey(orderKey);
    }

    @Override
    public void init() {
    }

    private boolean doQuery() {
        boolean ok = true;
        try {
            XQueryService service = (XQueryService) dataCollection.getService("XQueryService", "1.0");

            service.declareVariable("testName", getName());

            service.declareVariable("testId", getId());
            if (getLongName() != null) {
                service.declareVariable("longName", getLongName());
            }

            service.declareVariable("orderKey", orderKey);
            service.declareVariable("testDescription", getDescription());
            service.declareVariable("resultDescription", getResultDescription());
            if (metadataCollection != null) {
                service.declareVariable(
                        "metadataCollection", metadataCollection.getName());
            }
            if (dataCollection != null) {
                service.declareVariable("dataCollection", dataCollection.getName());
            }
            service.declareVariable("maxNumberOfResults", maxNumberOfResults);
            if (rootDirectory != null) {
                service.declareVariable(
                        "rootDirectory", rootDirectory.getPath());
            }
            if (documentDirectory != null) {
                service.declareVariable(
                        "documentDirectory", documentDirectory);
            }
            if (separatorChar != null) {
                service.declareVariable(
                        "separatorChar", separatorChar);
            }

            String query = null;
            try {
                query = Utils.getResourceAsString(queryName, Thread.currentThread().getContextClassLoader());
            } catch (UnsupportedEncodingException ex) {
                log(ex.getMessage(), Level.SEVERE);
            } catch (IOException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }
            if (debug) {
                log("DEBUG: query: " + query);
            }
            CompiledExpression compiled = service.compile(query);
            ResourceSet result = service.execute(compiled);
            if (result != null && result.getSize() == 1) {
                String content = (String) result.getResource(0).getContent();
                createActivity(content);
                if (debug) {
                    log("DEBUG: " + content);
                }
            }
        } catch (XMLDBException ex) {
                log(ex.getMessage(), Level.SEVERE);
            ok = false;
        }
        return ok;
    }

    @Override
    public void finish() {
        if (!finished) {
            finished = true;
        }
    }

    @Override
    public void itemRead(ReadEvent readEvent) {
        // This is called twice - in the beginning of 
        // read in XMLCollectionReader and in the end.
        // The processing takes place in the end.
        if (start) {
            // 1st time
            start = false;
        } else {
            // 2nd time
            Date date = new Date();
            log(date.toString() + " - Utfører prosess: "
                    + (getLongName() != null ? getLongName() : getName()));
            doQuery();
            if (activity != null) {
                try {
                    activity.setTimeEnded(XMLUtils.createTimeStamp());
                } catch (DatatypeConfigurationException ex) {
                log(ex.getMessage(), Level.SEVERE);
                }
            }
        }
    }
}
