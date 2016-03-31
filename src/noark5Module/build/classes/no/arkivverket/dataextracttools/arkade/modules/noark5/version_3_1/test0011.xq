xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 11 i versjon 14 av testoppleggsdokumentet.
 : Antall mapper for hvert år i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall mapper som er opprettet for hvert år i arkivstruktur.xml.
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
        let $antallManglendeOpprettetDato :=
                count($arkivdel//n5a:mappe/n5a:opprettetDato[normalize-space(./text())=""])
        let $mapper := $arkivdel//n5a:mappe
        let $antallMapper := count($mapper)
        let $antallOpprettetDato := count($arkivdel//n5a:mappe/n5a:opprettetDato)

        let $years := for $mappe in $mapper
                      let $opprettetDato := $mappe/normalize-space(string(data(n5a:opprettetDato)))
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
              <resultItem name="antallForekomsterAvMappe" type="{if ($antallMapper > 0)
                                                                 then 'info'
                                                                 else 'warning'}">
                <label>Antall forekomster av mappe</label>
                <content>{$antallMapper}</content> 
              </resultItem>
            
              <resultItem name="antallForekomsterAvOpprettetDato" type="{if ($antallOpprettetDato > 0)
                                                                         then 'info'
                                                                         else 'warning'}">
                <label>Antall forekomster av opprettetDato</label>
                <content>{$antallOpprettetDato}</content> 
              </resultItem>
            
              {
              if ($antallManglendeOpprettetDato > 0)
              then 
              (
              <resultItem name="antallUtenInnhold" type="error">
                <label>Antall opprettetDato uten innhold</label>
                <content>{$antallManglendeOpprettetDato}</content>
              </resultItem>
              ) else ()
              }
            </resultItems>
          </resultItem>
          
          {
          if ($antallMapper > 0 and $antallOpprettetDato > 0)
          then
          (
          <resultItem name="detaljertResultat">  
            <resultItems>
              <resultItem name="fordelingAvMapperPerAar" type="info">
                <label>Fordeling av mapper per år</label>
                <resultItems>
            
                {
                for $year in distinct-values($years)
                let $yearOccurrence := $years[. = $year]
                order by $year
                return 
                  <resultItem name="{if ($year != -1)
                                     then 'verdiOgAntall'
                                     else 'antallUgyldigeVerdier'}" type="{if ($year != -1)
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
