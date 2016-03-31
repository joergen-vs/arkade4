xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.13 2013-11-27
 :
 : Test 27 i versjon 14 av testoppleggsdokumentet.
 : Start- og sluttdato for dokumentene i arkivstrukturen.
 : Type: Analyse
 : Angivelse av første og siste dato for når dokumentene ble registrert, angitt i elementet
 : opprettetDato i registreringene i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Element: opprettetDato
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
    
      for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
      let $registreringer := $arkivdel//n5a:registrering
      let $antallRegistreringer := count($registreringer)
      let $opprettetDatoer := $registreringer[n5a:opprettetDato castable as xs:dateTime]/xs:dateTime(n5a:opprettetDato)
      let $antallUgyldigeOpprettetDatoer := count($registreringer[not(n5a:opprettetDato castable as xs:dateTime)])
      let $minOpprettetDato := min($opprettetDatoer)
      let $maxOpprettetDato := max($opprettetDatoer)  
      let $antallDokumentobjekt := count($arkivdel//n5a:dokumentobjekt)
      return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if (not(empty($minOpprettetDato)))
                                 then 'info'
                                 else 'error'}">          
                <label>Første opprettetDato</label>
                <content>{$minOpprettetDato}</content>
              </resultItem>
              <resultItem type="{if (not(empty($maxOpprettetDato)))
                                 then 'info'
                                 else 'error'}">
                <label>Siste opprettetDato</label>
                <content>{$maxOpprettetDato}</content>
              </resultItem>
              <resultItem type="{if ($antallUgyldigeOpprettetDatoer = 0)
                                 then 'info'
                                 else 'error'}">
                <label>Antall ugyldige opprettetDato i registrering</label>
                <content>{$antallUgyldigeOpprettetDatoer}</content>
              </resultItem>
              <resultItem type="{if ($antallRegistreringer > 0)
                                 then 'info'
                                 else 'error'}">
                <label>Antall registreringer</label>
                <content>{$antallRegistreringer}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
      </resultItem>
      )
      }
    </resultItems>
  </result>
</activity>




