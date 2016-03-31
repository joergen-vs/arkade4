/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.processes;

import java.io.StringReader;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Activity;
import no.arkivverket.dataextracttools.arkade.modules.reports.session.bind.Identifiers;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;

/**
 *
 * @version 0.12 2014-03-21
 * @author Riksarkivet
 */
public abstract class BasicDataExtractProcess implements DataExtractProcess, ReadListener {

    protected boolean debug = false;
    protected String id;
    protected String name;
    protected String longName;
    protected String orderKey; // The value the test result can be sorted by
    protected String structuralId; // A link between the element in the dataset description and the process
    protected String description = "";
    protected String resultDescription = "";
    protected DatasetDescription datasetDescription;
    protected int maxNumberOfResults = Integer.MAX_VALUE;
    protected boolean finished = false;
    protected Activity activity; // sessionReport
    protected String loggerName;

    /**
     *
     * @param id
     * @param name
     * @param longName
     */
    public BasicDataExtractProcess(String id, String name, String longName) {
        this.id = id;
        this.name = name;
        this.longName = longName;

        activity = new Activity();
        Identifiers identifiers = new Identifiers();
        Identifiers.Identifier idIdentifier = new Identifiers.Identifier();
        idIdentifier.setName("id");
        idIdentifier.setValue(id);
        identifiers.getIdentifier().add(idIdentifier);
        Identifiers.Identifier uuidIdentifier = new Identifiers.Identifier();
        uuidIdentifier.setName("uuid");
        uuidIdentifier.setValue(UUID.randomUUID().toString());
        identifiers.getIdentifier().add(uuidIdentifier);
        activity.setIdentifiers(identifiers);
        activity.setName(name);
        activity.setLongName(longName);
        loggerName = Logger.GLOBAL_LOGGER_NAME;
    }

    @Override
    public void init() {
        debug = Boolean.parseBoolean(System.getProperty("arkade.session.debug", "false"));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLongName() {
        return longName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getResultDescription() {
        return resultDescription;
    }

    @Override
    public void setResultDescription(String resultDescription) {
        this.resultDescription = resultDescription;
    }

    @Override
    public DatasetDescription getDatasetDescription() {
        return datasetDescription;
    }

    @Override
    public void setMaxNumberOfResults(int maxNumberOfResults) {
        this.maxNumberOfResults = maxNumberOfResults;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public void setOrderKey(String orderKey) {
        this.orderKey = orderKey;
        if (activity != null) {
            activity.setOrderKey(orderKey);
        }
    }

    public String getStructuralId() {
        return structuralId;
    }

    /**
     * Sets the id for identifying the process' relation to an element in the
     * dataset description. The id can be a path to the element in the dataset
     * description. Can only be set once. 
     * Nothing happens if setStructuralId() is called more than once.
     *
     * @param structuralId the id identifying the element in the dataset
     * description
     */
    public void setStructuralId(String structuralId) {
        if (this.structuralId != null) {
            return;
        }
        this.structuralId = structuralId;
        if (activity != null) {
            Identifiers identifiers = activity.getIdentifiers();
            Identifiers.Identifier identifier = new Identifiers.Identifier();
            identifier.setName("structuralId");
            identifier.setValue(structuralId);
            identifiers.getIdentifier().add(identifier);
        }
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public void log(String message) {
        log(message, Level.INFO);
    }

    public void log(String message, Level level) {
        Logger logger;
        Level l;

        if (loggerName != null) {
            logger = Logger.getLogger(loggerName);
        } else {
            logger = Logger.getGlobal();
        }
        if (level != null) {
            l = level;
        } else {
            l = Level.INFO;
        }
        if (message != null) {
            logger.log(l, message);
        } else {
            logger.log(l, logger.getName());
        }
    }

    protected void createActivity(String activityString) {
        try {
            JAXBContext jc = JAXBContext.newInstance("no.arkivverket.dataextracttools.arkade.modules.reports.session.bind");
            Unmarshaller u = jc.createUnmarshaller();
            Object o = u.unmarshal(new StreamSource(new StringReader(activityString)));

            if (o instanceof Activity) {
                activity = (Activity) o;
            }
        } catch (JAXBException ex) {
            log(ex.getMessage(), Level.SEVERE);
        }

    }

    @Override
    public String toString() {
        return getLongName() + ": " + getId();
    }
}
