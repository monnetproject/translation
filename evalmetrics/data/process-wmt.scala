// You have to remove DTD imports from all XML files :(
// Here is a one-liner to do it:
// find . -name \*.xml -exec perl -pi -e "s/\<\!DOCTYPE mteval SYSTEM \"ftp:\/\/jaguar.ncsl.nist.gov\/mt\/resources\/mteval-xml-v1.5.dtd\">//" {} \;
import java.io._
import scala.xml._

val name2lang = Map("Spanish" -> "es", "English" -> "en", "German" -> "de", "French" -> "fr", "Czech" -> "cz")

val out = Map("Spanish" -> new PrintWriter("wmt10-manualeval.es.csv"),
       "English" -> new PrintWriter("wmt10-manualeval.en.csv"),
       "German" -> new PrintWriter("wmt10-manualeval.de.csv"),
       "French" -> new PrintWriter("wmt10-manualeval.fr.csv"),
       "Czech" -> new PrintWriter("wmt10-manualeval.cz.csv"))

val dataRNK = new BufferedReader(new FileReader("data_RNK.csv"))

dataRNK.readLine() // discard header

var s = ""

def getRefFile(srcLang : String, trgLang : String) : Elem = XML.loadFile("wmt10-data/xml/ref/newssyscombtest2010."+name2lang(srcLang)+"-"+name2lang(trgLang)+".ref.xml")

def getSysFile(srcLang : String, trgLang : String, systemId : String) : Elem = XML.loadFile("wmt10-data/xml/tst-primary/"+name2lang(srcLang)+"-"+name2lang(trgLang)+"/newssyscombtest2010."+name2lang(srcLang)+"-"+name2lang(trgLang)+"."+systemId+".xml")

while({s = dataRNK.readLine();s } != null) {
  val Array(srcLang,trgLang,srcIndex,documentId,segmentId,judgeId,
            system1Number,system1Id,system2Number,system2Id,system3Number,system3Id,system4Number,
            system4Id,system5Number,system5Id,system1rank,system2rank,system3rank,system4rank,system5rank) = s.split(",")
  val refFile = getRefFile(srcLang,trgLang)
  val refString = (for(doc <- refFile \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption

  val sys1File = getSysFile(srcLang,trgLang,system1Id)
  val sys1String = (for(doc <- sys1File \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption
  
  if(refString != None && sys1String != None) {
    out(trgLang).println(sys1String.get.trim() + " ||| " + refString.get.trim() + " ||| " + system1rank)
  } else {
    System.err.println("XML extract failed")
  }

  val sys2File = getSysFile(srcLang,trgLang,system2Id)
  val sys2String = (for(doc <- sys2File \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption
  
  if(refString != None && sys2String != None) {
    out(trgLang).println(sys2String.get.trim() + " ||| " + refString.get.trim() + " ||| " + system2rank)
  } else {
    System.err.println("XML extract failed")
  }

  val sys3File = getSysFile(srcLang,trgLang,system3Id)
  val sys3String = (for(doc <- sys3File \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption
  
  if(refString != None && sys3String != None) {
    out(trgLang).println(sys3String.get.trim() + " ||| " + refString.get.trim() + " ||| " + system3rank)
  } else {
    System.err.println("XML extract failed")
  }

  val sys4File = getSysFile(srcLang,trgLang,system4Id)
  val sys4String = (for(doc <- sys4File \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption
  
  if(refString != None && sys4String != None) {
    out(trgLang).println(sys4String.get.trim() + " ||| " + refString.get.trim() + " ||| " + system4rank)
  } else {
    System.err.println("XML extract failed")
  }

  val sys5File = getSysFile(srcLang,trgLang,system5Id)
  val sys5String = (for(doc <- sys5File \\ "doc" ; if (doc \ "@docid").text == documentId ;
                       seg <- doc \\ "seg" ; if (seg \ "@id").text == segmentId) yield {
        seg.text
     }).headOption
  
  if(refString != None && sys5String != None) {
    out(trgLang).println(sys5String.get.trim() + " ||| " + refString.get.trim() + " ||| " + system5rank)
  } else {
    System.err.println("XML extract failed")
  }
}

for(pw <- out.values) {
   pw.close
}


