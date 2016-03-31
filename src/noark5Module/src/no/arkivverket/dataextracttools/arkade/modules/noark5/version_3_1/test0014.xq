xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 14 i versjon 14 av testoppleggsdokumentet.
 : Antall mapper uten undermapper eller registreringer i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall mapper som verken inneholder undermapper eller registreringer i arkivstruktur.xml.
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
        let $antallTommeMapper := count($arkivdel//n5a:mappe[not(n5a:mappe or n5a:registrering)])       
        return
          <resultItem name="arkivdel" type="{if ($antallTommeMapper = 0)
                                             then ('info')
                                             else ('error')}">
            <identifiers>
              <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
            </identifiers>
            <label>Antall mapper med verken undermapper eller registreringer</label>
            <content>{$antallTommeMapper}</content>
          </resultItem>
        )       
        }
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>
