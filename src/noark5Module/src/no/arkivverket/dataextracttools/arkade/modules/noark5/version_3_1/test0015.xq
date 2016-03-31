xquery version "1.0";

(:
 : @author Riksarkivet 
 : @version 0.15 2013-11-27
 :
 : Test 15 i versjon 14 av testoppleggsdokumentet.
 : Saksmappenes status i arkivstrukturen.
 : Type: Analyse
 : Opptelling av de forskjellige verdiene i saksmappenes status i arkivstruktur.xml.
 : Resultatet vises fordelt på arkivdel.
 : Element: saksstatus
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
        let $saksmapper := $arkivdel//n5a:mappe[@xsi:type = "saksmappe"]
        let $antallSaksmapperUtenSaksstatus := count($saksmapper[not(n5a:saksstatus)])
        return
            <resultItem name="arkivdel" type="info">
              <identifiers>
                <identifier name="systemID" value="{$arkivdel/n5a:systemID/normalize-space()}"/> 
              </identifiers>  
              <resultItems>
                <resultItem name="overordnetResultat">
                <resultItems>
                 <resultItem type="info">
                  <label>Antall saksmapper i arkivdelen</label>
                  <content>{count($saksmapper)}</content>
                </resultItem>
                </resultItems>
               </resultItem>
               {
               if ($saksmapper) then (
               <resultItem name="detaljertResultat">
                 <resultItems>
                   <resultItem name="fordelingAvSaksstatus" type="info">
                     <label>Fordeling av saksstatus</label>
                     <resultItems>
                     {
                     for $saksstatus in distinct-values($saksmapper/n5a:saksstatus)
                     let $antall := count($saksmapper[n5a:saksstatus = $saksstatus])
                     order by $saksstatus
                     return
                       <resultItem name="saksstatus" type="info">
                         <label>{$saksstatus}</label>
                         <content>{$antall}</content>
                       </resultItem>
                     }
                       <resultItem type="{if ($antallSaksmapperUtenSaksstatus = 0)
                                          then 'info'
                                          else 'error'}">
                         <label>[Uten saksstatus]</label>
                         <content>{$antallSaksmapperUtenSaksstatus}</content>
                       </resultItem>
                     </resultItems>
                   </resultItem>
                 </resultItems>
               </resultItem>
               ) else ()
               }
              </resultItems>
            </resultItem>
        )}         
      </resultItems>
    </result>
  </activity>

