How to do a bootstrap resampling significance evaluation
========================================================

Take your ontology and apply the split script

    mvn exec:java -Dexec.mainClass=eu.monnetproject.translation.bootstrapeval.SplitOntology \
       -Dexec.args="~/ontologies/foaf-with-es.ttl en es"

This will create two monolingual ontologies `foaf-with-es.ttl.en` and `foaf-with-es.ttl.en`

Translate the ontologies using _two different_ settings

   mvn exec:java -f ../controller/pom.xml                               \
     -Dexec.mainClass=eu.monnetproject.translation.controller.Translate \
     -Dexec.args="~/ontologies/foaf-with-es.ttl.es en lexicon1.en

Now we should have two lexica, for example `lexicon1.en` and `lexicon2.en`

We can do a significance evaluation as follows

   mvn exec:java -Dexec.mainClass=eu.monnetproject.translation.bootstrapeval.SignificanceTest \
     -Dexec.args="~/ontologies/foaf-with-es.ttl.en lexicon1.en lexicon2.en es en"

And we get the result as follows:

                                   BLEU	  BLEU-2 METEOR	NIST	PER	TER	WER	
    lexicon1.en                    0.5000 0.5400 0.5200	0.4600	0.4800	0.0800	0.0000	
    lexicon2.en                    0.5000 0.4600 0.4800	0.5400	0.4200	0.1800	0.0000	


