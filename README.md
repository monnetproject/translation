Monnet Translate
================

Monnet translation system for translation of ontologies based on statistical machine translation and ontological semantics.
The system is composed of the following modules

 * `core`: Interfaces used between components
 * `controller`: Central controller for translations
 * `chunker`: Input analysis and tokenization
 * `decomposer`: Compound noun analysis for German and Dutch
 * `sqlpt`: Phrase table extraction from SQL databases
 * `sources`: Additional candidate extraction from lexico-semantic resources
 * `feat`: Featurizers for semantic analyzers
 * `opm`: Observed phrase model for partial translation
 * `phrasal`: Decoder forked from [Stanford Phrasal](http://nlp.stanford.edu/phrasal/)
 * `phrasal.test`: Integration tests of Phrasal
 * `langmodel`: Language models
 * `fidel`: Alternative decoder 
 * `evalmetrics`: Evaluation metrics
 * `quality`: Quality estimation
 * `jmert`: Ranking SVM parameter tuning
 * `evaluation`: Command-line evaluation tools
 * `controller.web`: REST web service for translation


Configuring the system
----------------------

Standard configurations can be found [here](example-model/)


### `com.mysql.jdbc.cfg`

This is the file that specifies the details of the MySQL database the format is as follows:

    username=monnet
    password=???
    database=acquis
    server=localhost

### `eu.monnetproject.translation.langmodel.cfg`

This file contains the location of the language models to be used (in ARPA format). The files are of the form `lang=file`, e.g.,

    es=/home/shared/es/lm
    de=/home/shared/de/lm
    en=/home/shared/en/lm
    nl=/home/shared/nl/lm

### `eu.monnetproject.translation.wts.$src-$trg.cfg`

This file contains the weights on the log-linear model. The file is of the form `featureName=value`, note `:` must be back-slashed in feature names! There must be a weights file for each translation direction and features not in the weights file will not be used

    LinearDistortion=0.12499999999999997
    TM\:phi(t|f)=0.12499999999999997
    TM\:lex(t|f)=0.12499999999999997
    TM\:phi(f|t)=0.12499999999999997
    TM\:lex(f|t)=0.12499999999999997
    TM\:phrasePenalty=0.12499999999999997
    LM=0.12499999999999997
    WordPenalty=0.12499999999999997
    UnknownWord=0.0
    SentenceBoundary=0.0

### `eu.monnetproject.seals.cfg` (Optional)

To enable results to be stored with SEALS specify the name of the SEALS repository as follows:

    resourceLocator=http://pinto.dia.fi.upm.es:8080/rrs-web/

Running the translation system
------------------------------

The standard evaluation script is [e.m.t.evaluation.Evaluate](evaluation/src/main/java/eu/monnetproject/translation/evaluation/Evaluate.java). It can be invoked using the following command (see Maven FAQ), for the `eu.monnetproject.translation.evaluation` project folder.

    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.evaluation.Evaluate" -Dexec.args="folder srcLang trgLang"

Where

 * folder: Is the folder containing all ontologies to be translated
 * srcLang: Is the source language (as ISO code, e.g., "en")
 * trgLang: Is the target language (as ISO code, e.g., "es")

If run like this all results will be printed to STDOUT

To save the results the parameter -runName must be specified, e.g.,

    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.evaluation.Evaluate" -Dexec.args="-runName example MultilingualOntologies/ en es"

The result will be written as

* `results/runName_srcLang_trgLang_YYYY-MM-DD_hh:mm.xml`

Where YYYY-MM-DD_hh:mm is the time of execution e.g.,

* `results/example_en_es_2012-01-17_14:01.xml`

Example output is


    <results time="2012-01-16T16:31:27" sourceLanguage="en" targetLanguage="es" executionTime="1049934">
      <ontology uri="http://trajano.us.es/~isabel/EHR/Demographic_RM.owl" labelCount="22">
        <result metric="BLEU" score="0.0"/>
        <result metric="BLEU-2" score="0.0"/>
        <result metric="METEOR" score="0.0"/>
        <result metric="NIST" score="0.0"/>
        <result metric="PER" score="-1.0"/>
        <result metric="TER" score="-1.0"/>
        <result metric="WER" score="-1.0"/>
      </ontology>
      <ontology uri="http://xbrl.iasb.org/taxonomy/2009-04-01/ifrs" labelCount="37">
        <result metric="BLEU" score="0.0"/>
        <result metric="BLEU-2" score="0.05431183395717798"/>
        <result metric="METEOR" score="0.051048875835972445"/>
        <result metric="NIST" score="0.7366279797295097"/>
        <result metric="PER" score="-0.854251012145749"/>
        <result metric="TER" score="-0.9291497975708503"/>
        <result metric="WER" score="-0.9311740890688259"/>
      </ontology>
      ...
      <summary labelCount="311">
        <result metric="BLEU" score="0.0"/>
        <result metric="BLEU-2" score="0.01721014745117195"/>
        <result metric="METEOR" score="0.03802367416584292"/>
        <result metric="NIST" score="0.5087312368265963"/>
        <result metric="PER" score="-0.9775997190472975"/>
        <result metric="TER" score="-0.9961771043049569"/>
        <result metric="WER" score="-1.0054506062045343"/>
      </summary>
      ...
    </results>

### Tuning

Weights may be learnt as follows:

    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.controller.evaluate.Tune" \
       -Dexec.args="MultilingualOntologies/ en es nist 5 load/eu.monnetproject.translation.wts.en-es.cfg" \
       -Djmert.method=SVM

This optimizes NIST using 5 iterations:

Where `jmert.method` is one of

* `DUMMY`: Does nothing
* `MERT`: Och-Ney linear search
* `OLS`: Linear regression
* `SVM`: Ranking SVM (default)

### Cross-folded evaluation

You may also run cross-folded evaluation, which divides the ontologies into _n_ even sets, and then uses _n-1_ of these sets to train and tests on the held out test. The command is as follows


    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.evaluation.EvaluateNFolds" \
         -Dexec.args="-runName example MultilingualOntologies/ en es 10 5 nist"

This runs using 10 folds and tuning using 5 iterations on the NIST metric

### Single-ontology translation

For debugging the translation can read a single ontology as follows

    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.controller.Translate" \
         -Dexec.args="ontology.rdf en translation.rdf"

### Interactive mode

For debugging, the decoder may be used directly by reading from `STDIN`

    mvn exec:java -Dexec.mainClass="eu.monnetproject.translation.controller.RTPL" \
      -Dexec.args="en es 1"

1 is number of candidates to print.

Web interface
-------------

The web interface is compiled at `eu.monnetproject.translation.controller.web`, and can be deployed by uploading the WAR file to Tomcat or Jetty.


A REST API is provided with support for the following formats

 * SKOS
 * SKOS-XL
 * RDFS
 * Lemon

All can be provided in both RDF-XML as well as turtle format.

Input format is determined by the HTTP Content-Type header value.

The translation service accepts the following parameters which must be offered as a POST multipart request:

 * `ontology` (Required)
     - The source file containing the source ontology and the lexica 
 * `target-language` (Required)
     - IETF code, eg. "nl"
 * `scope`
     - Comma separated list of URI's (by default all URI's in the ontology will be translated)
 * `name-prefix`
     - The name prefix for URI's of new entities created by SKOS-XL or Lemon (defaults to blank nodes)
 * `custom-label`
     - Comma separated list of URI's for custom label properties
 * `n-best`
     - The maximum amount of candidates to return for a single label (defaults to 1)
 * `accept-vocabularies`
     - Comma separated list of URI's for the target vocabularies, e.g. Lemon is http://www.monnet-project.eu/lemon# (defaults to input vocabulary)
 * `include-source`
     - Include the source ontology and lexicon in the result. If true all existing candidates that were not created by this translation services will be untouched. (defaults to `true`)
 * `esimate-confidence`
     - Provide estimates of confidence of translation (Lemon Metadata only) (defaults to `false`)
 * `speed`
     - Optimize for speed not quality of translations, one of {`fast`,`normal`} (defaults to `fast`)


