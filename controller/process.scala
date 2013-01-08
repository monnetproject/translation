import scala.xml._

val xml = XML.loadFile("results/forEval_es_en_2012-08-02_15.46.xml")

val srcs = for(src <- xml \\ "src") yield { src.text }
val trgs = for(trg <- xml \\ "trg") yield { trg.text }

println("Source\tTarget\tAdequacy\tFluency")

for((src,trg) <- (srcs zip trgs)) {
  println(src + "\t" + trg + "\t\t")
}
