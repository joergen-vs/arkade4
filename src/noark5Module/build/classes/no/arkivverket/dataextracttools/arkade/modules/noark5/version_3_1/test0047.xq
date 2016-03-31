xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 47 i versjon 14 av testoppleggsdokumentet.
 : Kontroll av systemidentifikasjonene i arkivstrukturen.
 : Type: Kontroll
 : Kontroll på at alle systemidentifikasjoner i arkivstrukturen er unike.
 : Det kontrolleres at alle forekomster av elementet systemID i arkivstruktur.xml kun forekommer
 : en gang.
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
let $antallSystemID := count($arkivstrukturDoc//n5a:systemID)
let $antallUnikeSystemID := count(distinct-values($arkivstrukturDoc//n5a:systemID/normalize-space(.)))
let $antallIkkeUnikeSystemID := ($antallSystemID - $antallUnikeSystemID)

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
          if (not($arkivstrukturDoc))
          then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$arkivstrukturDocFileName}</content>
          </resultItem>
          ) else (
          <resultItem type="info">
            <label>Antall systemidentifikasjoner</label>
            <content>{$antallSystemID}</content>
          </resultItem>
          ,
          <resultItem type="{if ($antallIkkeUnikeSystemID = 0)
                             then 'info'
                             else 'error'}">
            <label>Antall ikke unike systemidentifikasjoner</label>
            <content>{$antallIkkeUnikeSystemID}</content>
          </resultItem>
          )
          }
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>