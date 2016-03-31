xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 16 i versjon 14 av testoppleggsdokumentet.
 : Antall registreringer i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall forekomster av registrering i arkivstruktur.xml.
 : I tillegg kontrolleres det totale antallet opp mot oppgitt antall i arkivuttrekk.xml.
 : Resultatet vises fordelt på arkivdel og registreringstype.
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

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))

let $totaltAntallRegistreringer := count($arkivstrukturDoc//n5a:registrering)
let $dataObjectArkivstruktur := $arkivuttrekkDoc//aml:dataObject[@name="arkivstruktur"]
let $antallRegistreringerOppgitt :=
$dataObjectArkivstruktur//aml:property[@name="info"]/aml:properties/aml:property[@name="numberOfOccurrences"
 and aml:value="registrering"]/aml:properties/aml:property[@name="value"]/data(aml:value)

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
      let $antallRegistreringerIArkivdel := count($registreringer)
      let $antallRegistreringerUtenType := count($registreringer[not(@xsi:type)])
      let $registreringstyper := distinct-values($registreringer/@xsi:type)                                                      
      return 
      <resultItem name="arkivdel" type="{if ($antallRegistreringerIArkivdel > 0)
                                         then 'info'
                                         else 'warning'}">  
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
          
              <resultItem name="antallRegisteringerIArkivdel" type="{if ($antallRegistreringerIArkivdel > 0)
                                                                     then 'info'
                                                                     else 'warning'}">
                <label>Antall registreringer i arkivdelen</label>
                <content>{$antallRegistreringerIArkivdel}</content>
              </resultItem>
                  
              <resultItem type="{if ($totaltAntallRegistreringer > 0)
                                 then 'info'
                                 else 'warning'}">
                <label>Totalt antall registreringer i arkivuttrekket.</label>
                <content>{$totaltAntallRegistreringer}</content>
              </resultItem>  
              <resultItem type="{if ($antallRegistreringerOppgitt > 0)
                                 then 'info'
                                 else 'warning'}">
                <label>Antall registreringer oppgitt i arkivuttrekk.xml</label>
                <content>{$antallRegistreringerOppgitt}</content> 
              </resultItem>   
              <resultItem type="{if ($totaltAntallRegistreringer = $antallRegistreringerOppgitt)
                                 then 'info'
                                 else 'error'}" >
                <label>Det faktiske antallet stemmer {if ($totaltAntallRegistreringer = $antallRegistreringerOppgitt)
                                                      then ()
                                                      else "ikke "
                                               }med det oppgitte.</label>
              </resultItem>  
            </resultItems>
          </resultItem>

          {
          if (not(empty($registreringstyper))) 
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="fordelingAvRegistreringstyper" type="info">
                <label>Fordeling av registreringstyper</label>
                <resultItems>
                  {
                  for $registreringstype in $registreringstyper
                  let $antallAvEnType := count($registreringer[@xsi:type=$registreringstype])
                  return 
                  <resultItem type="info">
                    <label>{$registreringstype}</label>
                    <content>{$antallAvEnType}</content>
                  </resultItem>
                  }
                  {
                  if ($antallRegistreringerUtenType > 0)
                  then (
                  <resultItem type="warning">
                    <label>[Type ikke spesifisert]</label>
                    <content>{$antallRegistreringerUtenType}</content>
                  </resultItem>
                  ) else () }
                </resultItems>
              </resultItem>
            </resultItems>
          </resultItem>
          ) else ()
          }
        </resultItems>
      </resultItem>
      )}
    </resultItems>
  </result>
</activity>
