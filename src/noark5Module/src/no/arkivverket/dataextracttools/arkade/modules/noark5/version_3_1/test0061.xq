xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 61 i versjon 14 av testoppleggsdokumentet.
 : Antall endringer i endringsloggen.
 : Type: Analyse
 : Opptelling av antall forekomster av endring i endringslogg.xml.
 :
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5l = "http://www.arkivverket.no/standarder/noark5/endringslogg";
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

let $docFileName := "endringslogg.xml"
let $endringsloggDoc := doc(concat($dataCollection, "/", $docFileName))
let $antallEndringer := count($endringsloggDoc/n5l:endringslogg/n5l:endring)

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
        {if (not($endringsloggDoc))
         then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$docFileName}</content>
          </resultItem>
         ) else (
          <resultItem type="{if ($antallEndringer > 0)
                             then 'info'
                             else 'error'}">
            <label>Antall endringer</label>
            <content>{$antallEndringer}</content>
          </resultItem>
        )}
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>