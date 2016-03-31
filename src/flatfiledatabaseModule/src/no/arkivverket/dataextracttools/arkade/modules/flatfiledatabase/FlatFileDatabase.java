/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.flatfiledatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.SessionReport;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Activity;
import no.arkivverket.dataextracttools.metadatastandards.addml.BasicElement;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Dataset;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinitions;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinitions;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Key;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Keys;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinitions;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.Exceptions;

/**
 * A class for creating, indexing and quering a JavaDB-database with the
 * contents of a dataset consisting of flat files described in a dataset
 * description following ADDML 8.2. The flat file database has access to the
 * dataset description. The database-object can create indexes based on
 * key-definitions in the dataset description and on field definitions for
 * mandatory fields. The field values are trimmed before they are stored.
 *
 * @version 0.12 2014-04-10
 * @author Riksarkivet
 */
public class FlatFileDatabase implements ReadListener {

    private final static int SHOW_INSERTS_INTERVAL = 50000;
    private final String protocol = "jdbc:derby:";
    private final String sessionId;
    private final Addml_8_2 datasetDescription;
    private final Logger logger;
    private boolean debug = false;
    private SessionReport sessionReport;
    private final Activity readActivity = new Activity();
    private final File sessionDirectory;
    private File databaseDirectory;
    private final String databaseName;
    private final boolean create;
    private boolean canRead = true;
    private final int numberOfInsertsBeforeCommit;
    private Connection connection;
    private boolean datasetStart = true;
    private boolean flatFileStart = true;
    private boolean recordStart = true;
    private StringBuilder insertBuilder;
    private Properties fieldValues;
    private String currentTableName;
    private String oldTableName;
    private int recordCount;
    private PreparedStatement insertStatement;

    /**
     *
     * @param sessionId
     * @param datasetDescription
     * @param sessionDirectory
     * @param databaseName
     * @param create
     * @param numberOfInsertsBeforeCommit
     */
    public FlatFileDatabase(String sessionId, Addml_8_2 datasetDescription, File sessionDirectory,
            String databaseName, boolean create, int numberOfInsertsBeforeCommit) {
        this.sessionId = sessionId;
        this.datasetDescription = datasetDescription;
        this.sessionDirectory = sessionDirectory;
        this.databaseName = databaseName;
        this.create = create;
        this.numberOfInsertsBeforeCommit = numberOfInsertsBeforeCommit;
        logger = Logger.getLogger(sessionId);
        debug = Boolean.parseBoolean(System.getProperty("arkade.session.debug", "false"));
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionReport(SessionReport sessionReport) {
        this.sessionReport = sessionReport;
    }

    /**
     * Connects to the database. It is created first if it does not exist.
     */
    public void connect() {
        System.setProperty("derby.storage.pageReservedSpace", "0");
        System.setProperty("derby.storage.pageSize", "16384");
        databaseDirectory = new File(sessionDirectory.getPath(), databaseName);
        System.setProperty("derby.system.home", sessionDirectory.getPath());
        if (debug) {
            // Log the query plan
            System.setProperty("derby.language.logQueryPlan", "true");
        }
        StringBuilder connectionUrl = new StringBuilder();
        connectionUrl.append(protocol).append(databaseDirectory.getPath());
        if (create) {
            logger.info("Oppretter database for flate filer: " + databaseName);
            connectionUrl.append(";create=true");
        } else {
            logger.info("Bruker eksisterende database for flate filer: " + databaseName);
        }

        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
            connection = DriverManager.getConnection(connectionUrl.toString());
            connection.setAutoCommit(false);
            recordCount = 0;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            Exceptions.printStackTrace(ex);
        }

    }

    public void read() {
        if (canRead) {
            canRead = false;
            if (connection != null && create) {
                createTables();
            }
        }
    }

    public boolean shutdown() {
        boolean gotSQLExc = false;
        try {
            if (connection != null) {
                if (!connection.getAutoCommit()) {
                    connection.commit();
                    connection.close();
                    connection = null;
                }
            }
            DriverManager.getConnection("jdbc:derby:" + ";shutdown=true");
        } catch (SQLException se) {
            if (se.getSQLState().equalsIgnoreCase("XJ015")) {
                gotSQLExc = true;
            }
            logger.info(se.getMessage() + ": " + se.getSQLState() + " (" + se.getErrorCode() + ")");
            Date end = new Date();
            logger.info(end.toString());
        }
        if (!gotSQLExc) {
            logger.severe("Databasen ble ikke lukket normalt.");
        }

        return gotSQLExc;
    }

