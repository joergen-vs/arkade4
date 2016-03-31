xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 18 i versjon 14 av testoppleggsdokumentet.
 : Antall registreringer for hvert år i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall registreringer som er opprettet hvert enkelt år i arkivstruktur.xml.
 : Opptellingen grupperes etter årstallet i den enkelte registrerings opprettetDato.
 : Resultatet vises fordelt på arkivdel og årstall.
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
        let $registreringer := $arkivdel//n5a:registrering
        let $antallRegistreringer := count($registreringer)
        let $antallRegistreringerUtenOpprettetdato := count($registreringer[not(n5a:opprettetDato)])
        let $antallManglendeOpprettetDato :=
                 count($registreringer/n5a:opprettetDato[normalize-space(./text())=""])
        let $antallOpprettetDato := count($registreringer/n5a:opprettetDato)
        let $years := for $registrering in $arkivdel//n5a:registrering
                      let $opprettetDato := $registrering/normalize-space(string(data(n5a:opprettetDato)))
                      where ($opprettetDato != "")
                      return if ($opprettetDato castable as xs:dateTime)
                             then year-from-dateTime(xs:dateTime(data($opprettetDato)))
                             else (-1)
        
        return 
        <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallRegistreringer > 0)
                                 then ('info')
                                 else ('error')}">
                <label>Antall registreringer i arkivdelen</label>
                <content>{$antallRegistreringer}</content>
              </resultItem>
              {
              if ($antallRegistreringer > 0)
              then 
              (
              <resultItem name="totaltAntallForekomster" type="{if ($antallOpprettetDato > 0)
                                                               then 'info'
                                                               else 'error'}">
                <label>Antall forekomster av opprettetDato</label>
                <content>{$antallOpprettetDato}</content> 
              </resultItem>,
              <resultItem type="{if ($antallRegistreringerUtenOpprettetdato = 0)
                                 then ('info')
                                 else ('error')}">
                <label>Antall registreringer uten opprettetDato</label>
                <content>{$antallRegistreringerUtenOpprettetdato}</content>
              </resultItem>
              ) else ()}
              {
              if ($antallManglendeOpprettetDato > 0)
              then 
              (
              <resultItem name="antallUtenInnhold" type="error">
                <label>Antall opprettetDato uten innhold</label>
                <content>{$antallManglendeOpprettetDato}</content>
              </resultItem>
              ) else ()}
            </resultItems>
          </resultItem>
          {
          if (not(empty($years))) 
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="fordelingAvRegistreringerPerAar" type="info">
                <label>Fordeling av registreringer per år</label>
                <resultItems>
                  {
                  for $year in distinct-values($years)
                  let $yearOccurrence := $years[. = $year]
                  order by $year
                  return
                  <resultItem name="{if ($year != -1)
                                     then 'verdiOgAntall'
                                     else 'AntallUgyldigeVerdier'}" type="{if ($year != -1)
                                                                           then 'info'
                                                                           else 'error'}">
                    <label>{if ($year != -1)
                          then $year
                          else "Antall ugyldige verdier i opprettetDato"}
                    </label>
                    <content>{count($yearOccurrence)}</content>
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
