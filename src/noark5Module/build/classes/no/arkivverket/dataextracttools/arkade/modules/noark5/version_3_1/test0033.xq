xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.16 2013-11-27
 :
 : Test 33 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at det ikke finnes dokumentfiler i arkivuttrekket som mangler referanse fra dokumentobjekt i arkivstrukturen 
 : Type: Kontroll
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace file = "http://exist-db.org/xquery/file";
declare namespace util = "http://exist-db.org/xquery/util";
declare namespace java_file = "java:java.io.File";
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
declare variable $separatorChar external;

declare function local:kontroller-filer($filkatalog as xs:string, $rotkatalog as xs:string, $separatorChar as xs:string, 
$interneReferanser as xs:string*) {
  let $dokumentfilerUtenReferanse := 
        for $f in java_file:list-files( java_file:new($filkatalog) )
        let $n := java_file:get-path($f)
        return     
          if (java_file:is-file($f)) 
          then 
            let $eksternReferanse := substring-after($n, concat($rotkatalog, $separatorChar))
            return
              if (not($interneReferanser = fn:lower-case($eksternReferanse))) 
              then 
                $eksternReferanse
              else ()
          else 
            local:kontroller-filer($n, $rotkatalog, $separatorChar, $interneReferanser)

return $dokumentfilerUtenReferanse
};


let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $dokumentkatalognavn := "dokumenter"
let $dokumentplassering := concat($rootDirectory, $separatorChar, $dokumentkatalognavn)

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
      let $separatorString := if ($separatorChar = '\') then 
                                concat($separatorChar, $separatorChar)
                              else $separatorChar
      let $referanser := $arkivstrukturDoc//n5a:referanseDokumentfil/fn:lower-case(fn:replace(data(.), '/|\\',
      $separatorString))
      let $manglerReferanse := local:kontroller-filer($dokumentplassering, $rootDirectory, $separatorChar, $referanser)
      let $antallManglerReferanse := count($manglerReferanse)
      return (
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallManglerReferanse = 0)
                                 then ('info')
                                 else ('error')}">
                <label>Antall dokumentfiler uten filreferanse i arkivuttrekket</label>
                <content>{$antallManglerReferanse}</content>
              </resultItem>
            </resultItems>
          </resultItem>,
          if ($antallManglerReferanse > 0) then
          (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="dokumentfilerUtenFilreferanse" type="info">
                <label>Dokumentfiler uten filreferanse. Viser 
                       {if ($maxNumberOfResults >= $antallManglerReferanse) 
                        then (concat(" ", $antallManglerReferanse, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallManglerReferanse, ".")}
                </label>
                <resultItems>
                  {
                  for $dokumentfil at $pos1 in $manglerReferanse
                  return
                  if ($pos1 <= $maxNumberOfResults)
                  then (
                  <resultItem type="error">
                    <content>{$dokumentfil}</content>
                  </resultItem>
                  ) else ()
                  }
                </resultItems>
              </resultItem>
            </resultItems>
          </resultItem>
          ) else ()
          )
      )
      }
    </resultItems>
  </result>
</activity>
