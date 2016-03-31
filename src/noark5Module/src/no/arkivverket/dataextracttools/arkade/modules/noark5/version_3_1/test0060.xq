xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 60 i versjon 14 av testoppleggsdokumentet.
 : Start- og sluttdato i arkivuttrekket.
 : Type: Analyse og kontroll
 : 
 : Sammenligning av start- og sluttdato for journalposter i arkivstrukturen med journalposter 
 : i løpende og offentlig journal.
 : Elementet opprettetDato i registrering i arkivstruktur.xml sammenlignes med elementet 
 : journaldato i loependeJournal.xml og offentligJournal.xml.
 :
 : Ved skarpt periodeskille i begge ender, skal alle startdatoene være like, og alle sluttdatoene.
 : Ved mykt periodeskille kan datoene i arkivstrukturen være forskjellig fra journalene.
 : Journalene skal alltid ha samme startdato og samme sluttdato.
 :
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

let $minOpprettetDato := min(for $opprettetDato in
$arkivstrukturDoc//n5a:opprettetDato[../@xsi:type="journalpost" and . castable as xs:dateTime] return
 xs:dateTime($opprettetDato))
let $maxOpprettetDato := max(for $opprettetDato in $arkivstrukturDoc//n5a:opprettetDato[../@xsi:type="journalpost" and . castable as xs:dateTime] return
 xs:dateTime($opprettetDato))

let $journalStartDatoLoependeJournal := 
  $loependeJournalDoc/n5lj:loependeJournal/n5lj:journalhode/data(n5lj:journalStartDato)
let $journalSluttDatoLoependeJournal :=
  $loependeJournalDoc/n5lj:loependeJournal/n5lj:journalhode/data(n5lj:journalSluttDato)

let $journalStartDatoOffentligJournal :=
  $offentligJournalDoc/n5oj:offentligJournal/n5oj:journalhode/data(n5oj:journalStartDato)
let $journalSluttDatoOffentligJournal :=
  $offentligJournalDoc/n5oj:offentligJournal/n5oj:journalhode/data(n5oj:journalSluttDato)
  
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
            <label>Laveste opprettetDato i arkivstrukturen (startdatoen)</label>
            <content>{$minOpprettetDato}</content>
          </resultItem>
          ,
          <resultItem type="info">
            <label>Høyeste opprettetDato i arkivstrukturen</label>
            <content>{$maxOpprettetDato}</content>
          </resultItem>
          ,
          if ($minOpprettetDato > $maxOpprettetDato)
          then
          <resultItem type='error'>
            <label>Startdatoen i arkivstrukturen er senere enn sluttdatoen.</label>
            <content>{$minOpprettetDato} / {$maxOpprettetDato}</content>
          </resultItem>
          else (if ($minOpprettetDato = $maxOpprettetDato)
          then
          <resultItem type='warning'>
            <label>Startdatoen i arkivstrukturen er lik sluttdatoen.</label>
            <content>{$minOpprettetDato} / {$maxOpprettetDato}</content>
          </resultItem>
          else ())
          ,
          <resultItem type="{if ($journalStartDatoLoependeJournal castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Løpende journal - journalStartDato</label>
            <content>{$journalStartDatoLoependeJournal}</content>
          </resultItem>
          ,
          <resultItem type="{if ($journalSluttDatoLoependeJournal castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Løpende journal - journalSluttDato</label>
            <content>{$journalSluttDatoLoependeJournal}</content>
          </resultItem>
          ,
          <resultItem type="{if ($journalStartDatoOffentligJournal castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Offentlig journal - journalStartDato</label>
            <content>{$journalStartDatoOffentligJournal}</content>
          </resultItem>
          ,
          <resultItem type="{if ($journalSluttDatoOffentligJournal castable as xs:date)
                             then 'info'
                             else 'error'}">
            <label>Offentlig journal - journalSluttDato</label>
            <content>{$journalSluttDatoOffentligJournal}</content>
          </resultItem>
          ,
          if ($journalStartDatoLoependeJournal != $journalStartDatoOffentligJournal)
          then
          <resultItem type='error'>
            <label>Startdatoen i løpende journal og startdatoen i offentlig journal er forskjellige.</label>
            <content>{$journalStartDatoLoependeJournal} / {$journalStartDatoOffentligJournal}</content>
          </resultItem>
          else ()
          ,
          if ($journalSluttDatoLoependeJournal != $journalSluttDatoOffentligJournal)
          then
          <resultItem type='error'>
            <label>Sluttdatoen i løpende journal og sluttdatoen i offentlig journal er forskjellige.</label>
            <content>{$journalSluttDatoLoependeJournal} / {$journalSluttDatoOffentligJournal}</content>
          </resultItem>
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
          then if ($journalSluttDatoLoependeJournal = $journalSluttDatoOffentligJournal)
                then()
                else (
          <resultItem type='error'>
            <label>Når både inngående og utgående periodeskille er skarpt, skal alle de tre startdatoene være like, og alle sluttdatoene.</label>
          </resultItem>
          ) 
          else ()
          
         ) else ()}
        </resultItems>
      </resultItem>
    </resultItems>      
  </result>
</activity>

