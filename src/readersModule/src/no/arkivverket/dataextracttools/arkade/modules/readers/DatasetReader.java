/**
 * The National Archives of Norway - 2014
 *
 */
package no.arkivverket.dataextracttools.arkade.modules.readers;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import no.arkivverket.dataextracttools.arkade.modules.readers.flatfile.FlatFileReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.xml.XmlCollectionReader;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.Dataset;

/**
 *
 * @version 0.13 2014-02-28
 * @author Riksarkivet
 *
 */
public class DatasetReader extends BasicReader {

    private final File dataDirectory;
    private ArrayList<FlatFileReader> flatFileReaders;
    private ArrayList<XmlCollectionReader> xmlCollectionReaders;

    /**
     *
     * @param id
     * @param dataset
     * @param datasetDescription
     * @param dataDirectory
     */
    public DatasetReader(String id, Dataset dataset, DatasetDescription datasetDescription,
            File dataDirectory) {
        super(id, dataset, datasetDescription);
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void init() {
        
    }
    
    public File getDataDirectory() {
        return dataDirectory;
    }

    public Dataset getDataset() {
        return (Dataset) getElement();
    }

    public ArrayList<FlatFileReader> getFlatFileReaders() {
        return flatFileReaders;
    }

    public void addFlatFileReader(FlatFileReader reader) {
        if (flatFileReaders == null) {
            flatFileReaders = new ArrayList<>();
        }
        flatFileReaders.add(reader);
    }

    public ArrayList<XmlCollectionReader> getXmlCollectionReaders() {
        return xmlCollectionReaders;
    }

    public void addXmlCollectionReader(XmlCollectionReader reader) {
        if (xmlCollectionReaders == null) {
            xmlCollectionReaders = new ArrayList<>();
        }
        xmlCollectionReaders.add(reader);
    }

    @Override
    public void read() {

        log("Leser datasettet");

        fireItemRead(null);

        // flatFiles
        if (flatFileReaders != null) {
            for (FlatFileReader flatFileReader : flatFileReaders) {
                if (flatFileReader.getFile() != null) {
                    flatFileReader.read();
                } else {
                    log("Får ikke lest fil. Flat fil mangler i filleser. "
                            + (flatFileReader.getFlatFile() != null
                            ? "Gjelder '" + flatFileReader.getFlatFile().getName() + "'"
                            : "Det er ikke angitt noe navn på den flate filen."), Level.SEVERE);
                }
            }
        }

        // dataObjects
        if (xmlCollectionReaders != null) {
            for (XmlCollectionReader xmlCollectionReader : xmlCollectionReaders) {
                xmlCollectionReader.read();
            }
        }

        fireItemRead(null);

        log("Ferdig med å lese datasettet");
    }

    @Override
    public void read(Item item) {
        throw new UnsupportedOperationException("Not supported.");
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }

    protected void fireItemRead(Item item) {
        ArrayList<ReadListener> list = (ArrayList<ReadListener>) readListeners.clone();

        for (int i = 0; i < list.size(); i++) {
            ReadListener readListener = list.get(i);
            readListener.itemRead(new ReadEvent(this, item));
        }
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + dataDirectory.getAbsolutePath();
    }
}
