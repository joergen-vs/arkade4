/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.reports.session;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Activity;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Addml;
import no.arkivverket.dataextracttools.utils.xml.XMLUtils;

/**
 * A class for creating a report with results from a session's activities. The
 * report's structure is based on the xml schema sessionReport.xsd and the
 * sessionReport classes in the bind package.
 *
 * @version 0.08 2014-04-24
 * @author Riksarkivet
 */
public class SessionReport {

    private static final String DEFAULT_REPORT_NAME = "sessionReport.xml";
    private static final String DEFAULT_NAMESPACE = "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
    private static final String ADDML_NAMESPACE = "http://www.arkivverket.no/standarder/addml";
    private final String sessionId;
    private final String reportName;
    private String timeCreated;
    private String createdBy;
    private final File reportFile;
    private final Addml_8_2 datasetDescription;
    private File reportDirectory;
    private final Logger logger;
    private PrintWriter printWriter;
    private XMLStreamWriter writer;
    private Marshaller m;
    private boolean canWrite;

    /**
     *
     * @param sessionId
     * @param datasetDescription
     * @param reportDirectory
     */
    public SessionReport(String sessionId, Addml_8_2 datasetDescription,
            File reportDirectory) {
        this.sessionId = sessionId;
        this.datasetDescription = datasetDescription;
        this.reportDirectory = reportDirectory;
        this.reportName = DEFAULT_REPORT_NAME;
        this.reportFile = new File(reportDirectory, reportName);
        logger = Logger.getLogger(sessionId);
    }

    /**
     *
     * @return
     */
    public File getReportDirectory() {
        return reportDirectory;
    }

    /**
     *
     * @param timeCreated
     */
    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    /**
     *
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * open() shall only be called once in the lifetime of the report. Does
     * nothing and returns with no notification if it is called more than once.
     */
    public void open() {
        if (canWrite) {
            return;
        }
        try {
            BufferedWriter bufferedWriter;
            canWrite = true;

            JAXBContext jc = JAXBContext.newInstance("no.arkivverket.dataextracttools.arkade.modules.reports.session.bind");
            m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);

            JAXBContext addmlJaxbContext = JAXBContext.newInstance("no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind");
            Marshaller addmlMarshaller = addmlJaxbContext.createMarshaller();
            addmlMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(reportFile), "UTF-8"));
            printWriter = new PrintWriter(bufferedWriter);
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            writer = outputFactory.createXMLStreamWriter(printWriter);
            writer.setDefaultNamespace(DEFAULT_NAMESPACE);

            writer.writeStartDocument("utf-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement(DEFAULT_NAMESPACE, "sessionReport");
            writer.writeNamespace("", DEFAULT_NAMESPACE);
            writer.writeNamespace("aml", ADDML_NAMESPACE);
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            writer.writeAttribute("xsi:schemaLocation",
                    DEFAULT_NAMESPACE + " sessionReport.xsd " + ADDML_NAMESPACE + " addml.xsd");

            // sessionInformation
            writer.writeStartElement(DEFAULT_NAMESPACE, "sessionInformation");
            writer.writeStartElement(DEFAULT_NAMESPACE, "identifiers");
            writer.writeStartElement(DEFAULT_NAMESPACE, "identifier");
            writer.writeAttribute("name", "id");
            writer.writeAttribute("value", sessionId);
            writer.writeEndElement(); // identifier
            writer.writeEndElement(); // identifiers

            writer.writeStartElement(DEFAULT_NAMESPACE, "timeCreated");
            if (timeCreated == null) {
                try {
                    timeCreated = XMLUtils.createTimeStamp().toXMLFormat();
                } catch (DatatypeConfigurationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            writer.writeCharacters(timeCreated);
            writer.writeEndElement(); // timeCreated
            if (createdBy != null && !createdBy.equals("")) {
                writer.writeStartElement(DEFAULT_NAMESPACE, "createdBy");
                writer.writeCharacters(createdBy);
                writer.writeEndElement(); // createdBy
            }
            writer.writeStartElement(DEFAULT_NAMESPACE, "application");
            writer.writeStartElement(DEFAULT_NAMESPACE, "name");
            writer.writeCharacters(System.getProperty("application.name"));
            writer.writeEndElement(); // name
            writer.writeStartElement(DEFAULT_NAMESPACE, "version");
            writer.writeCharacters(System.getProperty("application.version"));
            writer.writeEndElement(); // version

            writer.writeEndElement(); // application
            writer.writeEndElement(); // sessionInformation

            // datasetDescription
            writer.writeStartElement(DEFAULT_NAMESPACE, "datasetDescription");
            writer.writeCharacters("\n");
            writer.setPrefix("aml", ADDML_NAMESPACE);
            addmlMarshaller.marshal((Addml) datasetDescription.getRootElement(), writer);
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeStartElement(DEFAULT_NAMESPACE, "activities");
            writer.writeCharacters("\n");
            writer.flush();
        } catch (JAXBException | XMLStreamException |
                FileNotFoundException | UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * close() shall only be called once in the lifetime of the report. Does
     * nothing and returns with no notification if it is called more than once.
     */
    public void close() {
        if (!canWrite) {
            return;
        }
        try {
            writer.writeCharacters("\n");
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            printWriter.close();
            canWrite = false;
        } catch (XMLStreamException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes the activity to the report file.
     * @param activity the activity to be written
     */
    public void write(Activity activity) {
        try {
            writer.writeCharacters("\n");
            m.marshal(activity, writer);
            writer.flush();
        } catch (XMLStreamException | JAXBException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
