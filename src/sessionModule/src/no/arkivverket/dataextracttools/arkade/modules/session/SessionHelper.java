/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.session;

import java.util.ArrayList;
import java.util.Map;
import no.arkivverket.dataextracttools.arkade.modules.flatfiledatabase.FlatFileDatabase;
import no.arkivverket.dataextracttools.arkade.modules.processes.DataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.AnalyseCountRecords;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.AnalyseField;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.AnalyseFrequencyList;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlChecksum;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlCodes;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlDataFormat;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlField;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlForeignKey;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlKey;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlNumberOfRecords;
import no.arkivverket.dataextracttools.arkade.modules.processes.flatFile.ControlUniqueness;
import no.arkivverket.dataextracttools.arkade.modules.readers.DatasetReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.DelimitedFileReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.DelimitedRecordReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FieldReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FixedFileReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FixedRecordReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FlatFileReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.RecordReader;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Dataset;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DelimFileFormat;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldProcesses;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldType;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FixedFileFormat;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileProcesses;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileType;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFiles;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Key;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Processes;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordProcesses;
import org.openide.util.NbBundle;

/**
 * Helper class for various session functionality.
 *
 * @version 0.12 2014-04-10
 * @author Riksarkivet
 */
public class SessionHelper {

    public static int ORDER_KEY_SIZE = 8;

    /**
     * 
     * @param datasetReader
     * @param listenerList
     * @param delimitedFilesHaveHeader
     * @throws InvalidStructureException 
     */
    public static void createFlatFileReaders(DatasetReader datasetReader, ArrayList<ReadListener> listenerList,
            boolean delimitedFilesHaveHeader) throws InvalidStructureException {
        Dataset dataset = datasetReader.getDataset();
        FlatFiles flatFiles = dataset.getFlatFiles();

        for (FlatFile flatFile : flatFiles.getFlatFile()) {
            FlatFileReader flatFileReader = createFlatFileReader(datasetReader, flatFile, listenerList, delimitedFilesHaveHeader);
            if (flatFileReader != null) {
                datasetReader.addFlatFileReader(flatFileReader);
            }
        }
    }

