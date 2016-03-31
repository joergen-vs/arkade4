/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.util.ArrayList;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;

/**
 *
 * @version 0.06 2014-02-28
 * @author Riksarkivet
 */
public abstract class RecordReader extends BasicReader {

    protected ArrayList<FieldReader> fieldReaders;

    /**
     * 
     * @param id
     * @param recordDefinition
     * @param datasetDescription 
     */
    public RecordReader(String id, RecordDefinition recordDefinition,
            DatasetDescription datasetDescription) {
        super(id, recordDefinition, datasetDescription);
    }

    public RecordDefinition getRecordDefinition() {
        return (RecordDefinition) getElement();
    }

    public ArrayList<FieldReader> getFieldReaders() {
        return fieldReaders;
    }

    public void addFieldReader(FieldReader reader) {
        if (fieldReaders == null) {
            fieldReaders = new ArrayList<>();
        }
        fieldReaders.add(reader);
    }

    @Override
    public abstract void read(Item item);

    protected void fireItemRead(Item item) {
        ArrayList<ReadListener> list = (ArrayList<ReadListener>) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }
    
}
