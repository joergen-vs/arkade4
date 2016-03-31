xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 4 i versjon 14 av testoppleggsdokumentet.
 : Antall arkiver i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av arkiv i arkivstruktur.xml
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

declare function local:antall-underarkiver($arkiv) {
  let $systemID := data($arkiv/n5a:systemID)
  let $arkiver := $arkiv/n5a:arkiv
  let $antallUnderarkiver := count($arkiver)

  return 
    if (not(empty($arkiv)))
    then (
      <resultItem name="arkiv" type="info">
        <identifiers>
          <identifier name="systemID" value="{$systemID}"/> 
        </identifiers>
        <label>Antall underarkiver</label>
        <content>{$antallUnderarkiver}</content>
        {
        if (not(empty($arkiver)))
        then (
          <resultItems>
            {
            for $a in $arkiv/n5a:arkiv
            return local:antall-underarkiver($a)
            }
           </resultItems>
        ) else ()
        }
      </resultItem>
    ) else (
      <resultItem name="arkiv" type="error">
        <label>Arkivuttrekket inneholder ikke arkiv</label>
      </resultItem>
    )
};

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
       local:antall-underarkiver($arkivstrukturDoc/n5a:arkiv)
      )}
      </resultItems>
    </result>
  </activity>
