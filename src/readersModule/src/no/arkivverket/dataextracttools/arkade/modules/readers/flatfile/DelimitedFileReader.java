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
import no.arkivverket.dataextracttools.metadatastandards.addml.DatasetDescription;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.DelimFileFormat;
import no.arkivverket.dataextracttools.metadatastandards.addml.version_8_2.bind.FlatFile;

/**
 * Klasse for å lese tegnseparerte flate filer - "kommaseparerte" filer. Klassen
 * kan lese CSV-filer beskrevet i RFC 4180. <br /> (RFC 4180 - Common Format and
 * MIME Type for Comma-Separated Values (CSV) Files.<br />
 * {@link http://datatracker.ietf.org/doc/rfc4180/})<br /> I tillegg kan filer
 * med postskille, feltskille og omslutningstegn som avviker fra RFC 4180,
 * leses.<br /> Hver instans av leseren baserer seg på metainformasjon i en
 * forekomst av elementet flatFile i ADDML (Archival Data Description Markup
 * Language).
 *
 * @version 0.07 2014-02-28
 * @author Riksarkivet
 */
public class DelimitedFileReader extends FlatFileReader {

    private static final boolean DEBUG = false;
    private static final boolean DUMP_DATA = false;
    private java.io.BufferedReader in;
    private char[] recordSeparator;
    private char fieldSeparatingChar = 0;
    private char quotingChar = 0;
    private String header;
    private boolean hasHeader = false;
    private ArrayList<Integer> fieldSeparatingCharIndexesInHeader;
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
     * @param hasHeader 
     */
    public DelimitedFileReader(String id, FlatFile flatFile,
            DatasetDescription datasetDescription,
            String charset, DelimFileFormat format, int recordDefinitionFieldNumber,
            File dataDirectory, boolean hasHeader) {
        super(id, flatFile, datasetDescription, charset, format,
                recordDefinitionFieldNumber, dataDirectory);

        this.hasHeader = hasHeader;
    }

    @Override
    public void init() {
        super.init();
        DelimFileFormat delimFileFormat = (DelimFileFormat) getFormat();
        if (delimFileFormat.getRecordSeparator() != null) {
            if (delimFileFormat.getRecordSeparator().equalsIgnoreCase("CR")) {
                recordSeparator = new char[1];
                recordSeparator[0] = '\r';
            } else if (delimFileFormat.getRecordSeparator().equalsIgnoreCase("LF")) {
                recordSeparator = new char[1];
                recordSeparator[0] = '\n';
            } else if (delimFileFormat.getRecordSeparator().equalsIgnoreCase("CRLF")) {
                recordSeparator = new char[2];
                recordSeparator[0] = '\r';
                recordSeparator[1] = '\n';
            }
        }
        if (delimFileFormat.getFieldSeparatingChar() != null) {
            fieldSeparatingChar = delimFileFormat.getFieldSeparatingChar().charAt(0);
        }
        if (delimFileFormat.getQuotingChar() != null) {
            quotingChar = delimFileFormat.getQuotingChar().charAt(0);
        }
    }

    public String getHeader() {
        return header;
    }

    public ArrayList<Integer> getFieldSeparatingCharIndexesInHeader() {
        return fieldSeparatingCharIndexesInHeader;
    }

    @Override
    public long getNumberOfRecordsRead() {
        return recordCounter;
    }