    public ResultSet runQuery(String query) {
        ResultSet res = null;
        try {
            Statement sta = connection.createStatement();
            res = sta.executeQuery(query);
        } catch (SQLException ex) {
            logger.severe(ex.getMessage() + " - " + ex.getSQLState());
            Exceptions.printStackTrace(ex);
        }
        return res;

    }

    private void createTables() {
        // One-to-one relationship between flatFile and flatFileDefinition!
        FlatFileDefinitions flatFileDefinitions
                = datasetDescription.getDataset().getFlatFiles().getFlatFileDefinitions();
        for (FlatFileDefinition flatFileDefinition : flatFileDefinitions.getFlatFileDefinition()) {
            RecordDefinitions recordDefinitions = flatFileDefinition.getRecordDefinitions();
            for (RecordDefinition recordDefinition : recordDefinitions.getRecordDefinition()) {

                StringBuilder statementBuilder = new StringBuilder();
                statementBuilder.append("CREATE TABLE ").append(flatFileDefinition.getName()).
                        append("_").append(recordDefinition.getName());
                statementBuilder.append(" ( ");
                ArrayList<FieldDefinition> fieldDefinitionList
                        = (ArrayList<FieldDefinition>) recordDefinition.getFieldDefinitions().getFieldDefinition();
                for (int i = 0; i < fieldDefinitionList.size(); i++) {
                    FieldDefinition fieldDefinition = fieldDefinitionList.get(i);
                    statementBuilder.append(fieldDefinition.getName());
                    statementBuilder.append(" VARCHAR(");
                    if (fieldDefinition.getFixedLength() != null) {
                        statementBuilder.append(fieldDefinition.getFixedLength());
                    } else {
                        if (fieldDefinition.getEndPos() != null && fieldDefinition.getStartPos() != null) {
                            int endPos = fieldDefinition.getEndPos().intValue();
                            int startPos = fieldDefinition.getStartPos().intValue();
                            int fieldLength = endPos - startPos + 1;
                            statementBuilder.append(fieldLength);
                        } else {
                            logger.warning(fieldDefinition.getName() + ": Bruker maks feltlengde på 255");
                            statementBuilder.append("255");
                        }
                    }
                    statementBuilder.append(")");
                    if (i < (fieldDefinitionList.size() - 1)) {
                        statementBuilder.append(", ");
                    }
                }
                statementBuilder.append(" )");
                Statement statement;
                try {
                    statement = connection.createStatement();
                    statement.execute(statementBuilder.toString());
                    statement.close();
                    logger.info(statementBuilder.toString());
                } catch (SQLException e) {
                    logger.severe(e.getMessage());
                }
            }
        }
    }

    /**
     * Adds the contents of the dataset to the database. Gets noticed when read
     * in dataset, flat file, record and field.
     *
     * @param readEvent
     */
    @Override
    public void itemRead(ReadEvent readEvent) {
        BasicReader reader = (BasicReader) readEvent.getReader();
        BasicElement element = reader.getElement();

        if (element instanceof Dataset) {
            readDataset();
        } else if (element instanceof FlatFile) {
            readFlatFile(element);
        } else if (element instanceof RecordDefinition) {
            readRecord(element);
        } else if (element instanceof FieldDefinition) {
            readField(element, readEvent);
        }

    }

    private void readDataset() {

        if (datasetStart) {
            logger.info("Starter innlesing av datasett til database.");
            readActivity.setName("insertFlatFilesToDatabase");
            try {
                readActivity.setTimeStarted(XMLUtils.createTimeStamp());
            } catch (DatatypeConfigurationException ex) {
                logger.severe(ex.getMessage());
                Exceptions.printStackTrace(ex);
            }
            datasetStart = false;
        } else {
            logger.info("Ferdig med innlesing av datasett til database.");
            if (sessionReport != null) {
                try {
                    readActivity.setTimeEnded(XMLUtils.createTimeStamp());
                } catch (DatatypeConfigurationException ex) {
                    logger.severe(ex.getMessage());
                    Exceptions.printStackTrace(ex);
                }
                sessionReport.write(readActivity);
            }
        }
    }

