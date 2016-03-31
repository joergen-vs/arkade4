xquery version "3.0";

(:
 : @version 0.15 2013-11-27
 : @author Riksarkivet 
 :
 : Test 62 i versjon 14 av testoppleggsdokumentet.
 : Kontroll av referansene i endringsloggen.
 : Type: Kontroll
 : Kontroll på at alle endringer i endringsloggen refererer til arkivenheter i arkivstrukturen.
 : Hver forekomst av referanseArkivenhet i endringslogg.xml skal referere til en systemID i arkivstruktur.xml.
 : En forutsetning for at kontrollen skal uføres, er at systemID i arkivstruktur.xml er entydig. 
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
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

let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $endringsloggDocFileName := "endringslogg.xml"
let $endringsloggDoc := doc(concat($dataCollection, "/", $endringsloggDocFileName))
 
return
<activity name="{$testName}"
  longName="{if ($longName ne "") then ($longName) else ($testName)}"
  orderKey="{$orderKey}">
  <identifiers>
    <identifier name="id" value="{$testId}"/>
    <identifier name="uuid" value="{util:uuid()}"/>
  </identifiers>  

  {
  if ($testDescription ne "") 
  then ( 
  <description>{$testDescription}</description>
  ) else ()
  }
  
  <result>
  
    {
    if ($resultDescription ne "") 
    then (
    <description>{$resultDescription}</description>
    ) else()
    }

    <resultItems>
    
    {
    if (not($arkivstrukturDoc or $endringsloggDoc)) 
    then (
      <resultItem name="overordnetResultat">
        <resultItems>
      {
      if (not($arkivstrukturDoc)) 
      then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$arkivstrukturDocFileName}</content>
          </resultItem>
      ) else ()
      }

      {
      if (not($endringsloggDoc)) 
      then (
          <resultItem name="manglendeFil" type="error">
            <label>Manglende fil</label>
            <content>{$endringsloggDocFileName}</content>
          </resultItem>
      ) else ()
      }
      
        </resultItems>
      </resultItem>
    ) 
    else ( 
      if (count(distinct-values($arkivstrukturDoc//n5a:systemID)) ne
          count($arkivstrukturDoc//n5a:systemID)) 
      then (
      <resultItem name="overordnetResultat">
        <resultItems>
          <resultItem type="error">
            <label>systemID i arkivstrukturen ({$arkivstrukturDocFileName}) er ikke entydig.
                   Kontrollen vil ikke bli utført.</label>
          </resultItem>
        </resultItems>
      </resultItem>  
      )
      else (
        
        let $ugyldigeReferanser :=
          for $referanseArkivenhet in $endringsloggDoc//n5l:referanseArkivenhet
          return 
            if ($arkivstrukturDoc//n5a:systemID[. = $referanseArkivenhet])
            then ()
            else ($referanseArkivenhet)

        let $antallUgyldigeReferanser := count($ugyldigeReferanser)
        let $antallForskjelligeUgyldigeReferanser := count(distinct-values($ugyldigeReferanser))
                                                     
        return
        (
      <resultItem name="overordnetResultat">
        <resultItems>
          <resultItem type="{if ($antallUgyldigeReferanser = 0)
                             then 'info'
                             else 'error'}">
            <label>Antall ugyldige referanser til arkivstrukturen fra endringsloggen</label>
            <content>{$antallUgyldigeReferanser}</content>
          </resultItem>
          {
          if ($antallUgyldigeReferanser > 0)
          then (
          <resultItem type="error">
            <label>Antall forskjellige ugyldige referanser til arkivstrukturen fra endringsloggen</label>
            <content>{$antallForskjelligeUgyldigeReferanser}</content>
          </resultItem>
          ) else ()
          }
        </resultItems>
      </resultItem>
      ,
          if ($ugyldigeReferanser and $maxNumberOfResults > 0) 
          then (
      <resultItem name="detaljertResultat">
        <resultItems>        
          <resultItem name="ugyldigeReferanser">
            {
            if ($maxNumberOfResults >= $antallForskjelligeUgyldigeReferanser) 
            then (
            <label>Antall forekomster av {$antallForskjelligeUgyldigeReferanser} {
                    if ($antallForskjelligeUgyldigeReferanser > 1) then (" forskjellige ugyldige referanser.") 
                    else (" ugyldig referanse.")}
            </label>
            )
            else
            (
            <label>Antall forekomster av {$maxNumberOfResults} av
                   {$antallForskjelligeUgyldigeReferanser} forskjellige ugyldige referanser.
            </label>
            )
            }              
            <resultItems>
            {
            for $ref at $pos1 in distinct-values($ugyldigeReferanser)
            return 
              if ($pos1 <= $maxNumberOfResults)
              then ( 
              <resultItem type="error">
                <label>{$ref}</label>
                <content>{count($ugyldigeReferanser[. = $ref])}</content>
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
    )        
    }
    </resultItems>
  </result>
</activity>
