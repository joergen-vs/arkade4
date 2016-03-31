xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 45 i versjon 14 av testoppleggsdokumentet.
 : Antall utførte kassasjoner i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall kassasjoner knyttet til dokumentbeskrivelsene i arkivstrukturen.
 : Forekomster av elementet utfoertKassasjon i dokumentbeskrivelse i arkivstruktur.xml telles opp.
 : Det kontrolleres om arkivuttrekk.xml inneholder opplysning (omfatterDokumenterSomErKassert) om 
 : at det finnes kassasjonsvedtak i arkivuttrekket, og om denne opplysningen stemmer med innholdet i arkivstruktur.xml.
 : Element: utfoertKassasjon
 : 
 :)
                           
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
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

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $arkivstrukturDocFileName := "arkivstruktur.xml"
let $arkivstrukturDoc := doc(concat($dataCollection, "/", $arkivstrukturDocFileName))

let $omfatterDokumenterSomErKassert := 
$arkivuttrekkDoc/aml:addml/aml:dataset/aml:dataObjects/aml:dataObject[@name="Noark 5-arkivuttrekk"]/
  aml:properties/aml:property[@name="info"]/aml:properties/aml:property[@name="additionalInfo"]/
  aml:properties/aml:property[@name="omfatterDokumenterSomErKassert"]/aml:value/normalize-space(.)

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
      if (not($arkivuttrekkDoc))
      then (
      <resultItem name="manglendeFil" type="error">
        <label>Manglende fil</label>
        <content>{$arkivuttrekkDocFileName}</content>
      </resultItem>
      ) else ()
      }
      {
      if (not($arkivstrukturDoc))
      then (
      <resultItem name="manglendeFil" type="error">
        <label>Manglende fil</label>
        <content>{$arkivstrukturDocFileName}</content>
      </resultItem>
      ) else ()
      }

      {
      if ($arkivuttrekkDoc and $arkivstrukturDoc)
      then (
        for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
        let $antallUtfoerteKassasjonerArkivdel := count($arkivdel/n5a:utfoertKassasjon)
        let $antallUtfoerteKassasjonerDokumentbeskrivelse := count($arkivdel//n5a:dokumentbeskrivelse/n5a:utfoertKassasjon)
        let $antallUtfoerteKassasjoner := $antallUtfoerteKassasjonerArkivdel 
                                          + $antallUtfoerteKassasjonerDokumentbeskrivelse        
        return
      <resultItem name="arkivdel" type="{if (($antallUtfoerteKassasjoner > 0 and 
                                              $omfatterDokumenterSomErKassert = "false") or 
                                              not($omfatterDokumenterSomErKassert))
                                         then 'error'
                                         else (if ($antallUtfoerteKassasjoner = 0 and 
                                                   $omfatterDokumenterSomErKassert = "true")
                                               then 'warning'
                                               else 'info')}">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>  
           
            {
            if (not($omfatterDokumenterSomErKassert)) 
            then 
            (
              <resultItem name="samsvar" type="error">
                <label>Informasjon om arkivuttrekket inneholder dokumenter som er kassert, mangler i arkivuttrekk.xml.</label>
              </resultItem>
            )
            else
            (
              if ($antallUtfoerteKassasjoner > 0 and $omfatterDokumenterSomErKassert = "false") 
              then 
              (
              <resultItem name="samsvar" type="error">
                <label>Arkivdelen inneholder utførte kassasjoner. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket ikke inneholder dokumenter som er kassert.</label>
              </resultItem>
              )
              else 
              (
                if ($antallUtfoerteKassasjoner = 0 and $omfatterDokumenterSomErKassert = 'true') 
                then 
                (
              <resultItem name="samsvar" type="warning">
                <label>Arkivdelen inneholder ikke utførte kassasjoner. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket inneholder dokumenter som er kassert.</label>
              </resultItem>
                )
                else 
                (
                  if ($antallUtfoerteKassasjoner = 0 and $omfatterDokumenterSomErKassert = 'false') 
                  then 
                  (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder ikke utførte kassasjoner. 
                       Dette stemmer overens med at det i arkivuttrekk.xml er oppgitt at
                       arkivuttrekket ikke inneholder dokumenter som er kassert.</label>
              </resultItem>
                  )
                  else 
                  (
                    if ($antallUtfoerteKassasjoner > 0 and $omfatterDokumenterSomErKassert = 'true') 
                    then 
                    (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder utførte kassasjoner.
                       Dette stemmer overens med at det i arkivuttrekk.xml er oppgitt at
                       arkivuttrekket inneholder dokumenter som er kassert.</label>
              </resultItem>
                    )
                    else ()
                  )
                )
              )
            )
            }
              
              <resultItem type="info">
                <label>Det er {if ($antallUtfoerteKassasjonerArkivdel) then () else ("ikke ")}registrert
 utførte kassasjoner på arkivdelnivå.</label>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall utførte kassasjoner i dokumentbeskrivelse</label>
                <content>{$antallUtfoerteKassasjonerDokumentbeskrivelse}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
      </resultItem>
      ) else ()
      }      
    </resultItems>
  </result>
</activity>

