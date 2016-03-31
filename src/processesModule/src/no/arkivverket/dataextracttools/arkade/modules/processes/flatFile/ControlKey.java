/**
 * The National Archives of Norway - 2014
 *
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
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Key;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinitions;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.NbBundle;

/**
 * A class for controlling a primary or alternate key in a record
 * definition/table. Duplicate values are errors. Duplicate empty values are
 * also errors. The output of the process is a sessionReport activity.
 *
 * @version 0.10 2014-04-10
 * @author Riksarkivet
 *
 */
public class ControlKey extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final Key key;
    private String flatFileDefinitionName;
    private final RecordDefinition recordDefinition;
    private ArrayList<String> keyFieldNames;
    private final FlatFileDatabase database;
    private StringBuilder tableName;
    private String query;
    private long totalNumberOfErrorValues = 0;
    private long numberOfDifferentErrorValues = 0;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_Key'
     * @param longName a long name for the process
     * @param key the key definition
     * @param recordDefinition the record definition with the key definition
     * @param datasetDescription the dataset description describing the field
     * @param database the flat file database
     */
    public ControlKey(String id, String name, String longName,
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
            query = getQuery();
        } catch (NullPointerException ex) {
            log(this.getClass().getSimpleName() + " - init - " + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }
        activity.setDescription(getActivityInfo());
    }

    @Override
    public void finish() {
        if (!finished) {
            finished = true;

            Result result = getResult();
            activity.setResult(result);
            XMLGregorianCalendar timeStamp;
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
                NbBundle.getMessage(ControlForeignKey.class, "Control_Key_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // Key name
        ListItem keyNameListItem = new ListItem();
        keyNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "KeyName"));
        keyNameListItem.setContent(key.getName());
        descriptionList.getListItem().add(keyNameListItem);
        // Flat file definition name
        ListItem flatFileDefinitionNameListItem = new ListItem();
        flatFileDefinitionNameListItem.setLabel(
                NbBundle.getMessage(ControlForeignKey.class, "FlatFileDefinitionName"));
        flatFileDefinitionNameListItem.setContent(flatFileDefinitionName);
        descriptionList.getListItem().add(flatFileDefinitionNameListItem);
        // Record definition name
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
        return activityInfo;
    }

    /**
     *
     * @return the result of the activity
     */
    private Result getResult() {
        boolean isEmpty = true;
        ResultItem detailedResult = null;
        Result result = new Result();

        ResultSet resultSet = database.runQuery(query.toString());

        try {
            String resultType = ResultTypes.INFO_STRING;

            if (resultSet.next()) {
                // Errors
                isEmpty = false;
                resultType = ResultTypes.ERROR_STRING;
            }

            result.setType(resultType);
            result.setResultItems(new ResultItems());

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
            totalNumberOfErrorsItem.setType((totalNumberOfErrorValues == 0)
                    ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
            totalNumberOfErrorsItem.setLabel(
                    NbBundle.getMessage(ControlKey.class, "TotalNumberOfErrorValues"));
            totalNumberOfErrorsItem.setContent("" + totalNumberOfErrorValues);
            overallResult.getResultItems().getResultItem().add(totalNumberOfErrorsItem);
            if (numberOfDifferentErrorValues > 0) {
                ResultItem numberOfDifferentErrorValuesItem = new ResultItem();
                numberOfDifferentErrorValuesItem.setType(ResultTypes.ERROR_STRING);
                numberOfDifferentErrorValuesItem.setLabel(
                        NbBundle.getMessage(ControlKey.class, "NumberOfDifferentErrorValues"));
                numberOfDifferentErrorValuesItem.setContent("" + numberOfDifferentErrorValues);
                overallResult.getResultItems().getResultItem().add(numberOfDifferentErrorValuesItem);
            }

            result.getResultItems().getResultItem().add(overallResult);
            if (detailedResult != null) {
                result.getResultItems().getResultItem().add(detailedResult);
            }
        } catch (SQLException ex) {
            log(this.getClass().getSimpleName() + " - getResult - " + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }
        return result;
    }

    private ResultItem getDetailedResult(ResultSet resultSet) {
//        long resultCounter = 0;
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");
        detailedResult.setResultItems(new ResultItems());

//        ResultItem keyErrors = new ResultItem();
//        keyErrors.setName("keyErrors");
//        keyErrors.setType(ResultTypes.ERROR_STRING);
//        keyErrors.setResultItems(new ResultItems());
//        detailedResult.getResultItems().getResultItem().add(keyErrors);
        try {
            do {
//                resultCounter++;
                numberOfDifferentErrorValues++;
                totalNumberOfErrorValues += resultSet.getLong("Count");

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
            label.append(
                    NbBundle.getMessage(ControlKey.class, "Control_Key_DetailDescription"));
            label.append(" ");
            if (numberOfDifferentErrorValues <= maxNumberOfResults) {
                label.append(
                        NbBundle.getMessage(ControlKey.class, "ShowingAll", numberOfDifferentErrorValues));
            } else {
                label.append(
                        NbBundle.getMessage(ControlKey.class, "ShowingXofY", maxNumberOfResults, numberOfDifferentErrorValues));
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
            querySB.append(keyFieldNames.get(i));
            querySB.append(", ");
        }
        querySB.append("COUNT(*) AS Count ");
        querySB.append("FROM ").append(tableName).append(" ");
        querySB.append("GROUP BY ");
        for (int i = 0; i < numberOfFieldNames; i++) {
            querySB.append(keyFieldNames.get(i));
            if (i < (numberOfFieldNames - 1)) {
                querySB.append(",");
            }
            querySB.append(" ");
        }
        querySB.append("HAVING COUNT(*) > 1 ");
        querySB.append("ORDER BY Count DESC");

        if (debug) {
            log("DEBUG: " + querySB.toString());
        }
        return querySB.toString();
    }
}