    private static FlatFileReader createFlatFileReader(DatasetReader datasetReader,
            FlatFile flatFile, ArrayList<ReadListener> listenerList, boolean delimitedFilesHaveHeader)
            throws InvalidStructureException {
        FlatFileReader flatFileReader = null;
        DatasetDescription datasetDescription = datasetReader.getDatasetDescription();

        String flatFileDefinitionReference
                = flatFile.getDefinitionReference();
        StringBuilder elementId = new StringBuilder();
        elementId.append(datasetReader.getId()).append(":").append(flatFile.getName());
        elementId.append(":").append(flatFileDefinitionReference);

        FlatFileDefinition flatFileDefinition
                = ((Addml_8_2) datasetDescription).getFlatFileDefinition(flatFileDefinitionReference);
        String flatFileTypeReference
                = flatFileDefinition.getTypeReference();
        FlatFileType flatFileType
                = ((Addml_8_2) datasetDescription).getFlatFileType(flatFileTypeReference);

        String charset = flatFileType.getCharset();

        Object format = null;

        if (flatFileType.getFixedFileFormat() != null) {
            format = flatFileType.getFixedFileFormat();
        } else if (flatFileType.getDelimFileFormat() != null) {
            format = flatFileType.getDelimFileFormat();
        } else {
            throw new InvalidStructureException(elementId.toString() + ": File format missing");
        }

        // Finne feltnummer for postdef hvis det er flere enn
        // en postdefinisjon.
        int recordDefinitionFieldNumber = -1;
        ArrayList<RecordDefinition> recordDefinitionList = null;

        if (flatFileDefinition.getRecordDefinitions() != null
                && flatFileDefinition.getRecordDefinitions().
                getRecordDefinition() != null) {
            recordDefinitionList
                    = (ArrayList<RecordDefinition>) flatFileDefinition.getRecordDefinitions().getRecordDefinition();
        }

        if (recordDefinitionList != null && recordDefinitionList.size() > 1) {
            String recordDefinitionFieldIdentifier
                    = flatFileDefinition.getRecordDefinitionFieldIdentifier();

            // Indeksen til feltdefinisjonen
            // Starter på 1 i ADDML
            int identifier = -1;

            String tempIdentifier = null;

            try {
                // Indeksen til feltdefinisjonen?
                identifier = Integer.valueOf(recordDefinitionFieldIdentifier);
                tempIdentifier = "" + identifier;
            } catch (NumberFormatException e) {
                // Indeksen er navnet på feltdefinisjonen
                // Ikke gjøre noe her.
            }

            if (recordDefinitionFieldIdentifier.equals(tempIdentifier)) {
                // Field number identifier
                recordDefinitionFieldNumber = identifier - 1;
            } else {
                // Field name identifier

                // Forutsetter at alle postdefinisjonene har samme navn
                // på feltet som identifiserer postdef'en.
                ArrayList<FieldDefinition> fieldDefinitionList
                        = ((Addml_8_2) datasetDescription).getFieldDefinitionList(recordDefinitionList.get(0));

                for (int i = 0; i < fieldDefinitionList.size(); i++) {

                    if (fieldDefinitionList.get(i).getName().
                            equals(recordDefinitionFieldIdentifier)) {
                        recordDefinitionFieldNumber = i;
                        break;
                    }
                }
            }
        }

        if (flatFileType.getFixedFileFormat() != null) {
            // Fixed Format
            flatFileReader = new FixedFileReader(elementId.toString(),
                    flatFile, datasetDescription,
                    charset, (FixedFileFormat) format, recordDefinitionFieldNumber,
                    datasetReader.getDataDirectory());
        } else if (flatFileType.getDelimFileFormat() != null) {
            // Delimeted Format
            flatFileReader = new DelimitedFileReader(elementId.toString(),
                    flatFile, datasetDescription,
                    charset, (DelimFileFormat) format, recordDefinitionFieldNumber,
                    datasetReader.getDataDirectory(), delimitedFilesHaveHeader);
        }

        if (flatFileReader != null) {
            flatFileReader.setLoggerName(datasetReader.getLoggerName());
            flatFileReader.init();
            if (flatFileReader instanceof FixedFileReader
                    && ((FixedFileReader) flatFileReader).getRecordLength() == -1) {
                // Can't read without the record length.
                throw new InvalidStructureException("Kan ikke opprette leser for fast format-filen  '" + 
                        flatFile.getName() + "'\nPostlengden er ikke oppgitt.");
            }
            // Add other read listeners than processes
            if (listenerList != null && !(listenerList.isEmpty())) {
                for (ReadListener readListener : listenerList) {
                    flatFileReader.addReadListener(readListener);
                }
            }
            createRecordReaders(flatFileReader, listenerList);
        } else {
            throw new InvalidStructureException("Unknown file format");
        }

        return flatFileReader;
    }

    private static void createRecordReaders(FlatFileReader flatFileReader,
            ArrayList<ReadListener> listenerList) throws InvalidStructureException {
        FlatFile flatFile = flatFileReader.getFlatFile();

        ArrayList<RecordDefinition> recordDefinitionList
                = ((Addml_8_2) flatFileReader.getDatasetDescription()).getRecordDefinitionList(flatFile);

        for (RecordDefinition recordDefinition : recordDefinitionList) {
            String recordDefinitionFieldValue = recordDefinition.getRecordDefinitionFieldValue();
            RecordReader recordReader = createRecordReader(flatFileReader, recordDefinition, listenerList);
            if (recordReader != null) {
                flatFileReader.addRecordReader(
                        recordDefinitionFieldValue, recordReader);
            }
        }
    }

