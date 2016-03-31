xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 59 i versjon 14 av testoppleggsdokumentet.
 : Antall journalposter i arkivuttrekket.
 : Type: Analyse og kontroll
 : Sammenligning av det totale antallet journalposter i arkivstrukturen med antall 
 : journalposter i løpende og offentlig journal.
 : Antall forekomster av elementet registrering av typen journalpost i arkivstruktur.xml 
 : sammenlignes med antall forekomster av journalpost i loependeJournal.xml og offentligJournal.xml
 : Det forutsettes et en-til-en-forhold mellom journalpost og journalregistrering.
 :)
                           
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace xsi = "http://www.w3.org/2001/XMLSchema-instance";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace n5lj = "http://www.arkivverket.no/standarder/noark5/loependeJournal";
declare namespace n5oj = "http://www.arkivverket.no/standarder/noark5/offentligJournal";
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
let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $loependeJournalDocFileName := "loependeJournal.xml"
let $loependeJournalDoc := doc(concat($dataCollection, "/", $loependeJournalDocFileName))
let $offentligJournalDocFileName := "offentligJournal.xml"
let $offentligJournalDoc := doc(concat($dataCollection, "/", $offentligJournalDocFileName))

let $antallJournalPosterArkivstruktur := count($arkivstrukturDoc//n5a:registrering[@xsi:type = 'journalpost'])

let $antallJournalPosterLoependeJournal := count($loependeJournalDoc/n5lj:loependeJournal/n5lj:journalregistrering/n5lj:journalpost)
let $antallJournalPosterOppgittLoependeJournal :=
$loependeJournalDoc/n5lj:loependeJournal/n5lj:journalhode/data(n5lj:antallJournalposter)

let $antallJournalPosterOffentligJournal := count($offentligJournalDoc/n5oj:offentligJournal/n5oj:journalregistrering/n5oj:journalpost)
let $antallJournalPosterOppgittOffentligJournal := $offentligJournalDoc/n5oj:offentligJournal/n5oj:journalhode/data(n5oj:antallJournalposter)
 
let $periodeskille := 
  $arkivuttrekkDoc/aml:addml/aml:dataset/aml:dataObjects/aml:dataObject[@name="Noark 5-arkivuttrekk"]/
  aml:properties/aml:property[@name="info"]/aml:properties/aml:property[@name="additionalInfo"]/
  aml:properties/aml:property[@name="periode"]/aml:properties
  
let $inngaaendePeriodeskille := $periodeskille/aml:property[@name="inngaaendeSkille"]/data(aml:value)
let $utgaaendePeriodeskille := $periodeskille/aml:property[@name="utgaaendeSkille"]/data(aml:value)

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
          if (not($arkivstrukturDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$arkivstrukturDocFileName}</content>
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
          if (not($offentligJournalDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$offentligJournalDocFileName}</content>
          </resultItem>
          ) else ()
          }
         
          {
          if ($arkivuttrekkDoc and $arkivstrukturDoc and $loependeJournalDoc and $offentligJournalDoc)
          then (         

          <resultItem type="info">
            <label>Antall journalposter i arkivstrukturen</label>
            <content>{$antallJournalPosterArkivstruktur}</content>
          </resultItem>
          ,
          <resultItem type="info">
            <label>Antall journalposter i løpende journal</label>
            <content>{$antallJournalPosterLoependeJournal}</content>
          </resultItem>
          ,
          if ($antallJournalPosterOppgittLoependeJournal)
          then (
          <resultItem type="{if ($antallJournalPosterOppgittLoependeJournal castable as xs:integer)
                             then 'info'
                             else 'error'}">
            <label>Oppgitt antall journalposter i løpende journal</label>
            <content>{$antallJournalPosterOppgittLoependeJournal}</content>
          </resultItem>
          )
          else (
          <resultItem type="error">
            <label>Antall journalposter i løpende journal er ikke oppgitt i loependeJournal.xml (antallJournalposter).</label>
          </resultItem>        
          )
          ,
          if ($antallJournalPosterLoependeJournal and 
              $antallJournalPosterOppgittLoependeJournal castable as xs:integer and 
              $antallJournalPosterLoependeJournal != $antallJournalPosterOppgittLoependeJournal)
          then (
          <resultItem type="error">
            <label>Det faktiske antallet journalposter i løpende journal er forskjellig fra antallet oppgitt i loependeJournal.xml (antallJournalposter).</label>
          </resultItem>
          )
          else ()
          ,        
          <resultItem type="info">
            <label>Antall journalposter i offentlig journal</label>
            <content>{$antallJournalPosterOffentligJournal}</content>
          </resultItem>
          ,
          if ($antallJournalPosterOppgittOffentligJournal)
          then (
          <resultItem type="{if ($antallJournalPosterOppgittOffentligJournal castable as xs:integer)
                             then 'info'
                             else 'error'}">
            <label>Oppgitt antall journalposter i offentlig journal</label>
            <content>{$antallJournalPosterOppgittOffentligJournal}</content>
          </resultItem>
          )
          else (
          <resultItem type="error">
            <label>Antall journalposter i offentlig journal er ikke oppgitt i offentligJournal.xml (antallJournalposter).</label>
          </resultItem>        
          )
          ,
          if ($antallJournalPosterOffentligJournal and 
              $antallJournalPosterOppgittOffentligJournal castable as xs:integer and
              $antallJournalPosterOffentligJournal != $antallJournalPosterOppgittOffentligJournal)
          then (
          <resultItem type="error">
            <label>Det faktiske antallet journalposter i offentlig journal er forskjellig fra antallet oppgitt i offentligJournal.xml (antallJournalposter).</label>
          </resultItem>
          )
          else ()
          ,
          <resultItem type="{if ($inngaaendePeriodeskille != "")
                             then 'info'
                             else 'error'}">
            <label>Periodeskille - Inngående skille</label>
            <content>{if ($inngaaendePeriodeskille != "")
                      then $inngaaendePeriodeskille
                      else "[IKKE ANGITT]"}</content>
          </resultItem>
          ,  
          <resultItem type="{if ($utgaaendePeriodeskille != "")
                             then 'info'
                             else 'error'}">
            <label>Periodeskille - Utgående skille</label>
            <content>{if ($utgaaendePeriodeskille != "")
                      then $utgaaendePeriodeskille
                      else "[IKKE ANGITT]"}</content>
          </resultItem>
          ,
          if ($inngaaendePeriodeskille = "skarpt" and $utgaaendePeriodeskille = "skarpt")
          then (if ($antallJournalPosterArkivstruktur = $antallJournalPosterLoependeJournal and 
                    $antallJournalPosterLoependeJournal = $antallJournalPosterOffentligJournal)
                then ()
                else (
          <resultItem type='error'>
            <label>Når både inngående og utgående periodeskille er skarpt, skal antall journalposter i 
arkivstruktur, løpende journal og offentlig journal være likt.</label>
          </resultItem>
          ))
          else ()
         ) else ()}
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>

