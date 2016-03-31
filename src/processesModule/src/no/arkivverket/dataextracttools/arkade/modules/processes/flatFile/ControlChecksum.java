/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.flatFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
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
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2_Helper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Property;
import no.arkivverket.dataextracttools.utils.Utils;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;
import org.openide.util.NbBundle;

/**
 * A class for controlling a flat file's checksum. A checksum is calculated
 * based on the algorithm given in the dataset description, and controlled
 * against the checksum in the dataset description. The output of the process is
 * a sessionReport activity.
 *
 * @version 0.17 2014-04-10
 * @author Riksarkivet
 *
 */
public class ControlChecksum extends BasicDataExtractProcess {

    private final no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
            = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
    private final FlatFile flatFile;
    private boolean start = true;
    private final File dataDirectory;
    private String fileName;
    private String algorithm;
    private String checksumValue;

    /**
     * @param id an identifier for the process
     * @param name a short name for the process. For example
     * 'Analyse_CountRecords'
     * @param longName a long name for the process. Usually contains structural
     * information combined with the long name.
     * @param flatFile the flat file to analyse
     * @param datasetDescription the dataset description describing the flat
     * file
     * @param dataDirectory the directory with the data files
     */
    public ControlChecksum(String id, String name, String longName,
            FlatFile flatFile,
            DatasetDescription datasetDescription, File dataDirectory) {
        super(id, name, longName);
        this.flatFile = flatFile;
        this.datasetDescription = datasetDescription;
        this.dataDirectory = dataDirectory;
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

        Property checksumProperty = null;
        try {
            checksumProperty = Addml_8_2_Helper.getProperty(flatFile, "checksum");
        } catch (IllegalArgumentException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException ex) {
            // Do nothing
        }

        try {
            algorithm = Addml_8_2_Helper.getPropertyValue(checksumProperty, "algorithm");
        } catch (NullPointerException | IllegalArgumentException |
                IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            // Do nothing
        }

        try {
            checksumValue = Addml_8_2_Helper.getPropertyValue(checksumProperty, "value");
        } catch (NullPointerException | IllegalArgumentException | IllegalAccessException |
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
    }

    private Description getActivityInfo() {
        Description activityInfo = new Description();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock(
                NbBundle.getMessage(ControlChecksum.class, "Control_Checksum_Description"));
        activityInfo.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        activityInfo.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        // flatFile - name
        ListItem flatFileItem = new ListItem();
        flatFileItem.setLabel(NbBundle.getMessage(ControlNumberOfRecords.class, "FlatFile"));
        if (flatFile.getName() != null) {
            flatFileItem.setContent(flatFile.getName());
        }
        descriptionList.getListItem().add(flatFileItem);
        ListItem fileNameItem = new ListItem();
        fileNameItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "FileName"));
        if (fileName != null) {
            fileNameItem.setContent(fileName);
        }
        descriptionList.getListItem().add(fileNameItem);

        // flatFile - checksum algorithm
        ListItem checksumAlgorithmItem = new ListItem();
        checksumAlgorithmItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "Algorithm"));
        if (algorithm != null) {
            checksumAlgorithmItem.setContent(algorithm);
        }
        descriptionList.getListItem().add(checksumAlgorithmItem);
        // flatFile - checksum value
        ListItem checksumValueItem = new ListItem();
        checksumValueItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "Checksum"));
        if (checksumValue != null) {
            checksumValueItem.setContent(checksumValue);
        }
        descriptionList.getListItem().add(checksumValueItem);
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

        // The file's computed checksum
        ResultItem computedChecksumResultItem = new ResultItem();
        computedChecksumResultItem.setType(ResultTypes.INFO_STRING);
        String computedChecksum = null;
        if (algorithm != null) {
            File f = new File(dataDirectory, fileName);
            try {
                computedChecksum = Utils.createFileChecksum(f, algorithm);
                computedChecksumResultItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "ComputedChecksum"));
                computedChecksumResultItem.setContent(computedChecksum);
            } catch (NoSuchAlgorithmException | IOException | NullPointerException ex) {
                computedChecksumResultItem.setType(ResultTypes.ERROR_STRING);
                computedChecksumResultItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "ChecksumNotComputed"));
                computedChecksumResultItem.setContent(ex.getMessage());
            }
        } else {
            computedChecksumResultItem.setType(ResultTypes.WARNING_STRING);
            computedChecksumResultItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "ChecksumNotComputed"));
            computedChecksumResultItem.setContent(NbBundle.getMessage(ControlChecksum.class, "MissingAlgorithm"));
        }
        resultItems1.getResultItem().add(computedChecksumResultItem);

        // Compare values
        if (checksumValue != null && computedChecksum != null) {
            ResultItem comparedChecksumsResultItem = new ResultItem();
            comparedChecksumsResultItem.setLabel(NbBundle.getMessage(ControlChecksum.class, "ComparedChecksums"));
            if (checksumValue.equalsIgnoreCase(computedChecksum)) {
                // Identical checksums
                comparedChecksumsResultItem.setType(ResultTypes.INFO_STRING);
                comparedChecksumsResultItem.setContent(NbBundle.getMessage(ControlChecksum.class, "IdenticalChecksums"));
            } else {
                // Different checksums
                comparedChecksumsResultItem.setType(ResultTypes.ERROR_STRING);
                comparedChecksumsResultItem.setContent(NbBundle.getMessage(ControlChecksum.class, "NotIdenticalChecksums"));
            }
            resultItems1.getResultItem().add(comparedChecksumsResultItem);
        }
        return result;
    }

}
