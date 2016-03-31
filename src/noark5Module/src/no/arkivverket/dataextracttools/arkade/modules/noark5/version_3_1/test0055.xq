xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 55 i versjon 14 av testoppleggsdokumentet.
 : Antall skjermede journalposter i løpende journal.
 : Type: Analyse
 : Opptelling av antall forekomster av elementet journalregistrering (journalpost)
 : i loependeJournal.xml hvor elementet tilgangsrestriksjon forekommer.
 : Det forutsettes et en-til-en-forhold mellom journalpost og journalregistrering.
 :)
          
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
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

let $loependeJournalDocFileName := "loependeJournal.xml"
let $loependeJournalDoc := doc(concat($dataCollection, "/", $loependeJournalDocFileName))
let $antallSkjermedeJournalposter := count($loependeJournalDoc/n5lj:loependeJournal/n5lj:journalregistrering/
                      n5lj:journalpost[n5lj:tilgangsrestriksjon])

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
          if (not($loependeJournalDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$loependeJournalDocFileName}</content>
          </resultItem>
          ) else ()
          }
          <resultItem type="info">
            <label>Antall skjermede journalposter</label>
            <content>{$antallSkjermedeJournalposter}</content>
          </resultItem>
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>