    @Override
    public void read() {
        /*
         * Leser enkelttegn - c.
         * c kan være:
         * <br />
         * <ul>
         *   <li>
         *     Et tegn i et felt inkludert omslutningstegn, feltskilletegn eller
         *     postskilletegn, det siste hvis visse betingelser er oppfylt.
         *   </li>
         *   <li>
         *     Omslutningstegn i starten eller slutten av et felt
         *   </li>
         *   <li>
         *     Feltskilletegn
         *   </li>
         *   <li>
         *     Postskilletegn (kan være maks to forskjellige)
         *   </li>
         * </ul>
         *
         */
        log(getFile().getName() + " - Startet lesing");

        StringBuilder record = new StringBuilder();
        ArrayList<Integer> fieldSeparatingCharIndexes = new ArrayList<>();
        int charCounter = 0;

        boolean readingField = false;
        boolean quoted = false;

        try {
            InputStreamReader inputStreamReader
                    = new InputStreamReader(new FileInputStream(getFile()), getCharset());
            in = new BufferedReader(inputStreamReader);

            fireItemRead(new Item(getFile(), null));

            while (true) {
                int c = in.read();

                if (c == -1) {
                    // Har lest til filslutt
                    if (record.length() > 0) {
                        recordCounter++;
                        readRecord(recordCounter, record, fieldSeparatingCharIndexes);
                    }
                    break;
                }

                if (!readingField) {
                    if (c == recordSeparator[0]) {
                        // Postslutt
                        recordCounter++;
                        if (recordSeparator.length == 2) {
                            int next = in.read();
                            if (next != recordSeparator[1]) {
                                // TODO Feilsituasjon hvis manglende andre
                                // postskilletegn
                                log(recordCounter + ": Feil! Manglende andre postskilletegn!", Level.WARNING);
                            }
                        }
                        readRecord(recordCounter, record, fieldSeparatingCharIndexes);
                        record = new StringBuilder();
                        fieldSeparatingCharIndexes = new ArrayList<>();
                        charCounter = 0;
                    } else if (c == fieldSeparatingChar) {
                        // Feltslutt
                        record.append((char) c);
                        fieldSeparatingCharIndexes.add(charCounter);
                        charCounter++;
                    } else {
                        readingField = true;
                        record.append((char) c);
                        if (c == quotingChar) {
                            quoted = true;
                        }
                        charCounter++;
                    }
                } else {
                    // Leser feltinnhold inkludert eventuelle omslutningstegn
                    if (c == recordSeparator[0]) {
                        // Sjekke om postskillet markerer slutten på posten
                        // eller om det er inne i et omsluttet felt.
                        if (!quoted) {
                            // Postslutt
                            recordCounter++;
                            readingField = false;
                            if (recordSeparator.length == 2) {
                                int next = in.read();
                                if (next != recordSeparator[1]) {
                                    // TODO Feilsituasjon hvis manglende andre
                                    // postskilletegn
                                    log(recordCounter + ": Feil! Manglende andre postskilletegn!", Level.WARNING);
                                }
                            }
                            readRecord(recordCounter, record, fieldSeparatingCharIndexes);
                            record = new StringBuilder();
                            fieldSeparatingCharIndexes = new ArrayList<>();
                            charCounter = 0;
                        } else {
                            // Postskille inne i feltet
                            record.append((char) c);
                            charCounter++;
                            if (recordSeparator.length == 2) {
                                int next = in.read();
                                if (next != recordSeparator[1]) {
                                    // TODO Feilsituasjon hvis manglende andre
                                    // postskilletegn
                                    log(recordCounter + ": Feil! Manglende andre postskilletegn!", Level.WARNING);
                                }
                                record.append((char) next);
                                charCounter++;
                            }
                        }
                    } else if (c == quotingChar) {
                        // Aldri første quotingChar i et felt her.
                        // quoted skal derfor være true.
                        // TODO Feil hvis quoted er false.
                        record.append((char) c);
                        charCounter++;
                        int next = in.read();
                        if (next != -1
                                && next != recordSeparator[0]
                                && next != fieldSeparatingChar) {
                            record.append((char) next);
                            charCounter++;
                        } else if (next == -1) {
                            // TODO Filslutt
                        } else if (next == recordSeparator[0]) {
                            // Postslutt
                            recordCounter++;
                            readingField = false;
                            if (recordSeparator.length == 2) {
                                int next2 = in.read();
                                if (next2 != recordSeparator[1]) {
                                    // TODO Feilsituasjon hvis manglende andre
                                    // postskilletegn
                                    log(recordCounter + ": Feil! Manglende andre postskilletegn!", Level.WARNING);
                                }
                            }
                            readRecord(recordCounter, record, fieldSeparatingCharIndexes);
                            record = new StringBuilder();
                            fieldSeparatingCharIndexes = new ArrayList<>();
                            charCounter = 0;

                        } else if (next == fieldSeparatingChar) {
                            // Feltslutt
                            record.append((char) next);
                            fieldSeparatingCharIndexes.add(charCounter);
                            charCounter++;
                            readingField = false;
                            quoted = false;
                        }
                    } else if (c == fieldSeparatingChar) {
                        // Feltslutt
                        record.append((char) c);
                        fieldSeparatingCharIndexes.add(charCounter);
                        charCounter++;
                        readingField = false;
                        quoted = false;
                    } else {
                        // Vanlig feltinnhold
                        record.append((char) c);
                        charCounter++;
                    }
                }

                if (DUMP_DATA) {
                    if (c == '\r') {
                        System.out.println("CARRIAGE RETURN");

                    } else if (c == '\n') {
                        System.out.println("LINE FEED");

                    } else {

                        System.out.println((char) c);
                    }
                }
            }

        } catch (UnsupportedEncodingException e) {
            log(e.getMessage(), Level.SEVERE);
        } catch (FileNotFoundException e) {
            log(e.getMessage(), Level.SEVERE);
        } catch (IOException e) {
            log(e.getMessage(), Level.SEVERE);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log(e.getMessage(), Level.SEVERE);
                }
            }
        }

        fireItemRead(new Item(getFile(), null));

        log(getFile().getName() + " - Lesing ferdig");
        log(getFile().getName() + " - Antall poster lest: " + recordCounter);
    }

    @Override
    public void read(Item item) {
        throw new UnsupportedOperationException("Not supported.");
    }

    private void readRecord(long recordNumber,
            StringBuilder record, ArrayList<Integer> fieldSeparatingCharIndexes) {

        RecordReader recordReader;

        // Header
        if (hasHeader && recordNumber == 1) {
            header = record.toString();
            fieldSeparatingCharIndexesInHeader = fieldSeparatingCharIndexes;
            return;
        }

        // Vanlig post
        if (getRecordReaders().size() == 1 && getRecordReaders().containsKey(null)) {
            // En postleser
            recordReader = getRecordReaders().get(null);
        } else if (!getRecordReaders().isEmpty()) {
            // Flere postlesere

            // TODO Ikke testet!
            int fieldNumber = getRecordDefinitionFieldNumber();
            String recordDefinitionFieldValue = null;

            int beginIndex = 0;
            for (int i = 0; i < fieldSeparatingCharIndexes.size(); i++) {
                if (fieldNumber == i) {
                    recordDefinitionFieldValue = record.substring(beginIndex, fieldSeparatingCharIndexes.get(i));
                    break;
                } else {
                    beginIndex = fieldSeparatingCharIndexes.get(i) + 1;
                }
            }
            // TODO Her kan det oppstå en feilsituasjon hvis
            // fieldValue ikke er definert for en postdefinisjon.
            // Det er også feil hvis fieldValue er null.

            recordReader = getRecordReaders().get(recordDefinitionFieldValue);
        } else {
            log("Missing record reader", Level.SEVERE);
            return;
        }

        if (DEBUG) {
            System.out.println(recordNumber + ": " + record.toString());
        }

        TreeMap<String, Object> parameters = new TreeMap<>();
        parameters.put("recordNumber", recordNumber);
        parameters.put("fieldSeparatingCharIndexes", fieldSeparatingCharIndexes);
        Item item = new Item(record.toString(), parameters);

        recordReader.read(item);
    }

    // ReadListener
    @Override
    public void itemRead(ReadEvent readEvent) {
    }
}
