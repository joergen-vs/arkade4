/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.exist;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.arkivverket.dataextracttools.utils.Utils;
import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.XmldbURI;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

/**
 *
 * @version 0.07 2014-02-28
 * @author Riksarkivet
 */
public class ExistDatabase {

    public static final String DEFAULT_CONFIGURATION_FILE_PATH
            = "no/arkivverket/dataextracttools/arkade/modules/exist/conf.xml";
    public static final String DEFAULT_LOG4J_FILE_PATH
            = "no/arkivverket/dataextracttools/arkade/modules/exist/log4j.xml";
    public static final String FUNCTX
            = "no/arkivverket/dataextracttools/arkade/modules/exist/resources/functx-1.0-doc-2007-01.xq";
    public static final String CONFIGURATION_FILE_NAME = "conf.xml";
    public static final String LOG4J_FILE_NAME = "log4j.xml";
    public static final String EXIST_URI = "xmldb:exist://";
    public static final String EXIST_DIRECTORY = "eXist";
    public static final String EXIST_DATA_DIRECTORY = "data";
    public static final String EXIST_LIB_DIRECTORY = "lib";
    public static final String EXIST_LOGS_DIRECTORY = "logs";
    public static final String EXIST_JOURNAL_DIRECTORY = "journal";
    public static final String EXIST_USER_DIRECTORY = "user";
    private final Logger logger;
    private final boolean debug;
    private final File parentDirectory;
    private File existDirectory;
    private final File existConfigurationFile;
    private File currentExistConfigurationFile;
    private boolean canConnect = true;
    private boolean newDatabase = true;
    private Collection rootCollection;

    /**
     *
     * @param sessionId The id of the session this database belongs to.
     * @param parentDirectory The parent directory for the eXist database
     * @param existConfigurationFile Not in use. The standard config file is
     * always used.
     */
    public ExistDatabase(String sessionId, File parentDirectory,
            File existConfigurationFile) {
        this.logger = Logger.getLogger(sessionId);
        this.parentDirectory = parentDirectory;
        this.existConfigurationFile = existConfigurationFile;
        debug = Boolean.parseBoolean(System.getProperty("arkade.session.debug", "false"));
    }

