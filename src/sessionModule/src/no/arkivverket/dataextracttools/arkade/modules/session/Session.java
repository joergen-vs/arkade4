/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.session;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import no.arkivverket.dataextracttools.arkade.modules.exist.ExistDatabase;
import no.arkivverket.dataextracttools.arkade.modules.exist.ExistUtils;
import no.arkivverket.dataextracttools.arkade.modules.flatfiledatabase.FlatFileDatabase;
import no.arkivverket.dataextracttools.arkade.modules.noark5.version_3_1.Noark_5_v_3_1_ProcessHelper;
import no.arkivverket.dataextracttools.arkade.modules.processes.DataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.readers.DatasetReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FlatFileReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.xml.XmlCollectionReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.xml.XmlDocumentReader;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.SessionReport;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescriptionElement;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescriptionFactory;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescriptionHelper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2_Helper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DataObject;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DataObjects;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Dataset;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileProcesses;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFiles;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Processes;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Property;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordProcesses;
import no.arkivverket.dataextracttools.utils.xml.JAXBValidationEventHandler;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.xml.sax.SAXException;
import org.xmldb.api.base.XMLDBException;

/**
 * Main class for processing datasets.
 *
 * @version 0.16 2014-04-10
 * @author Riksarkivet
 *
 */
public class Session implements ReadListener {

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int CAN_RESTART = 2;
    public static final Object[] YES_NO_OPTIONS = new Object[]{
        NbBundle.getMessage(Session.class, "CTL_Yes"),
        NbBundle.getMessage(Session.class, "CTL_No")
    };
    // flat file
    public static final String ANALYSE_COUNT_RECORDS = "Analyse_CountRecords";
    public static final String CONTROL_NUMBER_OF_RECORDS = "Control_NumberOfRecords";
    public static final String CONTROL_CHECKSUM = "Control_Checksum";
    // record
    public static final String CONTROL_FOREIGN_KEY = "Control_ForeignKey";
    public static final String CONTROL_KEY = "Control_Key";
    // field
    public static final String ANALYSE_FIELD = "Analyse_Field";
    public static final String ANALYSE_FREQUENCY_LIST = "Analyse_FrequencyList";
    public static final String CONTROL_CODES = "Control_Codes";
    public static final String CONTROL_DATA_FORMAT = "Control_DataFormat";
    public static final String CONTROL_FIELD = "Control_Field";
    public static final String CONTROL_UNIQUENESS = "Control_Uniqueness";

    private static final String DATA_OBJECT_GENERIC_TYPE = "Generic";
    private static final String DATA_OBJECT_XML_TYPE = "XML";
    private static final String DATA_OBJECT_NOARK_5_TYPE = "Noark 5";
    private static final String DATA_OBJECT_NOARK_5_VERSION_3_1 = "3.1";
    private static final String DATA_OBJECT_UNKNOWN_TYPE = "Unknown";
    private static Logger logger;
    private final boolean debug;
    private final String id;
    private String name;
    private String sessionCreator;
    private Addml_8_2 datasetDescription;
    private File datasetDescriptionFile;
    private File dataDirectory;
    private File outputDirectory;
    private boolean createFlatFileDatabase;
    private boolean createFlatFileDatabaseIndexes;
    private int flatFileDatabaseNumberOfInsertsBeforeCommit;
    private boolean ignoreDatasetDescriptionProcesses;
    private boolean delimitedFilesHaveHeader;
    private boolean analyseFlatFiles; // If true, flat file-analysis are executed.
    private boolean controlFlatFiles; // If true, flat file-controls are executed.
    private boolean analyseFields; // If true, field-analysis in flat files are executed.
    private boolean controlFields; // If true, field-controls in flat files are executed. Not Control_Uniqueness.
    private boolean controlKeys;
    private boolean controlForeignKeys;
    private boolean controlUniqueness;
    private SessionReport sessionReport;
    private int maxNumberOfResults;
    private DatasetReader datasetReader;
    private FlatFileDatabase flatFileDatabase;
    private ExistDatabase existDatabase;
    private org.xmldb.api.base.Collection rootCollection;
    private final ArrayList<ReadListener> listenerList;
    private final ArrayList<String> datasetProcesses;
    // Prosesser
    // Inneholder referanse til samtlige prosesser som utføres på datasettet.
    // Brukes primært til å få tilgang til resultatet fra de prosessene som
    // har resultatet klart når lesingen er ferdig, f.eks. controlUnique.
    private ArrayList<DataExtractProcess> processList;
    private int orderCounter = 0;