    private void readFlatFile(BasicElement element) {
        if (flatFileStart) {
            flatFileStart = false;

            recordCount = 0;

            logger.info(((FlatFile) element).getName() + " - Startet innlesing til database");

        } else {
            try {
                connection.commit();
                if (insertStatement != null) {
                    insertStatement.close();
                    insertStatement = null;
                }
            } catch (SQLException ex) {
                logger.severe(ex.getMessage());
                Exceptions.printStackTrace(ex);
            }
            flatFileStart = true;
            logger.info(((FlatFile) element).getName() + " - Innlesing til database ferdig");
        }

    }

    private void readRecord(BasicElement element) {
        // Starter eller avslutter lesing av en post

        RecordDefinition recordDefinition = (RecordDefinition) element;
        if (recordStart) {
            // Starter lesing av en post
            recordStart = false;

            FlatFileDefinition flatFileDefinition = (FlatFileDefinition) recordDefinition.getParent().getParent();
            StringBuilder tableName = new StringBuilder();
            tableName.append(flatFileDefinition.getName()).append("_").append(recordDefinition.getName());

            if (currentTableName == null
                    || !(currentTableName.equals(tableName.toString()))) {
                // Første eller en annen tabell enn gjeldende
                oldTableName = currentTableName;
                currentTableName = tableName.toString();

                if (insertStatement != null) {
                    try {
                        insertStatement.close();
                    } catch (SQLException ex) {
                        logger.severe(ex.getMessage());
                        Exceptions.printStackTrace(ex);
                    }
                    insertStatement = null;
                }

                // Klargjør liste over felt og feltverdier i aktuell tabell
                fieldValues = new Properties();
                FieldDefinitions fieldDefinitions = recordDefinition.getFieldDefinitions();
                for (FieldDefinition fieldDefinition : fieldDefinitions.getFieldDefinition()) {
                    fieldValues.setProperty(fieldDefinition.getName(), "");
                }
            } else {
                // Nullstiller feltverdiene
                for (String fieldName : fieldValues.stringPropertyNames()) {
                    fieldValues.setProperty(fieldName, "");
                }
            }

            insertBuilder = new StringBuilder();
            insertBuilder.append("INSERT INTO ");
            insertBuilder.append(tableName.toString());
        } else {
            // Ferdig med lesing av posten
            recordStart = true;

            if (!currentTableName.equals(oldTableName)) {
                StringBuilder fields = new StringBuilder();
                fields.append(" (");
                StringBuilder values = new StringBuilder();
                values.append(" (");

                int numberOfFields = fieldValues.size();
                int i = 0;
                for (String keyName : fieldValues.stringPropertyNames()) {
                    fields.append(keyName);
                    values.append("?");
                    i++;
                    if (i < numberOfFields) {
                        fields.append(", ");
                        values.append(", ");
                    }
                }
                fields.append(") ");
                values.append(") ");
                insertBuilder.append(fields);
                insertBuilder.append("VALUES");
                insertBuilder.append(values);
                try {
                    insertStatement = connection.prepareStatement(insertBuilder.toString());
                } catch (SQLException ex) {
                    logger.severe(ex.getMessage());
                    Exceptions.printStackTrace(ex);
                }
            }

            if (insertStatement != null) {
                int i = 1;
                for (String fieldName : fieldValues.stringPropertyNames()) {
                    try {
                        // TODO Temp workaround for lange verdier
                        String fieldValue = fieldValues.getProperty(fieldName);
                        if (fieldValue != null && fieldValue.length() > 255) {
                            logger.warning("Kun de 255 første tegnene i " + fieldValue
                                    + " blir lagret i databasen!");
                            fieldValue = fieldValue.substring(0, 255);
                        }

                        insertStatement.setString(i, fieldValue);
                    } catch (SQLException ex) {
                        logger.severe(ex.getMessage());
                        Exceptions.printStackTrace(ex);
                    }

                    i++;
                }
                try {
                    insertStatement.executeUpdate();
                    insertStatement.clearParameters();
                    recordCount++;
                    if (recordCount % SHOW_INSERTS_INTERVAL == 0) {
                        logger.info("Poster lest inn i database: " + recordCount);
                    }

                    if (recordCount % numberOfInsertsBeforeCommit == 0) {
                        connection.commit();
                    }
                } catch (SQLException ex) {
                    logger.severe(ex.getMessage());
                    Exceptions.printStackTrace(ex);
                }
            }
        }

    }

