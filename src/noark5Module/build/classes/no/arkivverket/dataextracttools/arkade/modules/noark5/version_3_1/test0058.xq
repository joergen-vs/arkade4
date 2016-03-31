xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 58 i versjon 14 av testoppleggsdokumentet.
 : Start- og sluttdato for journalpostene i offentlig journal.
 : Type: Analyse
 : Angivelse av laveste og høyeste verdi i elementet journaldato i offentligJournal.xml. 
 : Det forutsettes et en-til-en-forhold mellom journalpost og journalregistrering.
 :)
          
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5oj = "http://www.arkivverket.no/standarder/noark5/offentligJournal";
declare namespace util = "http://exist-db.org/xquery/util";
declare variable $testName external;
declare variable $longName external;
declare variable $testId external;
declare variable $testDescription external;
declare variable $resultDescription external;
declare variable $orderKey external;
declare variable $dataCollection external;
declare variable $rootDirectory external;
declare variable $maxNumberOfResults external;

let $offentligJournalDocFileName := "offentligJournal.xml"
let $offentligJournalDoc := doc(concat($dataCollection, "/", $offentligJournalDocFileName))

let $journaldatoer := $offentligJournalDoc/n5oj:offentligJournal/n5oj:journalregistrering/
                      n5oj:journalpost[n5oj:journaldato castable as xs:date]/xs:date(n5oj:journaldato) 
let $minJournaldato := min($journaldatoer)
let $maxJournaldato := max($journaldatoer)

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
          if (not($offentligJournalDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$offentligJournalDocFileName}</content>
          </resultItem>
          ) else ()
          }
         
          {
          if ($offentligJournalDoc)
          then (         
          <resultItem type="{if ($minJournaldato castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Laveste journaldato i offentlig journal</label>
            <content>{$minJournaldato}</content>
          </resultItem>
          ,
          <resultItem type="{if ($maxJournaldato castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Høyeste journaldato i offentlig journal</label>
            <content>{$maxJournaldato}</content>
          </resultItem>
          ) else ()}
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>
