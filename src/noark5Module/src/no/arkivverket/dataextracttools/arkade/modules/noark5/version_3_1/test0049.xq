xquery version "1.0";

(:
 : @version 0.15 2013-11-27
 : @author Riksarkivet 
 :
 : Test 49 i versjon 14 av testoppleggsdokumentet.
 : Kontroll på at kryssreferansene i arkivstrukturen er gyldige.
 : Type: Kontroll
 : Kontroll på at alle referanser fra klasse til klasse, 
 : fra mappe til mappe, fra mappe til basisregistrering, 
 : fra basisregistrering til basisregistrering og fra basisregistrering til mappe 
 : i arkivstruktur.xml er gyldige.
 :
 : Element: referanseTilKlasse, referanseTilMappe, referanseTilRegistrering i kryssreferanse
 :
 : 
 : !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 : Kontroll av kryssreferanser fra klasse:
 : 1.    Viser antall kryssreferanser fra klasse til klasse.
 : 2.    Sjekker om det finnes referanser fra klasse til mappe og fra klasse til registrering (ulovlig).
 :       Viser i så fall hvilke referanser som er feil (begrenset av maks antall resultater).
 : 3.    Sjekker om referansene fra klasser refererer til klasser i samme arkivdel.
 : 3.1   Hvis ikke, sjekkes det om referansene fra klasser refererer til klasser i en annen arkivdel i uttrekket.
 : 3.1.1 Hvis ikke, rapporteres det at referansene kan være eksterne. 
 :       Viser i så fall disse referansene (begrenset av maks antall resultater).  
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
let $start := current-dateTime()

return 
<activity name="{$testName}"
  longName="{if ($longName ne "") then ($longName) else ($testName)}"
  orderKey="{$orderKey}">
  <identifiers>
    <identifier name="id" value="{$testId}"/>
    <identifier name="uuid" value="{util:uuid()}"/>
  </identifiers>,
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
      ) 
      else 
      (
     
      for $arkivdel in $arkivstrukturDoc//n5a:arkiv/n5a:arkivdel
      let $systemIDArkivdel := $arkivdel/n5a:systemID
      let $andreArkivdeler := $arkivstrukturDoc//n5a:arkivdel/n5a:systemID
          [normalize-space() != normalize-space($systemIDArkivdel)]/normalize-space()

      (: Kryssreferanser fra klasse :)
      
      let $kryssreferanserFraKlasseResultat := 
        if ($arkivdel//n5a:klasse/n5a:kryssreferanse) then (
        
          let $kryssreferanserFraKlasseTilKlasse :=
          $arkivdel//n5a:klasse/n5a:kryssreferanse/n5a:referanseTilKlasse
          
          let $totaltAntallReferanserFraKlasseTilKlasse := count($kryssreferanserFraKlasseTilKlasse)

          let $systemIDerKlasse := $arkivdel//n5a:klasse/n5a:systemID/normalize-space()

          let $kryssreferanserFraKlasseTilKlasseUtenforArkivdelen :=
              $kryssreferanserFraKlasseTilKlasse[not(. = $systemIDerKlasse)]
              
          let $antallReferanserFraKlasseTilKlasseUtenforArkivdelen :=
              count($kryssreferanserFraKlasseTilKlasseUtenforArkivdelen)
              
          let $antallReferanserFraKlasseTilKlasseIArkivdelen :=
              $totaltAntallReferanserFraKlasseTilKlasse - $antallReferanserFraKlasseTilKlasseUtenforArkivdelen
              
          let $andreKlasseIDer := $arkivstrukturDoc//n5a:arkivdel
              [n5a:systemID/normalize-space() = $andreArkivdeler]//n5a:klasse/n5a:systemID/normalize-space()        

          let $kryssreferanserFraKlasseTilKlasseIAnnenArkivdel :=
              $kryssreferanserFraKlasseTilKlasseUtenforArkivdelen[. = $andreKlasseIDer]
          let $antallReferanserFraKlasseTilKlasseIAnnenArkivdel :=
              count($kryssreferanserFraKlasseTilKlasseIAnnenArkivdel)
              
          let $muligEksterneReferanserFraKlasseTilKlasse :=
              $kryssreferanserFraKlasseTilKlasseUtenforArkivdelen[not(. =
              $kryssreferanserFraKlasseTilKlasseIAnnenArkivdel)]
          let $antallMuligEksterneReferanserFraKlasseTilKlasse :=
              count($muligEksterneReferanserFraKlasseTilKlasse)
          
          (: Ikke lov - Kryssreferanser fra klasse til mappe :)
          let $kryssreferanserFraKlasseTilMappe := $arkivdel//n5a:klasse/n5a:kryssreferanse/n5a:referanseTilMappe
          let $antallReferanserFraKlasseTilMappe := count($kryssreferanserFraKlasseTilMappe)
          
          (: Ikke lov - Kryssreferanser fra klasse til registrering :)
          let $kryssreferanserFraKlasseTilRegistrering := $arkivdel//n5a:klasse/n5a:kryssreferanse/n5a:referanseTilRegistrering
          let $antallReferanserFraKlasseTilRegistrering := count($kryssreferanserFraKlasseTilRegistrering)
          
          return (
            <resultItem name="kryssreferanserFraKlasser">
              <resultItems>
                <resultItem name="overordnetResultat">
                  <resultItems>
                    
                    <resultItem name="totaltAntallReferanserFraKlasseTilKlasse" type="info">
                      <label>Totalt antall kryssreferanser fra klasse til klasse</label>
                      <content>{$totaltAntallReferanserFraKlasseTilKlasse}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraKlasseTilKlasseIArkivdelen" type="info">
                      <label>Antall kryssreferanser fra klasse til klasse i arkivdelen</label>
                      <content>{$antallReferanserFraKlasseTilKlasseIArkivdelen}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraKlasseTilKlasseUtenforArkivdelen" 
                                type="{if ($antallReferanserFraKlasseTilKlasseUtenforArkivdelen = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra klasse til klasse utenfor arkivdelen</label>
                      <content>{$antallReferanserFraKlasseTilKlasseUtenforArkivdelen}</content>
                    </resultItem>
                                       
                    <resultItem name="antallReferanserFraKlasseTilKlasseIAnnenArkivdel"
                                type="{if ($antallReferanserFraKlasseTilKlasseIAnnenArkivdel = 0)
                                       then 'info'
                                       else 'warning'}">
                    <label>Antall kryssreferanser fra klasse til klasse i annen arkivdel</label>
                    <content>{$antallReferanserFraKlasseTilKlasseIAnnenArkivdel}</content>
                   </resultItem>
                   
                    <resultItem name="antallMuligEksterneReferanserFraKlasseTilKlasse" 
                                type="{if ($antallMuligEksterneReferanserFraKlasseTilKlasse = 0)
                                       then 'info'
                                       else 'warning'}">
                    <label>Antall mulig eksterne kryssreferanser fra klasse til klasse</label>
                    <content>{$antallMuligEksterneReferanserFraKlasseTilKlasse}</content>
                   </resultItem>
                   
                   <resultItem name="kryssreferanserFraKlasseTilMappe"
                                type="{if ($antallReferanserFraKlasseTilMappe = 0) 
                                       then ('info')
                                       else ('error')}">
                      <label>Antall kryssreferanser fra klasse til mappe</label>
                      <content>{$antallReferanserFraKlasseTilMappe}</content>
                    </resultItem>
                   
                    <resultItem name="kryssreferanserFraKlasseTilRegistrering"
                                type="{if ($antallReferanserFraKlasseTilRegistrering = 0) 
                                       then ('info')
                                       else ('error')}">
                      <label>Antall kryssreferanser fra klasse til registrering</label>
                      <content>{$antallReferanserFraKlasseTilRegistrering}</content>
                    </resultItem>
                  </resultItems>
                </resultItem>
                
                (: Detaljert resultat :)
                
                {
                if ($maxNumberOfResults > 0
                    and ($antallReferanserFraKlasseTilKlasseIAnnenArkivdel > 0
                         or $antallMuligEksterneReferanserFraKlasseTilKlasse > 0
                         or $antallReferanserFraKlasseTilMappe > 0
                         or $antallReferanserFraKlasseTilRegistrering >0))
                then (
                <resultItem name="detaljertResultat">
                  <resultItems>        
                    
                    (: Kryssreferanser fra klasse til klasse :)
                                        
                    {
                    if ($antallReferanserFraKlasseTilKlasseIAnnenArkivdel > 0)
                    then (
                    <resultItem name="kryssreferanserFraKlasseTilKlasseIAnnenArkivdel" type="warning">
                      <label>Kryssreferanser fra klasse til klasse i annen arkivdel. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraKlasseTilKlasseIAnnenArkivdel) 
                                    then (concat(" ", $antallReferanserFraKlasseTilKlasseIAnnenArkivdel, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraKlasseTilKlasseIAnnenArkivdel, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraKlasseTilKlasseIAnnenArkivdel/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:klasse/n5a:kryssreferanse[n5a:referanseTilKlasse/normalize-space()
                            = $referanse]/../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    {
                    if ($antallMuligEksterneReferanserFraKlasseTilKlasse > 0)
                    then (
                    <resultItem name="muligEksterneReferanserFraKlasseTilKlasse" type="warning">
                      <label>Mulig eksterne kryssreferanser fra klasse til klasse. 
                             Viser {if ($maxNumberOfResults >= $antallMuligEksterneReferanserFraKlasseTilKlasse) 
                                    then (concat(" ", $antallMuligEksterneReferanserFraKlasseTilKlasse, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallMuligEksterneReferanserFraKlasseTilKlasse, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $muligEksterneReferanserFraKlasseTilKlasse/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:klasse/n5a:kryssreferanse[n5a:referanseTilKlasse/normalize-space()
                            = $referanse]/../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }

                    (: Kryssreferanser fra klasse til mappe :)

                    {
                    if ($antallReferanserFraKlasseTilMappe > 0)
                    then (
                    <resultItem name="kryssreferanserFraKlasseTilMappe" type="error">
                      <label>Kryssreferanser fra klasse til mappe. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraKlasseTilMappe) 
                                    then (concat(" ", $antallReferanserFraKlasseTilMappe, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraKlasseTilMappe, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraKlasseTilMappe/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in
                          $arkivdel//n5a:klasse/n5a:kryssreferanse[n5a:referanseTilMappe/normalize-space()
                            = $referanse]/../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="error">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    (: Kryssreferanser fra klasse til registrering :)

                    {
                    if ($antallReferanserFraKlasseTilRegistrering > 0)
                    then (
                    <resultItem name="kryssreferanserFraKlasseTilRegistrering" type="error">
                      <label>Kryssreferanser fra klasse til registrering. 
                             Viser {if ($maxNumberOfResults >=
                             $antallReferanserFraKlasseTilRegistrering) 
                                    then (concat(" ", $antallReferanserFraKlasseTilRegistrering, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ",
                                    $antallReferanserFraKlasseTilRegistrering, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraKlasseTilRegistrering/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in
                          $arkivdel//n5a:klasse/n5a:kryssreferanse[n5a:referanseTilRegistrering/normalize-space()
                            = $referanse]/../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="error">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()                    
                    }
                  </resultItems>
                </resultItem>                    
                ) else ()
                }

              </resultItems>
            </resultItem>
          )
        )
        else ()
      
      (: Kryssreferanser fra mappe :)
      
      let $kryssreferanserFraMappeResultat := 
        if ($arkivdel//n5a:mappe/n5a:kryssreferanse) then (
        
          (: Kryssreferanser fra mappe til mappe :)

          let $kryssreferanserFraMappeTilMappe :=
          $arkivdel//n5a:mappe/n5a:kryssreferanse/n5a:referanseTilMappe
          
          let $totaltAntallReferanserFraMappeTilMappe := count($kryssreferanserFraMappeTilMappe)

          let $systemIDerMappe := $arkivdel//n5a:mappe/n5a:systemID/normalize-space()

          let $kryssreferanserFraMappeTilMappeUtenforArkivdelen :=
              $kryssreferanserFraMappeTilMappe[not(. = $systemIDerMappe)]
              
          let $antallReferanserFraMappeTilMappeUtenforArkivdelen :=
              count($kryssreferanserFraMappeTilMappeUtenforArkivdelen)
              
          let $antallReferanserFraMappeTilMappeIArkivdelen :=
              $totaltAntallReferanserFraMappeTilMappe - $antallReferanserFraMappeTilMappeUtenforArkivdelen
              
          let $andreMappeIDer := $arkivstrukturDoc//n5a:arkivdel
              [n5a:systemID/normalize-space() = $andreArkivdeler]//n5a:mappe/n5a:systemID/normalize-space()        

          let $kryssreferanserFraMappeTilMappeIAnnenArkivdel :=
              $kryssreferanserFraMappeTilMappeUtenforArkivdelen[. = $andreMappeIDer]
          let $antallReferanserFraMappeTilMappeIAnnenArkivdel :=
              count($kryssreferanserFraMappeTilMappeIAnnenArkivdel)
              
          let $muligEksterneReferanserFraMappeTilMappe :=
              $kryssreferanserFraMappeTilMappeUtenforArkivdelen[not(. =
              $kryssreferanserFraMappeTilMappeIAnnenArkivdel)]
          let $antallMuligEksterneReferanserFraMappeTilMappe :=
              count($muligEksterneReferanserFraMappeTilMappe)
          
          (: Kryssreferanser fra mappe til registrering :)
          
          let $kryssreferanserFraMappeTilRegistrering :=
          $arkivdel//n5a:mappe/n5a:kryssreferanse/n5a:referanseTilRegistrering
          
          let $totaltAntallReferanserFraMappeTilRegistrering := count($kryssreferanserFraMappeTilRegistrering)

          let $systemIDerRegistrering := $arkivdel//n5a:registrering/n5a:systemID/normalize-space()

          let $kryssreferanserFraMappeTilRegistreringUtenforArkivdelen :=
              $kryssreferanserFraMappeTilRegistrering[not(. = $systemIDerRegistrering)]
              
          let $antallReferanserFraMappeTilRegistreringUtenforArkivdelen :=
              count($kryssreferanserFraMappeTilRegistreringUtenforArkivdelen)
              
          let $antallReferanserFraMappeTilRegistreringIArkivdelen :=
              $totaltAntallReferanserFraMappeTilRegistrering - $antallReferanserFraMappeTilRegistreringUtenforArkivdelen
              
          let $andreRegistreringIDer := $arkivstrukturDoc//n5a:arkivdel
              [n5a:systemID/normalize-space() = $andreArkivdeler]//n5a:registrering/n5a:systemID/normalize-space()        

          let $kryssreferanserFraMappeTilRegistreringIAnnenArkivdel :=
              $kryssreferanserFraMappeTilRegistreringUtenforArkivdelen[. = $andreRegistreringIDer]
          let $antallReferanserFraMappeTilRegistreringIAnnenArkivdel :=
              count($kryssreferanserFraMappeTilRegistreringIAnnenArkivdel)
              
          let $muligEksterneReferanserFraMappeTilRegistrering :=
              $kryssreferanserFraMappeTilRegistreringUtenforArkivdelen[not(. =
              $kryssreferanserFraMappeTilRegistreringIAnnenArkivdel)]
          let $antallMuligEksterneReferanserFraMappeTilRegistrering :=
              count($muligEksterneReferanserFraMappeTilRegistrering)
          
          (: Ikke lov - Kryssreferanser fra mappe til klasse :)
          let $kryssreferanserFraMappeTilKlasse := $arkivdel//n5a:mappe/n5a:kryssreferanse/n5a:referanseTilKlasse
          let $antallReferanserFraMappeTilKlasse := count($kryssreferanserFraMappeTilKlasse)
          
          return (
            <resultItem name="kryssreferanserFraMapper">
              <resultItems>
                <resultItem name="overordnetResultat">
                  <resultItems>

                    (: Kryssreferanser fra mappe til mappe :)
                    
                    <resultItem name="totaltAntallReferanserFraMappeTilMappe" type="info">
                      <label>Totalt antall kryssreferanser fra mappe til mappe</label>
                      <content>{$totaltAntallReferanserFraMappeTilMappe}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraMappeTilMappeIArkivdelen" type="info">
                      <label>Antall kryssreferanser fra mappe til mappe i arkivdelen</label>
                      <content>{$antallReferanserFraMappeTilMappeIArkivdelen}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraMappeTilMappeUtenforArkivdelen"
                                type="{if ($antallReferanserFraMappeTilMappeUtenforArkivdelen = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra mappe til mappe utenfor arkivdelen</label>
                      <content>{$antallReferanserFraMappeTilMappeUtenforArkivdelen}</content>
                    </resultItem>
                                       
                    <resultItem name="antallReferanserFraMappeTilMappeIAnnenArkivdel"
                                type="{if ($antallReferanserFraMappeTilMappeIAnnenArkivdel = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra mappe til mappe i annen arkivdel</label>
                      <content>{$antallReferanserFraMappeTilMappeIAnnenArkivdel}</content>
                    </resultItem>
                   
                    <resultItem name="antallMuligEksterneReferanserFraMappeTilMappe"
                                type="{if ($antallMuligEksterneReferanserFraMappeTilMappe = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall mulig eksterne kryssreferanser fra mappe til mappe</label>
                      <content>{$antallMuligEksterneReferanserFraMappeTilMappe}</content>
                    </resultItem>
                   
                    (: Kryssreferanser fra mappe til registrering :)
                   
                    <resultItem name="kryssreferanserFraMappeTilRegistrering" type="info">
                      <label>Totalt antall kryssreferanser fra mappe til registrering</label>
                      <content>{$totaltAntallReferanserFraMappeTilRegistrering}</content>
                    </resultItem>

                    <resultItem name="antallReferanserFraMappeTilRegistreringIArkivdelen" type="info">
                      <label>Antall kryssreferanser fra mappe til registrering i arkivdelen</label>
                      <content>{$antallReferanserFraMappeTilRegistreringIArkivdelen}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraMappeTilRegistreringUtenforArkivdelen"
                                type="{if ($antallReferanserFraMappeTilRegistreringUtenforArkivdelen = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra mappe til registrering utenfor arkivdelen</label>
                      <content>{$antallReferanserFraMappeTilRegistreringUtenforArkivdelen}</content>
                    </resultItem>
                                       
                    <resultItem name="antallReferanserFraMappeTilRegistreringIAnnenArkivdel"
                                type="{if ($antallReferanserFraMappeTilRegistreringIAnnenArkivdel = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra mappe til registrering i annen arkivdel</label>
                      <content>{$antallReferanserFraMappeTilRegistreringIAnnenArkivdel}</content>
                    </resultItem>
                   
                    <resultItem name="antallMuligEksterneReferanserFraMappeTilRegistrering"
                                type="{if ($antallMuligEksterneReferanserFraMappeTilRegistrering = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall mulig eksterne kryssreferanser fra mappe til registrering</label>
                      <content>{$antallMuligEksterneReferanserFraMappeTilRegistrering}</content>
                    </resultItem>

                    (: Kryssreferanser fra mappe til klasse :)
                   
                    <resultItem name="kryssreferanserFraMappeTilKlasse"
                                type="{if ($antallReferanserFraMappeTilKlasse = 0) 
                                       then ('info')
                                       else ('error')}">
                      <label>Antall kryssreferanser fra mappe til klasse</label>
                      <content>{$antallReferanserFraMappeTilKlasse}</content>
                    </resultItem>
                   
                  </resultItems>
                </resultItem>
                
                (: Detaljert resultat :)
                
                {
                if ($maxNumberOfResults > 0
                    and ($antallReferanserFraMappeTilMappeIAnnenArkivdel > 0
                         or $antallMuligEksterneReferanserFraMappeTilMappe > 0
                         or $antallReferanserFraMappeTilRegistreringIAnnenArkivdel > 0
                         or $antallMuligEksterneReferanserFraMappeTilRegistrering > 0
                         or $antallReferanserFraMappeTilKlasse >0))
                then (
                <resultItem name="detaljertResultat">
                  <resultItems>
                  
                    (: Kryssreferanser fra mappe til mappe :)
                    
                    {
                    if ($antallReferanserFraMappeTilMappeIAnnenArkivdel > 0)
                    then (
                    <resultItem name="kryssreferanserFraMappeTilMappeIAnnenArkivdel" type="warning">
                      <label>Kryssreferanser fra mappe til mappe i annen arkivdel. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraMappeTilMappeIAnnenArkivdel) 
                                    then (concat(" ", $antallReferanserFraMappeTilMappeIAnnenArkivdel, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraMappeTilMappeIAnnenArkivdel, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraMappeTilMappeIAnnenArkivdel/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:mappe/n5a:kryssreferanse[n5a:referanseTilMappe/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    {
                    if ($antallMuligEksterneReferanserFraMappeTilMappe > 0)
                    then (
                    <resultItem name="muligEksterneReferanserFraMappeTilMappe" type="warning">
                      <label>Mulig eksterne kryssreferanser fra mappe til mappe. 
                             Viser {if ($maxNumberOfResults >= $antallMuligEksterneReferanserFraMappeTilMappe) 
                                    then (concat(" ", $antallMuligEksterneReferanserFraMappeTilMappe, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallMuligEksterneReferanserFraMappeTilMappe, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $muligEksterneReferanserFraMappeTilMappe/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:mappe/n5a:kryssreferanse[n5a:referanseTilMappe/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }

                    (: Kryssreferanser fra mappe til registrering :)
                    
                    {
                    if ($antallReferanserFraMappeTilRegistreringIAnnenArkivdel > 0)
                    then (
                    <resultItem name="kryssreferanserFraMappeTilRegistreringIAnnenArkivdel" type="warning">
                      <label>Kryssreferanser fra mappe til registrering i annen arkivdel. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraMappeTilRegistreringIAnnenArkivdel) 
                                    then (concat(" ", $antallReferanserFraMappeTilRegistreringIAnnenArkivdel, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraMappeTilRegistreringIAnnenArkivdel, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraMappeTilRegistreringIAnnenArkivdel/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:mappe/n5a:kryssreferanse[n5a:referanseTilRegistrering/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    {
                    if ($antallMuligEksterneReferanserFraMappeTilRegistrering > 0)
                    then (
                    <resultItem name="muligEksterneReferanserFraMappeTilRegistrering" type="warning">
                      <label>Mulig eksterne kryssreferanser fra mappe til registrering. 
                             Viser {if ($maxNumberOfResults >= $antallMuligEksterneReferanserFraMappeTilRegistrering) 
                                    then (concat(" ", $antallMuligEksterneReferanserFraMappeTilRegistrering, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallMuligEksterneReferanserFraMappeTilRegistrering, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $muligEksterneReferanserFraMappeTilRegistrering/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:mappe/n5a:kryssreferanse[n5a:referanseTilRegistrering/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }

                    (: Kryssreferanser fra mappe til klasse :)

                    {
                    if ($antallReferanserFraMappeTilKlasse > 0)
                    then (
                    <resultItem name="kryssreferanserFraMappeTilKlasse" type="error">
                      <label>Kryssreferanser fra mappe til klasse. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraMappeTilKlasse) 
                                    then (concat(" ", $antallReferanserFraMappeTilKlasse, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ",
                                    $antallReferanserFraMappeTilKlasse, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraMappeTilKlasse/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in
                          $arkivdel//n5a:mappe/n5a:kryssreferanse[n5a:referanseTilKlasse/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                       <resultItem name="kryssreferanse" type="error">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                  </resultItems>
                </resultItem>                    
                ) else ()
                }
              </resultItems>
            </resultItem>
          )
        )
        else ()


      (: Kryssreferanser fra registrering :)
      
      let $kryssreferanserFraRegistreringResultat := 
        if ($arkivdel//n5a:registrering/n5a:kryssreferanse) then (
        
          (: Kryssreferanser fra registrering til registrering :)
          
          let $kryssreferanserFraRegistreringTilRegistrering :=
          $arkivdel//n5a:registrering/n5a:kryssreferanse/n5a:referanseTilRegistrering
          
          let $totaltAntallReferanserFraRegistreringTilRegistrering := count($kryssreferanserFraRegistreringTilRegistrering)

          let $systemIDerRegistrering := $arkivdel//n5a:registrering/n5a:systemID/normalize-space()

          let $kryssreferanserFraRegistreringTilRegistreringUtenforArkivdelen :=
              $kryssreferanserFraRegistreringTilRegistrering[not(. = $systemIDerRegistrering)]
              
          let $antallReferanserFraRegistreringTilRegistreringUtenforArkivdelen :=
              count($kryssreferanserFraRegistreringTilRegistreringUtenforArkivdelen)
              
          let $antallReferanserFraRegistreringTilRegistreringIArkivdelen :=
              $totaltAntallReferanserFraRegistreringTilRegistrering - $antallReferanserFraRegistreringTilRegistreringUtenforArkivdelen
              
          let $andreRegistreringIDer := $arkivstrukturDoc//n5a:arkivdel
              [n5a:systemID/normalize-space() = $andreArkivdeler]//n5a:registrering/n5a:systemID/normalize-space()        

          let $kryssreferanserFraRegistreringTilRegistreringIAnnenArkivdel :=
              $kryssreferanserFraRegistreringTilRegistreringUtenforArkivdelen[. = $andreRegistreringIDer]
          let $antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel :=
              count($kryssreferanserFraRegistreringTilRegistreringIAnnenArkivdel)
              
          let $muligEksterneReferanserFraRegistreringTilRegistrering :=
              $kryssreferanserFraRegistreringTilRegistreringUtenforArkivdelen[not(. =
              $kryssreferanserFraRegistreringTilRegistreringIAnnenArkivdel)]
          let $antallMuligEksterneReferanserFraRegistreringTilRegistrering :=
              count($muligEksterneReferanserFraRegistreringTilRegistrering)
              
          (: Kryssreferanser fra registrering til mappe :)

          let $kryssreferanserFraRegistreringTilMappe :=
          $arkivdel//n5a:registrering/n5a:kryssreferanse/n5a:referanseTilMappe
          
          let $totaltAntallReferanserFraRegistreringTilMappe := count($kryssreferanserFraRegistreringTilMappe)

          let $systemIDerMappe := $arkivdel//n5a:mappe/n5a:systemID/normalize-space()

          let $kryssreferanserFraRegistreringTilMappeUtenforArkivdelen :=
              $kryssreferanserFraRegistreringTilMappe[not(. = $systemIDerMappe)]
              
          let $antallReferanserFraRegistreringTilMappeUtenforArkivdelen :=
              count($kryssreferanserFraRegistreringTilMappeUtenforArkivdelen)
              
          let $antallReferanserFraRegistreringTilMappeIArkivdelen :=
              $totaltAntallReferanserFraRegistreringTilMappe - $antallReferanserFraRegistreringTilMappeUtenforArkivdelen
              
          let $andreMappeIDer := $arkivstrukturDoc//n5a:arkivdel
              [n5a:systemID/normalize-space() = $andreArkivdeler]//n5a:mappe/n5a:systemID/normalize-space()        

          let $kryssreferanserFraRegistreringTilMappeIAnnenArkivdel :=
              $kryssreferanserFraRegistreringTilMappeUtenforArkivdelen[. = $andreMappeIDer]
          let $antallReferanserFraRegistreringTilMappeIAnnenArkivdel :=
              count($kryssreferanserFraRegistreringTilMappeIAnnenArkivdel)
              
          let $muligEksterneReferanserFraRegistreringTilMappe :=
              $kryssreferanserFraRegistreringTilMappeUtenforArkivdelen[not(. =
              $kryssreferanserFraRegistreringTilMappeIAnnenArkivdel)]
          let $antallMuligEksterneReferanserFraRegistreringTilMappe :=
              count($muligEksterneReferanserFraRegistreringTilMappe)
          
          (: Ikke lov - Kryssreferanser fra registrering til klasse :)
          let $kryssreferanserFraRegistreringTilKlasse := $arkivdel//n5a:registrering/n5a:kryssreferanse/n5a:referanseTilKlasse
          let $antallReferanserFraRegistreringTilKlasse := count($kryssreferanserFraRegistreringTilKlasse)
          
          return (
            <resultItem name="kryssreferanserFraRegistreringer">
              <resultItems>
                <resultItem name="overordnetResultat">
                  <resultItems>

                    (: Kryssreferanser fra registrering til registrering :)
                   
                    <resultItem name="kryssreferanserFraRegistreringTilRegistrering" type="info">
                      <label>Totalt antall kryssreferanser fra registrering til registrering</label>
                      <content>{$totaltAntallReferanserFraRegistreringTilRegistrering}</content>
                    </resultItem>

                    <resultItem name="antallReferanserFraRegistreringTilRegistreringIArkivdelen" type="info">
                      <label>Antall kryssreferanser fra registrering til registrering i arkivdelen</label>
                      <content>{$antallReferanserFraRegistreringTilRegistreringIArkivdelen}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraRegistreringTilRegistreringUtenforArkivdelen"
                                type="{if ($antallReferanserFraRegistreringTilRegistreringUtenforArkivdelen = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra registrering til registrering utenfor arkivdelen</label>
                      <content>{$antallReferanserFraRegistreringTilRegistreringUtenforArkivdelen}</content>
                    </resultItem>
                                       
                    <resultItem name="antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel"
                                type="{if ($antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra registrering til registrering i annen arkivdel</label>
                      <content>{$antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel}</content>
                    </resultItem>
                   
                    <resultItem name="antallMuligEksterneReferanserFraRegistreringTilRegistrering"
                                type="{if ($antallMuligEksterneReferanserFraRegistreringTilRegistrering = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall mulig eksterne kryssreferanser fra registrering til registrering</label>
                      <content>{$antallMuligEksterneReferanserFraRegistreringTilRegistrering}</content>
                    </resultItem>

                    (: Kryssreferanser fra registrering til mappe :)
                    
                    <resultItem name="totaltAntallReferanserFraRegistreringTilMappe" type="info">
                      <label>Totalt antall kryssreferanser fra registrering til mappe</label>
                      <content>{$totaltAntallReferanserFraRegistreringTilMappe}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraRegistreringTilMappeIArkivdelen" type="info">
                      <label>Antall kryssreferanser fra registrering til mappe i arkivdelen</label>
                      <content>{$antallReferanserFraRegistreringTilMappeIArkivdelen}</content>
                    </resultItem>
                    
                    <resultItem name="antallReferanserFraRegistreringTilMappeUtenforArkivdelen"
                                type="{if ($antallReferanserFraRegistreringTilMappeUtenforArkivdelen = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra registrering til mappe utenfor arkivdelen</label>
                      <content>{$antallReferanserFraRegistreringTilMappeUtenforArkivdelen}</content>
                    </resultItem>
                                       
                    <resultItem name="antallReferanserFraRegistreringTilMappeIAnnenArkivdel"
                                type="{if ($antallReferanserFraRegistreringTilMappeIAnnenArkivdel = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall kryssreferanser fra registrering til mappe i annen arkivdel</label>
                      <content>{$antallReferanserFraRegistreringTilMappeIAnnenArkivdel}</content>
                    </resultItem>
                   
                    <resultItem name="antallMuligEksterneReferanserFraRegistreringTilMappe"
                                type="{if ($antallMuligEksterneReferanserFraRegistreringTilMappe = 0)
                                       then 'info'
                                       else 'warning'}">
                      <label>Antall mulig eksterne kryssreferanser fra registrering til mappe</label>
                      <content>{$antallMuligEksterneReferanserFraRegistreringTilMappe}</content>
                    </resultItem>
                   
                    (: Kryssreferanser fra registrering til klasse :)
                   
                    <resultItem name="kryssreferanserFraRegistreringTilKlasse"
                                type="{if ($antallReferanserFraRegistreringTilKlasse = 0) 
                                       then ('info')
                                       else ('error')}">
                      <label>Antall kryssreferanser fra registrering til klasse</label>
                      <content>{$antallReferanserFraRegistreringTilKlasse}</content>
                    </resultItem>
                   
                  </resultItems>
                </resultItem>
                
                (: Detaljert resultat :)
                
                {
                if ($maxNumberOfResults > 0
                    and ($antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel > 0
                         or $antallMuligEksterneReferanserFraRegistreringTilRegistrering > 0
                         or $antallReferanserFraRegistreringTilMappeIAnnenArkivdel > 0
                         or $antallMuligEksterneReferanserFraRegistreringTilMappe > 0
                         or $antallReferanserFraRegistreringTilKlasse >0))
                then (
                <resultItem name="detaljertResultat">
                  <resultItems>        
                    
                    (: Kryssreferanser fra registrering til registrering :)
                    
                    {
                    if ($antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel > 0)
                    then (
                    <resultItem name="kryssreferanserFraRegistreringTilRegistreringIAnnenArkivdel" type="warning">
                      <label>Kryssreferanser fra registrering til registrering i annen arkivdel. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel) 
                                    then (concat(" ", $antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraRegistreringTilRegistreringIAnnenArkivdel, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraRegistreringTilRegistreringIAnnenArkivdel/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:registrering/n5a:kryssreferanse[n5a:referanseTilRegistrering/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    {
                    if ($antallMuligEksterneReferanserFraRegistreringTilRegistrering > 0)
                    then (
                    <resultItem name="muligEksterneReferanserFraRegistreringTilRegistrering" type="warning">
                      <label>Mulig eksterne kryssreferanser fra registrering til registrering. 
                             Viser {if ($maxNumberOfResults >= $antallMuligEksterneReferanserFraRegistreringTilRegistrering) 
                                    then (concat(" ", $antallMuligEksterneReferanserFraRegistreringTilRegistrering, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallMuligEksterneReferanserFraRegistreringTilRegistrering, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $muligEksterneReferanserFraRegistreringTilRegistrering/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:registrering/n5a:kryssreferanse[n5a:referanseTilRegistrering/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }

                    (: Kryssreferanser fra registrering til mappe :)
                    
                    {
                    if ($antallReferanserFraRegistreringTilMappeIAnnenArkivdel > 0)
                    then (
                    <resultItem name="kryssreferanserFraRegistreringTilMappeIAnnenArkivdel" type="warning">
                      <label>Kryssreferanser fra registrering til mappe i annen arkivdel. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraRegistreringTilMappeIAnnenArkivdel) 
                                    then (concat(" ", $antallReferanserFraRegistreringTilMappeIAnnenArkivdel, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallReferanserFraRegistreringTilMappeIAnnenArkivdel, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraRegistreringTilMappeIAnnenArkivdel/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:registrering/n5a:kryssreferanse[n5a:referanseTilMappe/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                    {
                    if ($antallMuligEksterneReferanserFraRegistreringTilMappe > 0)
                    then (
                    <resultItem name="muligEksterneReferanserFraRegistreringTilMappe" type="warning">
                      <label>Mulig eksterne kryssreferanser fra registrering til mappe. 
                             Viser {if ($maxNumberOfResults >= $antallMuligEksterneReferanserFraRegistreringTilMappe) 
                                    then (concat(" ", $antallMuligEksterneReferanserFraRegistreringTilMappe, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ", $antallMuligEksterneReferanserFraRegistreringTilMappe, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $muligEksterneReferanserFraRegistreringTilMappe/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:registrering/n5a:kryssreferanse[n5a:referanseTilMappe/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                        <resultItem name="kryssreferanse" type="warning">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }


                    (: Kryssreferanser fra registrering til klasse :)

                    {
                    if ($antallReferanserFraRegistreringTilKlasse > 0)
                    then (
                    <resultItem name="kryssreferanserFraRegistreringTilKlasse" type="error">
                      <label>Kryssreferanser fra registrering til klasse. 
                             Viser {if ($maxNumberOfResults >= $antallReferanserFraRegistreringTilKlasse) 
                                    then (concat(" ", $antallReferanserFraRegistreringTilKlasse, " (alle).")) 
                                    else concat($maxNumberOfResults, " av ",
                                    $antallReferanserFraRegistreringTilKlasse, ".")}
                      </label>
                      <resultItems>
                      {
                      for $referanse at $pos1 in $kryssreferanserFraRegistreringTilKlasse/normalize-space()
                      return
                        if ($pos1 <= $maxNumberOfResults)
                        then (
                          for $forelder in $arkivdel//n5a:registrering/n5a:kryssreferanse[n5a:referanseTilKlasse/normalize-space() = $referanse]
                                           /../n5a:systemID/normalize-space()
                          return (
                       <resultItem name="kryssreferanse" type="error">
                          <identifiers>
                            <identifier name="systemID" value="{$forelder}"/>
                          </identifiers>
                          <content>{$referanse}</content>
                        </resultItem>
                        )
                        ) else ()
                      }
                      </resultItems>
                    </resultItem>
                    ) else ()
                    }
                    
                  </resultItems>
                </resultItem>                    
                ) else ()
                }
              </resultItems>
            </resultItem>
          )
        )
        else ()

      return
      <resultItem name="arkivdel">
        <identifiers>
          <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
        </identifiers> 
        <resultItems>
              {$kryssreferanserFraKlasseResultat}
              {$kryssreferanserFraMappeResultat}
              {$kryssreferanserFraRegistreringResultat}
        </resultItems>
      </resultItem>
      )
    }
    </resultItems>
  </result>
</activity>
