/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.util.TreeMap;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.reports.ResultTypes;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Description;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.DescriptionList;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ListItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Result;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItems;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Code;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldType;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.NbBundle;

/**
 * A class for controlling codes in a field. The codes are defined in the
 * corresponding fieldDefinition in the dataset description. If there are codes
 * that are not used in the flat file, a warning is reported. If there are
 * values in the file which are not defined in the dataset description, an error
 * is reported. The output of the process is a sessionReport activity.
 *
 * @version 0.15 2014-03-21
 * @author Riksarkivet
 *
 */
public class ControlCodes extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private boolean start = true;
    private boolean hasCodes = false;
    private final FieldDefinition fieldDefinition;
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private long numberOfItemsRead = 0;
    private long numberOfDefinedCodes = 0;
    private long numberOfUndefinedCodes = 0;
    private TreeMap<String, CodeCounter> codeMap;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be 'Control_Codes'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the short name.
     * @param fieldDefinition the field (field definition) to control
     * @param datasetDescription the dataset description describing the flat
     * file
     */
    public ControlCodes(String id, String name, String longName,
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
        hasCodes = (fieldDefinition.getCodes() != null
                && !fieldDefinition.getCodes().getCode().isEmpty());

        if (hasCodes) {
            codeMap = new TreeMap();

            if (fieldDefinition.getCodes() != null) {
                numberOfDefinedCodes = fieldDefinition.getCodes().getCode().size();
                for (Code code : fieldDefinition.getCodes().getCode()) {
                    CodeCounter codeCounter
                            = new CodeCounter(code.getCodeValue(), code.getExplan(), false);
                    codeMap.put(code.getCodeValue(), codeCounter);
                }
            }
        }
        activity.setDescription(getActivityInfo());
    }

    @Override
    public void itemRead(ReadEvent readEvent) {
        // Set timestamp for started at first read
        if (start) {
            start = false;
            XMLGregorianCalendar timeStamp;
            try {
                timeStamp = XMLUtils.createTimeStamp();
                activity.setTimeStarted(timeStamp);
            } catch (DatatypeConfigurationException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }
        }
        if (!hasCodes) {
            return;
        }

        numberOfItemsRead++;

        String codeValue = "";
        Item item = readEvent.getItem();
        if (item != null && item.getValue() != null) {
            codeValue = ((String) item.getValue()).trim();
        }

        if (codeMap.get(codeValue) == null) {
            numberOfUndefinedCodes++;
            CodeCounter codeCounter = new CodeCounter(codeValue, null, true);
            codeMap.put(codeValue, codeCounter);
        }

        codeMap.get(codeValue).addOccurrence();
    }

    @Override
    public void finish() {
        if (!finished) {
            finished = true;

            Result result;
            if (!hasCodes) {
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
                NbBundle.getMessage(ControlCodes.class, "Control_Codes_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(ControlCodes.class, "Field"));
        if (fieldDefinition.getName() != null) {
            elementItem.setContent(fieldDefinition.getName());
        }
        descriptionList.getListItem().add(elementItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(ControlCodes.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(ControlCodes.class, "FieldFormat"));
        if (fieldFormat != null) {
            fieldFormatItem.setContent(fieldFormat);
        }
        descriptionList.getListItem().add(fieldFormatItem);
        return activityInfo;
    }

    private Result getNoProcessingResult() {
        Result result = new Result();
        ResultItems resultItems = new ResultItems();
        result.setResultItems(resultItems);

        // Overall result
        ResultItem overallResultItem = new ResultItem();
        resultItems.getResultItem().add(overallResultItem);
        overallResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultItem.setResultItems(resultItems1);

        // No codes
        ResultItem noCodesResultItem = new ResultItem();
        noCodesResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "CanNotControlCodes"));
        noCodesResultItem.setContent(NbBundle.getMessage(ControlCodes.class, "NoCodes"));
        resultItems1.getResultItem().add(noCodesResultItem);

        return result;
    }

    private Result getResult() {
        Result result = new Result();
        ResultItems resultItems = new ResultItems();
        result.setResultItems(resultItems);

        // Overall result
        ResultItem overallResultResultItem = new ResultItem();
        resultItems.getResultItem().add(overallResultResultItem);
        overallResultResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultResultItem.setResultItems(resultItems1);
        // Number of items read
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType(ResultTypes.INFO_STRING);
        numberOfItemsReadResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "TotalNumberOfItemsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);
        // Number of different codes
        ResultItem numberOfDifferentCodesResultItem = new ResultItem();
        numberOfDifferentCodesResultItem.setType(ResultTypes.INFO_STRING);
        numberOfDifferentCodesResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "NumberOfDifferentCodes"));
        numberOfDifferentCodesResultItem.setContent("" + codeMap.size());
        resultItems1.getResultItem().add(numberOfDifferentCodesResultItem);
        // Number of defined codes
        ResultItem numberOfDefinedCodesResultItem = new ResultItem();
        numberOfDefinedCodesResultItem.setType(ResultTypes.INFO_STRING);
        numberOfDefinedCodesResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "NumberOfDefinedCodes"));
        numberOfDefinedCodesResultItem.setContent("" + numberOfDefinedCodes);
        resultItems1.getResultItem().add(numberOfDefinedCodesResultItem);
        // Number of undefined codes
        ResultItem numberOfUndefinedCodesResultItem = new ResultItem();
        numberOfUndefinedCodesResultItem.setType(numberOfUndefinedCodes == 0 ? ResultTypes.INFO_STRING : ResultTypes.ERROR_STRING);
        numberOfUndefinedCodesResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "NumberOfUndefinedCodes"));
        numberOfUndefinedCodesResultItem.setContent("" + numberOfUndefinedCodes);
        resultItems1.getResultItem().add(numberOfUndefinedCodesResultItem);

        // Detailed result
        if (codeMap.size() > 0 && maxNumberOfResults > 0) {
            ResultItem detailedResultItem = getDetailedResult();
            resultItems.getResultItem().add(detailedResultItem);
        }   

        return result;
    }

    private ResultItem getDetailedResult() {
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");
        detailedResult.setResultItems(new ResultItems());
        ResultItem codesResultItem = new ResultItem();
        codesResultItem.setLabel(NbBundle.getMessage(ControlCodes.class, "CodesAndNumberOfOccurrences"));
        detailedResult.getResultItems().getResultItem().add(codesResultItem);
        codesResultItem.setResultItems(new ResultItems());
        // Ordered by the keys (TreeMap)
        for (CodeCounter codeCounter : codeMap.values()) {
            ResultItem resultItem = codeCounter.getResult();
            codesResultItem.getResultItems().getResultItem().add(resultItem);
        }
        return detailedResult;
    }
}
