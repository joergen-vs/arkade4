/*
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.util.ArrayList;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;

/**
 * 
 * Two itemRead per record. 
 * First not null (beginning). Second null (end).
 *
 * @version 0.14 2014-02-28
 * @author Riksarkivet
 *
 */
public class FixedRecordReader extends RecordReader {

    /**
     * 
     * @param id
     * @param recordDefinition
     * @param datasetDescription 
     */
    public FixedRecordReader(String id, RecordDefinition recordDefinition,
            DatasetDescription datasetDescription) {
        super(id, recordDefinition, datasetDescription);
    }

    @Override
    public void init() {
    }
    
    @Override
    public void read() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void read(Item item) {

        fireItemRead(item);

        if (item != null && item.getValue() != null) {
            // Første felt skal starte i pos 1 i datasettbeskrivelsen.
            int startPos = 0;
            int endPos = 0;
            int length;

            String recordValue = (String) item.getValue();

            ArrayList<FieldDefinition> fieldDefinitionList =
                    (ArrayList<FieldDefinition>) getRecordDefinition().getFieldDefinitions().getFieldDefinition();

            for (int i = 0; i < fieldDefinitionList.size(); i++) {
                FieldDefinition fieldDefinition = fieldDefinitionList.get(i);

                if (fieldDefinition.getStartPos() != null) {
                    // -1 pga. pos 1, ikke 0, som utgangspunkt
                    startPos = fieldDefinition.getStartPos().intValue() - 1;
                }
                if (fieldDefinition.getEndPos() != null) {
                    // -1 pga. pos 1, ikke 0, som utgangspunkt
                    endPos = fieldDefinition.getEndPos().intValue() - 1;
                }
                // fixedLength overstyrer endPos
                // TODO Kontrollere samsvar hvis begge
                if (fieldDefinition.getFixedLength() != null) {
                    length = fieldDefinition.getFixedLength().intValue();
                    endPos = startPos + length - 1;
                }

                // Sjekke om lengden på postverdien er kortere
                // enn feltets sluttposisjon.
                // OBS! Første tegn i posten kommer i posisjon 0.
                // TODO Hva hvis siste feltverdi ikke er paddet?
                if (recordValue.length() <= endPos) {
                    break;
                }

                Item field = new Item(recordValue.substring(startPos, endPos + 1),
                        item.getParameters());

                fieldReaders.get(i).read(field);
            }

            fireItemRead(null);
        }

    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }
}
