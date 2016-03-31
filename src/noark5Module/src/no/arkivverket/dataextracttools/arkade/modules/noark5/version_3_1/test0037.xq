xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 37 i versjon 14 av testoppleggsdokumentet.
 : Antall kryssreferanser i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av kryssreferanser i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Innenfor hver arkivdel er antall kryssreferanser fordelt på klasse, mappe og basisregistrering.
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
    
        for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
        let $antallKryssreferanserKlasse := count($arkivdel//n5a:klasse/n5a:kryssreferanse)
        let $antallKryssreferanserMappe := count($arkivdel//n5a:mappe/n5a:kryssreferanse)
        let $antallKryssreferanserBasisregistrering := count($arkivdel//n5a:registrering/n5a:kryssreferanse)
        return
      <resultItem name="arkivdel" type="{if ($antallKryssreferanserKlasse > 0 or 
                                             $antallKryssreferanserMappe > 0 or
                                             $antallKryssreferanserBasisregistrering > 0)
                                         then 'info'
                                         else 'warning'}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Antall kryssreferanser i klasser</label>
                <content>{$antallKryssreferanserKlasse}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall kryssreferanser i mapper</label>
                <content>{$antallKryssreferanserMappe}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall kryssreferanser i basisregistreringer</label>
                <content>{$antallKryssreferanserBasisregistrering}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
      </resultItem>
      )
      }
    </resultItems>
  </result>
</activity>


