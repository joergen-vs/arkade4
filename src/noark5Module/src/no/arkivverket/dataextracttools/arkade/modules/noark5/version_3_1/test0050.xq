xquery version "1.0";

(:
 : @version 0.12 2013-11-27
 : @author Riksarkivet 
 :
 : Test 50 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at avskrivningsreferansene i arkivstrukturen er gyldige.
 : Type: Kontroll
 : Kontroll på at alle referanser til journalpostene som avskriver denne journalposten i
 : arkivstruktur.xml er gyldige.
 : Resultatet vises fordelt på arkivdel.
 :
 : Element: referanseAvskrivesAvJournalpost
 : Forutsetning: referanseAvskrivesAvJournalpost kan kun forekomme i 
 :               registreringer av type journalpost
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
      let $mapperMedAvskrivningsreferanser :=
      $arkivdel//n5a:mappe[.//n5a:registrering/n5a:avskrivning/n5a:referanseAvskrivesAvJournalpost]
      let $antallMapperMedAvskrivningsreferanser := count($mapperMedAvskrivningsreferanser)
      let $antallAvskrivningsreferanser := 
      count($mapperMedAvskrivningsreferanser//n5a:registrering/n5a:avskrivning/n5a:referanseAvskrivesAvJournalpost)
      let $ugyldigeReferanser :=
        for $m in $mapperMedAvskrivningsreferanser
        let $ugyldige :=
          for $r in $m//n5a:registrering/n5a:avskrivning/n5a:referanseAvskrivesAvJournalpost
          return if (not($m//n5a:registrering/n5a:systemID/normalize-space(.) = normalize-space($r))) then
            <resultItem name="ugyldigReferanseAvskrivesAvJournalpost" type="error">
              <resultItems>
                <resultItem name="mappe" type="info">
                  <content>{$m/n5a:systemID/data(.)}</content>
                </resultItem>
                <resultItem name="registrering" type="info">
                  <content>{$r/../../n5a:systemID/data(.)}</content>
                </resultItem>
                <resultItem name="referanseAvskrivesAvJournalpost" type="info">
                  <content>{data($r)}</content>
                </resultItem>
              </resultItems>
            </resultItem>
                 else ()
        return if (not(empty($ugyldige))) then
                 $ugyldige
               else ()
      let $antallUgyldigeReferanser := count($ugyldigeReferanser)
      return
      <resultItem name="arkivdel">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers> 
        <resultItems>
          <resultItem name="overordnetResultat">
            <resultItems>
              <resultItem type="{if ($antallMapperMedAvskrivningsreferanser > 0)
                                 then ('info')
                                 else ('warning')}">
                <label>Antall mapper med avskrivningsreferanser i arkivdelen</label>
                <content>{$antallMapperMedAvskrivningsreferanser}</content>
              </resultItem>
              <resultItem type="{if ($antallAvskrivningsreferanser > 0)
                                 then ('info')
                                 else ('warning')}">
                <label>Antall avskrivningsreferanser i arkivdelen</label>
                <content>{$antallAvskrivningsreferanser}</content>
          </resultItem>    
              <resultItem type="{if ($antallUgyldigeReferanser > 0)
                                 then ('error')
                                 else ('info')}">
                <label>Antall ugyldige avskrivningsreferanser i arkivdelen</label>
                <content>{$antallUgyldigeReferanser}</content>
          </resultItem>    
          </resultItems>
          </resultItem>
         
          {
          if ($maxNumberOfResults > 0 and $antallUgyldigeReferanser > 0)
          then (
          <resultItem name="detaljertResultat">
            <resultItems>
              <resultItem name="ugyldigeAvskrivningsreferanser">
                <label>Ugyldige avskrivningsreferanser. Viser 
                       {if ($maxNumberOfResults >= $antallUgyldigeReferanser) 
                        then (concat(" ", $antallUgyldigeReferanser, " (alle). ")) 
                        else concat($maxNumberOfResults, " av ", $antallUgyldigeReferanser, ". ")}
                        Verdi i systemID vises for mappe, journalpost og ugyldig avskrivningsreferanse.
                </label>
                <resultItems>
                  {
                  for $ugyldigReferanse at $pos1 in $ugyldigeReferanser
                  return
                    if ($pos1 <= $maxNumberOfResults) then (
                      $ugyldigReferanse 
                    ) else ()
                  }
                </resultItems>
              </resultItem>
            </resultItems>    
          </resultItem>
          ) 
          else ()
          }
        </resultItems>
      </resultItem>   
      )
      }
    </resultItems>
  </result>
</activity>
