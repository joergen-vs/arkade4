/*
 *  The National Archives of Norway - 2014
 * 
 */
package no.arkivverket.dataextracttools.arkade.modules.noark5.version_3_1;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import no.arkivverket.dataextracttools.arkade.modules.noark5.Test;
import no.arkivverket.dataextracttools.arkade.modules.processes.DataExtractProcess;
import no.arkivverket.dataextracttools.arkade.modules.processes.xml.ReportingXmlValidator;
import no.arkivverket.dataextracttools.arkade.modules.readers.xml.XmlCollectionReader;
import no.arkivverket.dataextracttools.arkade.modules.readers.xml.XmlDocumentReader;
import org.xmldb.api.base.Collection;

/**
 * Helper class for adding processes to the XML readers.
 * The processes are Noark 5 version 3.1 specific.
 *
 * @version 0.16 2014-02-28
 * @author Riksarkivet
 *
 */
public class Noark_5_v_3_1_ProcessHelper {

    /**
     * Creates and adds processes to XML collection readers. It is used for
     * processes involving multiple XML documents or no XML documents at all. It
     * is called once for each collection.
     *
     * @param reader The XML Colleaction Reader
     * @param metadataCollection The collection in the eXist-database with the
     * dataset description
     * @param processList The list of processes the created processes are added
     * to
     * @param maxNumberOfResults The maximum number of results to send to the
     * report
     * @param dataDirectory The directory with the data files
     * @param documentDirectory The directory with the documents
     * @param loggerName The name of the internal logger messages are sent to
     */
    public static void addCollectionProcesses(
            XmlCollectionReader reader,
            Collection metadataCollection,
            ArrayList<DataExtractProcess> processList,
            int maxNumberOfResults,
            File dataDirectory, String documentDirectory,
            String loggerName) {

        Collection dataCollection = reader.getCollection();

        // Test 2 - Kontroll av sjekksummene for XML-filene og XML-skjemaene i avleveringspakken
        Test test2 = new Test(reader.getId(), "Test 2",
                "Test 2 - Kontroll av sjekksummene i arkivuttrekk.xml",
                "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0002.xq",
                "0002",
                metadataCollection, dataCollection,
                maxNumberOfResults,
                dataDirectory, documentDirectory);
        test2.setDescription("Kontroll på at sjekksummene for XML-filene og "
                + "XML-skjemaene i avleveringspakken er korrekte. "
                + "Sjekksummene for disse filene ligger i arkivuttrekk.xml.");
        test2.setResultDescription("Resultatet av sjekksumkontrollen for alle XML-filene og XML-skjemaene i arkivuttrekk.xml. "
                + "Resultattypen vises som 'Feil' hvis den beregnede sjekksummen avviker fra den oppgitte. "
                + "I tillegg blir resultattypen 'Feil' hvis kun en av algoritme eller sjekksumverdi er oppgitt i arkivuttrekk.xml. "
                + "Resultattypen 'Advarsel' vises hvis hverken algoritme eller sjekksumverdi er oppgitt.");
        test2.setLoggerName(loggerName);
        // Add process as read listener
        reader.addReadListener(test2);
        processList.add(test2);

        // Test 28 - Antall dokumentfiler i arkivuttrekket
        Test test28 = new Test(reader.getId(), "Test 28",
                "Test 28 - Antall dokumentfiler i arkivuttrekket",
                "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0028.xq",
                "0028",
                metadataCollection, dataCollection,
                maxNumberOfResults,
                dataDirectory, documentDirectory);
        test28.setDescription("Opptelling av antall dokumentfiler i arkivuttrekket. "
                + "Dokumentfilene ligger i en filkatalog kalt dokumenter. "
                + "Denne kan igjen være inndelt i eventuelle underkataloger. "
                + "Antallet dokumentfiler kontrolleres mot det som er oppgitt i arkivuttrekk.xml.");
        test28.setResultDescription("Viser antall dokumentfiler og oppgitt verdi i arkivuttrekk.xml");
        test28.setLoggerName(loggerName);
        // Add process as read listener
        reader.addReadListener(test28);
        processList.add(test28);

        // Test 59 - Analyse og kontroll av antall journalposter i arkivuttrekket
        Test test59 = new Test(reader.getId(), "Test 59",
                "Test 59 - Antall journalposter i arkivuttrekket",
                "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0059.xq",
                "0059",
                metadataCollection, dataCollection,
                maxNumberOfResults, null, null);
        test59.setDescription("Sammenligning av det totale antallet journalposter i "
                + "arkivstrukturen med antall journalposter i løpende og offentlig journal. "
                + "Antall forekomster av elementet registrering av typen journalpost i "
                + "arkivstruktur.xml sammenlignes med antall forekomster av journalpost i "
                + "loependeJournal.xml og offentligJournal.xml.");
        test59.setResultDescription("Resultatet viser antall journalposter og "
                + "hvorvidt de stemmer med oppgitt type periodeskille.");
        test59.setLoggerName(loggerName);
        // Add process as read listener
        reader.addReadListener(test59);
        processList.add(test59);

        // Test 60 - Analyse og kontroll av start- og sluttdato i arkivuttrekket
        Test test60 = new Test(reader.getId(), "Test 60",
                "Test 60 - Start- og sluttdato i arkivuttrekket",
                "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0060.xq",
                "0060",
                metadataCollection, dataCollection,
                maxNumberOfResults, null, null);
        test60.setDescription("Sammenligning av start- og sluttdato for journalposter i arkivstrukturen "
                + "med journalposter i løpende og offentlig journal. Elementet opprettetDato i registrering "
                + "i arkivstruktur.xml sammenlignes med elementet journaldato i loependeJournal.xml og offentligJournal.xml");
        test60.setResultDescription("Resultatet viser de aktuelle datoene og hvorvidt de stemmer "
                + "med oppgitt type periodeskille.");
        test60.setLoggerName(loggerName);
        // Add process as read listener
        reader.addReadListener(test60);
        processList.add(test60);

        // Test 62 - Kontroll av referansene i endringsloggen
        Test test62 = new Test(reader.getId(), "Test 62",
                "Test 62 - Kontroll av referansene i endringsloggen",
                "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0062.xq",
                "0062",
                null, dataCollection,
                maxNumberOfResults, null, null);
        test62.setDescription("Kontroll på at alle endringer i endringsloggen refererer til arkivenheter i arkivstrukturen. "
                + "Hver forekomst av referanseArkivenhet i endringslogg.xml skal referere til en systemID i arkivstruktur.xml.");
        test62.setResultDescription("Resultatet viser hver ugyldige referanse i endringsloggen, "
                + "og hvor mange ganger den forekommer. Hvis systemID i arkivstruktur.xml ikke er entydig, utføres ikke kontrollen.");
        test62.setLoggerName(loggerName);
        // Add process as read listener
        reader.addReadListener(test62);
        processList.add(test62);
    }

