import java.io._
import scala.xml._

val enFile = System.getProperty("enFile")

if(enFile == null) {
  throw new RuntimeException("Usage:\n\t scala wmt2onto.scala -DenFile=file.en.sgm -Dlangs=\"en,es,de\"")
}

val languages = System.getProperty("langs","").split(",")

val enXml = XML.loadFile(enFile)

val otherXMLs = for(lang <- languages if lang != "") yield {
  val otherFile = enFile.replaceAll("\\.en\\.sgm$","."+lang+".sgm")
  (lang,XML.loadFile(otherFile))
}

new File("ontologies").mkdir()

for(doc <- (enXml \ "doc")) {
   val docid = (doc \ "@docid").text
   val fileName = if(docid contains "/") {
     docid.substring(docid.lastIndexOf("/")) + ".rdf"
   } else {
     docid + ".rdf"
   }
   val out = new PrintWriter("ontologies/"+fileName)

   out.println("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\">")
   

   val otherDocs = for((lang,otherXML) <- otherXMLs) yield {
     (for(otherDoc <- (otherXML \ "doc") if (otherDoc \ "@docid").text == docid) yield {
       (lang,otherDoc)
     }).head
   }

   for(seg <- (doc \\ "seg")) {
       out.println("  <owl:NamedIndividual rdf:about=\"file:" + docid + ".rdf#"+(seg \ "@id").text + "\">")
       out.println("    <rdfs:label xml:lang=\"en\">"+Utility.escape(seg.text)+"</rdfs:label>")
       println(fileName + ": " + seg.text)
       for((lang,otherDoc) <- otherDocs ; otherSeg <- (otherDoc \\ "seg") if (otherSeg \ "@id").text == (seg \ "@id").text) {
         out.println("    <rdfs:label xml:lang=\""+lang+"\">" + Utility.escape(otherSeg.text)+"</rdfs:label>")
       }
       out.println("  </owl:NamedIndividual>")
       out.println()
   }
   out.println("</rdf:RDF>")
   out.close()
}

