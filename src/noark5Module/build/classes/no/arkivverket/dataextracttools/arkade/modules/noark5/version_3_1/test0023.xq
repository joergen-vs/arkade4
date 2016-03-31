xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.13 2013-11-27
 :
 : Test 23 i versjon 14 av testoppleggsdokumentet.
 : Antall dokumentbeskrivelser i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av dokumentbeskrivelse i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
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
      <resultItem name="overordnetResultat">
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
          let $antallDokumentbeskrivelser := count($arkivdel//n5a:registrering/n5a:dokumentbeskrivelse)
          return
          <resultItem name="arkivdel" type="info">
            <identifiers>
              <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
            </identifiers>
            <label>Antall dokumentbeskrivelser</label>
            <content>{$antallDokumentbeskrivelser}</content>
          </resultItem>
          )
          }
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>
