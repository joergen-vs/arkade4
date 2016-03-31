xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.12 2013-06-16
 :
 : Test 12 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at mappene bare er knyttet til klasser uten underklasser i arkivstrukturen.
 : Type: Kontroll
 : Kontrollerer at klassen som inneholder mappen ikke samtidig også inneholder en underklasse.
 : Opptelling av antall forekomster av klasse som inneholder både forekomst av klasse (underklasse) og mappe i arkivstruktur.xml.
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
      let $klasserMedUnderklasserOgMapper := $arkivdel//n5a:klasse[n5a:klasse and n5a:mappe]/n5a:systemID
      let $antallKlasserMedUnderklasserOgMapper := count($klasserMedUnderklasserOgMapper)
      return
      <resultItem name="arkivdel" type="{if ($antallKlasserMedUnderklasserOgMapper = 0)
                                         then 'info'
                                         else 'error'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>        
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallKlasserMedUnderklasserOgMapper = 0)
                                         then 'info'
                                         else 'error'}">
                <label>Antall klasser med underklasser og mapper</label>
                <content>{$antallKlasserMedUnderklasserOgMapper}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          
          {
          if ($maxNumberOfResults > 0 and $antallKlasserMedUnderklasserOgMapper > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>        
              {
              if ($antallKlasserMedUnderklasserOgMapper > 0)
              then (
              <resultItem name="klasserMedUnderklasserOgMapper">
                <label>Referanser til klasser med klasser (underklasser) og mapper. Viser 
                       {if ($maxNumberOfResults >= $antallKlasserMedUnderklasserOgMapper) 
                        then (concat(" ", $antallKlasserMedUnderklasserOgMapper, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallKlasserMedUnderklasserOgMapper, ".")}
                </label>
                <resultItems>
                  {
                  for $klasserMedUnderklasserOgMapper at $pos1 in $klasserMedUnderklasserOgMapper
                  let $klasseSystemID := $klasserMedUnderklasserOgMapper/../n5a:systemID
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