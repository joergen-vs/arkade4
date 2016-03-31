/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.xml;

import java.util.ArrayList;
import java.util.logging.Level;
import no.arkivverket.dataextracttools.arkade.modules.exist.ExistUtils;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DataObject;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

/**
 *
 * @version 0.11 2014-03-21
 * @author Riksarkivet
 *
 */
public class XmlCollectionReader extends BasicReader {

    private final Collection parentCollection;
    private Collection collection;
    private final String collectionName;
    private final boolean isNewDatabase;
    private ArrayList<XmlDocumentReader> xmlDocumentReaders;

    public XmlCollectionReader(String id, DataObject dataObject, DatasetDescription datasetDescription,
            Collection parentCollection, String collectionType, boolean isNewDatabase) {
        super(id, dataObject, datasetDescription);
        this.parentCollection = parentCollection;
        this.isNewDatabase = isNewDatabase;
        collectionName = dataObject.getName();
    }

    @Override
    public void init() {
        if (isNewDatabase) {
            try {
                collection = ExistUtils.createCollection(parentCollection, collectionName);
            } catch (XMLDBException ex) {
                log(ex.getMessage(), Level.SEVERE);
            }
        }
    }

    public ArrayList<XmlDocumentReader> getXmlDocumentReaders() {
        return xmlDocumentReaders;
    }

    public Collection getCollection() {
        return collection;
    }

    public void addXmlDocumentReader(XmlDocumentReader reader) {
        if (xmlDocumentReaders == null) {
            xmlDocumentReaders = new ArrayList<>();
        }
        xmlDocumentReaders.add(reader);
    }

    @Override
    public void read() {
        log("Leser filer inn i samlingen " + collectionName + ":");
        if (parentCollection != null) {

            fireItemRead(new Item(collectionName, null));

            for (BasicReader xmlDocumentReader : xmlDocumentReaders) {
                xmlDocumentReader.read();
            }
        }

        fireItemRead(new Item(collectionName, null));
    }

    protected void fireItemRead(Item item) {
        ArrayList list = (ArrayList) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = (ReadListener) list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
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
