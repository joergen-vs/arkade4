/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.processes.xml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import no.arkivverket.dataextracttools.arkade.modules.processes.BasicDataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Description;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.DescriptionList;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ListItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Result;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItem;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ResultItems;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 *
 *
 *
 * @version 0.08 2014-02-28
 * @author Riksarkivet
 *
 */
public class ReportingXmlValidator extends BasicDataExtractProcess {

    private static final boolean DEBUG = false;
    private static final String ERROR_DESCRIPTION_XML_VALIDATION
            = "Eventuelle feilmeldinger som blir vist i tabellen, kommer fra parseren som har validert XML-filen. "
            + "Disse feilmeldingene sier som oftest noe om hva som er avvik i forhold til skjema som definerer den aktuelle XML-strukturen. "
            + "De vanligste feilene går på manglende forekomster av obligatoriske elementer, flere forekomster av elementer som "
            + " kan forekomme maks en gang, feil rekkefølge på forekomster av elementer og feil datatype i forhold til hva som er definert "
            + "i aktuelle skjema. "
            + "I tillegg til selve feilmeldingen, vises også plasseringen i XML-filen hvor feilen er. "
            + "Det forekommer at det samme avviket blir rapportert to ganger, men da ut i fra forskjellige valideringsregler.";

    private boolean start = true;
    private boolean canValidate = true;
    private final File documentFile;
    private final File schemaFile;
    private Schema schema;
    private Validator validator;
    private ReportingErrorHandler errorHandler;

    /**
     *
     * @param id
     * @param name
     * @param longName
     * @param documentFile
     * @param schemaFile
     * @param maxNumberOfResults -1 means to use the default value in
     * ReportingXmlValidator which is Integer.MAX_VALUE.
     */
    public ReportingXmlValidator(String id, String name, String longName,
            File documentFile, File schemaFile,
            int maxNumberOfResults) {
        super(id, name, longName);
        this.documentFile = documentFile;
        this.schemaFile = schemaFile;
        if (maxNumberOfResults >= 0) {
            this.maxNumberOfResults = maxNumberOfResults;
        }
    }

