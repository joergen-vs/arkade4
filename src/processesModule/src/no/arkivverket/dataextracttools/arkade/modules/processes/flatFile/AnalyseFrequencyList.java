/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
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
 * A class for producing a frequency list from a field's values. The output is a
 * sessionReport activity.
 *
 * @version 0.16 2014-03-21
 * @author Riksarkivet
 *
 */
public class AnalyseFrequencyList extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private boolean start = true;
    private final FieldDefinition fieldDefinition;
    private FieldType fieldType;
    private String dataType;
    private String fieldFormat;
    private Map<String, Long> valueMap;
    private long numberOfItemsRead = 0;

    /**
     *
     * @param id an identifying name for the process
     * @param name a short name for the process. Could be
     * 'Analyse_FrequencyList'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the short name.
     * @param fieldDefinition the field to be analysed
     * @param datasetDescription the dataset description the field belongs to
     */
    public AnalyseFrequencyList(String id, String name, String longName,
            FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription) {
        super(id, name, longName);
        this.fieldDefinition = fieldDefinition;
        this.datasetDescription = datasetDescription;
    }

    /**
     * @return the FieldDefinition representing the field to process.
     */
    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    @Override
    public void init() {
        super.init();
        valueMap = new HashMap();
        try {
            Addml_8_2 addml_8_2 = (Addml_8_2) datasetDescription;
            fieldType = addml_8_2.getFieldType(fieldDefinition.getTypeReference());
            dataType = fieldType.getDataType();
            if (fieldType.getFieldFormat() != null && !fieldType.getFieldFormat().trim().equals("")) {
                fieldFormat = fieldType.getFieldFormat().trim();
            }
        } catch (NullPointerException ex) {
            log(ex.getMessage(), Level.SEVERE);
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

        String value = "";
        Item item = readEvent.getItem();
        if (item != null && item.getValue() != null) {
            value = ((String) item.getValue()).trim();
        }

        long valueCounter = 1;

        numberOfItemsRead++;

        if (valueMap.containsKey(value)) {
            valueCounter += valueMap.get(value);
        }
        valueMap.put(value, valueCounter);
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

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(AnalyseFrequencyList.class, "Analyse_FrequencyList_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // fieldDefinition - name
        ListItem elementItem = new ListItem();
        elementItem.setLabel(NbBundle.getMessage(AnalyseFrequencyList.class, "Field"));
        if (fieldDefinition.getName() != null) {
            elementItem.setContent(fieldDefinition.getName());
        }
        descriptionList.getListItem().add(elementItem);
        // fieldType - dataType
        ListItem dataTypeItem = new ListItem();
        dataTypeItem.setLabel(NbBundle.getMessage(AnalyseFrequencyList.class, "DataType"));
        if (dataType != null) {
            dataTypeItem.setContent(dataType);
        }
        descriptionList.getListItem().add(dataTypeItem);
        // fieldType - fieldFormat
        ListItem fieldFormatItem = new ListItem();
        fieldFormatItem.setLabel(NbBundle.getMessage(AnalyseFrequencyList.class, "FieldFormat"));
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

        // Overall result
        ResultItem overallResultResultItem = new ResultItem();
        resultItems.getResultItem().add(overallResultResultItem);
        overallResultResultItem.setName("overallResult");
        ResultItems resultItems1 = new ResultItems();
        overallResultResultItem.setResultItems(resultItems1);
        // Number of items read
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType((numberOfItemsRead > 0) ? ResultTypes.INFO_STRING : ResultTypes.WARNING_STRING);
        numberOfItemsReadResultItem.setLabel(NbBundle.getMessage(AnalyseFrequencyList.class, "TotalNumberOfItemsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);
        // Number of different values
        ResultItem numberOfDifferentValuesResultItem = new ResultItem();
        numberOfDifferentValuesResultItem.setType(ResultTypes.INFO_STRING);
        numberOfDifferentValuesResultItem.setLabel(NbBundle.getMessage(AnalyseFrequencyList.class, "NumberOfDifferentValues"));
        numberOfDifferentValuesResultItem.setContent("" + valueMap.size());
        resultItems1.getResultItem().add(numberOfDifferentValuesResultItem);

        // Detailed result
        if (valueMap.size() > 0 && maxNumberOfResults > 0) {
            ResultItem detailedResultItem = getDetailedResult();
            resultItems.getResultItem().add(detailedResultItem);
        }
        return result;
    }

    private ResultItem getDetailedResult() {
        ResultItem detailedResult = new ResultItem();
        detailedResult.setName("detailedResult");

        detailedResult.setResultItems(new ResultItems());
        ResultItem occurrencesResultItem = new ResultItem();
        StringBuilder occurrencesLabel = new StringBuilder();
        occurrencesLabel.append(NbBundle.getMessage(AnalyseFrequencyList.class, "Analyse_FrequencyList_DetailDescription"));
        occurrencesLabel.append(" ");
        detailedResult.getResultItems().getResultItem().add(occurrencesResultItem);
        occurrencesResultItem.setResultItems(new ResultItems());
        
        int resultCounter = 0;
        
        Set<Entry<String, Long>> set = valueMap.entrySet();
        List<Entry<String, Long>> list = new ArrayList<>(set);
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>()
        {
            @Override
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );
                
        for (Map.Entry<String, Long> entry : list) {
            resultCounter++;
            if (resultCounter <= maxNumberOfResults) {
                ResultItem resultItem = new ResultItem();
                resultItem.setLabel(entry.getKey());
                resultItem.setContent(entry.getValue().toString());
                occurrencesResultItem.getResultItems().getResultItem().add(resultItem);
            } else {
                resultCounter--;
                break;
            }
        }

        if (resultCounter == valueMap.size()) {
            occurrencesLabel.append(
                    NbBundle.getMessage(AnalyseFrequencyList.class, "ShowingAllDifferentValues", valueMap.size()));
        } else {
            occurrencesLabel.append(
                    NbBundle.getMessage(AnalyseFrequencyList.class, "ShowingXofYDifferentValues", maxNumberOfResults, valueMap.size()));
        }
        occurrencesResultItem.setLabel(occurrencesLabel.toString());
        return detailedResult;
    }
}
