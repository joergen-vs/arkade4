xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.13 2013-11-27
 :
 : Test 39 i versjon 14 av testoppleggsdokumentet.
 : Antall dokumenter fordelt på dokumentformat og filtype i arkivuttrekket.
 : Type: Analyse
 : Opptellingen av forskjellige dokumentformater, gruppert på kombinasjonen av verdien i elementet
 : format og filendelsen i elementet referanseDokumentfil i dokumentobjekt i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : 
 :)

import module namespace functx = "http://www.functx.com" at "xmldb:exist://db/lib/functx-1.0-doc-2007-01.xq"; 

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
        for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
        let $dokumentobjekter := $arkivdel//n5a:dokumentobjekt
        let $antallDokumentobjekter := count($dokumentobjekter)
        let $forskjelligeFormater := distinct-values($dokumentobjekter/n5a:format)
        let $antallForskjelligeFormater := count($forskjelligeFormater)
        let $forskjelligeFilendelser :=
        distinct-values($dokumentobjekter/functx:substring-after-last(data(n5a:referanseDokumentfil), "."))
        let $antallForskjelligeFilendelser := count($forskjelligeFilendelser)
        let $arkivdelresultat :=
          for $f in $forskjelligeFormater
          for $fe in $forskjelligeFilendelser
          let $forekomster := $dokumentobjekter[n5a:format = $f][functx:substring-after-last(data(n5a:referanseDokumentfil), ".") = $fe]
          return
          if ($forekomster)
           then (
          <resultItem name="kombinasjon" type="info">
            <resultItems>
              <resultItem name="format" type='info'>
                <content>{$f}</content>
              </resultItem>
              <resultItem name="filendelse" type='info'>
                <content>{$fe}</content>
              </resultItem>
              <resultItem name="antall" type='info'>
                <content>{count($forekomster)}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          ) else ()

        return 
      <resultItem name="arkivdel">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem name="antallDokumentobjekter" type="{if ($antallDokumentobjekter > 0)
                                                         then 'info'
                                                         else 'warning'}">
                <label>Antall forekomster av dokumentobjekt</label>
                <content>{$antallDokumentobjekter}</content>
              </resultItem>
              <resultItem name="antallForskjelligeFormater" type="{if ($antallForskjelligeFormater > 0)
                                                         then 'info'
                                                         else 'warning'}">
                <label>Antall forskjellige verdier i format i dokumentobjekt</label>
                <content>{$antallForskjelligeFormater}</content>
              </resultItem>
            
              <resultItem name="antallForskjelligeFilendelser" type="{if ($antallForskjelligeFilendelser > 0)
                                                         then 'info'
                                                         else 'warning'}">
                <label>Antall forskjellige filendelser til dokumentobjektenes dokumentfiler</label>
                <content>{$antallForskjelligeFilendelser}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        
        {
        if ($arkivdelresultat)
        then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="kombinasjoner">
                <label>Oversikt over kombinasjoner av format angitt i dokumentobjekt og tilhørende dokumentfils filendelse.</label>
                <resultItems>
                  {$arkivdelresultat}
                </resultItems>
              </resultItem>
            </resultItems>
          </resultItem>
        ) else ()
        }
        </resultItems>
      </resultItem>
      )}
    </resultItems>
  </result>
</activity>
