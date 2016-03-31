xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 32 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på om dokumentobjektene i arkivstrukturen refererer til eksisterende dokumentfiler i
 : arkivuttrekket.
 : Type: Kontroll
 : Kontroll på om sti og filnavn i elementet referanseDokumentfil i dokumentobjekt i
 : arkivstruktur.xml er gyldig, dvs. at forekomstene av referanseDokumentfil angir eksisterende
 : filer i arkivuttrekket.
 : Resultatet vises fordelt på arkivdel.
 : Element: referanseDokumentfil
 : Den systemID som vises for eventuelle ugyldige filreferanser, er IDen til dokumentbeskrivelsen
 : som inneholder dokumentobjektet med filreferansen.
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace file = "http://exist-db.org/xquery/file";
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
let $directory := concat($rootDirectory, '/')

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
     
      for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
      let $dokumentobjekter := $arkivdel//n5a:dokumentobjekt
      let $antallDokumentobjekter := count($dokumentobjekter)
      let $antallFilreferanser := count($arkivdel//n5a:referanseDokumentfil)
      let $ugyldigeFilreferanser := $dokumentobjekter[not(file:exists(concat($directory, data(n5a:referanseDokumentfil))))]
      let $antallUgyldigeFilreferanser := count($ugyldigeFilreferanser)
      return
      <resultItem name="arkivdel" type="{if ($antallUgyldigeFilreferanser > 0) 
                                         then ('error') 
                                         else ('info')}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space(.)}"/> 
        </identifiers> 
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallFilreferanser > 0)
                                 then ('info')
                                 else ('warning')}">
                <label>Antall filreferanser kontrollert</label>
                <content>{$antallFilreferanser}</content>
              </resultItem>
              <resultItem type="{if ($antallUgyldigeFilreferanser > 0)
                                 then ('error')
                                 else ('info')}">
                <label>Antall ugyldige filreferanser</label>
                <content>{$antallUgyldigeFilreferanser}</content>
              </resultItem>
            </resultItems>
          </resultItem>    
            
          {
          if ($antallUgyldigeFilreferanser > 0 and $maxNumberOfResults > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
            
              <resultItem name="ugyldigeFilreferanser">
                <label>Ugyldige referanser til dokumentfiler. Viser 
                       {if ($maxNumberOfResults >= $antallUgyldigeFilreferanser) 
                        then (concat(" ", $antallUgyldigeFilreferanser, " (alle).")) 
                        else concat($maxNumberOfResults, " av ", $antallUgyldigeFilreferanser, ".")}
                </label>
                <resultItems>
                  {
                  for $ugyldigFilreferanse at $pos1 in $ugyldigeFilreferanser
                  let $systemID := $ugyldigFilreferanse/../n5a:systemID
                  return
                    if ($pos1 <= $maxNumberOfResults)
                    then (
                  <resultItem name="ugyldigFilreferanse" type="error">
                    <identifiers>
                      <identifier name="systemID"
                                  value="{$ugyldigFilreferanse/../n5a:systemID/normalize-space()}"/>
                    </identifiers> 
                    <content>{$ugyldigFilreferanse/n5a:referanseDokumentfil/text()}</content>
                  </resultItem>
                  ) else ()
                  }
                </resultItems>
              </resultItem>
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