    private static RecordReader createRecordReader(FlatFileReader flatFileReader,
            RecordDefinition recordDefinition, ArrayList<ReadListener> listenerList)
            throws InvalidStructureException {
        String elementId = flatFileReader.getId() + ":" + recordDefinition.getName();
        Object format = flatFileReader.getFormat();
        RecordReader recordReader = null;

        if (format instanceof FixedFileFormat) {
            recordReader = new FixedRecordReader(elementId,
                    recordDefinition, flatFileReader.getDatasetDescription());
        } else if (format instanceof DelimFileFormat) {
            recordReader = new DelimitedRecordReader(elementId,
                    recordDefinition, flatFileReader.getDatasetDescription(),
                    (DelimFileFormat) format);
        }

        if (recordReader != null) {
            recordReader.setLoggerName(flatFileReader.getLoggerName());
            recordReader.init();
            // Add other read listeners than processes
            if (listenerList != null && !(listenerList.isEmpty())) {
                for (ReadListener readListener : listenerList) {
                    recordReader.addReadListener(readListener);
                }
            }

            createFieldReaders(recordReader, listenerList);
        } else {
            throw new InvalidStructureException("Unknown file format");
        }

        return recordReader;
    }

    private static void createFieldReaders(RecordReader recordReader,
            ArrayList<ReadListener> listenerList) throws InvalidStructureException {
        if (recordReader.getDatasetDescription() == null || recordReader.getElement() == null) {
            throw new InvalidStructureException();
        }
        RecordDefinition recordDefinition = (RecordDefinition) recordReader.getElement();
        if (recordDefinition.getFieldDefinitions() == null) {
            throw new InvalidStructureException();
        }
        ArrayList<FieldDefinition> fieldDefinitionList
                = (ArrayList) recordDefinition.getFieldDefinitions().getFieldDefinition();

        for (FieldDefinition fieldDefinition : fieldDefinitionList) {
            FieldReader fieldReader
                    = createFieldReader(recordReader, fieldDefinition, listenerList);
            if (fieldReader != null) {
                recordReader.addFieldReader(fieldReader);
            }
        }
    }

    private static FieldReader createFieldReader(RecordReader recordReader,
            FieldDefinition fieldDefinition,
            ArrayList<ReadListener> listenerList) throws InvalidStructureException {
        String elementId = recordReader.getId() + ":" + fieldDefinition.getName();
        FieldType fieldType = ((Addml_8_2) recordReader.getDatasetDescription()).getFieldType(fieldDefinition);
        FieldReader fieldReader = new FieldReader(elementId,
                fieldDefinition, recordReader.getDatasetDescription(), fieldType);
        fieldReader.setLoggerName(recordReader.getLoggerName());
        fieldReader.init();
        // Add other read listeners than processes
        if (listenerList != null && !(listenerList.isEmpty())) {
            for (ReadListener readListener : listenerList) {
                fieldReader.addReadListener(readListener);
            }
        }
        return fieldReader;
    }

