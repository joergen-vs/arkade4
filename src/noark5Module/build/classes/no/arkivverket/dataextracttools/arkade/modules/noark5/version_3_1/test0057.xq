xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 57 i versjon 14 av testoppleggsdokumentet.
 : Antall journalposter for hvert år i offentlig journal.
 : Type: Analyse
 : Opptelling av antall forekomster av elementet journalregistrering i 
 : offentligJournal.xml fordelt på årstallet journalposten ble opprettet.
 : Antallet grupperes på årstallet i elementet journaldato i journalpost.
 : Det forutsettes et en-til-en-forhold mellom journalpost og journalregistrering.
 : 
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
let $antallJournaldatoer :=
count($offentligJournalDoc/n5oj:offentligJournal/n5oj:journalregistrering/n5oj:journalpost/n5oj:journaldato)
let $antallManglendeJournaldatoer := count($offentligJournalDoc/n5oj:offentligJournal/n5oj:journalregistrering/n5oj:journalpost/n5oj:journaldato[normalize-space(./text())=""])
let $alleAarstall := for $journalpost in $offentligJournalDoc/n5oj:offentligJournal/n5oj:journalregistrering/n5oj:journalpost
              let $journaldato := $journalpost/normalize-space(string(data(n5oj:journaldato)))
              where ($journaldato != "")
              return if ($journaldato castable as xs:date)
                     then year-from-date(xs:date(data($journaldato)))
                     else (-1)
let $antallUgyldigeAarstall := count($alleAarstall[. = -1])

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
    ) else()
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
          <resultItem name="totaltAntallForekomster" type="{if ($antallJournaldatoer > 0)
                                                            then 'info'
                                                            else 'warning'}">
            <label>Antall forekomster av journaldato</label>
            <content>{$antallJournaldatoer}</content> 
          </resultItem>
          ,
          <resultItem name="antallUtenInnhold" type="{if ($antallManglendeJournaldatoer = 0)
                                                      then 'info'
                                                      else 'error'}">
            <label>Antall journaldato uten innhold</label>
            <content>{$antallManglendeJournaldatoer}</content>
          </resultItem>
          ,
          <resultItem name="antallUgyldigeVerdier" type="{if ($antallUgyldigeAarstall = 0)
                                                          then 'info'
                                                          else 'error'}">
            <label>Antall ugyldige journaldatoer</label>
            <content>{$antallUgyldigeAarstall}</content>
          </resultItem>
          ) else()
          }        
        </resultItems>
      </resultItem>
     
      {
      if ($antallJournaldatoer != $antallUgyldigeAarstall and $offentligJournalDoc)
      then (
      <resultItem name="detaljertResultat">
        <resultItems>        
           <resultItem name="fordelingAvAarstall">
             <label>Fordeling av journalposter basert på årstall i journaldato.</label>
             <resultItems>
               {
               for $aarstall in distinct-values($alleAarstall)
               let $aarstallForekomst := $alleAarstall[. = $aarstall]
               order by $aarstall
               return 
               if ($aarstall != -1)
               then (
               <resultItem name="verdiOgAntall" type="info">
                 <label>{$aarstall}</label>
                 <content>{count($aarstallForekomst)}</content>
               </resultItem>
               ) else ()
               }
             </resultItems>
           </resultItem>
        </resultItems>
      </resultItem>
      ) else ()
      }
    </resultItems>
  </result>
</activity>