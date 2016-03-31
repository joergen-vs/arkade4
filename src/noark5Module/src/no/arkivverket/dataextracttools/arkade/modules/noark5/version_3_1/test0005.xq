xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 5 i versjon 14 av testoppleggsdokumentet.
 : Antall arkivdeler i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av arkivdel i arkivstruktur.xml. 
 : Resultatet er fordelt på arkiv.
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
let $arkiv2 := $arkivstrukturDoc/n5a:arkiv[1]
let $antallArkivdeler := count($arkiv2//n5a:arkivdel)

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
            if (not($arkivstrukturDoc//n5a:arkivdel))
            then (
          <resultItem name="manglendeArkivdeler" type="error">
            <label>Arkivuttrekket inneholder ingen arkivdeler</label>
          </resultItem>
            ) 
            else
            (
              for $arkiv in $arkivstrukturDoc//n5a:arkiv
              let $antallArkivdeler := count($arkiv/n5a:arkivdel)      
              return
          <resultItem name="arkiv" type="{if ($antallArkivdeler > 0)
                                          then ('info')
                                          else ('warning')}">
            <identifiers>
              <identifier name="systemID" value="{$arkiv/n5a:systemID/normalize-space()}"/>
            </identifiers>
            <label>Antall arkivdeler</label>
            <content>{$antallArkivdeler}</content>
          </resultItem>
            )
          )
          }
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>