    /**
     * Creates and adds processes to XML document readers. Operates on single
     * XML documents. It is called once for each XML document.
     *
     * @param reader The XML Document Reader
     * @param metadataCollection The collection in the eXist-database with the
     * dataset description
     * @param dataCollection The main collection with the XML documents and
     * schemas
     * @param dataDirectory The directory with the data files
     * @param processList The list of processes the created processes are added
     * to
     * @param maxNumberOfResults The maximum number of results to send to the
     * report
     * @param documentDirectory The name of the root directory with the document files
     * @param loggerName The name of the internal logger messages are sent to
     */
    public static void addXmlDocumentProcesses(
            XmlDocumentReader reader,
            Collection metadataCollection,
            Collection dataCollection,
            ArrayList<DataExtractProcess> processList,
            int maxNumberOfResults,
            File dataDirectory, String documentDirectory,
            String loggerName) {

        File documentFile = reader.getDocumentFile();
        File mainSchemaFile = reader.getMainSchemaFile();

        // Test 3
        ReportingXmlValidator reportingXmlValidator =
                new ReportingXmlValidator(
                reader.getId(), "Test 3 - " + documentFile.getName(),
                "Test 3 - Validering av " + documentFile.getName(),
                documentFile, mainSchemaFile, maxNumberOfResults);
        reportingXmlValidator.init();
        reportingXmlValidator.setOrderKey("0003");
        reportingXmlValidator.setLoggerName(loggerName);

        StringBuilder description = new StringBuilder();
        description.append("<textBlock>Validering av XML-fil mot tilhørende XML-skjema.</textBlock>\n")
                .append("<list>")
                .append("<listItem><label>XML-fil:</label><content>")
                .append(documentFile.getName())
                .append("</content></listItem>\n")
                .append("<listItem><label>Skjemafil:</label><content>")
                .append(mainSchemaFile.getName())
                .append("</content></listItem>\n")
                .append("</list>");
        reportingXmlValidator.setDescription(description.toString());
        // Add process as read listener
        reader.addReadListener(reportingXmlValidator);
        processList.add(reportingXmlValidator);

        switch (reader.getId()) {
            case "Noark 5-arkivuttrekk/arkivstruktur":

                // Test 4 - Antall arkiver i arkivstrukturen
                Test test4 = new Test(reader.getId(), "Test 4",
                        "Test 4 - Antall arkiver i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0004.xq",
                        "0004",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test4.setDescription("Opptelling av antall forekomster av arkiv i arkivstruktur.xml.");
                test4.setResultDescription("Resultatet av opptellingen.");
                test4.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test4);
                processList.add(test4);

                // Test 5 - Antall arkivdeler i arkivstrukturen
                Test test5 = new Test(reader.getId(), "Test 5", "Test 5 - Antall arkivdeler i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0005.xq",
                        "0005",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test5.setDescription("Opptelling av antall forekomster av arkivdel i arkivstruktur.xml");
                test5.setResultDescription("Resultatet er fordelt på arkiv.");
                test5.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test5);
                processList.add(test5);

                // Test 6 - Arkivdelen[e]s status i arkivstrukturen
                Test test6 = new Test(reader.getId(), "Test 6", "Test 6 - Arkivdelen[e]s status i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0006.xq",
                        "0006",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test6.setDescription("Oversikt over verdien(e) i arkivdelen(e)s status. "
                        + "Viser verdien i arkivdelstatus under arkivdel i arkivstruktur.xml.");
                test6.setResultDescription("Resultatet viser verdien i arkivdelstatus og systemID til arkivdelen med statusen.");
                test6.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test6);
                processList.add(test6);

                // Test 7 - Antall klassifikasjonssystemer i arkivstrukturen
                Test test7 = new Test(reader.getId(), "Test 7", "Test 7 - Antall klassifikasjonssystemer i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0007.xq",
                        "0007",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test7.setDescription("Opptelling av antall forekomster av klassifikasjonssystem i arkivstruktur.xml.");
                test7.setResultDescription("Resultatet viser antall klassifikasjonssystemer per arkivdel.");
                test7.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test7);
                processList.add(test7);

                // Test 8 - Antall klasser i arkivstrukturen
                Test test8 = new Test(reader.getId(), "Test 8", "Test 8 - Antall klasser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0008.xq",
                        "0008",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test8.setDescription("Opptelling av antall forekomster av klasse på hvert nivå i arkivstruktur.xml.");
                test8.setResultDescription("Resultatet vises fordelt på arkivdel, klassifikasjonssystem og nivå.");
                test8.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test8);
                processList.add(test8);

                // Test 9 - Antall klasser uten underklasser, mapper eller registreringer i arkivstrukturen
                Test test9 = new Test(reader.getId(), "Test 9",
                        "Test 9 - Antall klasser uten underklasser, mapper eller registreringer i det primære klassifikasjonssystemet i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0009.xq",
                        "0009",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test9.setDescription("Opptelling av antall forekomster av klasse uten underklasser, mapper "
                        + "eller registreringer umiddelbart under klasse i det primære klassifikasjonssystemet "
                        + "i arkivstrukturen, dvs. i det klassifikasjonssystemet som "
                        + "inneholder mapper.");
                test9.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test9.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test9);
                processList.add(test9);

                // Test 10 - Antall mapper i arkivstrukturen
                Test test10 = new Test(reader.getId(), "Test 10", "Test 10 - Antall mapper i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0010.xq",
                        "0010",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test10.setDescription("Opptelling av antall forekomster av mappe i arkivstruktur.xml. "
                        + "I tillegg kontrolleres det totale antallet opp mot oppgitt antall i arkivuttrekk.xml.");
                test10.setResultDescription("Resultatet vises fordelt på arkivdel og mappetype.");
                test10.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test10);
                processList.add(test10);

                // Test 11 - Antall mapper for hvert år i arkivstrukturen
                Test test11 = new Test(reader.getId(), "Test 11", "Test 11 - Antall mapper for hvert år i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0011.xq",
                        "0011",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test11.setDescription("Opptelling av antall forekomster av mappe i arkivstruktur.xml. "
                        + "Opptellingen grupperes etter årstallet i den enkelte mappes opprettetDato.");
                test11.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test11.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test11);
                processList.add(test11);

                // Test 12 - Kontroll på at mappene er knyttet til klasser uten underklasser i arkivstrukturen
                Test test12 = new Test(reader.getId(), "Test 12",
                        "Test 12 - Kontroll på at mappene bare er knyttet til klasser uten underklasser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0012.xq",
                        "0012",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test12.setDescription("Kontrollerer at klassen som inneholder mappen ikke samtidig også inneholder en underklasse. "
                        + "Opptelling av antall forekomster av klasse som inneholder både forekomst av klasse (underklasse) og mappe "
                        + "i arkivstruktur.xml.");
                test12.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test12.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test12);
                processList.add(test12);

                // Test 13 - Antall mapper som er klassifisert med hver enkelt klasse i arkivstrukturen
                Test test13 = new Test(reader.getId(), "Test 13",
                        "Test 13 - Antall mapper som er klassifisert med hver enkelt klasse i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0013.xq",
                        "0013",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test13.setDescription("Opptelling av hvor mange mapper som er klassifisert med de forskjellige klassene i arkivstruktur.xml.");
                test13.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test13.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test13);
                processList.add(test13);

                // Test 14 - Antall mapper uten undermapper eller registreringer i arkivstrukturen
                Test test14 = new Test(reader.getId(), "Test 14",
                        "Test 14 - Antall mapper uten undermapper eller registreringer i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0014.xq",
                        "0014",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test14.setDescription("Opptelling av antall mapper som verken inneholder undermapper eller registreringer i arkivuttrekk.xml.");
                test14.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test14.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test14);
                processList.add(test14);

                // Test 15 - Saksmappenes status i arkivstrukturen
                Test test15 = new Test(reader.getId(), "Test 15",
                        "Test 15 - Saksmappenes status i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0015.xq",
                        "0015",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test15.setDescription("Opptelling av de forskjellige verdiene i saksmappenes status i arkivstruktur.xml.");
                test15.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test15.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test15);
                processList.add(test15);

                // Test 16 - Antall registreringer i arkivstrukturen
                Test test16 = new Test(reader.getId(), "Test 16", "Test 16 - Antall registreringer i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0016.xq",
                        "0016",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test16.setDescription("Opptelling av antall forekomster av registrering i arkivstruktur.xml. "
                        + "I tillegg kontrolleres det totale antallet opp mot oppgitt antall i arkivuttrekk.xml.");
                test16.setResultDescription("Resultatet vises fordelt på arkivdel og registreringstype.");
                test16.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test16);
                processList.add(test16);

                // Test 17 - Journalposttyper og journalposttilknytning i arkivstrukturen
                Test test17 = new Test(reader.getId(), "Test 17", "Test 17 - Journalposttyper og journalposttilknytning i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0017.xq",
                        "0017",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test17.setDescription("Opptelling av antall forskjellige journalposttyper i arkivstruktur.xml. "
                        + "Element i arkivstrukturen er registrering av type 'journalpost'. "
                        + "I tillegg kontrolleres det at alle journalposter har hovedokument, dvs. "
                        + "at hver journalpost har en dokumentbeskrivelse som er tilknyttet journalposten som hoveddokument. "
                        + "Aktuelt element i dokumentbeskrivelse er tilknyttetRegistreringSom.");
                test17.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test17.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test17);
                processList.add(test17);

                // Test 18 - Antall registreringer for hvert år i arkivstrukturen
                Test test18 = new Test(reader.getId(), "Test 18", "Test 18 - Antall registreringer for hvert år i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0018.xq",
                        "0018",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test18.setDescription("Opptelling av antall registreringer som er opprettet hvert enkelt år i arkivstruktur.xml. "
                        + "Opptellingen grupperes etter årstallet i den enkelte registrerings opprettetDato.");
                test18.setResultDescription("Resultatet vises fordelt på arkivdel og årstall.");
                test18.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test18);
                processList.add(test18);

                // Test 19 - Kontroll på at registreringer bare er knyttet til klasser uten underklasser i arkivstrukturen
                Test test19 = new Test(reader.getId(), "Test 19",
                        "Test 19 - Kontroll på at registreringer bare er knyttet til klasser uten underklasser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0019.xq",
                        "0019",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test19.setDescription("Kontrollerer at klassen som inneholder registreringen ikke samtidig også inneholder en underklasse. "
                        + "Opptelling av antall forekomster av klasse som inneholder både forekomst av klasse (underklasse) og registrering "
                        + "i arkivstruktur.xml.");
                test19.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test19.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test19);
                processList.add(test19);

                // Test 20 - Antall registreringer som er klassifisert med hver enkelt klasse i arkivstrukturen
                Test test20 = new Test(reader.getId(), "Test 20",
                        "Test 20 - Antall registreringer som er klassifisert med hver enkelt klasse i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0020.xq",
                        "0020",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test20.setDescription("Opptelling av hvor mange registreringer som er klassifisert med de forskjellige klassene "
                        + "i arkivstruktur.xml. "
                        + "Denne analysen gjelder bare fagsystemer uten mapper, dvs. fagsystemer hvor registreringene er "
                        + "knyttet direkte til klasser.");
                test20.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test20.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test20);
                processList.add(test20);

                // Test 21 - Antall registreringer uten dokumentbeskrivelse i arkivstrukturen.
                Test test21 = new Test(reader.getId(), "Test 21",
                        "Test 21 - Antall registreringer uten dokumentbeskrivelse i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0021.xq",
                        "0021",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test21.setDescription("Opptelling av antall forekomster av registrering uten dokumentbeskrivelse i arkivstruktur.xml.");
                test21.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test21.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test21);
                processList.add(test21);

                // Test 22 - Journalpostenes status i arkivstrukturen.
                Test test22 = new Test(reader.getId(), "Test 22",
                        "Test 22 - Journalpostenes status i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0022.xq",
                        "0022",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test22.setDescription("Opptelling av de forskjellige verdiene i journalpostenes status i arkivstruktur.xml.");
                test22.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test22.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test22);
                processList.add(test22);

                // Test 23 - Antall dokumentbeskrivelser i arkivstrukturen.
                Test test23 = new Test(reader.getId(), "Test 23",
                        "Test 23 - Antall dokumentbeskrivelser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0023.xq",
                        "0023",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test23.setDescription("Opptelling av antall forekomster av dokumentbeskrivelse i arkivstruktur.xml.");
                test23.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test23.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test23);
                processList.add(test23);

                // Test 24 - Antall dokumentbeskrivelser uten dokumentobjekt i arkivstrukturen.
                Test test24 = new Test(reader.getId(), "Test 24",
                        "Test 24 - Antall dokumentbeskrivelser uten dokumentobjekt i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0024.xq",
                        "0024",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test24.setDescription("Opptelling av antall forekomster av dokumentbeskrivelse uten dokumentobjekt i arkivstruktur.xml.");
                test24.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test24.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test24);
                processList.add(test24);

                // Test 25 - Dokumentbeskrivelsenes status i arkivstrukturen.
                Test test25 = new Test(reader.getId(), "Test 25",
                        "Test 25 - Dokumentbeskrivelsenes status i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0025.xq",
                        "0025",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test25.setDescription("Opptelling av de forskjellige verdiene i dokumentbeskrivelsenes status i arkivstruktur.xml.");
                test25.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test25.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test25);
                processList.add(test25);

                // Test 26 - Antall dokumentobjekter i arkivstrukturen.
                Test test26 = new Test(reader.getId(), "Test 26",
                        "Test 26 - Antall dokumentobjekter i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0026.xq",
                        "0026",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test26.setDescription("Opptelling av antall forekomster av dokumentobjekt i arkivstruktur.xml.");
                test26.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test26.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test26);
                processList.add(test26);

                // Test 27 - Start- og sluttdato for dokumentene i arkivstrukturen.
                Test test27 = new Test(reader.getId(), "Test 27",
                        "Test 27 - Start- og sluttdato for dokumentene i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0027.xq",
                        "0027",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test27.setDescription("Angivelse av første og siste dato for når dokumentene ble registrert, "
                        + "angitt i elementet opprettetDato i registreringen i arkivstruktur.xml.");
                test27.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test27.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test27);
                processList.add(test27);

                // Test 29 - Antall dokumenter fordelt på dokumentformat i arkivstrukturen.
                Test test29 = new Test(reader.getId(), "Test 29",
                        "Test 29 - Antall dokumenter i arkivuttrekket fordelt på dokumentformat",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0029.xq",
                        "0029",
                        null, dataCollection,
                        maxNumberOfResults, dataDirectory, null);
                test29.setDescription("Opptelling av forskjellige dokumentformater, "
                        + "gruppert på verdien av elementet format i arkivstruktur.xml og filendelsene i dokumentfilene");
                test29.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test29.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test29);
                processList.add(test29);

                // Test 30 - Kontroll av sjekksummer
                Test test30 = new Test(reader.getId(), "Test 30", "Test 30 - Kontroll av sjekksummer",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0030.xq",
                        "0030",
                        null, dataCollection,
                        maxNumberOfResults, dataDirectory, null);
                test30.setDescription("Kontroll på at hver dokumentfils sjekksum som er oppgitt i elementet sjekksum i "
                        + "dokumentobjekt i arkivstruktur.xml, stemmer med sjekksummen som blir beregnet i testen. "
                        + "Sjekksummen beregnes ut i fra verdien oppgitt i elementet sjekksumAlgoritme i aktuelt dokumentobjekt.");
                test30.setResultDescription("Resultatet av sjekksumkontrollen. Resultatet vises fordelt på arkivdel.");
                test30.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test30);
                processList.add(test30);

