xquery version "1.0";

(:
 : @version 0.14 2013-11-27
 : @author Riksarkivet 
 :
 : Test 42 i versjon 14 av testoppleggsdokumentet.
 : Antall skjerminger i arkivstrukturen.
 : Type: Analyse og kontroll
 : Opptelling av antall skjerminger i arkivstrukturen. 
 : Forekomster av elementet skjerming telles opp på arkivdelnivå, 
 : og i klasser, mapper, registreringer og dokumentbeskrivelser.
 : Det kontrolleres om arkivuttrekk.xml inneholder opplysning 
 : (inneholderSkjermetInformasjon) om at informasjon i arkivuttrekket skal skjermes, 
 : og om denne opplysningen stemmer med innholdet i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Element: skjerming
 : 
 :)
                           
declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace xsi = "http://www.w3.org/2001/XMLSchema-instance";
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

let $inneholderSkjermetInformasjon := 
  $arkivuttrekkDoc/aml:addml/aml:dataset/aml:dataObjects/aml:dataObject[@name="Noark 5-arkivuttrekk"]/
  aml:properties/aml:property[@name="info"]/aml:properties/aml:property[@name="additionalInfo"]/
  aml:properties/aml:property[@name="inneholderSkjermetInformasjon"]/aml:value/normalize-space(.)

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
        let $antallSkjerminger := count($arkivdel//n5a:skjerming)
        let $antallSkjermingerKlasse := count($arkivdel//n5a:klasse/n5a:skjerming)
        let $antallSkjermingerMappe := count($arkivdel//n5a:mappe/n5a:skjerming)
        let $antallSkjermingerRegistrering := count($arkivdel//n5a:registrering/n5a:skjerming)
        let $antallSkjermingerDokumentbeskrivelse := count($arkivdel//n5a:dokumentbeskrivelse/n5a:skjerming)
        
        return
      <resultItem name="arkivdel" type="{if ($antallSkjerminger > 0 and 
                                             $inneholderSkjermetInformasjon = "false")
                                         then 'error'
                                         else (if ($antallSkjerminger = 0 and 
                                                   $inneholderSkjermetInformasjon = "true")
                                               then 'warning'
                                               else 'info')}">
        <identifiers>
          <identifier name="systemID" value="{data($arkivdel/n5a:systemID)}"/> 
        </identifiers>  
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              {
              if ($antallSkjerminger > 0 and $inneholderSkjermetInformasjon = "false") 
              then 
              (
              <resultItem name="samsvar" type="error">
                <label>Arkivdelen inneholder skjerminger. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket ikke inneholder skjermet informasjon.</label>
              </resultItem>
              )
              else 
              (
                if ($antallSkjerminger = 0 and $inneholderSkjermetInformasjon = 'true') 
                then 
                (
              <resultItem name="samsvar" type="warning">
                <label>Arkivdelen inneholder ikke skjerminger. Det er oppgitt i arkivuttrekk.xml at
 arkivuttrekket inneholder skjermet informasjon.</label>
              </resultItem>
              )
              else 
              (
                if ($antallSkjerminger = 0 and $inneholderSkjermetInformasjon = 'false') 
                then 
                (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder ikke skjerminger. Dette stemmer overens med at det i
 arkivuttrekk.xml er oppgitt at
 arkivuttrekket ikke inneholder skjermet informasjon.</label>
              </resultItem>
                )
                else 
                (
                  if ($antallSkjerminger > 0 and $inneholderSkjermetInformasjon = 'true') 
                  then 
                  (
              <resultItem name="samsvar" type="info">
                <label>Arkivdelen inneholder skjerminger. Dette stemmer overens med at det i
 arkivuttrekk.xml er oppgitt at
 arkivuttrekket inneholder skjermet informasjon.</label>
              </resultItem>
                  )
                  else ()
                )
              )
              )
              }
              
              <resultItem type="info">
                <label>Det er {if ($arkivdel/n5a:skjerming) then () else ("ikke ")}registrert
 skjerming på arkivdelnivå.</label>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall skjerminger i klasser</label>
                <content>{$antallSkjermingerKlasse}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall skjerminger i mapper</label>
                <content>{$antallSkjermingerMappe}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall skjerminger i registreringer</label>
                <content>{$antallSkjermingerRegistrering}</content>
              </resultItem>
              <resultItem name="antall" type="info">
                <label>Antall skjerminger i dokumentbeskrivelse</label>
                <content>{$antallSkjermingerDokumentbeskrivelse}</content>
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
