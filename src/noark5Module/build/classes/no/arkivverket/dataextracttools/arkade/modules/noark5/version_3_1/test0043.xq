xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 43 i versjon 14 av testoppleggsdokumentet.
 : Antall graderinger i arkivstrukturen.
 : Type: Analyse
 : Opptelling av antall forekomster av gradering i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel. For hver arkivdel oppgis det om forekomst av gradering
 : finnes på arkivdelnivå, og antall øvrige graderinger vises fordelt på klasse, mappe,
 : registrering og dokumentbeskrivelse.
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

      for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
      let $antallGraderinger := count($arkivdel//n5a:gradering)
      let $antallGraderingerKlasse := count($arkivdel//n5a:klasse/n5a:gradering)
      let $antallGraderingerMappe := count($arkivdel//n5a:mappe/n5a:gradering)
      let $antallGraderingerRegistrering := count($arkivdel//n5a:registrering/n5a:gradering)
      let $antallGraderingerDokumentbeskrivelse :=
        count($arkivdel//n5a:dokumentbeskrivelse/n5a:gradering)
        
      return
      <resultItem name="arkivdel" type="info">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="info">
                <label>Det er {if ($arkivdel/n5a:gradering) then () else ("ikke ")}registrert
 gradering på arkivdelnivå.</label>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall graderinger i klasser</label>
                <content>{$antallGraderingerKlasse}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall graderinger i mapper</label>
                <content>{$antallGraderingerMappe}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall graderinger i registreringer</label>
                <content>{$antallGraderingerRegistrering}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall graderinger i dokumentbeskrivelse</label>
                <content>{$antallGraderingerDokumentbeskrivelse}</content>
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