    @Override
    public void init() {
        no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory objectFactory
                = new no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.ObjectFactory();
        Description activityDescription = objectFactory.createDescription();
        JAXBElement<String> textBlock = objectFactory.createDescriptionTextBlock("Validering av XML-fil mot tilhørende XML-skjema.");
        activityDescription.getContent().add(textBlock);
        DescriptionList descriptionList = new DescriptionList();
        ListItem listItem1 = new ListItem();
        listItem1.setLabel("XML-fil:");
        listItem1.setContent(documentFile.getName());
        descriptionList.getListItem().add(listItem1);
        ListItem listItem2 = new ListItem();
        listItem2.setLabel("Skjemafil:");
        listItem2.setContent(schemaFile.getName());
        descriptionList.getListItem().add(listItem2);
        activityDescription.getContent().add(objectFactory.createDescriptionDescriptionList(descriptionList));
        activity.setDescription(activityDescription);
        setResultDescription(ERROR_DESCRIPTION_XML_VALIDATION);
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(schemaFile);
            validator = schema.newValidator();
            Result result = new Result();
            result.setResultItems(new ResultItems());
            activity.setResult(result);
            errorHandler = new ReportingErrorHandler(this.maxNumberOfResults);
            validator.setErrorHandler(errorHandler);
        } catch (SAXException ex) {
            canValidate = false;
        }
    }

    public int getMaxNumberOfResults() {
        return maxNumberOfResults;
    }

    public ReportingErrorHandler getErrorHandler() {
        return errorHandler;
    }

    private void validate() {
        try {
            if (DEBUG) {
                Logger.getLogger(loggerName).info(
                        "Validerer '" + documentFile.getName() + "' mot '"
                        + schemaFile.getName() + "'");
            }

            SAXSource source = new SAXSource(
                    new InputSource(new java.io.FileInputStream(documentFile)));
            // validating the SAX source against the schema
            validator.validate(source);

            if (getDescription() != null && activity.getDescription() == null) {
                Description activityDescription = new Description();
                activityDescription.getContent().add(getDescription());
                activity.setDescription(activityDescription);
            }

            Result result = activity.getResult();
            if (getResultDescription() != null) {
                Description d = new Description();
                d.getContent().add(getResultDescription());
                result.setDescription(d);
            }

            ResultItem exceptionsResultItem = errorHandler.getExceptionsResultItem();

            long numberOfExceptionResultItems = (exceptionsResultItem != null && exceptionsResultItem.getResultItems() != null
                    ? exceptionsResultItem.getResultItems().getResultItem().size() : 0);

            long numberOfResults = errorHandler.getNumberOfResults();
            long numberOfWarnings = errorHandler.getNumberOfWarnings();
            long numberOfErrors = errorHandler.getNumberOfErrors();

            ResultItem totalsResultItem = new ResultItem();
            totalsResultItem.setName("overordnetResultat");
            ResultItems totalsResultItems = new ResultItems();
            totalsResultItem.setResultItems(totalsResultItems);

            // Number of warnings
            ResultItem numberOfWarningsResultItem = new ResultItem();
            numberOfWarningsResultItem.setName("antallAdvarsler");
            numberOfWarningsResultItem.setType(numberOfWarnings == 0 ? "info" : "warning");
            numberOfWarningsResultItem.setLabel("Antall advarsler");
            numberOfWarningsResultItem.setContent("" + numberOfWarnings);
            totalsResultItems.getResultItem().add(numberOfWarningsResultItem);

            // Number of errors
            ResultItem numberOfErrorsResultItem = new ResultItem();
            numberOfErrorsResultItem.setName("antallFeil");
            numberOfErrorsResultItem.setType(numberOfErrors == 0 ? "info" : "error");
            numberOfErrorsResultItem.setLabel("Antall feil");
            numberOfErrorsResultItem.setContent("" + numberOfErrors);
            totalsResultItems.getResultItem().add(numberOfErrorsResultItem);

            result.getResultItems().getResultItem().add(totalsResultItem);

            if (numberOfExceptionResultItems > 0) {
                StringBuilder label = new StringBuilder();
                label.append("Valideringsmeldinger. Viser ");
                label.append(numberOfExceptionResultItems);
                if (maxNumberOfResults >= numberOfResults) {
                    label.append(" (alle).");
                } else {
                    label.append(" av ").append(numberOfResults).append(".");
                }
                exceptionsResultItem.setLabel(label.toString());

                ResultItem detailsResultItem = new ResultItem();
                detailsResultItem.setName("detaljertResultat");
                ResultItems detailsResultItems = new ResultItems();
                detailsResultItem.setResultItems(detailsResultItems);
                detailsResultItems.getResultItem().add(exceptionsResultItem);

                result.getResultItems().getResultItem().add(detailsResultItem);
            }

        } catch (SAXException | IOException ex) {
        }
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
        // Denne kalles to ganger - i starten og slutten på
        // read i XMLDocumentReader.
        // Selve prosesseringen utføres på slutten av read.
        if (start) {
            // 1. gang
            start = false;
        } else {
            // 2. gang
            Logger.getLogger(loggerName).info("Utfører prosess: " + getName());
            if (canValidate) {
                Logger.getLogger(loggerName).info(" - Validerer '" + documentFile.getName() + "' mot '"
                        + schemaFile.getName() + "'");
                validate();
            } else {
                Logger.getLogger(loggerName).severe(" - Kan ikke validere '" + documentFile.getName() + "' mot '"
                        + schemaFile.getName() + "'");
            }
        }
    }

    // Processor
    @Override
    public void finish() {
        if (!finished) {
            finished = true;
        }
    }
}
