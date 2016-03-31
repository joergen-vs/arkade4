xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 52 i versjon 14 av testoppleggsdokumentet.
 : Antall journalposter i løpende journal.
 : Type: Analyse og kontroll
 : Opptelling av antall forekomster av elementet journalregistrering i loependeJournal.xml.
 : Antall journalregistreringer (journalposter) i løpende journal kontrolleres mot det som er
 : oppgitt i arkivuttrekk.xml.
 : Det forutsettes et en-til-en-forhold mellom journalpost og journalregistrering.
:)
          
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
declare namespace n5lj = "http://www.arkivverket.no/standarder/noark5/loependeJournal";
declare namespace util = "http://exist-db.org/xquery/util";
declare variable $testName external;
declare variable $longName external;
declare variable $testId external;
declare variable $testDescription external;
declare variable $resultDescription external;
declare variable $orderKey external;
declare variable $metadataCollection external;
declare variable $dataCollection external;
declare variable $rootDirectory external;
declare variable $maxNumberOfResults external;

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $loependeJournalDocFileName := "loependeJournal.xml"
let $loependeJournalDoc := doc(concat($dataCollection, "/", $loependeJournalDocFileName))
let $antallJournalposter := count($loependeJournalDoc/n5lj:loependeJournal/n5lj:journalregistrering/n5lj:journalpost)
let $dataObjectOffentligJournal := $arkivuttrekkDoc//aml:dataObject[@name="loependeJournal"]
let $oppgittAntallJournalposter := $dataObjectOffentligJournal//aml:property[@name="info"]/aml:properties/aml:property[@name="numberOfOccurrences"
 and aml:value="journalregistrering"]/aml:properties/aml:property[@name="value"]/data(aml:value)

return
<activity name="{$testName}"
  longName="{if ($longName ne "") then ($longName) else ($testName)}"
  orderKey="{$orderKey}">
  <identifiers>
    <identifier name="id" value="{$testId}"/>
    <identifier name="uuid" value="{util:uuid()}"/>
  </identifiers>  
  {
  if ($testDescription ne "") then ( 
  <description>{$testDescription}</description>
  ) else ()
  }
  <result>
     {
    if ($resultDescription ne "") then (
    <description>{$resultDescription}</description>
    )
    else()
    }
    <resultItems>
      <resultItem name="overordnetResultat">
        <resultItems>
          {
          if (not($arkivuttrekkDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$arkivuttrekkDocFileName}</content>
          </resultItem>
          ) else ()
          }
          
          {
          if (not($loependeJournalDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$loependeJournalDocFileName}</content>
          </resultItem>
          ) else ()
          }
         
          {
          if ($arkivuttrekkDoc and $loependeJournalDoc)
          then (         
          <resultItem type="{if ($antallJournalposter > 0)
                             then 'info'
                             else 'warning'}">
            <label>Antall journalposter</label>
            <content>{$antallJournalposter}</content>
          </resultItem>
          ,
          <resultItem type="{if ($oppgittAntallJournalposter > 0)
                             then 'info'
                             else 'warning'}">
            <label>Oppgitt antall journalposter</label>
            <content>{$oppgittAntallJournalposter}</content>
          </resultItem>
          ,
          if ($antallJournalposter and
              $oppgittAntallJournalposter castable as xs:integer and
              $antallJournalposter != $oppgittAntallJournalposter)
          then (
          <resultItem type="error">
            <label>Det faktiske antallet journalposter i løpende journal er forskjellig fra antallet oppgitt i arkivuttrekk.xml.</label>
          </resultItem>
          ) else (
            if ($antallJournalposter = $oppgittAntallJournalposter)
            then (
          <resultItem type="info">
            <label>Det faktiske antallet journalposter i løpende journal stemmer med antallet oppgitt i arkivuttrekk.xml.</label>
          </resultItem>
            ) else (
          <resultItem type="warning">
            <label>Kan ikke utføre kontrollen</label>
          </resultItem>
            )
          )
        ) else ()}
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>
