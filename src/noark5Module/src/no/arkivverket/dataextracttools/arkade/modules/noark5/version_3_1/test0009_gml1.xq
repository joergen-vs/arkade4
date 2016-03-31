xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.14 2013-11-27
 :
 : Test 9 i versjon 14 av testoppleggsdokumentet.
 : Antall klasser uten underklasser, mapper eller registreringer i det primære klassifikasjonssystemet i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av klasse uten underklasser, mapper eller registreringer umiddelbart under klasse  
 : i det primære klassifikasjonssystemet i arkivstruktur.xml.
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
declare function local:primaertKlassifikasjonssystemID($ad as element(n5a:arkivdel)) as element(n5a:systemID)*
{
  let $primaertKlassifikasjonssystemID :=
  $ad/n5a:klassifikasjonssystem[.//n5a:klasse/n5a:mappe or .//n5a:klasse/n5a:registrering]/n5a:systemID
  return $primaertKlassifikasjonssystemID
};

let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $start := current-dateTime()

return
<activity name="{$testName}"
  longName="{if ($longName ne "") then ($longName) else ($testName)}"
  orderKey="{$orderKey}">
  <identifiers>
    <identifier name="id" value="{$testId}"/>
    <identifier name="uuid" value="{util:uuid()}"/>
  </identifiers>  
  <timeStarted>{$start}</timeStarted>  
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
        return  
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>
        {
        let $pk := local:primaertKlassifikasjonssystemID($arkivdel)
        return
        if ($pk) then (        
        <resultItems>        
          <resultItem name="detaljertResultat">

            <resultItems>
              <resultItem name="klassifikasjonssystemerMedMapper" type="{if (count($pk) = 1)
                                                                         then 'info'
                                                                         else 'error'}">       
                <resultItems>
                  {
                  for $ks in $pk 
                  return
                  <resultItem name="klassifikasjonssystem" type="info">
                    <identifiers>
                      <identifier name="systemID" value="{$ks}"/> 
                    </identifiers>
                    <label>Antall klasser uten underklasser, mapper eller registreringer</label>
                    <content>
                    { if ($arkivdel//n5a:klassifikasjonssystem//n5a:klasse) then
                    (count($arkivdel//n5a:klassifikasjonssystem[n5a:systemID=data($ks)]//n5a:klasse[not(n5a:mappe
or n5a:klasse or n5a:registrering)])) else(0)}       
                    </content>
                  </resultItem>
                  }
                </resultItems>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
        ) else ()
        }
      </resultItem>
      )
      }          
    </resultItems>    
  </result>
</activity>
  
  