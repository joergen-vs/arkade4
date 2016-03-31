/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.reports.ResultTypes;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.RecordReader;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Description;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.DescriptionList;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ListItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Result;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItems;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2_Helper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Property;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.NbBundle;

/**
 * A class for controlling a flat file's number of records against the flat
 * file's numberOfOccurrences-property in the dataset description. If the
 * numberOfOccurrences-property is missing, the process only shows the number of
 * records in the file. The output of the process is a sessionReport activity.
 *
 * @version 0.07 2014-03-21
 * @author Riksarkivet
 *
 */
public class ControlNumberOfRecords extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final FlatFile flatFile;
    private String fileName;
    private boolean start = true; // The highest reader in the hierarchy of readers to which this processs is listening,
    // calls itemRead twice. 
    // start is used for telling these two reads apart.  
    private long numberOfItemsRead = 0;
    private long givenNumberOfOccurrences = -1;
    private boolean invalidGivenNumberOfOccurrences;
    private String numberOfOccurrences = null;

    /**
     *
     * @param id an identifier for the process
     * @param name a short name for the process. Could be
     * 'Control_NumberOfRecords'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the short name.
     * @param flatFile the flat file to analyse
     * @param datasetDescription the dataset description describing the flat
     * file
     */
    public ControlNumberOfRecords(String id, String name, String longName,
            FlatFile flatFile,
            DatasetDescription datasetDescription) {
        super(id, name, longName);
        this.flatFile = flatFile;
        this.datasetDescription = datasetDescription;
    }

    @Override
    public void init() {
        super.init();
        try {
            File file = new File(Addml_8_2_Helper.getFileName(flatFile));
            fileName = file.getName();
        } catch (IllegalArgumentException |  NullPointerException ex) {
            // Do nothing
        }
        try {
            Property numberOfOccurrencesProperty = Addml_8_2_Helper.getProperty(flatFile, "numberOfOccurrences");
            if (numberOfOccurrencesProperty != null) {
                numberOfOccurrences = numberOfOccurrencesProperty.getValue();
            }
        } catch (IllegalArgumentException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException ex) {
            // Do nothing
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

    /**
     *
     * @param readEvent the readEvent from the reader(s) this process is
     * listening to.
     */
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

        // Two itemRead per record. 
        // First not null (beginning). Second null (end).
        // Counts when not null.
        if (readEvent.getReader() instanceof RecordReader
                && readEvent.getItem() != null) {
            numberOfItemsRead++;
        }
    }

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(ControlNumberOfRecords.class, "Control_NumberOfRecords_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // flatFile - name
        ListItem nameItem = new ListItem();
        nameItem.setLabel(NbBundle.getMessage(ControlNumberOfRecords.class, "FlatFile"));
        if (flatFile.getName() != null) {
            nameItem.setContent(flatFile.getName());
        }
        descriptionList.getListItem().add(nameItem);
        // flatFile - fileName
        ListItem fileNameItem = new ListItem();
        fileNameItem.setLabel(NbBundle.getMessage(ControlNumberOfRecords.class, "FileName"));
        if (fileName != null) {
            fileNameItem.setContent(fileName);
        }
        descriptionList.getListItem().add(fileNameItem);
        // flatFile - numberOfRecords
        ListItem numberOfRecordsItem = new ListItem();
        numberOfRecordsItem.setLabel(
                NbBundle.getMessage(ControlNumberOfRecords.class, "NumberOfRecordsInTheDatasetDescription"));
        if (numberOfOccurrences != null) {
            numberOfRecordsItem.setContent(numberOfOccurrences);
        }
        descriptionList.getListItem().add(numberOfRecordsItem);
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
        // Number of items read. Warning if 0. 
        ResultItem numberOfItemsReadResultItem = new ResultItem();
        numberOfItemsReadResultItem.setType(
                numberOfItemsRead > 0 ? ResultTypes.INFO_STRING : ResultTypes.WARNING_STRING);
        numberOfItemsReadResultItem.setLabel(
                NbBundle.getMessage(ControlNumberOfRecords.class, "NumberOfRecordsRead"));
        numberOfItemsReadResultItem.setContent("" + numberOfItemsRead);
        resultItems1.getResultItem().add(numberOfItemsReadResultItem);

        // Is the number of items read equal to the number given in the dataset description?
        ResultItem resultItem = new ResultItem();

        if (numberOfOccurrences != null) {
            try {
                givenNumberOfOccurrences = Long.parseLong(numberOfOccurrences);
            } catch (NumberFormatException ex) {
                invalidGivenNumberOfOccurrences = true;
                log(this.getClass().getSimpleName() + " - getResult - " + ex.getClass().getSimpleName()
                        + (ex.getMessage() != null ? " - " + ex.getMessage() : ""), Level.SEVERE);
            }

            if (invalidGivenNumberOfOccurrences) {
                // Not a number
                resultItem.setType(ResultTypes.ERROR_STRING);
                resultItem.setLabel(
                        NbBundle.getMessage(ControlNumberOfRecords.class, "Control_NumberOfRecords_GivenNumber_Error"));
            } else {

                if (numberOfItemsRead == givenNumberOfOccurrences) {
                    // The number of items read is equal to the number given in the dataset description
                    resultItem.setType(ResultTypes.INFO_STRING);
                    resultItem.setLabel(
                            NbBundle.getMessage(ControlNumberOfRecords.class, "Control_NumberOfRecords_Result_Info"));
                } else {
                    // The number of items read is NOT equal to the number given in the dataset description
                    resultItem.setType(ResultTypes.ERROR_STRING);
                    resultItem.setLabel(
                            NbBundle.getMessage(ControlNumberOfRecords.class, "Control_NumberOfRecords_Result_Error"));
                }
            }
        } else {
            // No number of records
            resultItem.setType(ResultTypes.WARNING_STRING);
            resultItem.setLabel(
                    NbBundle.getMessage(ControlNumberOfRecords.class, "NoNumberOfRecords"));
        }
        resultItems1.getResultItem().add(resultItem);

        return result;
    }
}
