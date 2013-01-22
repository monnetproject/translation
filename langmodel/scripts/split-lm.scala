import java.io._
import scala.io._

val src = Source.fromFile(args(0)).getLines.map(_.trim)

def fail(msg : String) {
  System.err.println(msg)
  System.exit(-1)
}

src.find(_ == "\\data\\").getOrElse(fail("No header"))

var order = 0
while(src.next.matches("ngram \\d+=.*")) {
  order = order + 1
}

println("order="+order)

var ngCt = new Array[Int](order)

for(n <- 1 to order) {
  src.find(_ == ("\\" + n + "-grams:")).getOrElse(fail("No n-grams at order " + n))
  var line = src.next
  val out = new PrintWriter("lm_tmp."+n)
  var read = 0
  while(!line.matches("\\s*")) {
    val e = line.split("\t")
    if(e.length < 2) {
      fail("Bad line:" + line)
    }
    out.println((e(1) :: e(0) :: e.drop(2).toList).mkString("\t"))
    line = src.next
    read = read + 1
  }
  out.flush
  out.close
  println("Sorting")
  Runtime.getRuntime().exec(Array("sort","lm_tmp."+n,"-o","lm_tmp.sort."+n),Array("LC_ALL=C")).waitFor()
  println("Read " + read + " " + n + "-grams")
  ngCt(n-1) = read
}

src.find(_ == "\\end\\").getOrElse(fail("No end"))

val out = new PrintWriter(args(0)+".sorted")

out.println()
out.println("\\data\\")
for(n <- 1 to order) {
  out.println("ngram " + n + "=" + ngCt(n-1))
}
out.println()

for(n <- 1 to order) {
  out.println("\\"+n+"-grams:")
  val in = Source.fromFile("lm_tmp.sort."+n).getLines.map(_.trim)
  for(line <- in) {
    val e = line.split("\t")
    if(e.length < 2) {
      fail("Bad line:" + line)
    }
    out.println((e(1) :: e(0) :: e.drop(2).toList).mkString("\t"))
  }
  println("Wrote " + ngCt(n-1) + " n-grams")
  out.println()
}
out.println("\\end\\")
out.flush
out.close

for(n <- 1 to order) {
  new File("lm_tmp."+n).delete()
  new File("lm_tmp.sort."+n).delete()
}
    

