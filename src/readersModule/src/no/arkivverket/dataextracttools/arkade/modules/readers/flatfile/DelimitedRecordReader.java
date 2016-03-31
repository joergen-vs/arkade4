/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.readers.flatfile;

import java.util.ArrayList;
import java.util.TreeMap;
import no.arkivverket.dataextracttools.arkade.modules.readers.Item;
import no.arkivverket.dataextracttools.arkade.modules.readers.ReadEvent;
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DelimFileFormat;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.RecordDefinition;

/**
 * Klasse for å lese en tegnseparert post. Innholdet i posten blir delt opp i
 * henhold til de feltleserne som er registrert i
 * <code>DelimRecordReader</code>-objektet. Se superklassen
 * <code>RecordReader</code> for informasjon om registrering av feltlesere.
 * Feltleserne følger de feltdefinisjonene (fieldDefinitions) som tilhører
 * postleserens postdefinisjon (recordDefinition). Hver del av postinnholdet
 * blir sendt videre til respektiv feltleser. <br/ ><br/ > Eksempler på
 * postinnhold som kan leses av
 * <code>DelimRecordReader</code>: <ul> <li>a;1;b</li> <li>a;1;b;</li>
 * <li>"a";1;"b"</li> <li>"a";1;"b";</li> </ul>
 *
 * Two itemRead per record. 
 * First not null (beginning). Second null (end).
 *
 * @version 0.14 2014-02-28
 * @author Riksarkivet
 *
 */
public class DelimitedRecordReader extends RecordReader {

    private static final boolean DEBUG = false;
    private char fieldSeparatingChar = 0;
    private char quotingChar = 0;

    public DelimitedRecordReader(String id, RecordDefinition recordDefinition,
            DatasetDescription datasetDescription, DelimFileFormat format) {
        super(id, recordDefinition, datasetDescription);

        if (format.getFieldSeparatingChar() != null) {
            fieldSeparatingChar =
                    format.getFieldSeparatingChar().charAt(0);
        }

        if (format.getQuotingChar() != null) {
            quotingChar = format.getQuotingChar().charAt(0);
        }
    }

    @Override
    public void init() {
        
    }
    
    @Override
    public void read() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * 
     *
     * @param item
     */
    @Override
    public void read(Item item) {
        fireItemRead(item);

        if (item != null && item.getValue() != null) {
            String recordValue = (String) item.getValue();
            TreeMap<String, Object> parameters = item.getParameters();
            // TODO Feil hvis parameters er null
            String fieldValue;
            long recordNumber = (Long) parameters.get("recordNumber");
            ArrayList<Integer> fieldSeparatingCharIndexes =
                    (ArrayList<Integer>) parameters.get("fieldSeparatingCharIndexes");

            // Itererer over feltleserne, ikke skilletegnsindeksene
            if (getFieldReaders() != null) {
                int beginIndex = 0;
                int endIndex = 0;
                int i = 0;
                for (FieldReader fieldReader : getFieldReaders()) {
                    if (fieldSeparatingCharIndexes != null
                            && i <= fieldSeparatingCharIndexes.size()) {
                        if (i > 0) {
                            // beginIndex oppdateres fra og med andre felt
                            beginIndex = endIndex + 1;
                        }
                        if (i < fieldSeparatingCharIndexes.size()) {
                            endIndex = fieldSeparatingCharIndexes.get(i);
                        } else {
                            // Siste felt i posten
                            endIndex = recordValue.length();
                        }
                        fieldValue = recordValue.substring(beginIndex, endIndex);
                    } else {
                        fieldValue = "";
                    }
                    i++;

                    if (!"".equals(fieldValue)
                            && fieldValue.charAt(0) == quotingChar
                            && fieldValue.charAt(fieldValue.length() - 1) == quotingChar) {
                        fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
                        fieldValue = trimQuotes(fieldValue);
                    }

                    if (DEBUG) {
                        System.out.println("Felt " + i + ": "
                                + fieldValue);
                    }
                    TreeMap<String, Object> fieldParameters = new TreeMap<>();
                    fieldParameters.put("recordNumber", recordNumber);
                    Item field = new Item(fieldValue, fieldParameters);

                    fieldReader.read(field);
                }
            }

            fireItemRead(null);
        }
    }

    /**
     * Reduserer hver forekomst av to omslutningstegn ved siden av hverandre i
     * en tekststreng, til ett.<br /> Eksempler:<br /> ""A"" blir til "A"<br />
     * G""Y"" blir til "G"Y"<br /> ABC""DEF blir til ABC"DEF
     *
     * @param value tekststrengen med det opprinnelige innholdet
     * @return tekststreng med redusert antall omslutningstegn inne i teksten.
     */
    private String trimQuotes(String value) {
        return value.replaceAll("" + quotingChar + quotingChar, "" + quotingChar);
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }
}
