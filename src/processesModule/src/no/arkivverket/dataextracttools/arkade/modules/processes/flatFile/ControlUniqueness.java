/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.sql.ResultSet;
import java.sql.SQLException;
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
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldType;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.NbBundle;

/**
 * A class for controlling a field for duplicate values. Duplicate values are
 * errors. Empty values are not considered part of the controll. The output of
 * the process is a sessionReport activity.
 *
 * @version 0.10 2014-04-10
 * @author Riksarkivet
 *
 */
public class ControlUniqueness extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final FieldDefinition fieldDefinition;
    private String fieldName;
    private boolean unique; // Says the field definition unique values?
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private final FlatFileDatabase database;
    private final String tableName;
    private StringBuilder query;
    private long numberOfEmptyItems;
    private long totalNumberOfErrorValues = 0;
    private long numberOfDifferentErrorValues = 0;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_Uniqueness'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the long name.
     * @param fieldDefinition the field (field definition) to control
     * @param datasetDescription the dataset description describing the field
     * @param database the flat file database
     * @param tableName the table in the flat file database with the field
     */
    public ControlUniqueness(String id, String name, String longName,
            FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription, FlatFileDatabase database, String tableName) {
        super(id, name, longName);
        this.fieldDefinition = fieldDefinition;
        this.datasetDescription = datasetDescription;
        this.database = database;
        this.tableName = tableName;
    }

    @Override
    public void init() {
        super.init();
        unique = (fieldDefinition.getUnique() != null);
        fieldName = fieldDefinition.getName();
        try {
            Addml_8_2 addml_8_2 = (Addml_8_2) datasetDescription;
            fieldType = addml_8_2.getFieldType(fieldDefinition.getTypeReference());
            dataType = fieldType.getDataType();
            if (fieldType.getFieldFormat() != null && !fieldType.getFieldFormat().trim().equals("")) {
                fieldFormat = fieldType.getFieldFormat().trim();
            }
        } catch (NullPointerException ex) {
            log(this.getClass().getSimpleName() + " - init - " + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }

        query = new StringBuilder();
        query.append("SELECT ");
        query.append(fieldName).append(", ");
        query.append("COUNT(*) AS Count ");
        query.append("FROM ").append(tableName).append(" ");
        query.append("GROUP BY ").append(fieldName).append(" ");
        query.append("HAVING COUNT(*) > 1 ");
        query.append("ORDER BY Count DESC");

        if (debug) {
            log("DEBUG: " + query.toString());
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
                NbBundle.getMessage(ControlUniqueness.class, "Control_Uniqueness_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(ControlUniqueness.class, "Field"));
        elementItem.setContent(fieldDefinition.getName());
        descriptionList.getListItem().add(elementItem);
        // fieldDefinition - unique
        ListItem uniqueItem = new ListItem();
        uniqueItem.setLabel(NbBundle.getMessage(ControlUniqueness.class, "UniqueField"));
        uniqueItem.setContent(unique
                ? NbBundle.getMessage(ControlUniqueness.class, "Yes")
                : NbBundle.getMessage(ControlUniqueness.class, "No"));
        descriptionList.getListItem().add(uniqueItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(ControlUniqueness.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(ControlUniqueness.class, "FieldFormat"));
        if (fieldFormat != null) {
            fieldFormatItem.setContent(fieldFormat);
        }
        descriptionList.getListItem().add(fieldFormatItem);

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
            if (resultSet.next()) {
                // Errors
                isEmpty = false;
            }
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
                    NbBundle.getMessage(ControlUniqueness.class, "TotalNumberOfDuplicateValues"));
            totalNumberOfErrorsItem.setContent("" + totalNumberOfErrorValues);
            overallResult.getResultItems().getResultItem().add(totalNumberOfErrorsItem);
            if (numberOfDifferentErrorValues > 0) {
                ResultItem numberOfDifferentErrorValuesItem = new ResultItem();
                numberOfDifferentErrorValuesItem.setType(ResultTypes.ERROR_STRING);
                numberOfDifferentErrorValuesItem.setLabel(
                        NbBundle.getMessage(ControlUniqueness.class, "NumberOfDifferentDuplicateValues"));
                numberOfDifferentErrorValuesItem.setContent("" + numberOfDifferentErrorValues);
                overallResult.getResultItems().getResultItem().add(numberOfDifferentErrorValuesItem);
            }
            // Number of empty items
            ResultItem numberOfEmptyItemsResultItem = new ResultItem();
            numberOfEmptyItemsResultItem.setType(ResultTypes.INFO_STRING);
            numberOfEmptyItemsResultItem.setLabel(
                    NbBundle.getMessage(ControlUniqueness.class, "NumberOfEmptyItems"));
            numberOfEmptyItemsResultItem.setContent("" + numberOfEmptyItems);
            overallResult.getResultItems().getResultItem().add(numberOfEmptyItemsResultItem);

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
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");
        detailedResult.setResultItems(new ResultItems());
        try {
            do {
                String value = resultSet.getString(fieldDefinition.getName());

                // Do not count empty values as errors
                if ("".equals(value)) {
                    numberOfEmptyItems = resultSet.getLong("Count");
                } else {
                    numberOfDifferentErrorValues++;
                    totalNumberOfErrorValues += resultSet.getLong("Count");

                    if (numberOfDifferentErrorValues <= maxNumberOfResults) {
                        ResultItem duplicate = new ResultItem();
                        duplicate.setName("duplicate");
                        duplicate.setType(unique ? ResultTypes.ERROR_STRING : ResultTypes.WARNING_STRING);
                        duplicate.setLabel(value);
                        duplicate.setContent(resultSet.getString("Count"));
                        if (debug) {
                            log("DEBUG: " + fieldDefinition.getName()
                                    + ", Count: " + resultSet.getString("Count"));
                        }
                        detailedResult.getResultItems().getResultItem().add(duplicate);
                    }
                }
            } while (resultSet.next());
            resultSet.close();

            StringBuilder duplicatesLabel = new StringBuilder();
            duplicatesLabel.append(
                    NbBundle.getMessage(ControlUniqueness.class, "Control_Uniqueness_DetailDescription"));
            duplicatesLabel.append(" ");
            if (numberOfDifferentErrorValues <= maxNumberOfResults) {
                duplicatesLabel.append(
                        NbBundle.getMessage(ControlUniqueness.class, "ShowingAll", numberOfDifferentErrorValues));
            } else {
                duplicatesLabel.append(
                        NbBundle.getMessage(ControlUniqueness.class, "ShowingXofY", maxNumberOfResults, numberOfDifferentErrorValues));
            }
            detailedResult.setLabel(duplicatesLabel.toString());
        } catch (SQLException ex) {
            log(this.getClass().getSimpleName() + " - getDetailedResult - "
                    + ex.getClass().getSimpleName()
                    + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
        }

        return numberOfDifferentErrorValues > 0 ? detailedResult : null;
    }
}
