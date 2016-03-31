/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.exist;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import no.arkivverket.dataextracttools.utils.Utils;
import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.XQueryService;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

/**
 *
 * @version 0.11 2014-02-28
 * @author Riksarkivet
 */
public class ExistUtils {

    public static final String DEFAULT_CONFIGURATION_FILE_PATH
            = "no/arkivverket/dataextracttools/arkade/modules/exist/conf.xml";
    public static final String DEFAULT_LOG4J_FILE_PATH
            = "no/arkivverket/dataextracttools/arkade/modules/exist/log4j.xml";
    public static final String CONFIGURATION_FILE_NAME = "conf.xml";
    public static final String LOG4J_FILE_NAME = "log4j.xml";
    public static final String EXIST_URI = "xmldb:exist:///db";
    public static final String EXIST_DIRECTORY = "eXist";
    public static final String EXIST_DATA_DIRECTORY = "data";
    public static final String EXIST_LOGS_DIRECTORY = "logs";
    public static final String EXIST_JOURNAL_DIRECTORY = "journal";
    public static final String CLEAR_GRAMMAR_CACHE_XQUERY
            = "no/arkivverket/dataextracttools/arkade/modules/exist/clearGrammarCache.xq";

    /**
     *
     * @param collection
     * @throws XMLDBException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static void clearGrammarCache(
            Collection collection) throws XMLDBException, UnsupportedEncodingException, IOException {
        XQueryService service
                = (XQueryService) collection.getService("XQueryService", "1.0");

        String query = Utils.getResourceAsString(
                CLEAR_GRAMMAR_CACHE_XQUERY, ExistUtils.class.getClassLoader());
        service.query(query);
    }

    /**
     *
     * @param parentCollection
     * @param collectionName
     * @return
     * @throws XMLDBException
     */
    public static Collection createCollection(Collection parentCollection,
            String collectionName) throws XMLDBException {
        Collection collection;
        CollectionManagementService mgtService
                = (CollectionManagementService) parentCollection.getService(
                        "CollectionManagementService", "1.0");
        collection = mgtService.createCollection(collectionName);
        return collection;
    }

    /**
     *
     * @param collection
     * @throws XMLDBException
     */
    public static void shutdown(Collection collection) throws XMLDBException {
        DatabaseInstanceManager manager
                = (DatabaseInstanceManager) collection.getService("DatabaseInstanceManager", "1.0");
        manager.shutdown();
    }

    /**
     *
     * @param documentFile
     * @param collection
     * @throws XMLDBException
     */
    public static void storeDocument(File documentFile,
            Collection collection) throws XMLDBException {
        XMLResource document;
        // create new XMLResource
        document = (XMLResource) collection.createResource(
                documentFile.getName(), "XMLResource");
        document.setContent(documentFile);
        collection.storeResource(document);
        ((EXistResource) document).freeResources();
    }

    /**
     *
     * @param documentString
     * @param name
     * @param collection
     * @throws XMLDBException
     */
    public static void storeDocument(String documentString,
            String name, Collection collection) throws XMLDBException {
        XMLResource document;
        document = (XMLResource) collection.createResource(
                name, "XMLResource");
        document.setContent(documentString);
        collection.storeResource(document);
        ((EXistResource) document).freeResources();
    }
}