    /**
     *
     * @param flatFileReader
     * @param datasetProcesses
     * @param ignoreDatasetDescriptionProcesses
     * @param processList
     * @param orderCounter
     * @param maxNumberOfResults
     * @param flatFileDatabase
     * @return the updated order counter
     * @throws InvalidStructureException
     */
    public static int addFlatFileProcesses(FlatFileReader flatFileReader, ArrayList<String> datasetProcesses,
            boolean ignoreDatasetDescriptionProcesses, ArrayList<DataExtractProcess> processList,
            int orderCounter, int maxNumberOfResults, FlatFileDatabase flatFileDatabase)
            throws InvalidStructureException {
        int processCounter = orderCounter;
        Addml_8_2 addml_8_2 = (Addml_8_2) flatFileReader.getDatasetDescription();
        FlatFiles flatFiles = addml_8_2.getFlatFiles();
        FlatFile flatFile = flatFileReader.getFlatFile();
        FlatFileProcesses flatFileProcesses
                = ((Addml_8_2) flatFileReader.getDatasetDescription()).getFlatFileProcesses(flatFiles, flatFile.getName());
        Processes processes = flatFileProcesses != null ? flatFileProcesses.getProcesses() : null;

        // Analyse_CountRecords
        if (datasetProcesses.contains(Session.ANALYSE_COUNT_RECORDS)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.ANALYSE_COUNT_RECORDS))) {
            String id = flatFileReader.getId() + ":" + Session.ANALYSE_COUNT_RECORDS;
            String longName = NbBundle.getMessage(SessionHelper.class, "Analyse_CountRecords_Long_Name");
            AnalyseCountRecords analyseCountRecords
                    = new AnalyseCountRecords(id, Session.ANALYSE_COUNT_RECORDS, longName,
                            flatFile, addml_8_2);
            analyseCountRecords.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            analyseCountRecords.setStructuralId(flatFileReader.getId());
            analyseCountRecords.setLoggerName(flatFileReader.getLoggerName());
            analyseCountRecords.init();
            // Add the process as a read listener on the flatFileReader and the flatFile's recordReaders
            flatFileReader.addReadListener(analyseCountRecords);
            if (flatFileReader.getRecordReaders() != null) {
                for (Map.Entry<String, RecordReader> entry : flatFileReader.getRecordReaders().entrySet()) {
                    entry.getValue().addReadListener(analyseCountRecords);
                }
            }
            processList.add(analyseCountRecords);
        }

        // Control_NumberOfRecords
        if (datasetProcesses.contains(Session.CONTROL_NUMBER_OF_RECORDS)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.CONTROL_NUMBER_OF_RECORDS))) {
            String id = flatFileReader.getId() + ":" + Session.CONTROL_NUMBER_OF_RECORDS;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_NumberOfRecords_Long_Name");
            ControlNumberOfRecords controlNumberOfRecords
                    = new ControlNumberOfRecords(id, Session.CONTROL_NUMBER_OF_RECORDS, longName,
                            flatFile, addml_8_2);
            controlNumberOfRecords.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlNumberOfRecords.setStructuralId(flatFileReader.getId());
            controlNumberOfRecords.setLoggerName(flatFileReader.getLoggerName());
            controlNumberOfRecords.init();
            // Add the process as a read listener on the flatFileReader and the flatFile's recordReaders
            flatFileReader.addReadListener(controlNumberOfRecords);
            if (flatFileReader.getRecordReaders() != null) {
                for (Map.Entry<String, RecordReader> entry : flatFileReader.getRecordReaders().entrySet()) {
                    entry.getValue().addReadListener(controlNumberOfRecords);
                }
            }
            processList.add(controlNumberOfRecords);
        }

        // Control_Checksum
        if (datasetProcesses.contains(Session.CONTROL_CHECKSUM)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.CONTROL_CHECKSUM))) {
            String id = flatFileReader.getId() + ":" + Session.CONTROL_CHECKSUM;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_Checksum_Long_Name");
            ControlChecksum controlChecksum
                    = new ControlChecksum(id, Session.CONTROL_CHECKSUM, longName,
                            flatFile, addml_8_2, flatFileReader.getDataDirectory());
            controlChecksum.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlChecksum.setStructuralId(flatFileReader.getId());
            controlChecksum.setLoggerName(flatFileReader.getLoggerName());
            controlChecksum.init();
            // Add the process as a read listener on the flatFileReader
            flatFileReader.addReadListener(controlChecksum);
            processList.add(controlChecksum);
        }

        if (flatFileReader.getRecordReaders() != null) {
            for (Map.Entry<String, RecordReader> entry : flatFileReader.getRecordReaders().entrySet()) {
                String recordDefinitionName = entry.getValue().getRecordDefinition().getName();
                RecordProcesses recordProcesses = null;
                if (flatFileProcesses != null) {
                    recordProcesses
                            = addml_8_2.getRecordProcesses(flatFileProcesses, recordDefinitionName);
                }
                processCounter = addRecordProcesses(entry.getValue(), flatFileReader.getFlatFile(),
                        datasetProcesses,
                        ignoreDatasetDescriptionProcesses, recordProcesses,
                        processList, processCounter, maxNumberOfResults, flatFileDatabase);
            }
        }

        return processCounter;
    }

    /**
     *
     * @param recordReader
     * @param flatFile provides easier access to the dataset description
     * hierarchy
     * @param datasetProcesses
     * @param ignoreDatasetDescriptionProcesses
     * @param recordProcesses
     * @param processList
     * @param orderCounter
     * @param maxNumberOfResults
     * @param flatFileDatabase
     * @return the updated order counter
     * @throws
     * no.arkivverket.dataextracttools.arkade.modules.session.InvalidStructureException
     */
    public static int addRecordProcesses(RecordReader recordReader, FlatFile flatFile, ArrayList<String> datasetProcesses,
            boolean ignoreDatasetDescriptionProcesses, RecordProcesses recordProcesses,
            ArrayList<DataExtractProcess> processList, int orderCounter, int maxNumberOfResults,
            FlatFileDatabase flatFileDatabase) throws InvalidStructureException {
        int processCounter = orderCounter;
        Addml_8_2 addml_8_2 = (Addml_8_2) recordReader.getDatasetDescription();
        RecordDefinition recordDefinition = recordReader.getRecordDefinition();
        Processes processes = recordProcesses != null ? recordProcesses.getProcesses() : null;

        // Control_Key
        // Can perform key control if the record definition has key definitions 
        // and key control is wanted for every key in the dataset or 
        // for the keys in this record definition. 
        boolean canControlKey = (recordDefinition.getKeys() != null)
                && (datasetProcesses.contains(Session.CONTROL_KEY)
                || (!ignoreDatasetDescriptionProcesses
                && addml_8_2.hasProcess(processes, Session.CONTROL_KEY)));

        // It is an error if the record definition does not have any keys when 
        // the key control is explicitly wanted.
        boolean controlKeyError = (recordDefinition.getKeys() != null)
                && (!ignoreDatasetDescriptionProcesses
                && addml_8_2.hasProcess(processes, Session.CONTROL_KEY));

        if (controlKeyError) {
            throw new InvalidStructureException("Nøkkelkontroll er angitt, men nøkkeldefinisjon mangler.");
        } else if (canControlKey) {
            // Add process for each key (not foreign key)
            for (Key key : recordDefinition.getKeys().getKey()) {
                if (key.getForeignKey() == null) {
                    String id = recordReader.getId() + ":" + Session.CONTROL_KEY + ":" + key.getName();
                    String longName = NbBundle.getMessage(SessionHelper.class, "Control_Key_Long_Name")
                            + (key.getName() != null ? " - " + key.getName() : "");
                    ControlKey controlKey
                            = new ControlKey(id, Session.CONTROL_KEY, longName,
                                    key, recordDefinition, addml_8_2, flatFileDatabase);
                    controlKey.setOrderKey(SessionHelper.createOrderKey(++processCounter));
                    controlKey.setStructuralId(recordReader.getId());
                    controlKey.setLoggerName(recordReader.getLoggerName());
                    controlKey.init();
                    controlKey.setMaxNumberOfResults(maxNumberOfResults);
                    // Legge til prosess som leserlytter
                    recordReader.addReadListener(controlKey);
                    processList.add(controlKey);
                }
            }
        }

        // Control_ForeignKey
        // Can perform foreign key control if the record definition has 
        // key definitions and key control is wanted for every foreign key 
        // in the dataset or for the foreign keys in this record definition. 
        boolean canControlForeignKey = (recordDefinition.getKeys() != null)
                && (datasetProcesses.contains(Session.CONTROL_FOREIGN_KEY)
                || (!ignoreDatasetDescriptionProcesses
                && addml_8_2.hasProcess(processes, Session.CONTROL_FOREIGN_KEY)));

        // It is an error if the record definition does not have any keys when 
        // the foreign key control is explicitly wanted.
        boolean controlForeignKeyError = (recordDefinition.getKeys() != null)
                && (!ignoreDatasetDescriptionProcesses
                && addml_8_2.hasProcess(processes, Session.CONTROL_FOREIGN_KEY));

        if (controlForeignKeyError) {
            throw new InvalidStructureException("Fremmednøkkelkontroll er angitt, men nøkkeldefinisjon mangler.");
        } else if (canControlForeignKey) {
            // Add process for each foreign key
            for (Key key : recordDefinition.getKeys().getKey()) {
                if (key.getForeignKey() != null) {
                    String id = recordReader.getId() + ":" + Session.CONTROL_FOREIGN_KEY + ":" + key.getName();
                    String longName = NbBundle.getMessage(SessionHelper.class, "Control_ForeignKey_Long_Name")
                            + (key.getName() != null ? " - " + key.getName() : "");
                    ControlForeignKey controlForeignKey
                            = new ControlForeignKey(id, Session.CONTROL_FOREIGN_KEY, longName,
                                    key, recordDefinition, addml_8_2, flatFileDatabase);
                    controlForeignKey.setOrderKey(SessionHelper.createOrderKey(++processCounter));
                    controlForeignKey.setStructuralId(recordReader.getId());
                    controlForeignKey.setLoggerName(recordReader.getLoggerName());
                    controlForeignKey.init();
                    controlForeignKey.setMaxNumberOfResults(maxNumberOfResults);
                    // Legge til prosess som leserlytter
                    recordReader.addReadListener(controlForeignKey);
                    processList.add(controlForeignKey);
                }
            }
        }

        for (FieldReader fieldReader : recordReader.getFieldReaders()) {
            FieldProcesses fieldProcesses = null;
            if (recordProcesses != null) {
                fieldProcesses = addml_8_2.getFieldProcesses(recordProcesses, fieldReader.getFieldDefinition().getName());
            }
            processCounter = addFieldProcesses(fieldReader, recordReader.getRecordDefinition(), flatFile,
                    datasetProcesses, ignoreDatasetDescriptionProcesses,
                    fieldProcesses, processList, processCounter, maxNumberOfResults, flatFileDatabase);
        }

        return processCounter;
    }

    /**
     *
     * @param fieldReader
     * @param recordDefinition provides easier access to the dataset description
     * hierarchy
     * @param flatFile provides easier access to the dataset description
     * hierarchy
     * @param datasetProcesses
     * @param ignoreDatasetDescriptionProcesses
     * @param fieldProcesses
     * @param processList
     * @param orderCounter
     * @param maxNumberOfResults
     * @param flatFileDatabase
     * @return the updated order counter
     */
    public static int addFieldProcesses(FieldReader fieldReader, RecordDefinition recordDefinition,
            FlatFile flatFile, ArrayList<String> datasetProcesses,
            boolean ignoreDatasetDescriptionProcesses,
            FieldProcesses fieldProcesses, ArrayList<DataExtractProcess> processList,
            int orderCounter, int maxNumberOfResults, FlatFileDatabase flatFileDatabase) {
        int processCounter = orderCounter;
        Addml_8_2 addml_8_2 = (Addml_8_2) fieldReader.getDatasetDescription();
        FieldDefinition fieldDefinition = fieldReader.getFieldDefinition();
        Processes processes = fieldProcesses != null ? fieldProcesses.getProcesses() : null;
        boolean unique = (fieldDefinition.getUnique() != null);
        boolean hasCodes = (fieldDefinition.getCodes() != null
                && !fieldDefinition.getCodes().getCode().isEmpty());

        // Analyse_Field
        if (datasetProcesses.contains(Session.ANALYSE_FIELD)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.ANALYSE_FIELD))) {
            String id = fieldReader.getId() + ":" + Session.ANALYSE_FIELD;
            String longName = NbBundle.getMessage(SessionHelper.class, "Analyse_Field_Long_Name");
            AnalyseField analyseField
                    = new AnalyseField(id, Session.ANALYSE_FIELD, longName,
                            fieldDefinition, addml_8_2);
            analyseField.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            analyseField.setStructuralId(fieldReader.getId());
            analyseField.setLoggerName(fieldReader.getLoggerName());
            analyseField.init();
            // Legge til prosess som leselytter
            fieldReader.addReadListener(analyseField);
            processList.add(analyseField);
        }

        // Analyse_FrequencyList
        if (datasetProcesses.contains(Session.ANALYSE_FREQUENCY_LIST)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.ANALYSE_FREQUENCY_LIST))) {
            String id = fieldReader.getId() + ":" + Session.ANALYSE_FREQUENCY_LIST;
            String longName = NbBundle.getMessage(SessionHelper.class, "Analyse_FrequencyList_Long_Name");
            AnalyseFrequencyList analyseFrequencyList
                    = new AnalyseFrequencyList(id, Session.ANALYSE_FREQUENCY_LIST, longName,
                            fieldDefinition, addml_8_2);
            analyseFrequencyList.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            analyseFrequencyList.setStructuralId(fieldReader.getId());
            analyseFrequencyList.setLoggerName(fieldReader.getLoggerName());
            analyseFrequencyList.init();
            analyseFrequencyList.setMaxNumberOfResults(maxNumberOfResults);
            // Legge til prosess som leselytter
            fieldReader.addReadListener(analyseFrequencyList);
            processList.add(analyseFrequencyList);
        }

        // Control_Field
        if (datasetProcesses.contains(Session.CONTROL_FIELD)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.CONTROL_FIELD))) {
            String id = fieldReader.getId() + ":" + Session.CONTROL_FIELD;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_Field_Long_Name");
            ControlField controlField
                    = new ControlField(id, Session.CONTROL_FIELD, longName,
                            fieldDefinition, addml_8_2);
            controlField.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlField.setStructuralId(fieldReader.getId());
            controlField.setLoggerName(fieldReader.getLoggerName());
            controlField.init();
            controlField.setMaxNumberOfResults(maxNumberOfResults);
            // Legge til prosess som leselytter
            fieldReader.addReadListener(controlField);
            processList.add(controlField);
        }

        // Control_DataFormat
        if (datasetProcesses.contains(Session.CONTROL_DATA_FORMAT)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.CONTROL_DATA_FORMAT))) {
            String id = fieldReader.getId() + ":" + Session.CONTROL_DATA_FORMAT;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_DataFormat_Long_Name");
            ControlDataFormat controlDataFormat
                    = new ControlDataFormat(id, Session.CONTROL_DATA_FORMAT, longName,
                            fieldDefinition, addml_8_2);
            controlDataFormat.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlDataFormat.setStructuralId(fieldReader.getId());
            controlDataFormat.setLoggerName(fieldReader.getLoggerName());
            controlDataFormat.init();
            controlDataFormat.setMaxNumberOfResults(maxNumberOfResults);
            // Legge til prosess som leselytter
            fieldReader.addReadListener(controlDataFormat);
            processList.add(controlDataFormat);
        }

        // Control_Codes
        if (hasCodes && datasetProcesses.contains(Session.CONTROL_CODES)
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(
                        processes, Session.CONTROL_CODES))) {
            String id = fieldReader.getId() + ":" + Session.CONTROL_CODES;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_Codes_Long_Name");
            ControlCodes controlCodes
                    = new ControlCodes(id, Session.CONTROL_CODES, longName,
                            fieldDefinition, addml_8_2);
            controlCodes.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlCodes.setStructuralId(fieldReader.getId());
            controlCodes.setLoggerName(fieldReader.getLoggerName());
            controlCodes.init();
            controlCodes.setMaxNumberOfResults(maxNumberOfResults);
            // Legge til prosess som leselytter
            fieldReader.addReadListener(controlCodes);
            processList.add(controlCodes);
        }

        // Control_Uniqueness
        // Only process mandatory fields or explictly added process. 
        if ((unique && datasetProcesses.contains(Session.CONTROL_UNIQUENESS))
                || (!ignoreDatasetDescriptionProcesses && addml_8_2.hasProcess(processes, Session.CONTROL_UNIQUENESS))) {
            String id = fieldReader.getId() + ":" + Session.CONTROL_UNIQUENESS;
            String longName = NbBundle.getMessage(SessionHelper.class, "Control_Uniqueness_Long_Name");
            String tableName = flatFile.getDefinitionReference() + "_" + recordDefinition.getName();
            ControlUniqueness controlUniqueness
                    = new ControlUniqueness(id, Session.CONTROL_UNIQUENESS, longName,
                            fieldDefinition, addml_8_2, flatFileDatabase, tableName);
            controlUniqueness.setOrderKey(SessionHelper.createOrderKey(++processCounter));
            controlUniqueness.setStructuralId(fieldReader.getId());
            controlUniqueness.setLoggerName(fieldReader.getLoggerName());
            controlUniqueness.init();
            controlUniqueness.setMaxNumberOfResults(maxNumberOfResults);
            // Legge til prosess som leselytter
            fieldReader.addReadListener(controlUniqueness);
            processList.add(controlUniqueness);
        }

        return processCounter;
    }

    public static String createOrderKey(int number) {
        String numberString = String.valueOf(number);
        int numberOfZeroes = ORDER_KEY_SIZE - numberString.length();
        String zeroes = null;
        if (numberOfZeroes > 0) {
            zeroes = new String(new char[numberOfZeroes]).replace('\0', '0');
        }
        return zeroes != null ? zeroes + numberString : numberString;
    }

}
