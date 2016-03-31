xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 51 i versjon 14 av testoppleggsdokumentet.
 : Kontroll av referansene til sekundær klassifikasjon i arkivstrukturen.
 : Type: Kontroll
 : Kontroll på at alle referanser fra en mappe av typen saksmappe til en sekundær klassifikasjon i
 : arkivstruktur.xml, er gyldige.
 : Hver forekomst av referanseSekundaerKlassifikasjon i mappetypen saksmappe skal referere til en
 : eksisterende klasse.
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
      {
      if (not($arkivstrukturDoc))
      then (
      <resultItem name="manglendeFil" type="error">
        <label>Manglende fil</label>
        <content>{$arkivstrukturDocFileName}</content>
      </resultItem>
      ) else (
      
        for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
        let $systemIDer := data($arkivdel//n5a:systemID)
        let $manglendeReferanser := $arkivdel//n5a:referanseSekundaerKlassifikasjon[not(data(.) = $systemIDer)]
        let $antallManglendeReferanser := count($manglendeReferanser)
        let $referanseSekundaerKlassifikasjoner := data($arkivdel//n5a:referanseSekundaerKlassifikasjon)
        let $antallReferanserTilSekundaerKlassifikasjoner := count($referanseSekundaerKlassifikasjoner)
        let $feilrefererte := data($arkivdel//n5a:systemID[data(.) = $referanseSekundaerKlassifikasjoner and not(local-name(..) = 'klasse')])
        let $antallFeilrefererte := count($feilrefererte)
        let $feilreferanser := $arkivdel//n5a:referanseSekundaerKlassifikasjon[data(.) = $feilrefererte]
        let $antallFeilreferanser := count($feilreferanser)

        return
      <resultItem name="arkivdel" type="{if ($antallManglendeReferanser = 0 and $antallFeilreferanser = 0)
                                         then 'info'
                                         else 'error'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>  
              <resultItem name="antallReferanser" type="info">
                <label>Antall referanser til sekundær klassifikasjon</label>
                <content>{$antallReferanserTilSekundaerKlassifikasjoner}</content>
              </resultItem>
            
              <resultItem name="manglendeReferanser" type="{if ($antallManglendeReferanser = 0)
                                                            then 'info'
                                                            else 'error'}">
                <label>Antall referanser til ikke-eksisterende klasser</label>
                <content>{$antallManglendeReferanser}</content>
              </resultItem>
              
              <resultItem name="feilreferanser" type="{if ($antallFeilreferanser = 0)
                                                       then 'info'
                                                       else 'error'}">
                <label>Antall referanser til {if ($antallFeilrefererte > 0)
                                              then concat($antallFeilrefererte, ' ')
                                              else ()}element{if ($antallFeilrefererte > 1)
                                                              then 'er'
                                                              else ()} som ikke er klasse</label>
                <content>{$antallFeilreferanser}</content>
              </resultItem>
            </resultItems>
          </resultItem>
              
          {
          if ($maxNumberOfResults > 0 and ($antallManglendeReferanser > 0 or $antallFeilreferanser > 0))
          then (
          <resultItem name="detaljertResultat">
            <resultItems>        
              {
              if ($antallManglendeReferanser > 0)
              then (
              <resultItem name="manglendeReferanser">
                <label>Referanser til ikke-eksisterende klasser. Viser 
                       {if ($maxNumberOfResults >= $antallManglendeReferanser) 
                        then (concat(" ", $antallManglendeReferanser, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallManglendeReferanser, ".")}
                </label>
                <resultItems>
                  {
                  for $manglendeReferanse at $pos1 in $manglendeReferanser
                  let $mappeSystemID := $manglendeReferanse/../n5a:systemID
                  return
                    if ($pos1 <= $maxNumberOfResults)
                    then (
                  <resultItem name="referanse" type="error">
                    <identifiers>
                      <identifier name="systemID" value="{$mappeSystemID/normalize-space()}"/>
                    </identifiers>  
                    <label>Referanse til ikke-eksisterende klasse</label>
                    <content>{data($manglendeReferanse)}</content>
                  </resultItem>
                    ) else ()
                  }
                </resultItems>
              </resultItem>
              ) else ()
              }

              {
              if ($antallFeilreferanser > 0)
              then (
              <resultItem name="feilreferanser">
                <label>Referanser til elementer som ikke er klasser. Viser 
                       {if ($maxNumberOfResults >= $antallFeilreferanser) 
                        then (concat(" ", $antallFeilreferanser, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallFeilreferanser, ".")}
                </label>
                <resultItems>
                  {
                  for $feilreferanse at $pos2 in $feilreferanser
                  let $mappeSystemID := $feilreferanse/../n5a:systemID/normalize-space()            
                  return
                    if ($pos2 <= $maxNumberOfResults)
                    then (
                  <resultItem name="referanse" type="error">
                    <identifiers>
                      <identifier name="systemID" value="{$mappeSystemID}"/>
                    </identifiers>  
                    <label>Referanse til element som ikke er klasse</label>
                    <content>{data($feilreferanse)}</content>
                  </resultItem>
                    ) else ()
                  }
                </resultItems>
              </resultItem>
              ) else ()
              }
            </resultItems>              
          </resultItem>
          ) else () }
            
        </resultItems>
      </resultItem>
      )}
    </resultItems>
  </result>
</activity>
