/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.util.logging.Level;
import javax.xml.bind.DatatypeConverter;
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
 *
 * @version 0.14 2014-04-16
 * @author Riksarkivet
 *
 */
public class ControlDataFormat extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private static final String FNR = "fnr";
    private static final char SEPARATOR_CHAR = '-';
    private final FieldDefinition fieldDefinition;
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private int yearStart = -1;
    private int yearEnd = -1;
    private int yearLength = -1;
    private int monthStart = -1;
    private int monthEnd = -1;
    private int monthLength = -1;
    private int dayStart = -1;
    private int dayEnd = -1;
    private int dayLength = -1;
    private boolean canProcess = true;
    private boolean start = true;
    private long numberOfItemsRead = 0;
    private long numberOfEmptyItems = 0;
    private long numberOfValuesNotControlled = 0;
    private long numberOfErrors = 0;
    private ResultItems errorItems;
    // boolean
    private String trueValue = "";
    private String falseValue = "";

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_DataFormat'
     * @param longName a long name for the process. Can contain a more
     * descriptive name for the process.
     * @param fieldDefinition the field (field definition) to control
     * @param datasetDescription the dataset description describing the field
     */
    public ControlDataFormat(String id, String name, String longName,
            FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription) {
        super(id, name, longName);
        this.fieldDefinition = fieldDefinition;
        this.datasetDescription = datasetDescription;
    }

    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    @Override
    public void init() {
        super.init();
        Addml_8_2 addml_8_2 = (Addml_8_2) datasetDescription;
        fieldType = addml_8_2.getFieldType(fieldDefinition.getTypeReference());

        if (fieldType == null || fieldType.getDataType() == null) {
            // Missing fieldType or dataType
            canProcess = false;
        } else {
            dataType = fieldType.getDataType();
            if (fieldType.getFieldFormat() != null
                    && !fieldType.getFieldFormat().trim().equals("")) {
                fieldFormat = fieldType.getFieldFormat().trim();
            }
            switch (dataType) {
                case DataTypes.STRING:
                    break;
                case DataTypes.INTEGER:
                    break;
                case DataTypes.FLOAT:
                    break;
                case DataTypes.DATE:
                    // TODO use century when two digits year
                    if (fieldFormat != null) {
                        if (!fieldFormat.equals("xsd:dateTime")
                                && !fieldFormat.equals("xsd:date")) {

                            yearStart = fieldFormat.indexOf("YYYY");
                            if (yearStart > -1) {
                                yearLength = 4;
                                yearEnd = yearStart + yearLength;
                            } else {
                                yearLength = 2;
                                yearStart = fieldFormat.indexOf("YY");
                                yearEnd = yearStart > -1 ? (yearStart + yearLength) : -1;
                            }

                            monthLength = 2;
                            monthStart = fieldFormat.indexOf("MM");
                            monthEnd = monthStart > -1 ? (monthStart + monthLength) : -1;

                            dayLength = 2;
                            dayStart = fieldFormat.indexOf("DD");
                            dayEnd = dayStart > -1 ? (dayStart + dayLength) : -1;
                        }
                    }

                    break;
                case DataTypes.BOOLEAN:
                    if (fieldFormat != null && !fieldFormat.trim().equals("")) {
                        int booleanDelimeterPos = fieldFormat.indexOf('/');
                        if (booleanDelimeterPos > 0) {
                            trueValue = fieldFormat.substring(0, booleanDelimeterPos);
                            falseValue = fieldFormat.substring(booleanDelimeterPos + 1);
                        }
                    }
                    break;
                case DataTypes.LINK:
                    break;
                default:
                    break;
            }
        }
        activity.setDescription(getActivityInfo());
    }

    @Override
    public void itemRead(ReadEvent readEvent) {
        boolean error;
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

        // No processing - Missing fieldType or dataType
        if (!canProcess) {
            return;
        }

        String value = null;
        Item item = readEvent.getItem();
        if (item != null && item.getValue() != null) {
            value = ((String) item.getValue()).trim();
        }

        numberOfItemsRead++;

        if (value == null || value.trim().equals("")) {
            numberOfEmptyItems++;
            return;
        }

        error = controlValue(value);

        if (error) {
            numberOfErrors++;
            if (numberOfErrors <= maxNumberOfResults) {
                if (errorItems == null) {
                    errorItems = new ResultItems();
                }
                ResultItem errorItem = getErrorItem(value);
                errorItems.getResultItem().add(errorItem);
            }
        }
    }

    private boolean controlValue(String value) {
        boolean error = false;

        switch (dataType) {
            case DataTypes.STRING:
                if (fieldFormat != null) {
                    switch (fieldFormat) {
                        case FNR:
                            error = controlFnr(value);
                            break;
                    }
                }
                break;
            case DataTypes.INTEGER:
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    error = true;
                }
                break;
            case DataTypes.FLOAT:

                break;
            case DataTypes.DATE:
                error = controlDate(value);
                break;
            case DataTypes.BOOLEAN:
                error = controlBoolean(value);
                break;
            case DataTypes.LINK:
                break;
            default:
                break;
        }

        return error;
    }

    private boolean controlBoolean(String value) {
        boolean error = false;
        if (trueValue.equals("") || falseValue.equals("")) {
            numberOfValuesNotControlled++;
        } else {
            if (!trueValue.equals(value) && !falseValue.equals(value)) {
                error = true;
            }
        }

        return error;
    }

    /**
     * xsd:date xsd:dateTime
     *
     * YYYYMMDD YYYY-MM-DD
     *
     * YYMMDD YY-MM-DD
     *
     * DDMMYYYY DD-MM-YYYY
     *
     * DDMMYY DD-MM-YY
     *
     * MMDDYYYY MM-DD-YYYY
     *
     * MMDDYY MM-DD-YY
     *
     * @param value
     * @return
     */
    private boolean controlDate(String value) {
        boolean error = false;
        String dateValue = null; // Error if not xsd:dateTime

        if (fieldFormat.equals("xsd:dateTime")
                || fieldFormat.equals("xsd:date")) {
            dateValue = value;
        } else if (yearLength == 2) {
            // TODO use century when two digits year
            error = true;
        } else if (value.length() < 6) {
            error = true;
        } else if (value.length() <= 10) {
            try {
                String yearValue = value.substring(yearStart, yearEnd).trim();
                String monthValue = value.substring(monthStart, monthEnd).trim();
                String dayValue = value.substring(dayStart, dayEnd);
                if (yearValue.length() != yearLength
                        || monthValue.length() != monthLength
                        || dayValue.length() != dayLength) {
                    error = true;
                } else {
                    dateValue = yearValue + SEPARATOR_CHAR + monthValue + SEPARATOR_CHAR + dayValue;
                }
            } catch (IndexOutOfBoundsException e) {
                error = true;
            }
        } else {
            error = true;
        }

        if (!error) {
            try {
                DatatypeConverter.parseDateTime(dateValue);
            } catch (IllegalArgumentException e) {
                // Not xsd:dateTime
                error = true;
            }
        }

        return error;
    }

    private boolean controlFnr(String value) {
        // First:  3 - 7 - 6 - 1 - 8 - 9 - 4 - 5 - 2 
        // Second: 5 - 4 - 3 - 2 - 7 - 6 - 5 - 4 - 3 - 2  
        boolean error = false;

        // Only numbers?
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            error = true;
        }

        // Correct length?
        if (!error) {
            int len = value.length();
            if (len != 11) {
                error = true;
            }
        }

        // First control number
        if (!error) {
            int first = Character.getNumericValue(value.charAt(9));
            int v1
                    = 3 * Character.getNumericValue(value.charAt(0))
                    + 7 * Character.getNumericValue(value.charAt(1))
                    + 6 * Character.getNumericValue(value.charAt(2))
                    + 1 * Character.getNumericValue(value.charAt(3))
                    + 8 * Character.getNumericValue(value.charAt(4))
                    + 9 * Character.getNumericValue(value.charAt(5))
                    + 4 * Character.getNumericValue(value.charAt(6))
                    + 5 * Character.getNumericValue(value.charAt(7))
                    + 2 * Character.getNumericValue(value.charAt(8));

            int k1 = v1 % 11;
            if (k1 != 0) {
                k1 = 11 - k1;
            }

            if (first != k1) {
                error = true;
            }
        }

        // Second control number
        if (!error) {
            int second = Integer.parseInt("" + value.charAt(10));
            int v2
                    = 5 * Character.getNumericValue(value.charAt(0))
                    + 4 * Character.getNumericValue(value.charAt(1))
                    + 3 * Character.getNumericValue(value.charAt(2))
                    + 2 * Character.getNumericValue(value.charAt(3))
                    + 7 * Character.getNumericValue(value.charAt(4))
                    + 6 * Character.getNumericValue(value.charAt(5))
                    + 5 * Character.getNumericValue(value.charAt(6))
                    + 4 * Character.getNumericValue(value.charAt(7))
                    + 3 * Character.getNumericValue(value.charAt(8))
                    + 2 * Character.getNumericValue(value.charAt(9));

            int k2 = v2 % 11;
            if (k2 != 0) {
                k2 = 11 - k2;
            }

            if (second != k2) {
                error = true;
            }
        }

        return error;
    }

    @Override
    public void finish() {
        if (!finished) {
            finished = true;
            Result result;
            if (!canProcess) {
                result = getNoProcessingResult();
            } else {
                result = getResult();
            }
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

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(ControlDataFormat.class, "Control_DataFormat_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "Field"));
        if (fieldDefinition.getName() != null) {
            elementItem.setContent(fieldDefinition.getName());
        }
        descriptionList.getListItem().add(elementItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "FieldFormat"));
        if (fieldFormat != null) {
            fieldFormatItem.setContent(fieldFormat);
        }
        descriptionList.getListItem().add(fieldFormatItem);
        return activityInfo;
    }

    private Result getNoProcessingResult() {
        // Missing fieldType or dataType
        Result result = new Result();
        ResultItems resultItems = new ResultItems();
        result.setResultItems(resultItems);
        ResultItem overallResultItem = new ResultItem();
        resultItems.getResultItem().add(overallResultItem);
        overallResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultItem.setResultItems(resultItems1);
        ResultItem noProcessingItem = new ResultItem();
        noProcessingItem.setType(ResultTypes.ERROR_STRING);
        noProcessingItem.setLabel(
                NbBundle.getMessage(ControlDataFormat.class, "NoProcessing"));
        noProcessingItem.setContent(
                NbBundle.getMessage(ControlDataFormat.class, "MissingFieldTypeOrDataType"));
        resultItems1.getResultItem().add(noProcessingItem);
        return result;
    }

    private Result getResult() {
        // <result>
        //   <resultItems>
        //     <resultItem name="overallResult">
        //       <resultItems>
        //         <resultItem type="info">
        //           <label>totalNumberOfItemsRead</label> 
        //           <content>1450984</content> 
        //         </resultItem>        
        //         <resultItem type="info">
        //           <label>numberOfEmptyItems</label> 
        //           <content>762</content> 
        //         </resultItem>        
        //         <resultItem type="info/error">
        //           <label>numberOfValuesNotControlled</label> 
        //           <content>17</content> 
        //         </resultItem>        
        //         <resultItem type="info/error">
        //           <label>numberOfErrors</label> 
        //           <content>155</content> 
        //         </resultItem>
        //       </resultItems>
        //     </resultItem>
        //   </resultItems>
        // </result>

        Result result = new Result();
        ResultItems resultItems = new ResultItems();
        result.setResultItems(resultItems);
        ResultItem overallResultItem = new ResultItem();
        resultItems.getResultItem().add(overallResultItem);
        overallResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultItem.setResultItems(resultItems1);
        // Number of items read
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType(ResultTypes.INFO_STRING);
        numberOfItemsReadResultItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "TotalNumberOfItemsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);
        // Number of empty items
        ResultItem numberOfEmptyItemsResultItem = new ResultItem();
        numberOfEmptyItemsResultItem.setType(ResultTypes.INFO_STRING);
        numberOfEmptyItemsResultItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "NumberOfEmptyItems"));
        numberOfEmptyItemsResultItem.setContent("" + numberOfEmptyItems);
        resultItems1.getResultItem().add(numberOfEmptyItemsResultItem);
        // Number of values not controlled
        ResultItem numberOfValuesNotControlledResultItem = new ResultItem();
        numberOfValuesNotControlledResultItem.setType(
                (numberOfValuesNotControlled == 0) ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
        numberOfValuesNotControlledResultItem.setLabel(
                NbBundle.getMessage(ControlDataFormat.class, "NumberOfValuesNotControlled"));
        numberOfValuesNotControlledResultItem.setContent("" + numberOfValuesNotControlled);
        resultItems1.getResultItem().add(numberOfValuesNotControlledResultItem);
        // Number of errors
        ResultItem numberOfErrorsResultItem = new ResultItem();
        numberOfErrorsResultItem.setType(
                (numberOfErrors == 0) ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
        numberOfErrorsResultItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "NumberOfErrors"));
        numberOfErrorsResultItem.setContent("" + numberOfErrors);
        resultItems1.getResultItem().add(numberOfErrorsResultItem);

        if (errorItems != null && maxNumberOfResults > 0) {
            ResultItem detailedResult = getDetailedResult();
            resultItems.getResultItem().add(detailedResult);
        }
        return result;
    }

    private ResultItem getDetailedResult() {
        // <resultItem name="detailedResult">
        //   <resultItems>
        //     <resultItem>
        //       <label>DetailDescription + ShowingXofY</label>
        //       <errorItems>
        //       ...
        //       </errorItems>
        //     </resultItem>
        //   </resultItems>
        // </resultItem>
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");
        detailedResult.setResultItems(new ResultItems());
        ResultItem dataFormatResultItem = new ResultItem();

        StringBuilder label = new StringBuilder();
        label.append(
                NbBundle.getMessage(ControlDataFormat.class, "Control_DataFormat_DetailDescription"));
        label.append(" ");
        if (numberOfErrors <= maxNumberOfResults) {
            label.append(
                    NbBundle.getMessage(ControlDataFormat.class, "ShowingAll", numberOfErrors));
        } else {
            label.append(
                    NbBundle.getMessage(ControlDataFormat.class, "ShowingXofY", maxNumberOfResults, numberOfErrors));
        }
        dataFormatResultItem.setResultItems(errorItems);
        dataFormatResultItem.setLabel(label.toString());
        detailedResult.getResultItems().getResultItem().add(dataFormatResultItem);
        return detailedResult;
    }

    private ResultItem getErrorItem(String value) {
        // <resultItem type="error">
        //   <resultItems>
        //     <resultItem>
        //       <label>Field value</label>
        //       <content>xyz</content>
        //     </resultItem>
        //     <resultItem>
        //       <label>Record number</label>
        //       <content>4117</content>
        //     </resultItem>
        //   </resultItems>
        // </resultItem>
        ResultItem errorItem = new ResultItem();
        errorItem.setType(ResultTypes.ERROR_STRING);
        ResultItems resultItems = new ResultItems();
        errorItem.setResultItems(resultItems);
        ResultItem valueItem = new ResultItem();
        valueItem.setLabel(NbBundle.getMessage(ControlDataFormat.class, "FieldValue"));
        valueItem.setContent(value);
        resultItems.getResultItem().add(valueItem);
        ResultItem recordNumberItem = new ResultItem();
        recordNumberItem.setLabel(
                NbBundle.getMessage(ControlDataFormat.class, "RecordNumber"));
        recordNumberItem.setContent("" + numberOfItemsRead);
        resultItems.getResultItem().add(recordNumberItem);
        return errorItem;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass()).append(" - fieldDefinition: ").append(fieldDefinition.getName());
        return sb.toString();
    }
}
