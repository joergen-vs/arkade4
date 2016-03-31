xquery version "1.0";

(:
 : @version 0.13 2013-11-27
 : @author Riksarkivet 
 :
 : Test 48 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at referansene til arkivdel i arkivstrukturen er gyldige.
 : Type: Kontroll
 : Kontroll på at alle forekomster av referanseArkivdel i mappe, registrering eller
 : dokumentbeskrivelse refererer til en forekomst av arkivdel i arkivstruktur.xml.
 : Element: referanseArkivdel
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
let $arkivdelSystemIDer := $arkivstrukturDoc//n5a:arkivdel/n5a:systemID/normalize-space(.)
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
        let $referanser := $arkivdel//n5a:referanseArkivdel
        let $antallReferanser := count($referanser)
        let $ugyldigeReferanser := $referanser[not(normalize-space(.) = $arkivdelSystemIDer)]/..
        let $antallUgyldigeReferanser := count($ugyldigeReferanser)
        return
      <resultItem name="arkivdel" type="{if ($antallUgyldigeReferanser = 0)
                                         then 'info'
                                         else 'error'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem name="referanser" type="info">
                <label>Antall referanser til arkivdeler</label>
                <content>{$antallReferanser}</content>
              </resultItem>
              ,
              <resultItem name="ugyldigeReferanser" type="{if ($antallUgyldigeReferanser = 0)
                                                           then 'info'
                                                           else 'error'}">
                <label>Antall ugyldige referanser til arkivdeler</label>
                <content>{$antallUgyldigeReferanser}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          
          {
          if ($maxNumberOfResults > 0 and $antallUgyldigeReferanser > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
              {
              <resultItem name="ugyldigeReferanser">
                <label>Ugyldige referanser til arkivdeler. Viser 
                       {if ($maxNumberOfResults >= $antallUgyldigeReferanser) 
                        then (concat(" ", $antallUgyldigeReferanser, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallUgyldigeReferanser, ".")}
                </label>
                <resultItems>
                  {
                  for $ugyldigReferanse at $pos1 in $ugyldigeReferanser
                  let $systemID := $ugyldigReferanse/../n5a:systemID
                  return
                    if ($pos1 <= $maxNumberOfResults)
                    then (
                  <resultItem name="ugyldigReferanse" type="error">
                    <identifiers>
                      <identifier name="ugyldigSystemID"
                      value="{$ugyldigReferanse/n5a:referanseArkivdel/normalize-space()}"/>
                    </identifiers> 
                    <label>{local-name($ugyldigReferanse)}</label>
                    <content>{data($systemID)}</content>
                  </resultItem>
                  ) else ()
                  }
                </resultItems>
              </resultItem>
              }
            </resultItems>
          </resultItem>
          ) else ()
          }
        </resultItems>
      </resultItem> 
      )
      }
    </resultItems>
  </result>
</activity>
