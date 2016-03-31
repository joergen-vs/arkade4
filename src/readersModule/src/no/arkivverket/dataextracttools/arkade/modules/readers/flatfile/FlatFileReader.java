/*
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import no.arkivverket.dataextracttools.arkade.modules.readers.BasicReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadListener;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.Addml_8_2_Helper;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;


/**
 * @version 0.11 2014-02-28
 * @author Riksarkivet
 *
 */
public abstract class FlatFileReader extends BasicReader {

    private final String charset;
    private final Object format;
    private final File dataDirectory;
    private File file;

    // Angivelse av feltet som definerer hver RecordDefinition.
    // Er -1 hvis det kun er en RecordDefinition.
    private int recordDefinitionFieldNumber = -1;
    private Map<String, RecordReader> recordReaders;

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
    public FlatFileReader(String id, FlatFile flatFile,
            DatasetDescription datasetDescription,
            String charset, Object format, int recordDefinitionFieldNumber,
            File dataDirectory) {
        super(id, flatFile, datasetDescription);
        this.charset = charset;
        this.format = format;
        this.recordDefinitionFieldNumber = recordDefinitionFieldNumber;
        this.dataDirectory = dataDirectory;

    }

    @Override
    public void init() {
        String fileName = Addml_8_2_Helper.getFileName(getFlatFile());
        if (fileName == null) {
            log(getId() + " - Filnavn mangler!", Level.SEVERE);
        } else {
            File f = new File(fileName);
            file = new File(dataDirectory.getAbsolutePath(), f.getName());
        }                
    }
    
    public String getCharset() {
        return charset;
    }

    public FlatFile getFlatFile() {
        return (FlatFile) getElement();
    }

    public File getFile() {
        return file;
    }

    public Object getFormat() {
        return format;
    }

    public int getRecordDefinitionFieldNumber() {
        return recordDefinitionFieldNumber;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    public abstract long getNumberOfRecordsRead();

    public Map<String, RecordReader> getRecordReaders() {
        return recordReaders;
    }

    /**
     * 
     * @param recordDefinitionFieldValue 
     *        Navnet som identifiserer postleseren som skal legges til i 
     *        flat-filleseren. 
     *        <code>recordDefinitionFieldValue</code> kan være null hvis det kun
     *        er en postleser. 
     * @param reader Postleseren som skal legges til flat-filleseren.
     */
    public void addRecordReader(String recordDefinitionFieldValue,
            RecordReader reader) {

        if (recordReaders == null) {
            recordReaders = new HashMap<>();
        }
        recordReaders.put(recordDefinitionFieldValue, reader);

    }

    protected void fireItemRead(Item item) {
        ArrayList<ReadListener> list = (ArrayList<ReadListener>) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }
}
