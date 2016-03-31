/*
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.util.ArrayList;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldType;

/**
 * @version 0.13 2014-02-28
 * @author Riksarkivet
 *
 */
public class FieldReader extends BasicReader {

    // DEBUG
    private static final boolean DUMP_DATA = false;
    FieldType fieldType;

    public FieldReader(String id, FieldDefinition fieldDefinition,
            DatasetDescription datasetDescription, FieldType fieldType) {
        super(id, fieldDefinition, datasetDescription);
        this.fieldType = fieldType;
    }

    @Override
    public void init() {
    }

    public FieldDefinition getFieldDefinition() {
        return (FieldDefinition) getElement();
    }

    @Override
    public void read() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void read(Item item) {
        if (DUMP_DATA) {
            System.out.println(getFieldDefinition().getName() + ": " + item.getValue());
        }
        fireItemRead(item);
    }

    protected void fireItemRead(Item item) {
        ArrayList<ReadListener> list = (ArrayList<ReadListener>) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }

}
