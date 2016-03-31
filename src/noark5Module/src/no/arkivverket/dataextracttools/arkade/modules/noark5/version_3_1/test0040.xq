xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 40 i versjon 14 av testoppleggsdokumentet.
 : Antall avskrivninger i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av avskrivning i registreringstypen journalpost i arkivstruktur.xml.
 : Antall journalposter som inneholder referanse til en journalpost som blir avskrevet av denne journalposten, telles opp. 
 : Resultatet vises fordelt på arkivdel og avskrivningsmåte.
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
      ) else (
        for $arkivdel in $arkivstrukturDoc//n5a:arkivdel
        let $avskrivninger := $arkivdel//n5a:registrering[@xsi:type = 'journalpost']/n5a:avskrivning
        let $antallAvskrivninger := count($avskrivninger)
        let $avskrivningsmaater := distinct-values($avskrivninger/n5a:avskrivningsmaate/normalize-space())
        return 
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers> 
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem name="antallAvskrivningerIArkivdel" type="info">
                <label>Antall avskrivninger i arkivdelen</label>
                <content>{$antallAvskrivninger}</content>
              </resultItem>
            </resultItems>              
          </resultItem>
          
          {
          if ($antallAvskrivninger > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>    
              <resultItem name="fordelingAvAvskrivningsmaater" type="info">
                <label>Fordeling av avskrivningsmåter</label>
                <resultItems>
                  {
                  for $avskrivningsmaate in $avskrivningsmaater
                  let $antall := count($avskrivninger[n5a:avskrivningsmaate=$avskrivningsmaate])
                  order by $avskrivningsmaate
                  return
                  <resultItem type="info">
                    <label>{$avskrivningsmaate}</label>
                    <content>{$antall}</content>
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
  