    public void init() {
        boolean ok = true;
        existDirectory
                = new File(parentDirectory, ExistUtils.EXIST_DIRECTORY);
        System.setProperty("exist.home", existDirectory.getPath());

        boolean databaseExists = false;
        File existDataDirectory
                = new File(existDirectory, EXIST_DATA_DIRECTORY);
        currentExistConfigurationFile
                = new File(existDirectory, CONFIGURATION_FILE_NAME);

        // Finnes databasen fra før?
        // Sjekke om eXist-katalogen finnes og at den inneholder
        // en konfigurasjonsfil.
        if (existDirectory.exists()
                && currentExistConfigurationFile.exists()) {
            // Ja - sjekke om datakatalogen finnes
            // i eXist-katalogen og at den ikke er tom.
            if (existDataDirectory.exists()
                    && existDataDirectory.list().length > 0) {
                // Ja - Antar at databasen finnes fra før
                databaseExists = true;
            }
        }
        if (databaseExists) {
            newDatabase = false;
            return;
        }

        logger.info("Oppretter eXist-database");

        // Opprette eXist-katalogen
        existDirectory.mkdir();

        // Opprette datakatalogen med underkatalogen journal
        existDataDirectory.mkdir();
        File existJournalDirectory = new File(
                existDataDirectory,
                EXIST_JOURNAL_DIRECTORY);
        existJournalDirectory.mkdir();

        // Opprette loggkatalogen
        File existLogsDirectory = new File(
                existDirectory,
                EXIST_LOGS_DIRECTORY);
        existLogsDirectory.mkdir();

        // Kopiere log4j.xml til eXist-katalogen
        File log4jConfigurationFile = new File(existDirectory,
                LOG4J_FILE_NAME);
        try {
            Utils.copyResource(
                    DEFAULT_LOG4J_FILE_PATH,
                    log4jConfigurationFile,
                    this.getClass().getClassLoader());
        } catch (IOException ex) {
            Logger.getLogger(ExistDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.setProperty("log4j.configuration", "file:/" + log4jConfigurationFile.getPath());

        if (debug) {
            logger.info("DEBUG: " + System.getProperty("log4j.configuration"));
        }
        // Opprette konfigurasjonsfil
        boolean useStandardConfigFile = true;

        // Hvis en konfigurasjonsfil er oppgitt i
        // existConfigurationFile, skal denne brukes. Hvis ikke
        // brukes standard konfigurasjonsfil.
        if (existConfigurationFile != null) {
            // Kopiere filen til utkatalogen

            useStandardConfigFile = false;

            // Sjekke om filen er conf.xml
            // Hvis den ikke heter conf.xml, blir den ikke kopiert!
            // TODO Gi beskjed hvis navnet avviker og standard
            // konfigureringsfil må benyttes.
            if (!CONFIGURATION_FILE_NAME.equals(
                    existConfigurationFile.getName())) {
                logger.severe("Ugyldig navn på oppgitt konfigurasjonsfil.\n"
                        + existConfigurationFile.getPath()
                        + "\nKonfigurasjonsfilen må hete conf.xml.\n"
                        + "Bruker standard konfigurasjonsfil.");
                useStandardConfigFile = true;
            }

            if (!useStandardConfigFile) {
                // TODO Temp fjernet. Sjekk ok
//                    ok = Utils.copyFile(
//                            existConfigurationFile,
//                            existDirectory);
//                    logger.info("Konfigurasjonsfil: "
//                            + existConfigurationFile.getPath());
            }
        }

        if (useStandardConfigFile) {
            try {
                // Kopiere standard konfigurasjonsfil til
                // eXist-katalogen.
                // TODO sjekk om ok
                Utils.copyResource(
                        DEFAULT_CONFIGURATION_FILE_PATH,
                        new File(existDirectory,
                                CONFIGURATION_FILE_NAME),
                        this.getClass().getClassLoader());
                logger.info("Konfigurasjonsfil (conf.xml): Standard");
            } catch (IOException ex) {
                ok = false;
                logger.log(Level.SEVERE, null, ex);
            }
        }

        if (!ok) {
            // Noe har gått galt.
            logger.severe("Kan ikke åpne eXist-databasen.");
        }
    }

    public void addIndexFile(String path, String name, String indexFile) {
        Collection configCollection;
        Collection indexCollection;

        try {
            configCollection = DatabaseManager.getCollection(
                    EXIST_URI + XmldbURI.ROOT_COLLECTION + "/system/config/db",
                    "admin", "");
            indexCollection = createCollection(configCollection, path);
            String index = Utils.getResourceAsString(indexFile,
                    Thread.currentThread().getContextClassLoader());
            ExistUtils.storeDocument(index, name, indexCollection);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ExistDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ExistDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XMLDBException ex) {
            Logger.getLogger(ExistDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

        }
    }

    public boolean isNewDatabase() {
        return newDatabase;
    }

    public Collection connect() {
        if (rootCollection != null) {
            logger.warning("Already connected to eXist-database in "
                    + existDirectory.getPath());
            return rootCollection;
        }

        if (!canConnect && rootCollection == null) {
            logger.severe("Can not connect to eXist-database in "
                    + existDirectory.getPath());
            return null;
        }

        try {
            Class cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            Database database = (Database) cl.newInstance();

            database.setProperty("configuration", currentExistConfigurationFile.getPath());
            database.setProperty("create-database", "true");
            if (debug) {
                System.out.println("DEBUG: " + System.getProperty("exist.home"));
            }

            DatabaseManager.registerDatabase(database);
            rootCollection = DatabaseManager.getCollection(EXIST_URI + XmldbURI.ROOT_COLLECTION, "admin", "");
        } catch (XMLDBException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (InstantiationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        if (rootCollection != null) {
            canConnect = false;
        }

        return rootCollection;
    }

    public Collection getRootCollection() {
        return rootCollection;
    }

    public Collection createCollection(Collection parentCollection,
            String collectionName) {
        Collection collection = null;
        try {
            CollectionManagementService mgtService
                    = (CollectionManagementService) parentCollection.getService(
                            "CollectionManagementService", "1.0");
            collection = mgtService.createCollection(collectionName);
        } catch (XMLDBException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return collection;
    }

    public Collection getCollection(Collection parentCollection,
            String name) {
        Collection collection = null;
        try {
            collection = parentCollection.getChildCollection(name);
        } catch (XMLDBException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return collection;
    }

    public void shutdown(Collection collection) {
        try {
            if (collection == null) {
                return;
            }
            DatabaseInstanceManager manager
                    = (DatabaseInstanceManager) collection.getService("DatabaseInstanceManager", "1.0");
            collection.close();
            if (manager != null) {
                manager.shutdown();
            }
        } catch (XMLDBException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public boolean storeDocument(File documentFile,
            Collection collection) {
        boolean stored = true;
        XMLResource document = null;
        try {
            // create new XMLResource
            document = (XMLResource) collection.createResource(
                    documentFile.getName(), "XMLResource");
            document.setContent(documentFile);
            collection.storeResource(document);
        } catch (XMLDBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            stored = false;
        } finally {
            if (document != null) {
                try {
                    ((EXistResource) document).freeResources();
                } catch (XMLDBException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                    stored = false;
                }

            }
        }
        return stored;
    }

    public void storeCommonFunctions() {
        String query;
        try {
            query = Utils.getResourceAsString(
                    FUNCTX, ExistDatabase.class.getClassLoader());

            org.xmldb.api.base.Collection libCollection
                    = ExistUtils.createCollection(rootCollection, "lib");
            BinaryResource moduleResource = (BinaryResource) libCollection.createResource("functx-1.0-doc-2007-01.xq", "BinaryResource");
            ((EXistResource) moduleResource).setMimeType("application/xquery");

            moduleResource.setContent(query);
            libCollection.storeResource(moduleResource);
        } catch (XMLDBException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public boolean storeDocument(String name, String content,
            Collection collection) {
        boolean stored = true;
        XMLResource document = null;
        try {
            // create new XMLResource
            document = (XMLResource) collection.createResource(
                    name, "XMLResource");
            document.setContent(content);
            collection.storeResource(document);
        } catch (XMLDBException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            stored = false;
        } finally {
            if (document != null) {
                try {
                    ((EXistResource) document).freeResources();
                } catch (XMLDBException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                    stored = false;
                }
            }
        }
        return stored;
    }
}
