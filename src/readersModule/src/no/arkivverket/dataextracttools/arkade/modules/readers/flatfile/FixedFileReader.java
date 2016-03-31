/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FieldDefinition;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FixedFileFormat;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;

/**
 * Class for reading flat files where the field values are in fixed positions.
 *
 *
 * @version 0.06 2014-02-28
 * @author Riksarkivet
 */
public class FixedFileReader extends FlatFileReader {

    private static final boolean DUMP_DATA = false;
    private java.io.BufferedReader in;
    private int recordLength;
    private int totalRecordLength; // Med eventuelt postskille
    private int recordDefinitionFieldStartPos;
    private int recordDefinitionFieldEndPos;
    private char[] recordSeparator;
    // Counter for number of records read.
    // Used for information about totalt number of records in the file
    // and for giving each read record an id.
    private long recordCounter = 0;

    /**
     * 
     * @param id
     * @param flatFile
     * @param datasetDescription
     * @param charset
     * @param format
     * @param recordDefinitionFieldNumber
     * @param dataDirectory 
     */
    public FixedFileReader(String id, FlatFile flatFile,
            DatasetDescription datasetDescription,
            String charset, FixedFileFormat format, int recordDefinitionFieldNumber,
            File dataDirectory) {
        super(id, flatFile, datasetDescription, charset, format,
                recordDefinitionFieldNumber, dataDirectory);
    }

    /**
     *
     */
    @Override
    public void init() {
        super.init();
        FixedFileFormat fixedFileFormat = (FixedFileFormat) getFormat(); 

        if (fixedFileFormat.getRecordSeparator() != null) {
            if (fixedFileFormat.getRecordSeparator().equalsIgnoreCase("CR")) {
                recordSeparator = new char[1];
                recordSeparator[0] = '\r';
            } else if (fixedFileFormat.getRecordSeparator().equalsIgnoreCase("LF")) {
                recordSeparator = new char[1];
                recordSeparator[0] = '\n';
            } else if (fixedFileFormat.getRecordSeparator().equalsIgnoreCase("CRLF")) {
                recordSeparator = new char[2];
                recordSeparator[0] = '\r';
                recordSeparator[1] = '\n';
            }
        }

        ArrayList<RecordDefinition> recordDefinitionList =
                ((Addml_8_2) getDatasetDescription()).getRecordDefinitionList(getFlatFile());

        RecordDefinition firstRecordDefinition = recordDefinitionList.get(0);

        recordLength = (firstRecordDefinition.getFixedLength() != null)
                ? firstRecordDefinition.getFixedLength().intValue() : -1;

        if (recordLength == -1) {
            log(getFlatFile().getName() + ": Postlengden er ikke oppgitt.", Level.SEVERE);
        }
        
        totalRecordLength = recordLength;

        if (recordSeparator != null && recordSeparator.length > 0) {
            totalRecordLength += recordSeparator.length;
        }

        // TODO Verify this!
        if (getRecordDefinitionFieldNumber() > -1) {
            // Multiple record definitions - probably...

            RecordDefinition recordDefinition = firstRecordDefinition;
            FieldDefinition fieldDefinition =
                    recordDefinition.getFieldDefinitions().
                    getFieldDefinition().get(getRecordDefinitionFieldNumber());

            recordDefinitionFieldStartPos =
                    (fieldDefinition.getStartPos() != null)
                    ? fieldDefinition.getStartPos().intValue() - 1 : -1;

            if (fieldDefinition.getEndPos() != null) {
                recordDefinitionFieldEndPos =
                        (fieldDefinition.getEndPos() != null)
                        ? fieldDefinition.getEndPos().intValue() - 1 : -1;
            } else {
                int length =
                        (fieldDefinition.getFixedLength() != null)
                        ? fieldDefinition.getFixedLength().intValue() - 1 : -1;
                recordDefinitionFieldEndPos = recordDefinitionFieldStartPos + length;
            }
        }        
    }
    
    public int getRecordDefinitionFieldEndPos() {
        return recordDefinitionFieldEndPos;
    }

    public int getRecordDefinitionFieldStartPos() {
        return recordDefinitionFieldStartPos;
    }
    
    public int getRecordLength() {
        return recordLength;
    }

    @Override
    public void read() {
        log(getFile().getName() + " - Startet lesing");

        fireItemRead(new Item(getFile(), null));

        char[] record = new char[totalRecordLength];

        try {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(new FileInputStream(getFile()), getCharset());
            in = new BufferedReader(inputStreamReader);

            int numberOfChars;
            while (true) {
                numberOfChars = in.read(record, 0, totalRecordLength);

                if (numberOfChars == -1) {
                    break;
                }

                String r = new String(record, 0, recordLength);

                if (DUMP_DATA) {
                    log("DUMP (RECORD): " + recordCounter + ": " + r);
                }

                readRecord(r, ++recordCounter);
            }

            in.close();
        } catch (UnsupportedEncodingException e) {
            log(e.getMessage(), Level.SEVERE);
        } catch (FileNotFoundException e) {
            log(e.getMessage(), Level.SEVERE);
        } catch (IOException e) {
            log(e.getMessage(), Level.SEVERE);
        }

        fireItemRead(new Item(getFile(), null));

        log(getFile().getName() + " - Lesing ferdig");
        log(getFile().getName() + " - Antall poster lest: " + recordCounter);
    }

    @Override
    public void read(Item item) {
        throw new UnsupportedOperationException("Not supported.");
    }

    protected void readRecord(String record, long recordNumber) {

        RecordReader recordReader = null;

        if (getRecordReaders().size() == 1 && getRecordReaders().containsKey(null)) {
            // One record reader
            recordReader = getRecordReaders().get(null);
        } else if (!getRecordReaders().isEmpty()) {
            // More than one record reader
            String fieldValue = record.substring(
                    getRecordDefinitionFieldStartPos(), getRecordDefinitionFieldEndPos() + 1);
            recordReader = getRecordReaders().get(fieldValue);
        }
        
        if (recordReader == null) {
            log("Missing record reader for record: '" + record + "'", Level.SEVERE);
            return;
        }

        TreeMap<String, Object> parameters;
        parameters = new TreeMap<>();
        parameters.put("recordNumber", recordNumber);
        recordReader.read(new Item(record, parameters));
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }

    @Override
    protected void fireItemRead(Item item) {
        ArrayList<ReadListener> list = (ArrayList<ReadListener>) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }

    @Override
    public long getNumberOfRecordsRead() {
        return recordCounter;
    }
}
