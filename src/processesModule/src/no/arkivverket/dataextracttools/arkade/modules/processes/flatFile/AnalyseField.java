/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.reports.ResultTypes;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
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
 * A class for analysing a field in a flat file. 
 * The result contains the total number of values/items read, number of empty fields and the extreme field
 * values (min/max) and extreme field lengths (min/max) in the file. A field/item is considered empty
 * if the length of the value is zero after spaces are removed.
 * The analyse contains the record numbers where the min and max values first were found, 
 * and also the record numbers where the shortest and longest field values first were found. 
 * What is considered the min or max field value, depends on the field's data type.
 * The output of the process is a sessionReport activity.
 *
 * @version 0.07 2014-04-16
 * @author Riksarkivet
 *
 */
public class AnalyseField extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private boolean start = true;
    private long numberOfItemsRead = 0;
    private long numberOfEmptyItems = 0;
    private long numberOfInvalidItems = 0;
    private final FieldDefinition fieldDefinition;
    private String minValue = "";
    private String maxValue = "";
    private int minLength = Integer.MAX_VALUE;
    private int maxLength = 0;
    private long minValueRecordNumber = 0;
    private long maxValueRecordNumber = 0;
    private long minLengthRecordNumber = 0;
    private long maxLengthRecordNumber = 0;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Analyse_Field'
     * @param longName a long name for the process. Can contain a more descriptive name for the process.
     * @param fieldDefinition the field (field definition) to analyse
     * @param datasetDescription the dataset description describing the field
     */
    public AnalyseField(String id, String name, String longName,
            FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription) {
        super(id, name, longName);
        this.fieldDefinition = fieldDefinition;
        this.datasetDescription = datasetDescription;
    }

    @Override
    public void init() {
        super.init();
        try {
            Addml_8_2 addml_8_2 = (Addml_8_2) datasetDescription;
            fieldType = addml_8_2.getFieldType(fieldDefinition.getTypeReference());
            dataType = fieldType.getDataType();
            if (fieldType.getFieldFormat() != null && !fieldType.getFieldFormat().trim().equals("")) {
                fieldFormat = fieldType.getFieldFormat().trim();
            }
        } catch (NullPointerException ex) {
            log(this.getClass().getSimpleName() + " - init - " + ex.getClass().getSimpleName() + 
                    (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);        }
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
        if (start) {
            // Set start timestamp
            start = false;
            XMLGregorianCalendar timeStamp;
            try {
                timeStamp = XMLUtils.createTimeStamp();
                activity.setTimeStarted(timeStamp);
            } catch (DatatypeConfigurationException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }
        }

        String value = null;
        Item item = readEvent.getItem();
        if (item != null && item.getValue() != null) {
            value = ((String) item.getValue()).trim();
        }

        numberOfItemsRead++;

        if (value == null || value.equals("")) {
            numberOfEmptyItems++;
            return;
        }
        if (value.length() < minLength) {
            minLength = value.length();
            minLengthRecordNumber = numberOfItemsRead;
        }
        if (value.length() > maxLength) {
            maxLength = value.length();
            maxLengthRecordNumber = numberOfItemsRead;
        }

        if (dataType != null && !dataType.equals("")) {
            switch (dataType) {
                case DataTypes.STRING:
                case DataTypes.DATE:
                    if (minValue.equals("") || value.compareTo(minValue) < 0) {
                        minValue = value;
                        minValueRecordNumber = numberOfItemsRead;
                   }
                    if (maxValue.equals("") || value.compareTo(maxValue) > 0) {
                        maxValue = value;
                        maxValueRecordNumber = numberOfItemsRead;
                    }
                    break;
                case DataTypes.INTEGER:
                    boolean error = false;
                    int integerValue = 0;
                    try {
                        integerValue = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        error = true;
                        numberOfInvalidItems++;
                    }
                    if (!error) {
                        if (minValue.equals("")
                                || integerValue < Integer.parseInt(minValue)) {
                            minValue = "" + integerValue;
                            minValueRecordNumber = numberOfItemsRead;
                        }
                        if (maxValue.equals("")
                                || integerValue > Integer.parseInt(maxValue)) {
                            maxValue = "" + integerValue;
                            maxValueRecordNumber = numberOfItemsRead;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(AnalyseField.class, "Analyse_Field_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(AnalyseField.class, "Field"));
        if (fieldDefinition.getName() != null) {
            elementItem.setContent(fieldDefinition.getName());
        }
        descriptionList.getListItem().add(elementItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(AnalyseField.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(AnalyseField.class, "FieldFormat"));
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
        Result result = new Result();
        ResultItems resultItems = new ResultItems();
        result.setResultItems(resultItems);

        ResultItem resultItem1 = new ResultItem();
        resultItems.getResultItem().add(resultItem1);
        resultItem1.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        resultItem1.setResultItems(resultItems1);
        // Number of items read
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType(ResultTypes.INFO_STRING);
        numberOfItemsReadResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "TotalNumberOfItemsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);
        // Number of empty items
        ResultItem numberOfEmptyItemsResultItem = new ResultItem();
        numberOfEmptyItemsResultItem.setType(ResultTypes.INFO_STRING);
        numberOfEmptyItemsResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "NumberOfEmptyItems"));
        numberOfEmptyItemsResultItem.setContent("" + numberOfEmptyItems);
        resultItems1.getResultItem().add(numberOfEmptyItemsResultItem);
        // Number of invalid items. Warning if > 0.
        ResultItem numberOfInvalidItemsResultItem = new ResultItem();
        numberOfInvalidItemsResultItem.setType(numberOfInvalidItems == 0 ? ResultTypes.INFO_STRING : ResultTypes.WARNING_STRING);
        numberOfInvalidItemsResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "NumberOfInvalidItems"));
        numberOfInvalidItemsResultItem.setContent("" + numberOfInvalidItems);
        resultItems1.getResultItem().add(numberOfInvalidItemsResultItem);
        // Min length
        if (minLength == Integer.MAX_VALUE) {
            minLength = 0;
        }
        ResultItem minLengthResultItem = new ResultItem();
        minLengthResultItem.setType(ResultTypes.INFO_STRING);
        minLengthResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MinLength"));
        minLengthResultItem.setContent("" + minLength);
        resultItems1.getResultItem().add(minLengthResultItem);
        // Min length record number
        ResultItem minLengthRecordNumberResultItem = new ResultItem();
        minLengthRecordNumberResultItem.setType(ResultTypes.INFO_STRING);
        minLengthRecordNumberResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MinLengthRecordNumber"));
        minLengthRecordNumberResultItem.setContent("" + minLengthRecordNumber);
        resultItems1.getResultItem().add(minLengthRecordNumberResultItem);
        // Max length
        ResultItem maxLengthResultItem = new ResultItem();
        maxLengthResultItem.setType(ResultTypes.INFO_STRING);
        maxLengthResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MaxLength"));
        maxLengthResultItem.setContent("" + maxLength);
        resultItems1.getResultItem().add(maxLengthResultItem);
        // Max length record number
        ResultItem maxLengthRecordNumberResultItem = new ResultItem();
        maxLengthRecordNumberResultItem.setType(ResultTypes.INFO_STRING);
        maxLengthRecordNumberResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MaxLengthRecordNumber"));
        maxLengthRecordNumberResultItem.setContent("" + maxLengthRecordNumber);
        resultItems1.getResultItem().add(maxLengthRecordNumberResultItem);
        // Min value
        ResultItem minValueResultItem = new ResultItem();
        minValueResultItem.setType(ResultTypes.INFO_STRING);
        minValueResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MinValue"));
        minValueResultItem.setContent(minValue);
        resultItems1.getResultItem().add(minValueResultItem);
        // Min value record number
        ResultItem minValueRecordNumberResultItem = new ResultItem();
        minValueRecordNumberResultItem.setType(ResultTypes.INFO_STRING);
        minValueRecordNumberResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MinValueRecordNumber"));
        minValueRecordNumberResultItem.setContent("" + minValueRecordNumber);
        resultItems1.getResultItem().add(minValueRecordNumberResultItem);
        // Max value
        ResultItem maxValueResultItem = new ResultItem();
        maxValueResultItem.setType(ResultTypes.INFO_STRING);
        maxValueResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MaxValue"));
        maxValueResultItem.setContent(maxValue);
        resultItems1.getResultItem().add(maxValueResultItem);
        // Max value record number
        ResultItem maxValueRecordNumberResultItem = new ResultItem();
        maxValueRecordNumberResultItem.setType(ResultTypes.INFO_STRING);
        maxValueRecordNumberResultItem.setLabel(NbBundle.getMessage(AnalyseField.class, "MaxValueRecordNumber"));
        maxValueRecordNumberResultItem.setContent("" + maxValueRecordNumber);
        resultItems1.getResultItem().add(maxValueRecordNumberResultItem);

        return result;
    }

}
