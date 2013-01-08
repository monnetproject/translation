import java.io._

val user = "me"

val refs = new BufferedReader(new FileReader("refs-ontoeval"))
val scores = new BufferedReader(new FileReader(user +"-ontoeval.csv"))

val out = new PrintWriter(user+"-ontoeval.en.csv")

var sr = ""
var ss = ""

def scoreSim(x : String)(y : String) : Double = -1.0 * (x.toSet & y.toSet).size.toDouble / (x.toSet.size + y.toSet.size)

while({ sr = refs.readLine() ; ss = scores.readLine() ; sr} != null) {
   val Array(src,trg,flue,adeq) = ss.split(",")
   val ref = sr.split(" \\|\\|\\| ").sortBy(scoreSim(trg)).head
   out.println(trg + " ||| " + ref + " ||| " + flue + " ||| " + adeq)
}

out.flush
out.close
