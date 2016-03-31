xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 25 i versjon 14 av testoppleggsdokumentet.
 : Dokumentbeskrivelsenes status i arkivstrukturen.
 : Type: Analyse
 : Opptelling av de forskjellige verdiene i dokumentbeskrivelsenes status i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Element: dokumentstatus
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
      let $dokumentbeskrivelser := $arkivdel//n5a:dokumentbeskrivelse
      let $antallDokumentbeskrivelserUtenDokumentstatus := count($dokumentbeskrivelser[not(n5a:dokumentstatus)])
      return
      <resultItem name="arkivdel">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Antall dokumentbeskrivelser i arkivdelen</label>
                <content>{count($dokumentbeskrivelser)}</content>
              </resultItem>
            </resultItems>
          </resultItem>
          {
          if ($dokumentbeskrivelser) then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="fordelingAvDokumentstatus" type="info">
                <label>Fordeling av dokumentstatus</label>
                <resultItems>
                  {
                  for $dokumentstatus in distinct-values($dokumentbeskrivelser/n5a:dokumentstatus)
                  let $antall := count($dokumentbeskrivelser[n5a:dokumentstatus = $dokumentstatus])
                  order by $dokumentstatus
                  return
                  <resultItem name="dokumentstatus" type="info">
                    <label>{$dokumentstatus}</label>
                    <content>{$antall}</content>
                  </resultItem>
                  }
                  <resultItem type="{if ($antallDokumentbeskrivelserUtenDokumentstatus = 0)
                                     then 'info'
                                     else 'error'}">
                    <label>[Uten dokumentstatus]</label>
                    <content>{$antallDokumentbeskrivelserUtenDokumentstatus}</content>
                  </resultItem>
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

