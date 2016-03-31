xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 22 i versjon 14 av testoppleggsdokumentet.
 : Journalpostenes status i arkivstrukturen.
 : Type: Analyse
 : Opptelling av de forskjellige verdiene i journalpostenes status i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Element: journalstatus
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
      ) 
      else 
      (
      for $arkivdel in $arkivstrukturDoc//n5a:arkivdel
      let $journalposter := $arkivdel//n5a:registrering[@xsi:type="journalpost"]
      let $antallJournalposterUtenJournalstatus := count($journalposter[not(n5a:journalstatus)])
      return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Antall journalposter i arkivdelen</label>
                <content>{count($journalposter)}</content>
              </resultItem>
              <resultItem type="{if ($antallJournalposterUtenJournalstatus = 0)
                                 then ('info')
                                 else ('error')}">
                <label>Antall journalposter uten journalstatus</label>
                <content>{$antallJournalposterUtenJournalstatus}</content>
              </resultItem>
            </resultItems>
          </resultItem> 
          {
          if ($journalposter) then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="fordelingAvJournalstatus" type="info">
                <label>Fordeling av journalstatus</label>
                <resultItems>
                  {
                  for $journalstatus in distinct-values($journalposter/n5a:journalstatus)
                  let $antall := count($journalposter[n5a:journalstatus=$journalstatus])
                  order by $journalstatus
                  return
                  <resultItem name="journalstatus" type="info">
                    <label>{$journalstatus}</label>
                    <content>{$antall}</content>
                  </resultItem>
                  }
                </resultItems>
              </resultItem>
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