                // Test 32 - Kontroll på om dokumentobjektene i arkivstrukturen refererer til eksisterende dokumentfiler i arkivuttrekket.
                Test test32 = new Test(reader.getId(), "Test 32", "Test 32 - Kontroll på om dokumentobjektene i arkivstrukturen refererer til eksisterende dokumentfiler i arkivuttrekket",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0032.xq",
                        "0032",
                        null, dataCollection,
                        maxNumberOfResults, dataDirectory, null);
                test32.setDescription("Kontroll på om dokumentobjektene i arkivstrukturen refererer til eksisterende "
                        + "dokumentfiler i arkivuttrekket");
                test32.setResultDescription("Kontroll på om sti og filnavn i elementet referanseDokumentfil i dokumentobjekt i "
                        + "arkivstruktur.xml er gyldig, dvs. at forekomstene av referanseDokumentfil angir eksisterende filer "
                        + "i arkivuttrekket. Resultatet vises fordelt på arkivdel.");
                test32.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test32);
                processList.add(test32);

                // Test 33 - Kontroll på at det ikke finnes dokumentfiler i arkivuttrekket som mangler referanse fra arkivstrukturen.
                Test test33 = new Test(reader.getId(), "Test 33", "Test 33 - Kontroll på at det ikke finnes dokumentfiler i arkivuttrekket som mangler referanse fra arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0033.xq",
                        "0033",
                        null, dataCollection,
                        maxNumberOfResults, dataDirectory, null, File.separator);
                test33.setDescription("Kontroll på at det ikke finnes dokumentfiler i arkivuttrekket som det ikke blir referert til "
                        + "fra elementet referanseDokumentfil i dokumentobjektet i arkivstruktur.xml");
                test33.setResultDescription("Dokumentfiler i arkivstrukturen som ikke har noen referanse i arkivstruktur.xml.");
                test33.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test33);
                processList.add(test33);

                // Test 34 - Antall dokumentfiler i arkivuttrekket som blir referert til av flere enn ett dokumentobjekt i arkivstrukturen.
                Test test34 = new Test(reader.getId(), "Test 34", "Test 34 - Antall dokumentfiler som blir referert til av flere enn ett dokumentobjekt",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0034.xq",
                        "0034",
                        null, dataCollection,
                        maxNumberOfResults, dataDirectory, null);
                test34.setDescription("Antall dokumentfiler i arkivuttrekket som blir referert til av flere enn ett "
                        + "dokumentobjekt i arkivstrukturen.");
                test34.setResultDescription("Antall dokumentobjekter i arkivstruktur.xml hvor inneholdet i elementet "
                        + "referanseDokumentfil peker på en fil som det også har blitt referert til fra et eller flere "
                        + "andre dokumentobjekt.");
                test34.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test34);
                processList.add(test34);

                // Test 35 - Antall saksparter i arkivstrukturen.
                Test test35 = new Test(reader.getId(), "Test 35", "Test 35 - Antall saksparter i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0035.xq",
                        "0035",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test35.setDescription("Opptelling av antall saksparter i arkivstruktur.xml. "
                        + "Forekomster av elementet sakspart i mappetypen saksmappe telles opp.");
                test35.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test35.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test35);
                processList.add(test35);

                // Test 36 - Antall merknader i arkivstrukturen.
                Test test36 = new Test(reader.getId(), "Test 36", "Test 36 - Antall merknader i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0036.xq",
                        "0036",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test36.setDescription("Opptelling av antall merknader i arkivstruktur.xml. "
                        + "Forekomster av elementet merknad i mappe, registreringstypen basisregistrering og dokumentbeskrivelse telles opp.");
                test36.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "Innenfor hver arkivdel er antall merknader fordelt på mappe, basisregistrering og "
                        + "dokumentbeskrivelse.");
                test36.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test36);
                processList.add(test36);

                // Test 37 - Antall kryssreferanser i arkivstrukturen.
                Test test37 = new Test(reader.getId(), "Test 37", "Test 37 - Antall kryssreferanser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0037.xq",
                        "0037",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test37.setDescription("Opptelling av antall kryssreferanser i arkivstruktur.xml. "
                        + "Forekomster av elementet kryssreferanse i mappe, klasse og i registreringstypen basisregistrering telles opp.");
                test37.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "Innenfor hver arkivdel er antall kryssreferanser fordelt på "
                        + "klasse, mappe og basisregistrering.");
                test37.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test37);
                processList.add(test37);

                // Test 38 - Antall presedenser i arkivstrukturen.
                Test test38 = new Test(reader.getId(), "Test 38", "Test 38 - Antall presedenser i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0038.xq",
                        "0038",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test38.setDescription("Opptelling av antall presedenser i arkivstruktur.xml. "
                        + "Forekomster av elementet presedens i mappetypen saksmappe og i registreringstypen journalpost telles opp.");
                test38.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "Innenfor hver arkivdel er antall presedenser fordelt på saksmapper og journalposter.");
                test38.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test38);
                processList.add(test38);

                // Test 39 - Antall korrespondanseparter i arkivstrukturen.
                Test test39 = new Test(reader.getId(), "Test 39", "Test 39 - Antall korrespondanseparter i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0039.xq",
                        "0039",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test39.setDescription("Opptelling av antall korrespondanseparter i arkivstruktur.xml. "
                        + "Forekomster av elementet korrespondansepart i registreringstypen journalpost telles opp.");
                test39.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test39.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test39);
                processList.add(test39);

                // Test 40 - Antall avskrivninger i arkivstrukturen.
                Test test40 = new Test(reader.getId(), "Test 40", "Test 40 - Antall avskrivninger i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0040.xq",
                        "0040",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test40.setDescription("Opptelling av antall forekomster av avskrivning i registreringstypen "
                        + "journalpost i arkivstruktur.xml. "
                        + "Antall journalposter som inneholder referanse til en journalpost som blir avskrevet "
                        + "av denne journalposten, telles opp.");
                test40.setResultDescription("Resultatet vises fordelt på arkivdel og viser antall journalposter "
                        + "som avskriver andre journalposter fordelt på avskrivningsmåte.");
                test40.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test40);
                processList.add(test40);

                // Test 41 - Antall dokumentflyter i arkivstrukturen.
                Test test41 = new Test(reader.getId(), "Test 41", "Test 41 - Antall dokumentflyter i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0041.xq",
                        "0041",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test41.setDescription("Opptelling av antall dokumentflyter i arkivstruktur.xml. "
                        + "Forekomster av elementet dokumentflyt i registreringstypen journalpost telles opp.");
                test41.setResultDescription("Resultatet vises fordelt på arkivdel.");
                test41.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test41);
                processList.add(test41);

                // Test 42 - Antall skjerminger i arkivstrukturen.
                Test test42 = new Test(reader.getId(), "Test 42", "Test 42 - Antall skjerminger i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0042.xq",
                        "0042",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test42.setDescription("Opptelling av antall skjerminger i arkivstruktur.xml. "
                        + "Forekomster av elementet skjerming telles opp på arkivdelnivå, "
                        + "og i klasser, mapper, registreringer og dokumentbeskrivelser. "
                        + "Det kontrolleres om arkivuttrekk.xml inneholder opplysning "
                        + "(inneholderSkjermetInformasjon) om at informasjon i arkivuttrekket skal skjermes, "
                        + "og om denne opplysningen stemmer med innholdet i arkivstruktur.xml.");
                test42.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "For hver arkivdel oppgis det om forekomst av skjerming finnes på arkivdelnivå, "
                        + "og antall øvrige skjerminger vises fordelt på klasse, mappe, registrering og dokumentbeskrivelse.");
                test42.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test42);
                processList.add(test42);

                // Test 43 - Antall graderinger i arkivstrukturen.
                Test test43 = new Test(reader.getId(), "Test 43", "Test 43 - Antall graderinger i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0043.xq",
                        "0043",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test43.setDescription("Opptelling av antall forekomster av gradering i arkivstruktur.xml. "
                        + "Forekomster av elementet gradering telles opp på arkivdelnivå, og i klasser, "
                        + "mapper, registreringer og dokumentbeskrivelser.");
                test43.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "For hver arkivdel oppgis det om forekomst av gradering finnes på arkivdelnivå, "
                        + "og antall øvrige graderinger vises fordelt på klasse, mappe, registrering og dokumentbeskrivelse.");
                test43.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test43);
                processList.add(test43);

                // Test 44 - Antall kassasjonsvedtak i arkivstrukturen.
                Test test44 = new Test(reader.getId(), "Test 44", "Test 44 - Antall kassasjonsvedtak i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0044.xq",
                        "0044",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test44.setDescription("Opptelling av antall kassasjonsvedtak i arkivstruktur.xml. "
                        + "Forekomster av elementet kassasjon telles opp på arkivdelnivå, "
                        + "og i klasser, mapper, registreringer og dokumentbeskrivelser. "
                        + "Det kontrolleres om arkivuttrekk.xml inneholder opplysning "
                        + "(inneholderDokumenterSomSkalKasseres) om at det finnes kassasjonsvedtak i arkivuttrekket, "
                        + "og om denne opplysningen stemmer med innholdet i arkivstruktur.xml.");
                test44.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "For hver arkivdel oppgis det om forekomst av kassasjon finnes på arkivdelnivå, "
                        + "og antall øvrige kassasjonsvedtak vises fordelt på klasse, mappe, registrering og dokumentbeskrivelse.");
                test44.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test44);
                processList.add(test44);

                // Test 45 - Antall utførte kassasjoner i arkivstrukturen.
                Test test45 = new Test(reader.getId(), "Test 45", "Test 45 - Antall utførte kassasjoner i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0045.xq",
                        "0045",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test45.setDescription("Opptelling av antall kassasjoner knyttet til dokumentbeskrivelsene "
                        + "i arkivstrukturen. "
                        + "Forekomster av elementet utfoertKassasjon i dokumentbeskrivelse i arkivstruktur.xml telles opp. "
                        + "Det kontrolleres om arkivuttrekk.xml inneholder opplysning (omfatterDokumenterSomErKassert) "
                        + "om at det finnes kassasjonsvedtak i arkivuttrekket, og om denne opplysningen stemmer "
                        + "med innholdet i arkivstruktur.xml.");
                test45.setResultDescription("Resultatet vises fordelt på arkivdel. "
                        + "For hver arkivdel oppgis det om forekomst av utfoertKassasjon "
                        + "finnes på arkivdelnivå, og antall utførte kassasjoner på dokumentbeskrivelse vises.");
                test45.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test45);
                processList.add(test45);

                // Test 46 - Antall konverterte dokumenter i arkivstrukturen.
                Test test46 = new Test(reader.getId(), "Test 46", "Test 46 - Antall konverterte dokumenter i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0046.xq",
                        "0046",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test46.setDescription("Opptelling av antall konvertinger knyttet til dokumentobjektene i arkivstrukturen. "
                        + "Forekomster av elementet konvertering i dokumentobjekt i arkivstruktur.xml telles opp.");
                test46.setResultDescription("Viser antall forekomster av konvertering.");
                test46.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test46);
                processList.add(test46);

                // Test 47 - Kontroll av systemidentifikasjonene i arkivstrukturen.
                Test test47 = new Test(reader.getId(), "Test 47", "Test 47 - Kontroll av systemidentifikasjonene i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0047.xq",
                        "0047",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test47.setDescription("Kontroll på at alle systemidentifikasjoner i arkivstrukturen er unike. "
                        + "Det kontrolleres at alle forekomster av elementet systemID i arkivstruktur.xml kun forekommer en gang.");
                test47.setResultDescription("Viser samlet antall forekomster av systemID og antall som ikke er unike.");
                test47.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test47);
                processList.add(test47);

                // Test 48 - Kontroll av referansene til arkivdel i arkivstrukturen.
                Test test48 = new Test(reader.getId(), "Test 48", "Test 48 - Kontroll av referansene til arkivdel i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0048.xq",
                        "0048",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test48.setDescription("Kontroll på at alle referanser fra mappe, registrering "
                        + "eller dokumentbeskrivelse til arkivdel i arkivstruktur.xml er gyldige. "
                        + "Alle forekomster av elementet referanseArkivdel skal inneholde verdien "
                        + "til en arkivdels systemID.");
                test48.setResultDescription("Resultatet er fordelt på arkivdel.");
                test48.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test48);
                processList.add(test48);

                // Test 49 - Kontroll på at alle kryssreferanser i arkivstrukturen er gyldige.
                Test test49 = new Test(reader.getId(), "Test 49", "Test 49 - Kontroll på at alle kryssreferanser i arkivstrukturen er gyldige",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0049.xq",
                        "0049",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test49.setDescription("Kontroll på at alle referanser "
                        + "fra klasse til klasse, "
                        + "fra mappe til mappe, "
                        + "fra mappe til basisregistrering, "
                        + "fra basisregistrering til basisregistrering og "
                        + "fra basisregistrering til mappe i arkivstruktur.xml er gyldige.");
                test49.setResultDescription("Resultatet er fordelt på arkivdel.");
                test49.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test49);
                processList.add(test49);

                // Test 50 - Kontroll på at avskrivningsreferansene i arkivstrukturen er gyldige.
                Test test50 = new Test(reader.getId(), "Test 50",
                        "Test 50 - Kontroll på at avskrivningsreferansene i arkivstrukturen er gyldige",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0050.xq",
                        "0050",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test50.setDescription("Kontroll på at alle referanser til "
                        + "journalpostene som avskriver en journalpost i "
                        + "arkivstruktur.xml er gyldige.");
                test50.setResultDescription("Resultatet er fordelt på arkivdel.");
                test50.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test50);
                processList.add(test50);

                // Test 51 - Kontroll av referansene til sekundær klassifikasjon i arkivstrukturen
                Test test51 = new Test(reader.getId(), "Test 51", "Test 51 - Kontroll av referansene til sekundær klassifikasjon i arkivstrukturen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0051.xq",
                        "0051",
                        null, dataCollection,
                        1, null, null);
                test51.setDescription("Kontroll på at alle referanser fra en mappe av typen "
                        + "saksmappe til en sekundær klassifikasjon i arkivstruktur.xml, er gyldige. "
                        + "Hver forekomst av referanseSekundaerKlassifikasjon i mappetypen saksmappe "
                        + "skal referere til en eksisterende klasse.");
                test51.setResultDescription("Resultatet er fordelt på arkivdel.");
                test51.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test51);
                processList.add(test51);
                break;
            case "Noark 5-arkivuttrekk/loependeJournal":
                // Test 52 - Antall journalposter i løpende journal 
                Test test52 = new Test(reader.getId(), "Test 52", "Test 52 - Antall journalposter i løpende journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0052.xq",
                        "0052",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test52.setDescription("Opptelling av antall forekomster av elementet journalregistrering "
                        + "i loependeJournal.xml. "
                        + "Antall journalregistreringer (journalposter) i løpende journal kontrolleres mot det "
                        + "som er oppgitt i arkivuttrekk.xml.");
                test52.setResultDescription("Resultatet viser antall journalregistreringer (journalposter) "
                        + "i løpende journal og hvorvidt dette stemmer med oppgitt antall.");
                test52.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test52);
                processList.add(test52);

                // Test 53 - Antall journalposter for hvert år i løpende journal
                Test test53 = new Test(reader.getId(), "Test 53", "Test 53 - Antall journalposter for hvert år i løpende journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0053.xq",
                        "0053",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test53.setDescription("Opptelling av antall forekomster av elementet "
                        + "journalregistrering i loependeJournal.xml fordelt på årstallet journalposten ble opprettet. "
                        + "Antallet grupperes på årstallet i elementet journaldato i journalpost.");
                test53.setResultDescription("Resultatet viser antall journalregistreringer (journalposter), eventuelle avvik og "
                        + "en fordeling av journalpostene basert på årstall i journaldato.");
                test53.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test53);
                processList.add(test53);

                // Test 54 - Start- og sluttdato for journalpostene i løpende journal
                Test test54 = new Test(reader.getId(), "Test 54",
                        "Test 54 - Start- og sluttdato for journalpostene i løpende journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0054.xq",
                        "0054",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test54.setDescription("Angivelse av laveste og høyeste verdi i "
                        + "elementet journaldato i loependeJournal.xml.");
                test54.setResultDescription("Resultatet viser første og siste journaldato.");
                test54.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test54);
                processList.add(test54);

                // Test 55 - Antall skjermede journalposter i løpende journal
                Test test55 = new Test(reader.getId(), "Test 55", "Test 55 - Antall skjermede journalposter i løpende journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0055.xq",
                        "0055",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test55.setDescription("Opptelling av antall forekomster av elementet "
                        + "journalregistrering (journalpost) i loependeJournal.xml "
                        + "hvor elementet tilgangsrestriksjon forekommer.");
                test55.setResultDescription("Resultatet viser antall skjermede journalregistreringer (journalposter) "
                        + "i løpende journal.");
                test55.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test55);
                processList.add(test55);
                break;
            case "Noark 5-arkivuttrekk/offentligJournal":
                // Test 56 - Antall journalposter i offentlig journal 
                Test test56 = new Test(reader.getId(), "Test 56", "Test 56 - Antall journalposter i offentlig journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0056.xq",
                        "0056",
                        metadataCollection, dataCollection,
                        maxNumberOfResults, null, null);
                test56.setDescription("Opptelling av antall forekomster av elementet journalregistrering "
                        + "i offentligJournal.xml. "
                        + "Antall journalregistreringer (journalposter) i offentlig journal kontrolleres mot det "
                        + "som er oppgitt i arkivuttrekk.xml.");
                test56.setResultDescription("Resultatet viser antall journalregistreringer (journalposter) "
                        + "i offentlig journal og hvorvidt dette stemmer med oppgitt antall.");
                test56.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test56);
                processList.add(test56);

                // Test 57 - Antall journalposter for hvert år i offentlig journal
                Test test57 = new Test(reader.getId(), "Test 57", "Test 57 - Antall journalposter for hvert år i offentlig journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0057.xq",
                        "0057",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test57.setDescription("Opptelling av antall forekomster av elementet "
                        + "journalregistrering i offentligJournal.xml fordelt på årstallet journalposten ble opprettet. "
                        + "Antallet grupperes på årstallet i elementet journaldato i journalpost.");
                test57.setResultDescription("Resultatet viser antall journalregistreringer (journalposter), eventuelle avvik og "
                        + "en fordeling av journalpostene basert på årstall i journaldato.");
                test57.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test57);
                processList.add(test57);

                // Test 58 - Start- og sluttdato for journalpostene i offentlig journal
                Test test58 = new Test(reader.getId(), "Test 58",
                        "Test 58 - Start- og sluttdato for journalpostene i offentlig journal",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0058.xq",
                        "0058",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test58.setDescription("Angivelse av laveste og høyeste verdi i "
                        + "elementet journaldato i offentligJournal.xml.");
                test58.setResultDescription("Resultatet viser første og siste journaldato.");
                test58.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test58);
                processList.add(test58);
                break;
            case "Noark 5-arkivuttrekk/endringslogg":
                // Test 61 - Antall endringer i endringsloggen
                Test test61 = new Test(reader.getId(), "Test 61", "Test 61 - Antall endringer i endringsloggen",
                        "no/arkivverket/dataextracttools/arkade/modules/noark5/version_3_1/test0061.xq",
                        "0061",
                        null, dataCollection,
                        maxNumberOfResults, null, null);
                test61.setDescription("Opptelling av antall forekomster av endring i endringslogg.xml.");
                test61.setResultDescription("Resultatet av opptellingen.");
                test61.setLoggerName(loggerName);
                // Add process as read listener
                reader.addReadListener(test61);
                processList.add(test61);
                break;
            default:
                Logger.getLogger(loggerName).severe("Ukjent leser-id: " + reader.getId());
                break;
        }
    }
}
