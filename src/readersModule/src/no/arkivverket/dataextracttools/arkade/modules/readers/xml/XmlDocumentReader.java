/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.xml;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import no.arkivverket.dataextracttools.arkade.modules.exist.ExistUtils;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2_Helper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DataObject;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Properties;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Property;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

/**
 *
 * @version 0.17 2014-03-21
 * @author Riksarkivet
 *
 */
public class XmlDocumentReader extends BasicReader {

    private final File dataDirectory;
    private final Collection collection;
    private File documentFile;
    private ArrayList<File> schemaFiles;
    private File mainSchemaFile;
    private final boolean storeXmlDocuments;
    // Angir om det som er nødvendig for å lese, er på plass.
    private final boolean canRead = true;

    /**
     *
     * @param id
     * @param dataObject
     * @param datasetDescription
     * @param dataDirectory
     * @param collection
     * @param storeXmlDocuments
     */
    public XmlDocumentReader(String id, DataObject dataObject,
            DatasetDescription datasetDescription, File dataDirectory, Collection collection,
            boolean storeXmlDocuments) {
        super(id, dataObject, datasetDescription);
        this.dataDirectory = dataDirectory;
        this.collection = collection;
        this.storeXmlDocuments = storeXmlDocuments;
    }

    @Override
    public void init() {
        // Dataobjekt for XML-dokumentet
        DataObject documentDataObject
                = (DataObject) getDatasetDescriptionElement().getElement();

        // Feil hvis documentDataObject er null
        if (documentDataObject == null) {
            log("dataObject mangler i XmlDocumentReader."
                    + "\nKan ikke lese XML-dokument.", Level.SEVERE);
            return;
        }

        Properties properties = documentDataObject.getProperties();

        // Feil hvis properties er null
        if (properties == null) {
            log("properties mangler i dataObject (XMLDocumentReader)", Level.SEVERE);
            return;
        }

        schemaFiles = new ArrayList<>();

        for (Property property : properties.getProperty()) {
            String name = property.getName();

            if ("file".equals(name)) {
                Property nameProperty;
                try {
                    nameProperty = Addml_8_2_Helper.getProperty(property, "name");
                    String fileName = nameProperty != null ? nameProperty.getValue() : null;
                    if (fileName != null) {
                        documentFile = new File(dataDirectory.getAbsolutePath(), fileName);
                    }
                } catch (IllegalArgumentException | IllegalAccessException |
                        InvocationTargetException | NoSuchMethodException ex) {
                    log(ex.getMessage(), Level.SEVERE);
                }
            } else if ("schema".equals(name)) {
                Properties schemaProperties = property.getProperties();
                if (schemaProperties == null) {
                    break;
                }
                Property schemaFileProperty = Addml_8_2_Helper.getProperty(
                        schemaProperties, "file");
                if (schemaFileProperty != null) {
                    // Opprett File-objekt
                    Property nameProperty;
                    try {
                        nameProperty = Addml_8_2_Helper.getProperty(schemaFileProperty, "name");
                        String fileName = nameProperty != null ? nameProperty.getValue() : null;
                        if (fileName != null) {
                            File file = new File(dataDirectory.getAbsolutePath(), fileName);
                            // Hovedskjemaet?
                            String schemaPropertyValue = property.getValue();
                            if (schemaPropertyValue != null && schemaPropertyValue.equals("main")) {
                                mainSchemaFile = file;
                            }
                            schemaFiles.add(file);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException |
                            InvocationTargetException | NoSuchMethodException ex) {
                        log(ex.getMessage(), Level.SEVERE);
                    }

                }

            } else if ("info".equals(name)) {
            } else if ("keys".equals(name)) {
            }
        }

        // Sjekke om nødvendige ting er på plass
        if (documentFile == null) {
            log("XML-dokumentfil mangler for " + getId() + "!", Level.SEVERE);
        }
    }

    public File getDocumentFile() {
        return documentFile;
    }

    public File getMainSchemaFile() {
        return mainSchemaFile;
    }

    @Override
    public void read() {
        boolean ok;
        log(getId());

        // Prosesser som ikke bruker eXist, kan utføres
        // selv om innlesing i eXist droppes.
        // Det går også an å utføre prosesser på allerede innleste
        // XML-dokumenter.
        // Avbryte hvis noe mangler for å lese
        if (!canRead) {
            log("Kan ikke prosessere XML-dokumentfilen '"
                    + (documentFile != null ? documentFile.getName() : "[MANGLER]")
                    + "'! (ID: " + getId() + ")", Level.SEVERE);
            return;
        }

        fireItemRead(new Item(documentFile.getName(), null));

        if (!storeXmlDocuments) {
            log("Innlesing ikke angitt");
            if (mainSchemaFile != null) {
                log("Skjemafil " + mainSchemaFile.getName() + " ikke lest inn");
            }
            if (documentFile != null) {
                log("Datafil " + documentFile.getName() + " ikke lest inn");
            }
        }

        if (storeXmlDocuments && mainSchemaFile != null) {
            // Read schema file
            for (File file : schemaFiles) {
                log("Leser skjemafil " + file.getName());
                ok = readSchemaFile(file);
                if (ok) {
                    log("OK");
                } else {
                    log("En feil oppstod ved innlesing av " + file.getName(), Level.SEVERE);
                }
            }
        }

        if (storeXmlDocuments && documentFile != null) {
            // Read document file
            log("Leser XML-dokumentfil (datafil) " + documentFile.getName());
            ok = readDocumentFile();
            if (ok) {
                log("OK");
            } else {
                log("En feil oppstod ved innlesing av " + documentFile.getName(), Level.SEVERE);
            }
        }

        fireItemRead(new Item(documentFile.getName(), null));
    }

    protected void fireItemRead(Item item) {
        ArrayList list = (ArrayList) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = (ReadListener) list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }

    private boolean readSchemaFile(File file) {
        boolean stored = false;
        try {
            ExistUtils.clearGrammarCache(collection);
            ExistUtils.storeDocument(file, collection);
            stored = true;
        } catch (UnsupportedEncodingException ex) {
            log(ex.getMessage(), Level.SEVERE);
        } catch (IOException | XMLDBException ex) {
            log(ex.getMessage(), Level.SEVERE);
        }
        return stored;
    }

    private boolean readDocumentFile() {
        boolean stored = false;
        try {
            ExistUtils.storeDocument(documentFile, collection);
            stored = true;
        } catch (XMLDBException ex) {
            log(ex.getMessage(), Level.SEVERE);
        }
        return stored;
    }

    @Override
    public void read(Item item) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void itemRead(ReadEvent readEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
