xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 8 i versjon 14 av testoppleggsdokumentet.
 : Antall klasser i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av klasse på hvert nivå i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel, klassifikasjonssystem og nivå.
 : Element: klasse
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

let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
  
let $arkiv := $arkivstrukturDoc/n5a:arkiv[1]

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
        return 
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        {
        if ($arkivdel/n5a:klassifikasjonssystem) then (
        <resultItems>
         {
         for $klassifikasjonssystem in $arkivdel/n5a:klassifikasjonssystem
         let $antallKlasser := count($klassifikasjonssystem//n5a:klasse)
         return
          <resultItem name="klassifikasjonssystem">
            <identifiers>
              <identifier name="systemID"
              value="{$klassifikasjonssystem/n5a:systemID/normalize-space()}"/> 
            </identifiers>
            <label>Antall klasser i {if ($klassifikasjonssystem//n5a:mappe)
                                     then ("det primære klassifikasjonssystemet: ")
                                     else ("sekundært klassifikasjonssystem: ")}</label>
            <content>{$antallKlasser}</content>
              
            <resultItems>
              {local:antallElementer($klassifikasjonssystem, "n5a:klasse", 1)}
            </resultItems>
          </resultItem>
          }
        </resultItems>
        ) else ()
        }
      </resultItem>
      )
      }
    </resultItems>
  </result>
</activity>
