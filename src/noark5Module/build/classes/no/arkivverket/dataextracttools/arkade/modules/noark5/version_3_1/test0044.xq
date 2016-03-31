xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 44 i versjon 14 av testoppleggsdokumentet.
 : Antall kassasjonsvedtak i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall kassasjonsvedtak i arkivstruktur.xml.
 : Forekomster av elementet kassasjon telles opp på arkivdelnivå, 
 : og i klasser, mapper, registreringer og dokumentbeskrivelser. 
 : Det kontrolleres om arkivuttrekk.xml inneholder opplysning 
 : (inneholderDokumenterSomSkalKasseres) om at det finnes kassasjonsvedtak i arkivuttrekket, 
 : og om denne opplysningen stemmer med innholdet i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel. 
 : For hver arkivdel oppgis det om forekomst av kassasjon finnes på arkivdelnivå, 
 : og antall øvrige kassasjonsvedtak vises fordelt på klasse, mappe, registrering og dokumentbeskrivelse.
 : Element: kassasjon
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

let $inneholderDokumenterSomSkalKasseres := 
  $arkivuttrekkDoc/aml:addml/aml:dataset/aml:dataObjects/aml:dataObject[@name="Noark 5-arkivuttrekk"]/
  aml:properties/aml:property[@name="info"]/aml:properties/aml:property[@name="additionalInfo"]/
  aml:properties/aml:property[@name="inneholderDokumenterSomSkalKasseres"]/aml:value/normalize-space(.)

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
        let $antallKassasjonerArkivdel := count($arkivdel/n5a:kassasjon)
        let $antallKassasjonerKlasse := count($arkivdel//n5a:klasse/n5a:kassasjon)
        let $antallKassasjonerMappe := count($arkivdel//n5a:mappe/n5a:kassasjon)
        let $antallKassasjonerRegistrering := count($arkivdel//n5a:registrering/n5a:kassasjon)
        let $antallKassasjonerDokumentbeskrivelse := count($arkivdel//n5a:dokumentbeskrivelse/n5a:kassasjon)
        let $antallKassasjoner := $antallKassasjonerArkivdel + $antallKassasjonerKlasse 
                                  + $antallKassasjonerMappe + $antallKassasjonerRegistrering
                                  + $antallKassasjonerDokumentbeskrivelse        
        return
      <resultItem name="arkivdel" type="{if ($antallKassasjoner > 0 and 
                                             $inneholderDokumenterSomSkalKasseres = "false" or 
                                             not($inneholderDokumenterSomSkalKasseres))
                                         then 'error'
                                         else (if ($antallKassasjoner = 0 and 
                                                   $inneholderDokumenterSomSkalKasseres = "true")
                                               then 'warning'
                                               else 'info')}">
        <identifiers>
          <identifier name="systemID" value="{data($arkivdel/n5a:systemID)}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
            {
            if (not($inneholderDokumenterSomSkalKasseres)) 
            then 
            (
              <resultItem name="samsvar" type="error">
                <label>Informasjon om arkivuttrekket inneholder dokumenter som skal kasseres, mangler i arkivuttrekk.xml.</label>
              </resultItem>
            )
            else
            (
              if ($antallKassasjoner > 0 and $inneholderDokumenterSomSkalKasseres = "false") 
              then 
              (
              <resultItem name="samsvar" type="error">
                <label>Arkivdelen inneholder kassasjonsvedtak. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket ikke inneholder dokumenter som skal kasseres.</label>
              </resultItem>
              )
              else 
              (
                if ($antallKassasjoner = 0 and $inneholderDokumenterSomSkalKasseres = 'true') 
                then 
                (
              <resultItem name="samsvar" type="warning">
                <label>Arkivdelen inneholder ikke kassasjonsvedtak. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket inneholder dokumenter som skal kasseres.</label>
              </resultItem>
                )
                else 
                (
                  if ($antallKassasjoner = 0 and $inneholderDokumenterSomSkalKasseres = 'false') 
                  then 
                  (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder ikke kassasjonsvedtak. Dette stemmer overens med at det i
 arkivuttrekk.xml er oppgitt at
 arkivuttrekket ikke inneholder dokumenter som skal kasseres.</label>
              </resultItem>
                  )
                  else 
                  (
                    if ($antallKassasjoner > 0 and $inneholderDokumenterSomSkalKasseres = 'true') 
                    then 
                    (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder kassasjonsvedtak. Dette stemmer overens med at det i
 arkivuttrekk.xml er oppgitt at
 arkivuttrekket inneholder dokumenter som skal kasseres.</label>
              </resultItem>
                    )
                    else ()
                  )
                )
              )
              )
              }
              
              <resultItem type="info">
                <label>Det er {if ($antallKassasjonerArkivdel) 
                               then () 
                               else ("ikke ")}registrert kassasjonsvedtak på arkivdelnivå.</label>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall kassasjonsvedtak i klasser</label>
                <content>{$antallKassasjonerKlasse}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall kassasjonsvedtak i mapper</label>
                <content>{$antallKassasjonerMappe}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall kassasjonsvedtak i registreringer</label>
                <content>{$antallKassasjonerRegistrering}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall kassasjonsvedtak i dokumentbeskrivelse</label>
                <content>{$antallKassasjonerDokumentbeskrivelse}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
      </resultItem>
      ) else ()}      
    </resultItems>
  </result>
</activity>

