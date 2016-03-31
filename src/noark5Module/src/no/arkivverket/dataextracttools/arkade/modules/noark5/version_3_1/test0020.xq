xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 20 i versjon 14 av testoppleggsdokumentet.
 : Antall registreringer som er klassifisert med hver enkelt klasse i arkivstrukturen.
 : Type: Analyse
 : Opptelling av hvor mange registreringer som er klassifisert med de forskjellige klassene i arkivstruktur.xml.
 : Denne analysen gjelder bare fagsystemer uten mapper, dvs. fagsystemer hvor registreringene er knyttet direkte til klasser.
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
        let $klasser := $arkivdel/n5a:klassifikasjonssystem[.//n5a:registrering]//n5a:klasse
        let $antallKlasser := count($klasser)
        let $antallKlasserMedUnderklasserUtenRegistreringer := count($klasser[n5a:klasse and not(n5a:registrering)])
        let $antallKlasserMedRegistreringer := count($klasser[n5a:registrering])
        let $antallKlasserUtenUnderklasserEllerRegistreringer := count($klasser[not(n5a:klasse or n5a:registrering)])         
        return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Totalt antall klasser</label>
                <content>{$antallKlasser}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall klasser med registreringer</label>
                <content>{$antallKlasserMedRegistreringer}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall klasser med underklasser og uten registreringer</label>
                <content>{$antallKlasserMedUnderklasserUtenRegistreringer}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall klasser med verken underklasser eller registreringer</label>
                <content>{$antallKlasserUtenUnderklasserEllerRegistreringer}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          {
          if ($klasser) then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="antallRegistreringerIKlasser" type="info">
                <label>Antall registreringer i klasser</label>
                <resultItems>
                {
                for $klasse in $klasser
                let $antallRegistreringer := count($klasse//n5a:registrering)
                return
                  <resultItem name="klasse" type="info">
                    <identifiers>
                      <identifier name="systemID" value="{$klasse/n5a:systemID/normalize-space()}"/>  
                    </identifiers>  
                    <label>{data($klasse/n5a:tittel)}</label>
                    <content>{$antallRegistreringer}</content>
                  </resultItem>
                }
                </resultItems>
              </resultItem>
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
