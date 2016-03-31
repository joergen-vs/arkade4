/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.util.ArrayList;
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
 * A class for controlling if a field in a flat file has values, and if the
 * values are within the min and max lengths if they are defined in the dataset
 * description. Missing values in a mandatory field are errors, and warnings
 * otherwise. Values shorter or longer than the min and max lengths are always
 * errors.
 * The output of the process is a sessionReport activity.
 *
 * @version 0.09 2014-04-10
 * @author Riksarkivet
 *
 */
public class ControlField extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final FieldDefinition fieldDefinition;
    private boolean notNull; // Is the field mandatory?
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private boolean start = true;
    private long numberOfItemsRead = 0;
    private long numberOfEmptyItems = 0;
    private long numberOfMinErrors = 0;
    private long numberOfMaxErrors = 0;
    private int minLength = Integer.MAX_VALUE;
    private int maxLength = Integer.MIN_VALUE;
    private ArrayList<Long> nullList;
    private ArrayList<Long> minErrorList;
    private ArrayList<Long> maxErrorList;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_Field'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the long name.
     * @param fieldDefinition the field (field definition) to control
     * @param datasetDescription the dataset description describing the field
     */
    public ControlField(String id, String name, String longName,
            FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription) {
        super(id, name, longName);
        this.fieldDefinition = fieldDefinition;
        this.datasetDescription = datasetDescription;
    }

    @Override
    public void init() {
        super.init();
        notNull = (fieldDefinition.getNotNull() != null);
        if (fieldDefinition.getMinLength() != null) {
            minLength = fieldDefinition.getMinLength().intValue();
        }
        if (fieldDefinition.getMaxLength() != null) {
            maxLength = fieldDefinition.getMaxLength().intValue();
        }
        nullList = new ArrayList();
        minErrorList = new ArrayList();
        maxErrorList = new ArrayList();
        try {
            Addml_8_2 addml_8_2 = (Addml_8_2) datasetDescription;
            fieldType = addml_8_2.getFieldType(fieldDefinition.getTypeReference());
            dataType = fieldType.getDataType();
            if (fieldType.getFieldFormat() != null && !fieldType.getFieldFormat().trim().equals("")) {
                fieldFormat = fieldType.getFieldFormat().trim();
            }
        } catch (NullPointerException ex) {
            log(this.getClass().getSimpleName() + " - init - " + ex.getClass().getSimpleName() + 
                    (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
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
            if (numberOfEmptyItems <= maxNumberOfResults) {
                nullList.add(numberOfItemsRead); // Record number with empty value
            }
        }
        if (value != null && value.length() > 0
                && value.length() < minLength && minLength < Integer.MAX_VALUE) {
            numberOfMinErrors++;
            if (numberOfMinErrors <= maxNumberOfResults) {
                minErrorList.add(numberOfItemsRead); // Record number with invalid value length
            }
        }
        if (value != null && value.length() > maxLength && maxLength > Integer.MIN_VALUE) {
            numberOfMaxErrors++;
            if (numberOfMaxErrors <= maxNumberOfResults) {
                maxErrorList.add(numberOfItemsRead); // Record number with invalid value length
            }
        }
    }

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(ControlField.class, "Control_Field_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(ControlField.class, "Field"));
        if (fieldDefinition.getName() != null) {
            elementItem.setContent(fieldDefinition.getName());
        }
        descriptionList.getListItem().add(elementItem);
        // fieldDefinition - notNull
        ListItem notNullItem = new ListItem();
        notNullItem.setLabel(NbBundle.getMessage(ControlField.class, "MandatoryField"));
        notNullItem.setContent(notNull
                ? NbBundle.getMessage(ControlField.class, "Yes")
                : NbBundle.getMessage(ControlField.class, "No"));
        descriptionList.getListItem().add(notNullItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(ControlField.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(ControlField.class, "FieldFormat"));
        if (fieldFormat != null) {
            fieldFormatItem.setContent(fieldFormat);
        }
        descriptionList.getListItem().add(fieldFormatItem);
        // fieldDefinition - minLength
        ListItem minLengthItem = new ListItem();
        minLengthItem.setLabel(NbBundle.getMessage(ControlField.class, "MinLength"));
        if (fieldDefinition.getMinLength() != null) {
            minLengthItem.setContent(fieldDefinition.getMinLength().toString());
        }
        descriptionList.getListItem().add(minLengthItem);
        // fieldDefinition - maxLength
        ListItem maxLengthItem = new ListItem();
        maxLengthItem.setLabel(NbBundle.getMessage(ControlField.class, "MaxLength"));
        if (fieldDefinition.getMaxLength() != null) {
            maxLengthItem.setContent(fieldDefinition.getMaxLength().toString());
        }
        descriptionList.getListItem().add(maxLengthItem);
        return activityInfo;
    }

    /**
     *
     * @return the result of the activity
     */
    private Result getResult() {
        Result result = new Result();
        ResultItems mainResultItems = new ResultItems();

        result.setResultItems(mainResultItems);

        // Overall result
        ResultItem overallResultItem = new ResultItem();
        mainResultItems.getResultItem().add(overallResultItem);
        overallResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultItem.setResultItems(resultItems1);
        // Number of items read
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType(ResultTypes.INFO_STRING);
        numberOfItemsReadResultItem.setLabel(NbBundle.getMessage(ControlField.class, "TotalNumberOfItemsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);
        // Number of empty items
        ResultItem numberOfEmptyItemsResultItem = new ResultItem();
        if (numberOfEmptyItems == 0) {
            numberOfEmptyItemsResultItem.setType(ResultTypes.INFO_STRING);
        } else {
            numberOfEmptyItemsResultItem.setType(notNull ? ResultTypes.ERROR_STRING : ResultTypes.WARNING_STRING);
        }
        numberOfEmptyItemsResultItem.setLabel(NbBundle.getMessage(ControlField.class, "NumberOfEmptyItems"));
        numberOfEmptyItemsResultItem.setContent("" + numberOfEmptyItems);
        resultItems1.getResultItem().add(numberOfEmptyItemsResultItem);
        // Number of min errors
        ResultItem numberOfMinErrorsResultItem = new ResultItem();
        numberOfMinErrorsResultItem.setType(numberOfMinErrors == 0 ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
        numberOfMinErrorsResultItem.setLabel(NbBundle.getMessage(ControlField.class, "NumberOfMinErrors"));
        numberOfMinErrorsResultItem.setContent("" + numberOfMinErrors);
        resultItems1.getResultItem().add(numberOfMinErrorsResultItem);
        // Number of max errors
        ResultItem numberOfMaxErrorsResultItem = new ResultItem();
        numberOfMaxErrorsResultItem.setType(numberOfMaxErrors == 0 ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
        numberOfMaxErrorsResultItem.setLabel(NbBundle.getMessage(ControlField.class, "NumberOfMaxErrors"));
        numberOfMaxErrorsResultItem.setContent("" + numberOfMaxErrors);
        resultItems1.getResultItem().add(numberOfMaxErrorsResultItem);

        // Detailed result
        if (!(nullList.isEmpty() && minErrorList.isEmpty() && maxErrorList.isEmpty())) {
            ResultItem detailedResultItem = new ResultItem();
            mainResultItems.getResultItem().add(detailedResultItem);
            detailedResultItem.setName("detailedResult");
            ResultItems resultItems2 = new ResultItems();
            detailedResultItem.setResultItems(resultItems2);

            // Not null errors
            if (!(nullList.isEmpty())) {
                resultItems2.getResultItem().add(getNullErrors());
            }

            // Min errors
            if (!(minErrorList.isEmpty())) {
                resultItems2.getResultItem().add(getMinErrors());
            }

            // Max errors
            if (!(maxErrorList.isEmpty())) {
                resultItems2.getResultItem().add(getMaxErrors());
            }
        }

        return result;
    }

    private ResultItem getNullErrors() {
        ResultItem nullErrorsResultItem = new ResultItem();
        StringBuilder nullErrorsLabel = new StringBuilder();
        nullErrorsLabel.append(
                NbBundle.getMessage(ControlField.class, "Control_NotNull_DetailDescription", fieldDefinition.getName()));
        nullErrorsLabel.append(" ");
        if (numberOfEmptyItems <= maxNumberOfResults) {
            nullErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingAll", numberOfEmptyItems));
        } else {
            nullErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingXofY", maxNumberOfResults, numberOfEmptyItems));
        }
        nullErrorsResultItem.setLabel(nullErrorsLabel.toString());
        ResultItems resultItems = new ResultItems();
        nullErrorsResultItem.setResultItems(resultItems);
        for (int i = 0; i < nullList.size(); i++) {
            ResultItem resultItem = new ResultItem();
            resultItem.setContent("" + nullList.get(i));
            resultItems.getResultItem().add(resultItem);
        }
        return nullErrorsResultItem;
    }

    private ResultItem getMinErrors() {
        ResultItem minErrorsResultItem = new ResultItem();
        StringBuilder minErrorsLabel = new StringBuilder();
        minErrorsLabel.append(
                NbBundle.getMessage(ControlField.class, "Control_ExtremeLengths_MinErrors_Description", fieldDefinition.getName()));
        minErrorsLabel.append(" ");
        if (minErrorList.size() <= maxNumberOfResults) {
            minErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingAll", numberOfMinErrors));
        } else {
            minErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingXofY", maxNumberOfResults, numberOfMinErrors));
        }
        minErrorsResultItem.setLabel(minErrorsLabel.toString());
        ResultItems resultItems = new ResultItems();
        minErrorsResultItem.setResultItems(resultItems);
        for (int i = 0; i < minErrorList.size(); i++) {
            ResultItem resultItem = new ResultItem();
            resultItem.setContent("" + minErrorList.get(i));
            resultItems.getResultItem().add(resultItem);
        }
        return minErrorsResultItem;
    }

    private ResultItem getMaxErrors() {
        ResultItem maxErrorsResultItem = new ResultItem();
        StringBuilder maxErrorsLabel = new StringBuilder();
        maxErrorsLabel.append(
                NbBundle.getMessage(ControlField.class, "Control_ExtremeLengths_MaxErrors_Description", fieldDefinition.getName()));
        maxErrorsLabel.append(" ");
        if (maxErrorList.size() <= maxNumberOfResults) {
            maxErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingAll", numberOfMaxErrors));
        } else {
            maxErrorsLabel.append(
                    NbBundle.getMessage(ControlField.class, "ShowingXofY", maxNumberOfResults, numberOfMaxErrors));
        }
        maxErrorsResultItem.setLabel(maxErrorsLabel.toString());
        ResultItems resultItems = new ResultItems();
        maxErrorsResultItem.setResultItems(resultItems);
        for (int i = 0; i < maxErrorList.size(); i++) {
            ResultItem resultItem = new ResultItem();
            resultItem.setContent("" + maxErrorList.get(i));
            resultItems.getResultItem().add(resultItem);
        }
        return maxErrorsResultItem;
    }

}
