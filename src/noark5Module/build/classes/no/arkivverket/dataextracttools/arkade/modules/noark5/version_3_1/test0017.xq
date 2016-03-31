xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-26
 :
 : Test 17 i versjon 14 av testoppleggsdokumentet.
 : Journalposttyper og journalposttilknytning i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall forskjellige journalposttyper i arkivstruktur.xml. 
 : Element i arkivstrukturen: <registrering> av type "journalpost".
 : I tillegg kontrolleres det at alle journalposter har hovedokument, dvs. 
 : at hver journalpost har en dokumentbeskrivelse som er tilknyttet journalposten 
 : som hoveddokument. Aktuelt element i dokumentbeskrivelse er tilknyttetRegistreringSom.
 :
 : Resultatet vises fordelt på arkivdel.
 : 
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
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

let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))

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
      {
      if (not($arkivstrukturDoc))
      then (
      <resultItem name="manglendeFil" type="error">
        <label>Manglende fil</label>
        <content>{$arkivstrukturDocFileName}</content>
      </resultItem>
      ) else (
      
      for $arkivdel in $arkivstrukturDoc//n5a:arkivdel
      let $journalposter := $arkivdel//n5a:registrering[@xsi:type="journalpost"]
      let $journalposterUtenHoveddokument :=
      $arkivdel//n5a:registrering[@xsi:type="journalpost"][n5a:dokumentbeskrivelse][not(n5a:dokumentbeskrivelse/n5a:tilknyttetRegistreringSom
      = 'Hoveddokument')]
      let $antallJournalposter := count($journalposter)
      let $antallJournalposterUtenHoveddokument := count($journalposterUtenHoveddokument)

      let $journalposttyper := distinct-values($arkivdel//n5a:registrering[@xsi:type="journalpost"]/n5a:journalposttype)
      return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem name="antallJournalposterIArkivdel" type="info">
                <label>Antall journalposter i arkivdelen</label>
                <content>{$antallJournalposter}</content>
              </resultItem>
              <resultItem name="antallJournalposterUtenHoveddokument" type="{if
              ($antallJournalposterUtenHoveddokument eq 0) then 'info' else 'error'}">
                <label>Antall journalposter uten hoveddokument</label>
                <content>{$antallJournalposterUtenHoveddokument}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          {
          if ($antallJournalposter gt 0) then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="fordelingAvJournalposttyper" type="info">
                <label>Fordeling av journalposttyper</label>
                <resultItems>
                  {
                  for $journalposttype in $journalposttyper
                  let $antall := count($journalposter[n5a:journalposttype=$journalposttype])
                  order by $journalposttype
                  return
                  <resultItem type="info">
                    <label>{$journalposttype}</label>
                    <content>{$antall}</content>
                  </resultItem>
                  }
                </resultItems>
              </resultItem>
               {if ($antallJournalposterUtenHoveddokument gt 0 and $maxNumberOfResults gt 0) then (
              <resultItem name="journalposterUtenHoveddokument">
                <label>Journalposter uten hoveddokument.
                       Viser {if ($maxNumberOfResults >= $antallJournalposterUtenHoveddokument) 
                              then (concat(" ", $antallJournalposterUtenHoveddokument, " (alle).")) 
                              else concat($maxNumberOfResults, " av ", $antallJournalposterUtenHoveddokument, ".")} 
                </label>
                <resultItems>
                {
                for $journalpostUtenHoveddokument at $pos in $journalposterUtenHoveddokument
                return
                  if ($pos <= $maxNumberOfResults)
                  then (
                  <resultItem type="error">
                    <identifiers>
                      <identifier name="systemID"
                                  value="{$journalpostUtenHoveddokument/n5a:systemID/normalize-space()}"/>
                    </identifiers>
                    <label>Antall dokumentbeskrivelser i journalposten</label>
                    <content>{count($journalpostUtenHoveddokument/n5a:dokumentbeskrivelse)}</content>
                  </resultItem>
                  ) else ()
                }  
                </resultItems>
              </resultItem>
               
               ) else ()
               }
            </resultItems>
          </resultItem>
          ) else ()
          }
        </resultItems>
      </resultItem>
      )
      }         
    </resultItems>
  </result>
</activity>
  