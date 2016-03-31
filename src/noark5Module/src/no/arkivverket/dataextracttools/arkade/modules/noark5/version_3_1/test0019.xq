xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.13 2013-11-27
 :
 : Test 19 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at registreringene bare er knyttet til klasser uten underklasser i arkivstrukturen.
 : Type: Kontroll
 : Kontrollerer at klassen som inneholder registreringen ikke samtidig også inneholder en underklasse.
 : Opptelling av antall forekomster av klasse som inneholder både forekomst av klasse (underklasse) og registrering i arkivstruktur.xml.
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
      let $klasserMedUnderklasserOgRegistreringer := $arkivdel//n5a:klasse[n5a:klasse and n5a:registrering]/n5a:systemID
      let $antallKlasserMedUnderklasserOgRegistreringer := count($klasserMedUnderklasserOgRegistreringer)
      return
      <resultItem name="arkivdel" type="{if ($antallKlasserMedUnderklasserOgRegistreringer = 0)
                                         then 'info'
                                         else 'error'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>        
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallKlasserMedUnderklasserOgRegistreringer = 0)
                                 then 'info'
                                 else 'error'}">
                <label>Antall klasser med underklasser og registreringer</label>
                <content>{$antallKlasserMedUnderklasserOgRegistreringer}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          
          {
          if ($maxNumberOfResults > 0 and $antallKlasserMedUnderklasserOgRegistreringer > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>        
              {
              if ($antallKlasserMedUnderklasserOgRegistreringer > 0)
              then (
              <resultItem name="klasserMedUnderklasserOgRegistreringer">
                <label>Referanser til klasser med klasser (underklasser) og registreringer. Viser 
                       {if ($maxNumberOfResults >= $antallKlasserMedUnderklasserOgRegistreringer) 
                        then (concat(" ", $antallKlasserMedUnderklasserOgRegistreringer, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallKlasserMedUnderklasserOgRegistreringer, ".")}
                </label>
                <resultItems>
                  {
                  for $klasserMedUnderklasserOgRegistreringer at $pos1 in $klasserMedUnderklasserOgRegistreringer
                  let $klasseSystemID := $klasserMedUnderklasserOgRegistreringer/../n5a:systemID
                  return
                    if ($pos1 <= $maxNumberOfResults)
                    then (
                  <resultItem name="klasse" type="error">
                    <identifiers>
                      <identifier name="systemID" value="{data($klasseSystemID)}"/>
                    </identifiers>  
                  </resultItem>
                    ) else ()
                  }
                </resultItems>
              </resultItem>
              ) else ()
              }
                </resultItems>
              </resultItem>
          ) else ()}
          
        </resultItems>
      </resultItem>
      )
      }
    </resultItems>
  </result>
</activity>