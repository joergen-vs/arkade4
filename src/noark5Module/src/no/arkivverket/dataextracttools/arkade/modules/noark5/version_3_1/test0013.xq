xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 13 i versjon 14 av testoppleggsdokumentet.
 : Antall mapper som er klassifisert med hver enkelt klasse i arkivstrukturen.
 : Type: Analyse
 : Opptelling av hvor mange mapper som er klassifisert med de forskjellige klassene i arkivstruktur.xml.
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

(: Primært klassifikasjonssystem skal være det eneste ene klassifikasjonssystemet
   under en arkivdel som inneholder klasser med mapper eller registreringer.
   Returner allikevel systemID til alle klassifikasjonssystemer med mapper eller registreringer 
 :)
declare function local:primaertKlassifikasjonssystemID($ad as element(n5a:arkivdel)) as element(n5a:systemID)?
{
  let $primaertKlassifikasjonssystemID :=
  $ad/n5a:klassifikasjonssystem[.//n5a:klasse/n5a:mappe]/n5a:systemID
  return $primaertKlassifikasjonssystemID
};

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
        let $pk := local:primaertKlassifikasjonssystemID($arkivdel)
        let $klasser := $arkivdel/n5a:klassifikasjonssystem[n5a:systemID=data($pk)]//n5a:klasse
        let $antallKlasser := count($klasser)
        let $antallKlasserMedUnderklasserUtenMapper := count($arkivdel/n5a:klassifikasjonssystem[n5a:systemID=data($pk)]//n5a:klasse[n5a:klasse and not(n5a:mappe)])
        let $antallKlasserMedMapper := count($klasser[n5a:mappe])
        let $antallKlasserUtenUnderklasserEllerMapper := count($arkivdel/n5a:klassifikasjonssystem[n5a:systemID=data($pk)]//n5a:klasse[not(n5a:klasse or n5a:mappe)])         
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
                <label>Antall klasser med mapper</label>
                <content>{$antallKlasserMedMapper}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall klasser med underklasser og uten mapper</label>
                <content>{$antallKlasserMedUnderklasserUtenMapper}</content>
              </resultItem>
              <resultItem type="info">
                <label>Antall klasser med verken underklasser eller mapper</label>
                <content>{$antallKlasserUtenUnderklasserEllerMapper}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          {
          if ($klasser) then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="antallMapperIKlasser" type="info">
                <label>Antall mapper i klasser</label>
                <resultItems>
                {
                for $klasse in $klasser[n5a:mappe]
                let $antallMapper := count($klasse//n5a:mappe)
                return
                  <resultItem name="klasse" type="info">
                    <identifiers>
                      <identifier name="systemID" value="{$klasse/n5a:systemID/normalize-space()}"/>  
                    </identifiers>  
                    <label>{data($klasse/n5a:tittel)}</label>
                    <content>{$antallMapper}</content>
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