    private void readField(BasicElement element, ReadEvent readEvent) {
        FieldDefinition fieldDefinition = (FieldDefinition) element;
        String fieldName = fieldDefinition.getName();
        String fieldValue = readEvent.getItem().getValue().toString().trim();
        fieldValues.setProperty(fieldName, fieldValue);
    }

    public void createIndexesFromKeys() {
        logger.info("Oppretter indekser i databasen");

        FlatFileDefinitions flatFileDefinitions = datasetDescription.getFlatFileDefinitions();
        for (FlatFileDefinition flatFileDefinition : flatFileDefinitions.getFlatFileDefinition()) {
            String flatFileDefinitionName = flatFileDefinition.getName();
            RecordDefinitions recordDefinitions = flatFileDefinition.getRecordDefinitions();
            for (RecordDefinition recordDefinition : recordDefinitions.getRecordDefinition()) {
                Keys rDKeys = recordDefinition.getKeys();
                if (rDKeys != null) {
                    StringBuilder tableName = new StringBuilder();
                    tableName.append(flatFileDefinitionName).append("_").append(recordDefinition.getName());
                    for (Key key : rDKeys.getKey()) {
                        String indexName = "i" + UUID.randomUUID().toString().replace('-', '_');
                        logger.info("Oppretter indeks: " + indexName + (key.getName() != null ?  " (" + key.getName() + ")" : ""));
                        List<String> fieldNames = new ArrayList();
                        for (FieldDefinitionReference fieldDefinitionReference
                                : key.getFieldDefinitionReferences().getFieldDefinitionReference()) {
                            fieldNames.add(fieldDefinitionReference.getName());
                        }
                        if (!hasIndex(tableName.toString(), fieldNames)) {
                            createIndex(indexName, tableName.toString(), fieldNames);
                        }

                        // Foreign key
                        if (key.getForeignKey() != null) {
                            try {
                                FlatFileDefinitionReference flatFileDefinitionReference = key.getForeignKey().getFlatFileDefinitionReference();
                                RecordDefinitionReference recordDefinitionReference = flatFileDefinitionReference.getRecordDefinitionReferences().getRecordDefinitionReference().get(0);
                                StringBuilder foreignTableName = new StringBuilder();
                                foreignTableName.append(flatFileDefinitionReference.getName()).append("_").append(recordDefinitionReference.getName());

                                List<String> foreignFieldNames = new ArrayList();
                                for (FieldDefinitionReference fieldDefinitionReference
                                        : recordDefinitionReference.getFieldDefinitionReferences().getFieldDefinitionReference()) {
                                    foreignFieldNames.add(fieldDefinitionReference.getName());
                                }
                                if (!hasIndex(foreignTableName.toString(), foreignFieldNames)) {
                                    String foreignIndexName = "i" + UUID.randomUUID().toString().replace('-', '_');
                                    createIndex(foreignIndexName, foreignTableName.toString(), foreignFieldNames);
                                }

                            } catch (NullPointerException ex) {
                                logger.severe("Feil i fremmednøkkeldefinisjonen: " + key.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param ignoreDatasetDescriptionProcesses
     */
    public void createIndexesFromFields(boolean ignoreDatasetDescriptionProcesses) {
        // One-to-one relationship between flatFile and flatFileDefinition!

        // Index on fields with notNull (mandatory fields)
        FlatFileDefinitions flatFileDefinitions
                = datasetDescription.getDataset().getFlatFiles().getFlatFileDefinitions();
        for (FlatFileDefinition flatFileDefinition : flatFileDefinitions.getFlatFileDefinition()) {
            RecordDefinitions recordDefinitions = flatFileDefinition.getRecordDefinitions();
            for (RecordDefinition recordDefinition : recordDefinitions.getRecordDefinition()) {
                StringBuilder tableName = new StringBuilder();
                tableName.append(flatFileDefinition.getName()).append("_").append(recordDefinition.getName());
                ArrayList<FieldDefinition> fieldDefinitionList
                        = (ArrayList<FieldDefinition>) recordDefinition.getFieldDefinitions().getFieldDefinition();
                for (int i = 0; i < fieldDefinitionList.size(); i++) {
                    FieldDefinition fieldDefinition = fieldDefinitionList.get(i);
                    if (fieldDefinition.getNotNull() != null) {
                        // Create index
                        String indexName = "i" + UUID.randomUUID().toString().replace('-', '_');
                        List<String> fieldNames = new ArrayList();
                        fieldNames.add(fieldDefinition.getName());
                        createIndex(indexName, tableName.toString(), fieldNames);
                    }
                }
            }
        }
    }

    /**
     * 
     * @param indexName
     * @param tableName
     * @param fieldNames 
     */
    public void createIndex(String indexName, String tableName, List<String> fieldNames) {
        StringBuilder index = new StringBuilder();
        index.append("CREATE INDEX ");
        index.append(indexName);
        index.append(" ON ");
        index.append(tableName);
        index.append(" (");
        for (int i = 0; i < fieldNames.size(); i++) {
            index.append(fieldNames.get(i));
            if (i < (fieldNames.size() - 1)) {
                index.append(", ");
            }
        }
        index.append(")");
        logger.info(index.toString());
        try {
            Statement indexStatement = connection.createStatement();
            indexStatement.execute(index.toString());
            indexStatement.close();
            logger.info("Indeks opprettet");
        } catch (SQLException ex) {
            logger.severe(ex.getMessage());
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * 
     * @param tableName
     * @param fieldNames
     * @return 
     */
    public boolean hasIndex(String tableName, List<String> fieldNames) {
        TreeMap<String, List<String>> indexes = getIndexes(tableName);

        for (String indexName : indexes.keySet()) {
            boolean hasIndex = true;
            // Order matters
            List<String> columnNames = indexes.get(indexName);
            if (fieldNames.size() != columnNames.size()) {
                hasIndex = false;
            } else {
                for (int i = 0; i < fieldNames.size(); i++) {
                    if (!fieldNames.get(i).equalsIgnoreCase(columnNames.get(i))) {
                        hasIndex = false;
                    }
                    break;
                }
            }
            if (hasIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param tableName
     * @return 
     */
    public TreeMap<String, List<String>> getIndexes(String tableName) {
        TreeMap<String, List<String>> indexes = new TreeMap();
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet res = meta.getTables(null, null, null,
                    new String[]{"TABLE"});
            while (res.next()) {
                if (res.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                    ResultSet rs = meta.getIndexInfo(null, null,
                            res.getString("TABLE_NAME"), false, false);
                    while (rs.next()) {
                        if (!indexes.containsKey(rs.getString("INDEX_NAME"))) {
                            indexes.put(rs.getString("INDEX_NAME"), new ArrayList());
                        }
                        List<String> columns = indexes.get(rs.getString("INDEX_NAME"));
                        columns.add(rs.getString("COLUMN_NAME"));
                    }
                    break;
                }
            }

        } catch (SQLException ex) {
            logger.severe(ex.getMessage());
            Exceptions.printStackTrace(ex);
        }
        return indexes;
    }

    public void getIndexInfo() {
        TreeMap<String, List<String>> indexes = new TreeMap();
        try {
            DatabaseMetaData meta = connection.getMetaData();

            ResultSet res = meta.getTables(null, null, null,
                    new String[]{"TABLE"});
            while (res.next()) {
                logger.info(res.getString("TABLE_NAME"));
                ResultSet rset = meta.getIndexInfo(null, null,
                        res.getString("TABLE_NAME"), false, false);
                while (rset.next()) {
                    if (!indexes.containsKey(rset.getString("INDEX_NAME"))) {
                        indexes.put(rset.getString("INDEX_NAME"), new ArrayList());
                    }
                    List<String> columns = indexes.get(rset.getString("INDEX_NAME"));
                    columns.add(rset.getString("COLUMN_NAME"));
                }
                rset.close();
            }
            res.close();
        } catch (SQLException ex) {
            logger.severe(ex.getMessage() + " - " + ex.getSQLState());
            Exceptions.printStackTrace(ex);
        }
        if (!indexes.isEmpty()) {
            for (String key : indexes.keySet()) {
                logger.info("Index: " + key);
                List<String> columns = indexes.get(key);
                for (String field : columns) {
                    logger.info("Field: " + field);
                }
            }
        }
    }
}
