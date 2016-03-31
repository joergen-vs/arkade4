xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 10 i versjon 14 av testoppleggsdokumentet.
 : Antall mapper i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall forekomster av mappe i arkivstruktur.xml. 
 : I tillegg kontrolleres det totale antallet opp mot oppgitt antall i arkivuttrekk.xml.
 : Resultatet vises fordelt på arkivdel og mappetype.
 :
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
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

declare function local:antallElementer($f as element()*, $b as xs:string, $nivaa as xs:integer) as element()* {
  let $barn := util:eval(concat("$f/", $b))
  let $antallElementer := count($barn)
  return 
    if (not(empty($barn)))
    then (
        <resultItem type="info">
          <label>Nivå {$nivaa}</label>
          <content>{$antallElementer}</content>
        </resultItem>,
        local:antallElementer($barn, $b, $nivaa+1)
     ) else ()
};

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))

let $totaltAntallMapper := count($arkivstrukturDoc//n5a:mappe)
let $dataObjectArkivstruktur := $arkivuttrekkDoc//aml:dataObject[@name="arkivstruktur"]
let $antallMapperOppgitt :=
$dataObjectArkivstruktur//aml:property[@name="info"]/aml:properties/aml:property[@name="numberOfOccurrences"
 and aml:value="mappe"]/aml:properties/aml:property[@name="value"]/data(aml:value)

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
      let $mapper := $arkivdel//n5a:mappe
      let $antallMapperIArkivdel := count($mapper)
      let $antallMapperUtenType := count($mapper[not(@xsi:type)])
      let $mappetyper := distinct-values($mapper/@xsi:type)
      let $klasser := $arkivdel//n5a:klasse
      let $mapperIKlasser := $klasser[n5a:mappe]
      return 
      <resultItem name="arkivdel" type="{if ($antallMapperIArkivdel > 0)
                                                 then 'info'
                                                 else 'warning'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
          
              <resultItem name="elementtypeMedMappestruktur" type="info">
                <label>Elementtype med mappestruktur</label>
                <content>{if (not(empty($mapperIKlasser)))
                          then name($klasser[1])
                          else if ($antallMapperIArkivdel > 0)
                               then (name($arkivdel[1]))
                               else ("[Ukjent]")}</content>
              </resultItem>

              <resultItem name="antallMapperIArkivdel" type="info">
                 <label>Antall mapper i arkivdelen</label>
                 <content>{$antallMapperIArkivdel}</content>
              </resultItem>
                      
              <resultItem type="{if ($totaltAntallMapper > 0)
                                 then 'info'
                                 else 'warning'}">
                <label>Totalt antall mapper i arkivuttrekket.</label>
                <content>{$totaltAntallMapper}</content>
              </resultItem>  
        
              <resultItem type="info">
                <label>Antall mapper oppgitt i arkivuttrekk.xml</label>
                <content>{$antallMapperOppgitt}</content> 
              </resultItem>  
        
              <resultItem type="{if ($totaltAntallMapper = $antallMapperOppgitt)
                                 then 'info'
                                 else 'error'}" >
                <label>Det faktiske antallet stemmer {if ($totaltAntallMapper = $antallMapperOppgitt)
                                                      then ()
                                                      else "ikke "
                                                     }med det oppgitte.</label>
              </resultItem>
            </resultItems>
          </resultItem>
          
          {
          if (not (empty($mapper))) 
          then (
          <resultItem name="detaljertResultat">  
            <resultItems>
              <resultItem name="fordelingAvMappetyper" type="info">
                <label>Fordeling av mappetyper</label>
                <resultItems>
                  {
                  for $mappetype in distinct-values($mapper/@xsi:type)
                  let $antallAvEnType := count($mapper[@xsi:type=$mappetype])
                  order by $mappetype
                  return
                  <resultItem type="info">
                    <label>{$mappetype}</label>
                    <content>{$antallAvEnType}</content>
                  </resultItem>
                  }
                  {
                  if ($antallMapperUtenType > 0)
                  then (
                  <resultItem type="warning">
                    <label>[Type ikke spesifisert]</label>
                    <content>{$antallMapperUtenType}</content>
                  </resultItem>
                  ) else () }
                </resultItems>
              </resultItem>
            
              <resultItem name="antallMapperPerNivaa">
                <label>Antall mapper per nivå</label>
                <resultItems>
                  {local:antallElementer(if (not(empty($mapperIKlasser)))
                                         then $klasser
                                         else $arkivdel, "n5a:mappe", 1)}
                </resultItems>
              </resultItem>
            </resultItems>
          </resultItem>  
          ) else ()}
        </resultItems>    
      </resultItem>
      )
      }
    </resultItems>
  </result>
</activity>
