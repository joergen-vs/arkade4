xquery version "1.0";

(: 
 : @author Riksarkivet 
 : @version 0.13 2013-11-27
 :
 : Test 28 i versjon 14 av testoppleggsdokumentet.
 : Antall dokumentfiler i arkivuttrekket.
 : Type: Analyse og kontroll
 : Opptelling av antall dokumentfiler i arkivuttrekket.
 : Antallet dokumentfiler kontrolleres mot det som er oppgitt i arkivuttrekk.xml. 
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
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
declare variable $documentDirectory external;

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))
let $oppgittAntallDokumentfiler := $arkivuttrekkDoc//aml:property[@name="antallDokumentfiler"]/data(aml:value)
let $faktiskAntallDokumentfiler := count(file:directory-list(concat($rootDirectory, "/",
$documentDirectory), "**/*.*")//file:file)

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
      <resultItem name="overordnetResultat">
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
          <resultItem type="{if ($faktiskAntallDokumentfiler != -1)
                             then 'info'
                             else 'error'}">                                                                                 
          {
            if ($faktiskAntallDokumentfiler != -1)
            then (<label>Faktisk antall dokumentfiler </label>,  
                <content>{$faktiskAntallDokumentfiler}</content>)
            else (<label>Antall faktiske dokumentfiler mangler</label>)
          }      
          </resultItem>,
          <resultItem type="{if ($oppgittAntallDokumentfiler != -1)
                             then 'info'
                             else 'error'}">
          {
            if ($oppgittAntallDokumentfiler != -1)
            then (<label>Antall dokumentfiler oppgitt i arkivuttrekk.xml</label>,  
                <content>{$oppgittAntallDokumentfiler}</content>)
            else (<label>Antall dokumentfiler mangler i arkivuttrekk.xml</label>)
          }
          </resultItem>,
          <resultItem type="{if ($faktiskAntallDokumentfiler = $oppgittAntallDokumentfiler 
                                 and $faktiskAntallDokumentfiler != -1)
                             then 'info'
                             else 'error'}">
          {
          if ($faktiskAntallDokumentfiler = $oppgittAntallDokumentfiler 
                               and $faktiskAntallDokumentfiler != -1)
          then (<label>Faktisk antall dokumentfiler stemmer med antall oppgitt i arkivuttrekk.xml.</label>)
          else if ($faktiskAntallDokumentfiler != $oppgittAntallDokumentfiler 
                    and $faktiskAntallDokumentfiler != -1 and $oppgittAntallDokumentfiler != -1)
               then (<label>Faktisk antall dokumentfiler stemmer ikke med antall oppgitt i arkivuttrekk.xml.</label>)
               else (<label>Kan ikke sammenligne faktisk og oppgitt antall dokumentfiler.</label>)
          }      
          </resultItem>
          )
          }
        </resultItems>
      </resultItem>
    </resultItems>
  </result>
</activity>

