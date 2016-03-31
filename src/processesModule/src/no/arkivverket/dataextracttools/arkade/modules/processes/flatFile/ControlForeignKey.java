/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import no.arkivverket.dataextracttools.arkade.modules.flatfiledatabase.FlatFileDatabase;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.reports.ResultTypes;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Description;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.DescriptionList;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ListItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Result;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItems;
import no.arkivverket.dataextracttools.metadatastandards.addml.BasicElement;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFileDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.ForeignKey;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Key;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinitionReference;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinitions;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @version 0.11 2014-04-10
 * @author Riksarkivet
 */
public class ControlForeignKey extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final Key key;
    private String flatFileDefinitionName;
    private final RecordDefinition recordDefinition;
    private ArrayList<String> keyFieldNames;
    private ForeignKey foreignKey;
    private FlatFileDefinitionReference foreignFlatFileDefinitionReference;
    private RecordDefinitionReference foreignRecordDefinitionReference;
    private ArrayList<String> foreignKeyFieldNames;
    private StringBuilder tableName;
    private StringBuilder foreignTableName;
    private final FlatFileDatabase database;
    private String query;
    private long totalNumberOfErrors = 0;
    private long numberOfDifferentErrorValues = 0;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_ForeignKey'
     * @param longName a long name for the process
     * @param key the key definition
     * @param recordDefinition the record definition with the foreign key definition
     * @param datasetDescription the dataset description describing the field
     * @param database the flat file database
     */
    public ControlForeignKey(String id, String name, String longName,
            Key key, RecordDefinition recordDefinition,
            DatasetDescription datasetDescription,
            FlatFileDatabase database) {
        super(id, name, longName);
        this.key = key;
        this.recordDefinition = recordDefinition;
        this.datasetDescription = datasetDescription;
        this.database = database;
    }

    @Override
    public void init() {
        super.init();
        if (recordDefinition.getParent() instanceof RecordDefinitions) {
            BasicElement recordDefinitions = recordDefinition.getParent();
            if (recordDefinitions.getParent() instanceof FlatFileDefinition) {
                flatFileDefinitionName
                        = ((FlatFileDefinition) recordDefinitions.getParent()).getName();
            }
        }

        try {
            tableName = new StringBuilder();
            tableName.append(flatFileDefinitionName).append("_").append(recordDefinition.getName());
            ArrayList<FieldDefinitionReference> fieldDefinitionReferences
                    = (ArrayList) key.getFieldDefinitionReferences().getFieldDefinitionReference();
            keyFieldNames = new ArrayList<>();
            for (int i = 0; i < fieldDefinitionReferences.size(); i++) {
                keyFieldNames.add(fieldDefinitionReferences.get(i).getName());
            }

            // Foreign
            foreignKey = key.getForeignKey();
            foreignFlatFileDefinitionReference = foreignKey.getFlatFileDefinitionReference();
            foreignRecordDefinitionReference
                    = foreignFlatFileDefinitionReference.getRecordDefinitionReferences().getRecordDefinitionReference().get(0);
            foreignTableName = new StringBuilder();
            foreignTableName.append(foreignFlatFileDefinitionReference.getName());
            foreignTableName.append("_");
            foreignTableName.append(foreignRecordDefinitionReference.getName());
            ArrayList<FieldDefinitionReference> foreignFieldDefinitionReferences
                    = (ArrayList) foreignRecordDefinitionReference.getFieldDefinitionReferences().getFieldDefinitionReference();
            foreignKeyFieldNames = new ArrayList<>();
            for (int i = 0; i < foreignFieldDefinitionReferences.size(); i++) {
                foreignKeyFieldNames.add(foreignFieldDefinitionReferences.get(i).getName());
            }

            query = getQuery();
        } catch (NullPointerException ex) {
            log(this.getClass().getSimpleName() + " - init - " + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }
        activity.setDescription(getActivityInfo());
    }

    /**
     * Executes the process when finish() is called. The query is run against
     * the internal database and the result is registered in ControlForeignKey's
     * Activity-object.
     */
    @Override
    public void finish() {
        if (!finished) {
            finished = true;
            XMLGregorianCalendar timeStamp;
            try {
                timeStamp = XMLUtils.createTimeStamp();
                activity.setTimeStarted(timeStamp);
            } catch (DatatypeConfigurationException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }

            Result result = getResult();
            activity.setResult(result);
            try {
                timeStamp = XMLUtils.createTimeStamp();
                activity.setTimeEnded(timeStamp);
            } catch (DatatypeConfigurationException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }
        }
    }

    @Override
    public void itemRead(ReadEvent readEvent) {
    }

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(ControlForeignKey.class, "Control_ForeignKey_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // keyName
        ListItem keyNameListItem = new ListItem();
        keyNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "KeyName"));
        keyNameListItem.setContent(key.getName());
        descriptionList.getListItem().add(keyNameListItem);
        // flatFileDefinitionName
        ListItem flatFileDefinitionNameListItem = new ListItem();
        flatFileDefinitionNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "FlatFileDefinitionName"));
        flatFileDefinitionNameListItem.setContent(flatFileDefinitionName);
        descriptionList.getListItem().add(flatFileDefinitionNameListItem);
        // recordDefinitionName
        ListItem recordDefinitionNameListItem = new ListItem();
        recordDefinitionNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "RecordDefinitionName"));
        recordDefinitionNameListItem.setContent(recordDefinition.getName());
        descriptionList.getListItem().add(recordDefinitionNameListItem);
        for (int i = 0; i < keyFieldNames.size(); i++) {
            ListItem fieldNameListItem = new ListItem();
            fieldNameListItem.setLabel(
                    NbBundle.getMessage(ControlForeignKey.class, "KeyFieldName"));
            fieldNameListItem.setContent(keyFieldNames.get(i));
            descriptionList.getListItem().add(fieldNameListItem);
        }

        // Foreign key
        // Foreign flat file definition name
        ListItem foreignFlatFileDefinitionNameListItem = new ListItem();
        foreignFlatFileDefinitionNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "ForeignKeyFlatFileDefinitionName"));
        foreignFlatFileDefinitionNameListItem.setContent(foreignFlatFileDefinitionReference.getName());
        descriptionList.getListItem().add(foreignFlatFileDefinitionNameListItem);
        // Record definition name
        ListItem foreignRecordDefinitionNameListItem = new ListItem();
        foreignRecordDefinitionNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "ForeignKeyRecordDefinitionName"));
        foreignRecordDefinitionNameListItem.setContent(foreignRecordDefinitionReference.getName());
        descriptionList.getListItem().add(foreignRecordDefinitionNameListItem);
        for (int i = 0; i < foreignKeyFieldNames.size(); i++) {
            ListItem foreignFieldNameListItem = new ListItem();
            foreignFieldNameListItem.setLabel(
                    NbBundle.getMessage(ControlForeignKey.class, "ForeignKeyFieldName"));
            foreignFieldNameListItem.setContent(foreignKeyFieldNames.get(i));
            descriptionList.getListItem().add(foreignFieldNameListItem);
        }

        return activityInfo;
    }

    private Result getResult() {
        boolean isEmpty = true;
        ResultItem detailedResult = null;
        Result result = new Result();
        String resultType = ResultTypes.INFO_STRING;

        if (database.hasIndex(foreignTableName.toString(), foreignKeyFieldNames)) {
            if (!isDistinct()) {
                log(longName + " - Indeksen i " + foreignTableName + " er ikke entydig.", Level.SEVERE);
                resultType = ResultTypes.ERROR_STRING;
                Description notDistinctDescription = new Description();
                JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                        "Indeksen i " + foreignTableName + " er ikke entydig.");
                notDistinctDescription.getContent().add(textBlock);
                result.setDescription(notDistinctDescription);
            }
        } else {
            // No index! Abort.
            log(longName + " - Indeks mangler i " + foreignTableName, Level.SEVERE);
            log("Prosessen avbrytes.", Level.SEVERE);
            result.setType(ResultTypes.ERROR_STRING);
            return result;
        }

        ResultSet resultSet;

        if (database.hasIndex(tableName.toString(), keyFieldNames)) {
            resultSet = database.runQuery(query.toString());
        } else {
            // No index! Abort.
            log(longName + " - Indeks mangler i " + tableName, Level.SEVERE);
            log("Prosessen avbrytes.", Level.SEVERE);
            result.setType(ResultTypes.ERROR_STRING);
            return result;
        }

        try {
            if (resultSet.next()) {
                // Errors
                isEmpty = false;
                resultType = ResultTypes.ERROR_STRING;
            }

            result.setType(resultType);
            ResultItems mainResultItems = new ResultItems();
            result.setResultItems(mainResultItems);

            // Detailed result
            if (!isEmpty) {
                detailedResult = getDetailedResult(resultSet);
            }

            // Overall result
            ResultItem overallResult = new ResultItem();
            overallResult.setName("overallResult");
            overallResult.setResultItems(new ResultItems());
            ResultItem totalNumberOfErrorsItem = new ResultItem();
            totalNumberOfErrorsItem.setType((totalNumberOfErrors == 0)
                    ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
            totalNumberOfErrorsItem.setLabel(
                    NbBundle.getMessage(ControlForeignKey.class, "TotalNumberOfErrors"));
            totalNumberOfErrorsItem.setContent("" + totalNumberOfErrors);
            overallResult.getResultItems().getResultItem().add(totalNumberOfErrorsItem);
            if (numberOfDifferentErrorValues > 0) {
                ResultItem numberOfDifferentErrorValuesItem = new ResultItem();
                numberOfDifferentErrorValuesItem.setType(ResultTypes.ERROR_STRING);
                numberOfDifferentErrorValuesItem.setLabel(
                        NbBundle.getMessage(ControlForeignKey.class, "NumberOfDifferentErrorValues"));
                numberOfDifferentErrorValuesItem.setContent("" + numberOfDifferentErrorValues);
                overallResult.getResultItems().getResultItem().add(numberOfDifferentErrorValuesItem);
            }

            result.getResultItems().getResultItem().add(overallResult);
            if (detailedResult != null) {
                result.getResultItems().getResultItem().add(detailedResult);
            }

        } catch (SQLException ex) {
            log(ex.getMessage(), Level.SEVERE);
        }
        return result;
    }

    private ResultItem getDetailedResult(ResultSet resultSet) {
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");
        detailedResult.setResultItems(new ResultItems());

        try {
            do {
                numberOfDifferentErrorValues++;
                totalNumberOfErrors += resultSet.getLong("Count");

                if (numberOfDifferentErrorValues <= maxNumberOfResults) {
                    ResultItem keyError = new ResultItem();
                    keyError.setName("keyError");
                    keyError.setType(ResultTypes.ERROR_STRING);
                    keyError.setResultItems(new ResultItems());
                    ResultItem fieldValues = new ResultItem();
                    fieldValues.setName("fieldValues");
                    fieldValues.setResultItems(new ResultItems());
                    for (int i = 0; i < keyFieldNames.size(); i++) {
                        ResultItem fieldValue = new ResultItem();
                        fieldValue.setLabel(keyFieldNames.get(i));
                        fieldValue.setContent(resultSet.getString(keyFieldNames.get(i)));
                        fieldValue.setType(ResultTypes.INFO_STRING);
                        fieldValues.getResultItems().getResultItem().add(fieldValue);
                        if (debug) {
                            log("DEBUG: " + keyFieldNames.get(i)
                                    + ": " + resultSet.getString(keyFieldNames.get(i)));
                        }
                    }
                    keyError.getResultItems().getResultItem().add(fieldValues);
                    ResultItem numberOfOccurrences = new ResultItem();
                    numberOfOccurrences.setName("numberOfOccurrences");
                    numberOfOccurrences.setContent(resultSet.getString("Count"));
                    keyError.getResultItems().getResultItem().add(numberOfOccurrences);

                    detailedResult.getResultItems().getResultItem().add(keyError);
                    if (debug) {
                        log("DEBUG: Antall: " + resultSet.getString("Count"));
                    }
                }
            } while (resultSet.next());
            resultSet.close();

            StringBuilder label = new StringBuilder();
            label.append("Forskjellige verdier i nøkkelen ").append(key.getName()).append(" i ");
            label.append(flatFileDefinitionName).append(":");
            label.append(recordDefinition.getName());
            label.append(" som ikke finnes i ");
            label.append(foreignFlatFileDefinitionReference.getName()).append(":");
            label.append(foreignRecordDefinitionReference.getName()).append(". ");
            if (numberOfDifferentErrorValues <= maxNumberOfResults) {
                label.append(NbBundle.getMessage(ControlForeignKey.class, "ShowingAll", numberOfDifferentErrorValues));
            } else {
                label.append(
                        NbBundle.getMessage(ControlForeignKey.class, "ShowingXofY", maxNumberOfResults, numberOfDifferentErrorValues));
            }
            detailedResult.setLabel(label.toString());
        } catch (SQLException ex) {
            log(this.getClass().getSimpleName() + " - getDetailedResult - "
                    + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }
        return detailedResult;
    }

    private String getQuery() {
        StringBuilder querySB = new StringBuilder();
        int numberOfFieldNames = keyFieldNames.size();

        querySB.append("SELECT ");
        for (int i = 0; i < numberOfFieldNames; i++) {
            querySB.append("a.").append(keyFieldNames.get(i));
            querySB.append(", ");
        }
        querySB.append("COUNT(*) AS Count ");
        querySB.append("\nFROM ").append(tableName.toString()).append(" a ");
        querySB.append("LEFT JOIN ").append(foreignTableName.toString()).append(" b ");
        querySB.append("ON ");
        for (int i = 0; i < numberOfFieldNames; i++) {
            querySB.append("a.").append(keyFieldNames.get(i));
            querySB.append(" = ");
            querySB.append("b.").append(foreignKeyFieldNames.get(i));
            if (i < (numberOfFieldNames - 1)) {
                querySB.append(" AND ");
            }
        }
        querySB.append(" WHERE ");
        for (int i = 0; i < foreignKeyFieldNames.size(); i++) {
            querySB.append("b.").append(foreignKeyFieldNames.get(i)).append(" IS NULL ");
            if (i < (foreignKeyFieldNames.size() - 1)) {
                querySB.append("AND ");
            }
        }
        querySB.append("GROUP BY ");
        for (int i = 0; i < numberOfFieldNames; i++) {
            querySB.append("a.").append(keyFieldNames.get(i));
            if (i < (numberOfFieldNames - 1)) {
                querySB.append(",");
            }
            querySB.append(" ");
        }
        querySB.append("ORDER BY Count DESC");

        if (debug) {
            log("DEBUG: " + querySB.toString());
        }

        return querySB.toString();
    }

    private boolean isDistinct() {
        int numberOfForeignFieldNames = foreignKeyFieldNames.size();

        StringBuilder distinctQuery = new StringBuilder();
        distinctQuery.append("SELECT ");
        for (int i = 0; i < numberOfForeignFieldNames; i++) {
            distinctQuery.append(foreignKeyFieldNames.get(i)).append(", ");
        }
        distinctQuery.append("COUNT(*) AS Count ");
        distinctQuery.append("FROM ").append(foreignTableName).append(" ");
        distinctQuery.append("GROUP BY ");
        for (int i = 0; i < numberOfForeignFieldNames; i++) {
            distinctQuery.append(foreignKeyFieldNames.get(i));
            if (i < (numberOfForeignFieldNames - 1)) {
                distinctQuery.append(",");
            }
            distinctQuery.append(" ");
        }
        distinctQuery.append("HAVING COUNT(*) > 1 ");
        distinctQuery.append("ORDER BY Count DESC");

        ResultSet resultSet = database.runQuery(distinctQuery.toString());

        if (debug) {
            log("DEBUG: " + distinctQuery.toString());
        }

        try {
            return !resultSet.next();
        } catch (SQLException ex) {
            log(ex.getMessage(), Level.SEVERE);
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
}
