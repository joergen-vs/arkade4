xquery version "3.0";

(:
 : @author Riksarkivet 
 : @version 0.16 2013-11-27
 :
 : Test 34 i versjon 14 av testoppleggsdokumentet.
 : 
 : Antall dokumentfiler i arkivuttrekket som blir referert til av flere enn ett dokumentobjekt i arkivstrukturen.
 : Antall dokumentobjekter i arkivstruktur.xml hvor inneholdet i elementet referanseDokumentfil peker på en fil 
 : som det også har blitt referert til fra et eller flere andre dokumentobjekt.
 : Type: Analyse
 : Resultatet vises fordelt på arkivdel.
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
      let $antallFilreferanser := count($dokumentobjekter/n5a:referanseDokumentfil)
      
      let $dupliserteFilreferanser := 
        for $filreferanse in $arkivdel//n5a:referanseDokumentfil
        group by $key := $filreferanse
        return $filreferanse[2]

      let $antallDupliserteFilreferanser := count($dupliserteFilreferanser)
(:      let $forskjellige := distinct-values($dupliserteFilreferanser):)
(:      let $forskjellige := distinct-values($dupliserteFilreferanser):)
(:      let $antallForskjellige := count($forskjellige):)
      return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
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
              <resultItem type="info">
                <label>Antall filreferanser som forekommer mer enn en gang</label>
                <content>{$antallDupliserteFilreferanser}</content>
              </resultItem>
            </resultItems>
          </resultItem>    
            
          {
          if ($maxNumberOfResults > 0 and $antallDupliserteFilreferanser > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
            
              <resultItem name="dupliserteFilreferanser">
                <label>Filreferanser som forekommer mer enn en gang. Viser 
                       {if ($maxNumberOfResults >= $antallDupliserteFilreferanser) 
                        then (concat(" ", $antallDupliserteFilreferanser, " (alle) ")) 
                        else concat($maxNumberOfResults, " av ", $antallDupliserteFilreferanser, " ")}
                        og hvor mange ganger den enkelte forekommer.
                </label>
                <resultItems>
                  {
                  for $duplisertFilreferanse at $pos1 in $dupliserteFilreferanser
                  order by $duplisertFilreferanse
                  return
                    if ($pos1 <= $maxNumberOfResults)
                    then (
                  <resultItem name="duplisertFilreferanse" type="info">
                    <label>{$duplisertFilreferanse/text()}</label>
                    <content>{count($arkivdel//n5a:referanseDokumentfil[. = $duplisertFilreferanse])}</content>
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