    public Session(String id) {
        this.id = id;
        logger = Logger.getLogger(id);
        debug = Boolean.parseBoolean(System.getProperty("arkade.session.debug", "false"));
        listenerList = new ArrayList<>();
        datasetProcesses = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSessionCreator() {
        return sessionCreator;
    }

    public void setSessionCreator(String sessionCreator) {
        this.sessionCreator = sessionCreator;
    }

    public void setDatasetDescriptionFile(File datasetDescriptionFile) {
        this.datasetDescriptionFile = datasetDescriptionFile;
    }

    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setCreateFlatFileDatabase(boolean createFlatFileDatabase) {
        this.createFlatFileDatabase = createFlatFileDatabase;
    }

    public void setCreateFlatFileDatabaseIndexes(boolean createFlatFileDatabaseIndexes) {
        this.createFlatFileDatabaseIndexes = createFlatFileDatabaseIndexes;
    }

    public void setFlatFileDatabaseNumberOfInsertsBeforeCommit(int flatFileDatabaseNumberOfInsertsBeforeCommit) {
        this.flatFileDatabaseNumberOfInsertsBeforeCommit
                = flatFileDatabaseNumberOfInsertsBeforeCommit;
    }

    public void setMaxNumberOfResults(int maxNumberOfResults) {
        this.maxNumberOfResults = maxNumberOfResults;
    }

    public void setIgnoreProcesses(boolean ignoreProcesses) {
        this.ignoreDatasetDescriptionProcesses = ignoreProcesses;
    }

    public void setDelimitedFilesHaveHeader(boolean delimitedFilesHaveHeader) {
        this.delimitedFilesHaveHeader = delimitedFilesHaveHeader;
    }

    public void setAnalyseFlatFiles(boolean analyseFlatFiles) {
        this.analyseFlatFiles = analyseFlatFiles;
    }

    public void setControlFlatFiles(boolean controlFlatFiles) {
        this.controlFlatFiles = controlFlatFiles;
    }

    public void setAnalyseFields(boolean analyseFields) {
        this.analyseFields = analyseFields;
    }

    public void setControlFields(boolean controlFields) {
        this.controlFields = controlFields;
    }

    public void setControlKeys(boolean controlKeys) {
        this.controlKeys = controlKeys;
    }

    public void setControlForeignKeys(boolean controlForeignKeys) {
        this.controlForeignKeys = controlForeignKeys;
    }

    public void setControlUniqueness(boolean controlUniqueness) {
        this.controlUniqueness = controlUniqueness;
    }

    public int startSession() {
        int result = SUCCESS;
        boolean canRead = true;
        boolean validDatasetDescription = false;

        try {
            logger.info("Leser inn og validerer datasettbeskrivelsen.");
            logger.info(" - Datasettbeskrivelse: " + datasetDescriptionFile.getPath());
            logger.info(" - Metadatastandard...: " + DatasetDescriptionFactory.ADDML_8_2);
            JAXBValidationEventHandler validationEventHandler = new JAXBValidationEventHandler(logger);
            datasetDescription = (Addml_8_2) DatasetDescriptionHelper.openDatasetDescription(
                    datasetDescriptionFile, DatasetDescriptionFactory.ADDML_8_2, validationEventHandler);
            validDatasetDescription = validationEventHandler.isValid();
        } catch (FileNotFoundException | ParserConfigurationException | SAXException | JAXBException ex) {
            Exceptions.printStackTrace(ex);
        }

        if (datasetDescription == null) {
            logger.severe("Datasettbeskrivelsen kunne ikke leses inn.");
            result = FAILURE;
            canRead = false;
        } else {
            if (!validDatasetDescription) {
                // Continue with invalid dataset description?
                logger.info("Datasettbeskrivelsen validerer ikke mot skjemaet.");
                String message = NbBundle.getMessage(Session.class,
                        "MSG_NotValidDatasetDescription");
                String title = NbBundle.getMessage(Session.class,
                        "LBL_ValidateDatasetDescription");
                NotifyDescriptor descriptor
                        = new NotifyDescriptor(message,
                                title,
                                NotifyDescriptor.YES_NO_OPTION,
                                NotifyDescriptor.WARNING_MESSAGE,
                                Session.YES_NO_OPTIONS,
                                Session.YES_NO_OPTIONS[0]);

                DialogDisplayer.getDefault().notify(descriptor);
                if (descriptor.getValue().equals(
                        Session.YES_NO_OPTIONS[1])) {
                    result = FAILURE;
                    canRead = false;
                }
            } else {
                logger.info("Datasettbeskrivelsen validerer mot skjemaet.");
            }
        }

        if (canRead) {
            logger.info("Oppretter rapport for resultatet fra arbeidsøkten.");
            sessionReport = new SessionReport(id, datasetDescription, outputDirectory);
            sessionReport.setCreatedBy(sessionCreator);
            sessionReport.open();

            // Only when flat files
            FlatFiles flatFiles = datasetDescription.getFlatFiles();
            if (flatFiles != null) {
                canRead = prepareFlatFileReading(flatFiles);

                if (!canRead) {
                    NotifyDescriptor descriptor
                            = new NotifyDescriptor("Noe har medført at lesing av datasettet ikke kan starte. "
                                    + "\nØnsker du å forandre noen av opplysningene og starte på nytt?",
                                    "Starte kjøring",
                                    NotifyDescriptor.YES_NO_OPTION,
                                    NotifyDescriptor.WARNING_MESSAGE,
                                    Session.YES_NO_OPTIONS,
                                    Session.YES_NO_OPTIONS[0]);

                    DialogDisplayer.getDefault().notify(descriptor);
                    if (descriptor.getValue().equals(
                            Session.YES_NO_OPTIONS[0])) {
                        result = CAN_RESTART;
                    } else {
                        result = FAILURE;
                    }

                    return result;
                }
            }

            if (canRead) {
                processList = new ArrayList<>();
                canRead = createDatasetReader(datasetDescription.getDataset());
            }

            if (canRead && datasetReader != null) {
                datasetReader.read();

                // cleanup
                datasetReader = null;

                if (flatFileDatabase != null && createFlatFileDatabaseIndexes) {
                    flatFileDatabase.createIndexesFromKeys();
                    flatFileDatabase.createIndexesFromFields(true);
                }
                if (debug) {
                    flatFileDatabase.getIndexInfo();
                }

                if (processList != null && !processList.isEmpty()) {
                    logger.info("Ferdiggjør prosesser og utfører eventuelle prosesser mot database...");
                    for (DataExtractProcess process : processList) {

                        logger.info(process.getId());

                        process.finish();
                        sessionReport.write(process.getActivity());
                    }
                }

                close();
            }
        }
        return result;
    }

    /**
     * Closes the session immediately. Use with care!
     */
    public void close() {
        Date date = new Date();
        logger.info("Avslutter arbeidsøkten: " + date.toString());
        if (processList != null && !processList.isEmpty()) {
            // Clean up process list
            processList.clear();
            processList = null;
        }

        if (flatFileDatabase != null) {
            // TODO Temp
            flatFileDatabase.getIndexInfo();

            flatFileDatabase.shutdown();
            flatFileDatabase = null;
        }

        if (existDatabase != null && rootCollection != null) {
            existDatabase.shutdown(rootCollection);
            existDatabase = null;
        }

        if (sessionReport != null) {
            sessionReport.close();
            sessionReport = null;
        }        
    }

    private boolean prepareFlatFileReading(FlatFiles flatFiles) {
        boolean canReadFlatFiles = true;
        boolean needsFlatFileDatabase;
        boolean hasFlatFileDatabase;
        String databaseName = datasetDescription.getDataset().getName();
        if (databaseName == null) {
            String fileName = datasetDescriptionFile.getName();
            databaseName = fileName.substring(0, fileName.lastIndexOf(".")) + "_database";
        }

        // Database er nødvendig når datasettbeskrivelsen inneholder 
        // prosessangivelse for nøkkelkontroll:
        // Control_Key, Control_ForeignKey
        // eller når bruker ber eksplisitt om det.
        // Control_Uniqueness trenger også database.
        needsFlatFileDatabase = needsFlatFileDatabase(flatFiles);

        // Has flat file database?
        File file = new File(outputDirectory, databaseName);
        hasFlatFileDatabase = file.exists();

        if (needsFlatFileDatabase) {
            logger.info("Trenger database for å utføre visse prosesser "
                    + "på flate filer i datasettet.");
            createFlatFileDatabase = true;
        }

        if (createFlatFileDatabase && !hasFlatFileDatabase && !needsFlatFileDatabase) {
            NotifyDescriptor descriptor
                    = new NotifyDescriptor("En database for prosessering av flate "
                            + "filer skal opprettes i utkatalogen. "
                            + "\nKjøringen er ikke avhengig av denne databasen."
                            + "\n\nVær obs på at innlesing til en database kan medføre at "
                            + "kjøringen tar merkbart lengre tid.\n"
                            + "Det er " + (!createFlatFileDatabaseIndexes ? "ikke " : "")
                            + "angitt at indekser skal opprettes. "
                            + (!createFlatFileDatabaseIndexes ? "Dette kan eventuelt angis før kjøringen startes." : "")
                            + "\n\nØnsker du å fortsette med kjøringen?",
                            "Opprette database for flate filer",
                            NotifyDescriptor.YES_NO_OPTION,
                            NotifyDescriptor.INFORMATION_MESSAGE,
                            Session.YES_NO_OPTIONS,
                            Session.YES_NO_OPTIONS[0]);

            DialogDisplayer.getDefault().notify(descriptor);
            if (descriptor.getValue().equals(
                    Session.YES_NO_OPTIONS[1])) {
                canReadFlatFiles = false;
            }
        }

        if (createFlatFileDatabase && !hasFlatFileDatabase && canReadFlatFiles) {
            if (!createFlatFileDatabaseIndexes) {
                NotifyDescriptor descriptor
                        = new NotifyDescriptor("Skal databasen indekseres?",
                                "Opprette database for flate filer",
                                NotifyDescriptor.YES_NO_OPTION,
                                NotifyDescriptor.INFORMATION_MESSAGE,
                                Session.YES_NO_OPTIONS,
                                Session.YES_NO_OPTIONS[0]);

                DialogDisplayer.getDefault().notify(descriptor);
                if (descriptor.getValue().equals(
                        Session.YES_NO_OPTIONS[0])) {
                    createFlatFileDatabaseIndexes = true;
                }
            }
        }

        if (hasFlatFileDatabase) {
            logger.info("En database for prosessering av flate "
                    + "filer finnes allerede i utkatalogen:");
            logger.info(" - '" + databaseName + "'");
        }

        if (canReadFlatFiles && (createFlatFileDatabase || hasFlatFileDatabase)) {
            if (hasFlatFileDatabase) {
                NotifyDescriptor descriptor
                        = new NotifyDescriptor(
                                (needsFlatFileDatabase
                                ? "Kjøringen er avhengig av en database for prosessering av flate filer.\n" : "")
                                + "En database for flate filer finnes allerede i utkatalogen."
                                + "\nHvis den ikke skal brukes, avbrytes kjøringen."
                                + "\n\nØnsker du å bruke den eksisterende databasen?",
                                "Database for flate filer",
                                NotifyDescriptor.YES_NO_OPTION,
                                NotifyDescriptor.WARNING_MESSAGE,
                                Session.YES_NO_OPTIONS,
                                Session.YES_NO_OPTIONS[0]);

                DialogDisplayer.getDefault().notify(descriptor);
                if (descriptor.getValue().equals(
                        Session.YES_NO_OPTIONS[0])) {
                    // Use exsisting database. Re-index? 

                    if (!createFlatFileDatabaseIndexes) {
                        NotifyDescriptor descriptor2
                                = new NotifyDescriptor("Skal den eksisterende databasen indekseres/re-indekseres?",
                                        "Opprette database for flate filer",
                                        NotifyDescriptor.YES_NO_OPTION,
                                        NotifyDescriptor.WARNING_MESSAGE,
                                        Session.YES_NO_OPTIONS,
                                        Session.YES_NO_OPTIONS[1]);

                        DialogDisplayer.getDefault().notify(descriptor2);
                        if (descriptor2.getValue().equals(
                                Session.YES_NO_OPTIONS[0])) {
                            createFlatFileDatabaseIndexes = true;
                        }
                    }
                } else {
                    canReadFlatFiles = false;
                }
            }
        }

        // index non-exsisting database!!?
        if (canReadFlatFiles
                && !hasFlatFileDatabase && !createFlatFileDatabase && createFlatFileDatabaseIndexes) {
            NotifyDescriptor descriptor
                    = new NotifyDescriptor("Det er angitt at det skal "
                            + "opprettes indekser, men ingen flat fil-database finnes. "
                            + "Opprette database og indeksere denne?",
                            "Opprette indekser",
                            NotifyDescriptor.YES_NO_OPTION,
                            NotifyDescriptor.WARNING_MESSAGE,
                            Session.YES_NO_OPTIONS,
                            Session.YES_NO_OPTIONS[0]);

            DialogDisplayer.getDefault().notify(descriptor);
            if (descriptor.getValue().equals(
                    Session.YES_NO_OPTIONS[1])) {
                canReadFlatFiles = false;
            } else {
                createFlatFileDatabase = true;
            }
        }

        // Conclusive information
        if (canReadFlatFiles) {
            if (createFlatFileDatabase && !hasFlatFileDatabase) {
                logger.info("Database skal opprettes.");
            } else if (hasFlatFileDatabase) {
                logger.info("Eksisterende database skal brukes.");
            }
            if (createFlatFileDatabase || hasFlatFileDatabase) {
                logger.info("Indekser skal "
                        + (!createFlatFileDatabaseIndexes ? "ikke " : "") + "opprettes.");
            }
        }
        
        if (canReadFlatFiles
                && (createFlatFileDatabase || createFlatFileDatabaseIndexes)) {
            boolean create = false;
            if (!hasFlatFileDatabase) {
                create = true;
            }
            flatFileDatabase = new FlatFileDatabase(id, datasetDescription,
                    outputDirectory, databaseName, create,
                    flatFileDatabaseNumberOfInsertsBeforeCommit);
            flatFileDatabase.setSessionReport(sessionReport);
            flatFileDatabase.connect();
            flatFileDatabase.read();
            if (create) {
                listenerList.add(flatFileDatabase);
            }
        }

        if (!canReadFlatFiles) {
            logger.severe("Datasettet blir ikke lest.");
        }

        return canReadFlatFiles;
    }

    /**
     * Determines if the session needs to have a database for processing flat
     * files in the dataset. If for example the session includes controlling
     * keys of any kind, a database is required.
     *
     * @param flatFiles
     * @return true if a flat file database is required
     */
    private boolean needsFlatFileDatabase(FlatFiles flatFiles) {
        boolean needsFlatFileDatabase = false;

        if (controlKeys || controlForeignKeys || controlUniqueness) {
            needsFlatFileDatabase = true;
        } else if (!ignoreDatasetDescriptionProcesses) {
            List<FlatFileProcesses> flatFileProcessesList = flatFiles.getFlatFileProcesses();
            for (FlatFileProcesses flatFileProcesses : flatFileProcessesList) {
                List<RecordProcesses> recordProcessesList = flatFileProcesses.getRecordProcesses();
                for (RecordProcesses recordProcesses : recordProcessesList) {
                    if (recordProcesses.getProcesses() != null) {
                        if (datasetDescription.hasProcess(recordProcesses.getProcesses(), CONTROL_KEY)
                                || datasetDescription.hasProcess(recordProcesses.getProcesses(), CONTROL_FOREIGN_KEY)) {
                            needsFlatFileDatabase = true;
                            break;
                        }
//                        List
                    }
                }
            }
        }
        return needsFlatFileDatabase;
    }

    /**
     *
     * @param dataset
     * @param listenerList
     * @return true if the reader tree was created and the dataset can be read
     */
    private boolean createDatasetReader(Dataset dataset) {
        boolean canRead = true;

        // Gir hver forekomst i datasettstrukturen en unik ID.
        // For dataset-nivået brukes name.
        String elementId = dataset.getName();
        if (elementId == null) {
            elementId = "dataset";
        }

        datasetReader = new DatasetReader(elementId,
                dataset, datasetDescription, dataDirectory);

        if (datasetReader == null) {
            logger.severe("Ingen datasettleser ble opprettet");
            return false;
        }

        datasetReader.setLoggerName(id);
        datasetReader.init();

        // Flat files
        if (dataset.getFlatFiles() != null) {
            try {
                SessionHelper.createFlatFileReaders(
                        datasetReader, listenerList, delimitedFilesHaveHeader);

                // Add processes
                // datasetProcesses
                if (analyseFlatFiles) {
                    datasetProcesses.add(ANALYSE_COUNT_RECORDS);
                }
                if (controlFlatFiles) {
                    datasetProcesses.add(CONTROL_CHECKSUM);
                    datasetProcesses.add(CONTROL_NUMBER_OF_RECORDS);
                }
                
                if (controlKeys) {
                    datasetProcesses.add(CONTROL_KEY);
                }

                if (controlForeignKeys) {
                    datasetProcesses.add(CONTROL_FOREIGN_KEY);
                }

                if (analyseFields) {
                    datasetProcesses.add(ANALYSE_FIELD);
                }
                if (controlFields) {
                    datasetProcesses.add(CONTROL_FIELD);
                    datasetProcesses.add(CONTROL_DATA_FORMAT);
                    datasetProcesses.add(CONTROL_CODES);
                }
                if (controlUniqueness) {
                    datasetProcesses.add(CONTROL_UNIQUENESS);
                }

                if (datasetReader.getFlatFileReaders() != null) {
                    for (FlatFileReader flatFileReader : datasetReader.getFlatFileReaders()) {
                        orderCounter = SessionHelper.addFlatFileProcesses(flatFileReader, datasetProcesses,
                                ignoreDatasetDescriptionProcesses, processList, orderCounter, maxNumberOfResults,
                                flatFileDatabase);
                    }
                }

            } catch (InvalidStructureException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        // Data objects
        if (dataset.getDataObjects() != null) {
            canRead = createDataObjectReaders();
        }

        if (canRead) {
            // Read listeners
            if (listenerList != null && !(listenerList.isEmpty())) {
                for (ReadListener readListener : listenerList) {
                    datasetReader.addReadListener(readListener);
                }
            }
            datasetReader.addReadListener(this);
        }

        return canRead;
    }

    private boolean createDataObjectReaders() {
        // Only one occurrence of DataObject is allowed under the highest 
        // DataObjects.
        // (The type property of DataObject in the first level of DataObject 
        // children determines the type of reader.)

        // Øverste nivå av documentDataObject'er bestemmer leseren
        // Forutsetter kun ett toppnivå med XML (eller Noark 5) som type
        // dataObjects - dataObject(name) - properties - property(info) -
        // properties - property(type)
        DataObjects dataObjects = datasetReader.getDataset().getDataObjects();

        if (dataObjects.getDataObject() == null) {
            logger.severe("Øverste forekomst av dataObjects mangler forekomst av dataObject.");
            return false;
        } else if (dataObjects.getDataObject().size() > 1) {
            logger.severe("Øverste forekomst av dataObjects kan kun inneholde en forekomst av dataObject.");
            return false;
        }
        DataObject dataObject = dataObjects.getDataObject().get(0);

        String collectionType = null;
        String typeVersion = null;
        try {
            Property infoProperty = Addml_8_2_Helper.getProperty(dataObject, "info");
            Property typeProperty = Addml_8_2_Helper.getProperty(infoProperty, "type");
            collectionType = typeProperty.getValue();
            typeVersion = Addml_8_2_Helper.getPropertyValue(typeProperty, "version");
        } catch (IllegalArgumentException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException ex) {
            logger.severe(ex.getMessage());
        }

        if (collectionType == null) {
            collectionType = DATA_OBJECT_UNKNOWN_TYPE;
        }

        boolean legalType = false;
        switch (collectionType) {
            case DATA_OBJECT_XML_TYPE:
                legalType = true;
                break;
            case DATA_OBJECT_NOARK_5_TYPE:
                // Only Noark 5 version 3.1
                if (typeVersion != null
                        && typeVersion.equals(DATA_OBJECT_NOARK_5_VERSION_3_1)) {
                    legalType = true;
                }
                break;
        }

        if (legalType) {
            createXmlDataObjectReaders(dataObject, collectionType, typeVersion);
        } else {
            logger.severe("Kan ikke prosessere følgende type arkivuttrekk: "
                    + collectionType + (typeVersion != null ? (" versjon " + typeVersion) : ""));
            return false;
        }

        return true;
    }

    private void createXmlDataObjectReaders(DataObject dataObject, String collectionType, String typeVersion) {
        existDatabase = new ExistDatabase(id, outputDirectory, null);
        existDatabase.init();
        boolean isNewDatabase = existDatabase.isNewDatabase();
        org.xmldb.api.base.Collection metadataCollection = null;
        rootCollection = existDatabase.connect();

        if (isNewDatabase) {
            existDatabase.storeCommonFunctions();

            try {
                metadataCollection = ExistUtils.createCollection(rootCollection, "addml");
                ExistUtils.storeDocument(new File(datasetDescription.getFile().getParentFile(), "addml.xsd"), metadataCollection);
                ExistUtils.storeDocument(datasetDescription.getFile(), metadataCollection);
            } catch (XMLDBException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        createXmlCollectionReader(dataObject, collectionType, typeVersion, metadataCollection, isNewDatabase);
    }

    private void createXmlCollectionReader(DataObject collectionDataObject,
            String collectionType, String typeVersion,
            org.xmldb.api.base.Collection metadataCollection,
            boolean isNewDatabase) {

        XmlCollectionReader xmlCollectionReader
                = new XmlCollectionReader(
                        collectionDataObject.getName(),
                        collectionDataObject, datasetDescription,
                        rootCollection, collectionType, isNewDatabase);
        xmlCollectionReader.init();

        datasetReader.addXmlCollectionReader(xmlCollectionReader);

        xmlCollectionReader.addReadListener(this);

        // Registrere prosesser som omfatter flere XML-dokumenter eller som ikke
        // berører noen XML-dokumenter
        // Processes
        switch (collectionType) {
            case DATA_OBJECT_XML_TYPE:
                break;
            case DATA_OBJECT_NOARK_5_TYPE:
                if (DATA_OBJECT_NOARK_5_VERSION_3_1.equals(typeVersion)) {

                    existDatabase.addIndexFile(collectionDataObject.getName(),
                            "collection.xconf",
                            "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/collection.xconf");
                    Noark_5_v_3_1_ProcessHelper.addCollectionProcesses(
                            xmlCollectionReader, metadataCollection,
                            processList, maxNumberOfResults,
                            dataDirectory, "dokumenter",
                            id);
                    createXmlDocumentReaders(xmlCollectionReader, metadataCollection,
                            collectionDataObject.getProcesses(),
                            collectionType, typeVersion);
                }
                break;
            default:
                logger.severe("Kan ikke bestemme samlingstypen!");
                break;
        }
    }

    /**
     * Creates one XmlDocumentReader for each dataObject (XML Document) in the
     * XmlCollectionReader's dataObjects. The created XmlDocumentReaders are
     * added to the XmlCollectionReader.
     *
     * @param xmlCollectionReader
     * @param metadataCollection
     * @param collectionProcesses
     * @param collectionType
     */
    private void createXmlDocumentReaders(XmlCollectionReader xmlCollectionReader,
            org.xmldb.api.base.Collection metadataCollection,
            Processes collectionProcesses,
            String collectionType, String typeVersion) {

        String parentID = xmlCollectionReader.getId();
        DatasetDescriptionElement dde
                = xmlCollectionReader.getDatasetDescriptionElement();
        DataObject dataObject = (DataObject) dde.getElement();
        org.xmldb.api.base.Collection collection = xmlCollectionReader.getCollection();

        if (dataObject != null && dataObject.getDataObjects() != null) {

            for (DataObject d : dataObject.getDataObjects().getDataObject()) {
                String elementID = parentID + "/" + d.getName();
                if (debug) {
                    logger.info("DEBUG: " + elementID);
                }

                XmlDocumentReader xmlDocumentReader = new XmlDocumentReader(
                        elementID, d,
                        datasetReader.getDatasetDescription(), dataDirectory, collection, true);
                xmlDocumentReader.init();

                xmlCollectionReader.addXmlDocumentReader(xmlDocumentReader);

                xmlDocumentReader.addReadListener(this);

                switch (collectionType) {
                    case DATA_OBJECT_XML_TYPE:
                        break;
                    case DATA_OBJECT_NOARK_5_TYPE:
                        if (DATA_OBJECT_NOARK_5_VERSION_3_1.equals(typeVersion)) {
                            Noark_5_v_3_1_ProcessHelper.addXmlDocumentProcesses(
                                    xmlDocumentReader, metadataCollection, collection,
                                    processList, maxNumberOfResults,
                                    dataDirectory, "dokumenter",
                                    id);
                        }
                        break;
                    default:
                        // Error if collectionType is missing or unknown!
                        logger.severe("Kan ikke bestemme samlingstypen!");
                        break;
                }
            }
        }
    }
    
    @Override
    public void itemRead(ReadEvent readEvent) {
    }
}
