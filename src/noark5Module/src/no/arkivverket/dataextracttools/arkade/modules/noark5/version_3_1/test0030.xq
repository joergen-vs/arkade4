xquery version "1.0";

(: 
 : @author Riksarkivet 
 : @version 0.16 2013-11-27
 :
 : Test 30 i versjon 14 av testoppleggsdokumentet.
 : Kontroll av sjekksummen for hver dokumentfil i arkivuttrekket.
 : Type: Kontroll
 : Kontroll på at dokumentfilenes sjekksum stemmer med den respektive sjekksummen 
 : oppgitt i elementet dokumentobjekt i arkivstruktur.xml. 
 : Resultatet vises fordelt på arkivdel.
 : Element: dokumentobjekt
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace file = "http://exist-db.org/xquery/file";
declare namespace util = "http://exist-db.org/xquery/util";
declare namespace java_file = "java:java.io.File";
declare namespace ra_utils = "java:no.arkivverket.dataextracttools.utils.Utils";
declare variable $testName external;
declare variable $longName external;
declare variable $testId external;
declare variable $testDescription external;
declare variable $resultDescription external;
declare variable $orderKey external;
declare variable $dataCollection external;
declare variable $rootDirectory external;
declare variable $maxNumberOfResults external;

declare function local:beregn-sjekksum($dokumentfil as xs:string, $algoritme as xs:string,
$lovligeAlgoritmer as xs:string+) as xs:string? {
  if ($algoritme = $lovligeAlgoritmer) then 
  (
    if (file:exists($dokumentfil)) then (
      let $fil := java_file:new($dokumentfil)
      return
        ra_utils:create-file-checksum($fil, $algoritme)          
    ) else ()
  ) else ()   
};

let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $directory := concat($rootDirectory, '/')

return 
<activity name="{$testName}" 
  longName="{if ($longName != "") then ($longName) else ($testName)}"
  orderKey="{$orderKey}">
  <identifiers>
    <identifier name="id" value="{$testId}"/>
    <identifier name="uuid" value="{util:uuid()}"/>
  </identifiers>  
  {
  if ($testDescription != "") then ( 
  <description>{$testDescription}</description>
  ) else ()
  }

  <result>
    {
    if ($resultDescription != "") then (
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
      let $antallDokumentobjekter := count($arkivdel//n5a:dokumentobjekt)
      let $ugyldigeFilreferanser := $arkivdel//n5a:dokumentobjekt[not(file:exists(concat($directory,
      n5a:referanseDokumentfil/normalize-space())))]
      let $antallUgyldigeFilreferanser := count($ugyldigeFilreferanser)
      let $ugyldigeSjekksummer := $arkivdel//n5a:dokumentobjekt[(upper-case(n5a:sjekksum/normalize-space()))
        ne
        (upper-case(local:beregn-sjekksum(concat($directory, n5a:referanseDokumentfil/normalize-space()),
        n5a:sjekksumAlgoritme/normalize-space(), "SHA-256")))]
      let $antallUgyldigeSjekksummer := count($ugyldigeSjekksummer)
      return
      <resultItem name="arkivdel">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
          
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Antall dokumentobjekter kontrollert</label>
                <content>{$antallDokumentobjekter}</content>
              </resultItem>
              <resultItem type="{if ($antallUgyldigeFilreferanser = 0)
                                  then 'info'
                                  else 'error'}">
                <label>Antall ugyldige filreferanser</label>
                <content>{$antallUgyldigeFilreferanser}</content>
              </resultItem>
              <resultItem type="{if ($antallUgyldigeSjekksummer = 0)
                                 then 'info'
                                 else 'error'}">
                <label>Antall ugyldige sjekksummer</label>
                <content>{$antallUgyldigeSjekksummer}</content>                                   
              </resultItem>
            </resultItems>
          </resultItem>
                    
          {
          if ($antallUgyldigeSjekksummer > 0 and $maxNumberOfResults > 0) then
          (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="ugyldigeSjekksummer">
                <label>Dokumentfiler med ugyldig sjekksum.
                       Viser {if ($maxNumberOfResults >= $antallUgyldigeSjekksummer) 
                              then (concat(" ", $antallUgyldigeSjekksummer, " (alle).")) 
                              else concat($maxNumberOfResults, " av ", $antallUgyldigeSjekksummer, ".")} 
                </label>
                <resultItems>
                {
                for $ugyldigSjekksum at $pos2 in $ugyldigeSjekksummer
                return
                  if ($pos2 <= $maxNumberOfResults)
                  then (
                  <resultItem type="error">
                    <identifiers>
                      <identifier name="systemID"
                                  value="{$ugyldigSjekksum/../n5a:systemID/normalize-space()}"/>
                    </identifiers>
                    <label>{$ugyldigSjekksum/n5a:referanseDokumentfil/text()}</label>
                    <content>{concat($ugyldigSjekksum/n5a:sjekksumAlgoritme, ": ", 
                                     $ugyldigSjekksum/n5a:sjekksum, " ")}</content>
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
      )}
    </resultItems>
  </result>
</activity>