Implementation specific features:
- Automatically infer the language of the ontology to be translated

Error conditions:
- The ontology contains labels without language tags
- The ontology contains no labels

The translation service will provide a maximum of n candidates for each label in the source language. The translation service can use translation candidates that are already present in the source lexicon but only those that are marked as accepted. Candidates with a draft status should be ignored.

Metadata included per translation candidate

 * `confidence`
     - Confidence level provided by the translator, a value between 0 and 1, using property http://monnet01.sindice.net/ontologies/translation.owl#confidence
 * `logprob`
     - Logaritmic probability of the translation
 * `translation-service-identifier`
     - Identifier of the translation service, e.g. Monnet
 * `source`
     - Source of the translation (used by the translation service)
 * `status`
     - Either accepted, for-review, rejected. If no status is available it's presumed to be accepted. (Not sure if we really return this?)

### Testing with CURL

    curl -F "ontology=@foaf.rdf" -F "target-language=de" http://monnet01.sindice.net:8080/translate/

### Using from Java


    HttpClient httpClient = new HttpClient();
    PostMethod method = new PostMethod(config.getProperty("http://monnet01.sindice.net:8888/translate"));
    HttpClientParams params = new HttpClientParams();
    int timeout = 60000;
    String ontologyAsString = "<your ontology goes in here>";
    params.setSoTimeout(timeout);
      Part[] parts = {
        new StringPart("ontology", ontologyAsString),
        new StringPart("n-best", "5"),
        new StringPart("target-language", "nl"),
        new StringPart("estimate-confidence","false")
    };
    method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
    httpClient.setParams(params);
    httpClient.executeMethod(method);
    String result = method.getResponseBodyAsString();


