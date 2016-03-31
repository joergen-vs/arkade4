xquery version "1.0";

(: 
 : @version 0.04 2013-11-27
 : @author Riksarkivet 
 :
 : Test 2 i versjon 14 av testoppleggsdokumentet.
 : 
 : Kontroll av sjekksummene for XML-filene og XML-skjemaene i avleveringspakken.
 : Type: Kontroll
 : Kontroll på at sjekksummene for XML-filene og XML-skjemaene i avleveringspakken er korrekte.
 : Sjekksummene for disse filene ligger i arkivuttrekk.xml.
 :)

declare default element namespace "http://www.arkivverket.no/dataextracttools/arkade/sessionreport";
declare namespace aml = "http://www.arkivverket.no/standarder/addml";
declare namespace n5a = "http://www.arkivverket.no/standarder/noark5/arkivstruktur";
declare namespace file = "http://exist-db.org/xquery/file";
declare namespace util = "http://exist-db.org/xquery/util";
declare namespace java_file = "java:java.io.File";
declare namespace ra_utils = "java:no.arkivverket.dataextracttools.utils.Utils";
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

declare function local:beregn-sjekksum($dokumentfil as xs:string, $algoritme as xs:string) {
  if (file:exists($dokumentfil)) then
  (
    let $fil := java_file:new($dokumentfil)
    return
      ra_utils:create-file-checksum($fil, $algoritme)          
   )
   else ()
};

let $arkivuttrekkDocFileName := "arkivuttrekk.xml"
let $arkivuttrekkDoc := doc(concat($metadataCollection, "/", $arkivuttrekkDocFileName))
let $directory := concat($rootDirectory, '/')

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
      for $file in $arkivuttrekkDoc//aml:property[@name='file']
      let $fileName := $file/aml:properties/aml:property[@name='name']/aml:value/normalize-space()
      let $checksum := $file/aml:properties/aml:property[@name='checksum']
      let $algorithm := $checksum//aml:property[@name='algorithm']/aml:value/normalize-space()
      let $value := $checksum//aml:property[@name='value']/aml:value/normalize-space()
      let $computedValue := if ($algorithm) then (
                            upper-case(local:beregn-sjekksum(concat($directory, $fileName), $algorithm)))
                            else ()
      return
      <resultItem name="file" type="{if (upper-case($value) eq $computedValue) then ('info') 
                                     else (
                                       if ($value and $computedValue) then ('error') 
                                       else (
                                         if (not($algorithm and $value) and ($algorithm or $value)) then ('error')
                                         else ('warning')))}">
        <resultItems>
          <resultItem name="fileName">
            <content>{$fileName}</content>
          </resultItem>
          <resultItem name="checksum">
            <resultItems>
              <resultItem name="algorithm">
                <content>{$algorithm}</content>
              </resultItem>
              <resultItem name="value">
                <content>{$value}</content>
              </resultItem>
              <resultItem name="computedValue">
                <content>{$computedValue}</content>
              </resultItem>
            </resultItems>
          </resultItem>
        </resultItems>
      </resultItem>
      }
    </resultItems>
  </result>
</activity>

