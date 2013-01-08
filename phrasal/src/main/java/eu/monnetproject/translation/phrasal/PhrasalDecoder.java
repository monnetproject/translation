/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************
 */
package eu.monnetproject.translation.phrasal;

import edu.stanford.nlp.mt.base.CombinedPhraseGenerator;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.IStrings;
import edu.stanford.nlp.mt.base.IdentityPhraseGenerator;
import edu.stanford.nlp.mt.base.RichTranslation;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;
import edu.stanford.nlp.mt.decoder.feat.CombinedFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.DTULinearDistortionFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.FeaturizerFactory;
import edu.stanford.nlp.mt.decoder.feat.IncrementalFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.IsolatedPhraseFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.LinearDistortionFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.LinearFutureCostFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.MSDFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.UnknownWordFeaturizer;
import edu.stanford.nlp.mt.decoder.h.HeuristicFactory;
import edu.stanford.nlp.mt.decoder.h.SearchHeuristic;
import edu.stanford.nlp.mt.decoder.inferer.AbstractBeamInfererBuilder;
import edu.stanford.nlp.mt.decoder.inferer.Inferer;
import edu.stanford.nlp.mt.decoder.inferer.InfererBuilderFactory;
import edu.stanford.nlp.mt.decoder.recomb.RecombinationFilter;
import edu.stanford.nlp.mt.decoder.recomb.RecombinationFilterFactory;
import edu.stanford.nlp.mt.decoder.util.ConstrainedOutputSpace;
import edu.stanford.nlp.mt.decoder.util.Hypothesis;
import edu.stanford.nlp.mt.decoder.util.HypothesisBeamFactory;
import edu.stanford.nlp.mt.decoder.util.PhraseGenerator;
import edu.stanford.nlp.mt.decoder.util.Scorer;
import edu.stanford.nlp.mt.decoder.util.ScorerFactory;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import eu.monnetproject.lang.Language;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.framework.services.Services;
import eu.monnetproject.translation.Chunk;
import eu.monnetproject.translation.ChunkList;
import eu.monnetproject.translation.Decoder;
import eu.monnetproject.translation.DecoderWeights;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.LanguageModel;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.Translation;
import eu.monnetproject.translation.TranslationPhraseChunker;
import eu.monnetproject.translation.TranslationPhraseChunkerFactory;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.TranslationSourceFactory;
import eu.monnetproject.translation.phrasal.lm.ARPALanguageModelFactory;
import eu.monnetproject.translation.phrasal.pt.MemoryMappedPhraseTableSourceFactory;
import eu.monnetproject.translation.phrasal.pt.PhraseTableImpl;
import eu.monnetproject.translation.phrasal.pt.WrappingPhraseTable;
import eu.monnetproject.translation.monitor.Messages;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author John McCrae
 */
public class PhrasalDecoder implements eu.monnetproject.translation.Decoder {

    public static final String DISTORTION_WT_OPT = "weight-d";
    public static final String LANGUAGE_MODEL_WT_OPT = "weight-l";
    public static final String TRANSLATION_MODEL_WT_OPT = "weight-t";
    public static final String WORD_PENALTY_WT_OPT = "weight-w";
    public static final String GAPS_OPT = "gaps";
    public static final String MAX_GAP_SPAN_OPT = "max-gap-span";
    public static final String MOSES_COMPATIBILITY_OPT = "moses-compatibility";
    public static final String DISTORTION_LIMIT = "distortion-limit";
    public static final String RECOMBINATION_HEURISTIC = "recombination-heuristic";
    public static final String USE_ITG_CONSTRAINTS = "use-itg-constraints";
    public static final String BEAM_SIZE = "stack";
    public static final String WEIGHTS_FILE = "weights-file";
    public static final String INLINE_WEIGHTS = "inline-weights";
    static final int DEFAULT_DISTORTION_LIMIT = 5;
    public static final String DEFAULT_RECOMBINATION_HEURISTIC = RecombinationFilterFactory.CLASSICAL_TRANSLATION_MODEL;
    private final boolean withGaps;
    private final boolean mosesMode;
    private final List<String> gapOpts;
    private final String recombinationHeuristic;
    private final Inferer<IString, String> inferer;
    private final int distortionLimit;
    private WrappingPhraseTable<String> phraseTableWrapper;
    public int beamCapacity = Inferer.DEFAULT_BEAM_SIZE;
    public int fastBeamCapacity = Integer.parseInt(System.getProperty("fast_stack", "10"));

    public PhrasalDecoder(LanguageModel model, DecoderWeights weights) throws IOException, ClassNotFoundException {
        Messages.info("Using weights " + weights.toString());
        Properties config = Configurator.getConfig("eu.monnetproject.translation.phrasal");

//        if(!config.containsKey(DISTORTION_WT_OPT) || !config.containsKey(LANGUAGE_MODEL_WT_OPT) || !config.containsKey(TRANSLATION_MODEL_WT_OPT) || !config.containsKey(WORD_PENALTY_WT_OPT)) {
//            throw new IllegalArgumentException("Weights are not configured");
//        }

        mosesMode = config.containsKey(MOSES_COMPATIBILITY_OPT);
        withGaps = config.containsKey(GAPS_OPT)
                || config.containsKey(MAX_GAP_SPAN_OPT);
        if (withGaps) {
            gapOpts = config.containsKey(MAX_GAP_SPAN_OPT) ? Arrays.asList(config.get(MAX_GAP_SPAN_OPT).toString().split("\\s+"))
                    : Arrays.asList(config.get(GAPS_OPT).toString().split("\\s+"));
        } else {
            gapOpts = null;
        }
        if (config.containsKey(RECOMBINATION_HEURISTIC)) {
            recombinationHeuristic = config.get(RECOMBINATION_HEURISTIC).toString();
        } else {
            recombinationHeuristic = DEFAULT_RECOMBINATION_HEURISTIC;
        }

        if (config.containsKey(DISTORTION_LIMIT)) {
            List<String> strDistortionLimit = Arrays.asList(config.get(DISTORTION_LIMIT).toString().split("\\s+"));
            if (strDistortionLimit.size() != 1) {
                throw new RuntimeException(String.format(
                        "Parameter '%s' takes one and only one argument", DISTORTION_LIMIT));
            }
            try {
                distortionLimit = Integer.parseInt(strDistortionLimit.get(0));
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        String.format(
                        "Argument '%s' to parameter '%s' can not be parsed as an integer value\n",
                        strDistortionLimit.get(0), DISTORTION_LIMIT));
            }
        } else {
            distortionLimit = DEFAULT_DISTORTION_LIMIT;
        }


        final CombinedFeaturizer<IString, String> featurizer = getFeaturizer(model);
        final Scorer<String> scorer = getScorer(getWeightConfig(weights));
        final PhraseGenerator<IString> phraseGenerator = getPhraseGenerator(featurizer, scorer);
        final SearchHeuristic<IString, String> heuristic = getHeuristic(featurizer, scorer);
        final RecombinationFilter<Hypothesis<IString, String>> filter = getFilter(featurizer);
        FeaturizerFactory.GapType gapT = !withGaps ? FeaturizerFactory.GapType.none
                : ((gapOpts.size() > 1) ? FeaturizerFactory.GapType.both
                : FeaturizerFactory.GapType.source);
        boolean dtuDecoder = (gapT != FeaturizerFactory.GapType.none);
        // Configure InfererBuilder
        AbstractBeamInfererBuilder<IString, String> infererBuilder = (AbstractBeamInfererBuilder<IString, String>) InfererBuilderFactory.factory(dtuDecoder ? InfererBuilderFactory.DTU_DECODER
                : InfererBuilderFactory.MULTIBEAM_DECODER);
        infererBuilder.setIncrementalFeaturizer((CombinedFeaturizer<IString, String>) featurizer);
        infererBuilder.setPhraseGenerator((PhraseGenerator<IString>) phraseGenerator);
        infererBuilder.setScorer(scorer);
        infererBuilder.setSearchHeuristic((SearchHeuristic<IString, String>) heuristic);
        infererBuilder.setRecombinationFilter((RecombinationFilter<Hypothesis<IString, String>>) filter);

        infererBuilder.setBeamType(HypothesisBeamFactory.BeamType.sloppybeam);

        if (distortionLimit != -1) {
            infererBuilder.setMaxDistortion(distortionLimit);
        }

        if (config.containsKey(USE_ITG_CONSTRAINTS)) {
            infererBuilder.useITGConstraints(Boolean.parseBoolean(config.get(
                    USE_ITG_CONSTRAINTS).toString()));
        }

        if (config.containsKey(BEAM_SIZE)) {
            try {
                beamCapacity = Integer.parseInt(config.get(BEAM_SIZE).toString());
                System.err.println("Beam size: " + beamCapacity);
                infererBuilder.setBeamCapacity(beamCapacity);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        String.format(
                        "Beam size %s, as specified by argument %s, can not be parsed as an integer value\n",
                        config.get(BEAM_SIZE).toString(), BEAM_SIZE));
            }

        }

        inferer = infererBuilder.build();
    }

    private static String makePair(String label, String value) {
        return String.format("%s:%s", label, value);
    }

    private CombinedFeaturizer<IString, String> getFeaturizer(LanguageModel model) throws IOException {
        CombinedFeaturizer<IString, String> featurizer;

        String linearDistortion = withGaps ? DTULinearDistortionFeaturizer.class.getName() : (mosesMode ? LinearDistortionFeaturizer.class.getName()
                : LinearFutureCostFeaturizer.class.getName());

        FeaturizerFactory.GapType gapT = !withGaps ? FeaturizerFactory.GapType.none
                : ((gapOpts.size() > 1) ? FeaturizerFactory.GapType.both
                : FeaturizerFactory.GapType.source);
        String gapType = gapT.name();
        //System.err.println("Gap type: " + gapType);

        featurizer = FeaturizerFactory2.factory(model,
                FeaturizerFactory.PSEUDO_PHARAOH_GENERATOR,
                makePair(FeaturizerFactory.LINEAR_DISTORTION_PARAMETER,
                linearDistortion),
                makePair(FeaturizerFactory.GAP_PARAMETER, gapType),
                makePair(FeaturizerFactory.DISCRIMINATIVE_LM_PARAMETER, "0"),
                makePair(FeaturizerFactory.DISCRIMINATIVE_TM_PARAMETER, "false"));

        List<IncrementalFeaturizer<IString, String>> additionalFeaturizers = new LinkedList<IncrementalFeaturizer<IString, String>>();

        //if (lexReorderFeaturizer != null) {
        //    additionalFeaturizers.add(lexReorderFeaturizer);
        //}

        if (!additionalFeaturizers.isEmpty()) {
            List<IncrementalFeaturizer<IString, String>> allFeaturizers = new ArrayList<IncrementalFeaturizer<IString, String>>();
            allFeaturizers.addAll(featurizer.featurizers);
            allFeaturizers.addAll(additionalFeaturizers);
            featurizer = new CombinedFeaturizer<IString, String>(allFeaturizers);
        }

        return featurizer;

    }

    private PhraseGenerator<IString> getPhraseGenerator(IsolatedPhraseFeaturizer<IString, String> phraseFeaturizer,
            Scorer<String> scorer) throws IOException {

        List<PhraseGenerator<IString>> pharoahList = new LinkedList<PhraseGenerator<IString>>();
        List<PhraseGenerator<IString>> finalList = new LinkedList<PhraseGenerator<IString>>();
        phraseTableWrapper = new WrappingPhraseTable<String>(phraseFeaturizer, scorer);

        pharoahList.add(phraseTableWrapper);

        finalList.add(new CombinedPhraseGenerator<IString>(pharoahList,
                CombinedPhraseGenerator.Type.CONCATENATIVE));

        finalList.add(new IdentityPhraseGenerator<IString, String>(phraseFeaturizer,
                scorer, UnknownWordFeaturizer.UNKNOWN_PHRASE_TAG));

        CombinedPhraseGenerator.Type combinationType = CombinedPhraseGenerator.Type.STRICT_DOMINANCE;

        return new CombinedPhraseGenerator<IString>(finalList, combinationType);
    }

    private Counter<String> getWeightConfig(DecoderWeights weights) {
        final ClassicCounter<String> counter = new ClassicCounter<String>();
        for (Map.Entry<String, Double> weight : weights.entrySet()) {
            counter.setCount(weight.getKey(), weight.getValue());
        }
        return counter;
    }

    /*private Counter<String> getWeightConfig(Properties config) throws IOException, ClassNotFoundException {
     Counter<String> weightConfig = new ClassicCounter<String>();

     if (config.containsKey(WEIGHTS_FILE)) {
     if (config.get(WEIGHTS_FILE).toString().endsWith(".binwts")) {
     ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
     config.get(WEIGHTS_FILE).toString()));
     weightConfig = (Counter<String>) ois.readObject();
     ois.close();
     } else {

     BufferedReader reader = new BufferedReader(new FileReader(config.get(
     WEIGHTS_FILE).toString()));
     for (String line; (line = reader.readLine()) != null;) {
     String[] fields = line.split("\\s+");
     weightConfig.incrementCount(fields[0], Double.parseDouble(fields[1]));
     }
     reader.close();
     }
     } else {
     if (config.containsKey(INLINE_WEIGHTS)) {
     List<String> inlineWts = Arrays.asList(config.get(TRANSLATION_MODEL_WT_OPT).toString().split("\\s+"));
     for (String inlineWt : inlineWts) {
     String[] fields = inlineWt.split("=");
     weightConfig.setCount(fields[0], Double.parseDouble(fields[1]));
     }
     }
     weightConfig.setCount(NGramLanguageModelFeaturizer.FEATURE_NAME,
     Double.parseDouble(config.get(LANGUAGE_MODEL_WT_OPT).toString()));
     weightConfig.setCount(LinearDistortionFeaturizer.FEATURE_NAME,
     Double.parseDouble(config.get(DISTORTION_WT_OPT).toString()));
     final List<String> distortionWtOpt = Arrays.asList(config.get(DISTORTION_WT_OPT).toString().split("\\s+"));
     if (distortionWtOpt.size() > 1) {
     int numAdditionalWts = distortionWtOpt.size() - 1;
     //if (lexReorderFeaturizer == null) {
     throw new RuntimeException(
     String.format(
     "Additional weights given for parameter %s but no lexical reordering file was specified",
     DISTORTION_WT_OPT));
     //                }
     //                if (lexReorderFeaturizer instanceof LexicalReorderingFeaturizer) {
     //                    LexicalReorderingFeaturizer mosesLexReorderFeaturizer = (LexicalReorderingFeaturizer) lexReorderFeaturizer;
     //                    if (numAdditionalWts != mosesLexReorderFeaturizer.mlrt.positionalMapping.length) {
     //                        throw new RuntimeException(
     //                                String.format(
     //                                "%d re-ordering weights given with parameter %s, but %d expected",
     //                                numAdditionalWts, DISTORTION_WT_OPT,
     //                                mosesLexReorderFeaturizer.mlrt.positionalMapping.length));
     //                    }
     //                    for (int i = 0; i < mosesLexReorderFeaturizer.mlrt.positionalMapping.length; i++) {
     //                        weightConfig.setCount(mosesLexReorderFeaturizer.featureTags[i],
     //                                Double.parseDouble(config.get(DISTORTION_WT_OPT).get(i + 1)));
     //                    }
     //                }
     }
     weightConfig.setCount(WordPenaltyFeaturizer.FEATURE_NAME,
     Double.parseDouble(config.get(WORD_PENALTY_WT_OPT).toString()));
     weightConfig.setCount(UnknownWordFeaturizer.FEATURE_NAME, 1.0);
     weightConfig.setCount(SentenceBoundaryFeaturizer.FEATURE_NAME, 1.0);

     List<String> tmodelWtsStr = Arrays.asList(config.get(TRANSLATION_MODEL_WT_OPT).toString().split("\\s+"));
     if (tmodelWtsStr.size() == 5) {
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.FIVESCORE_PHI_t_f), Double.parseDouble(tmodelWtsStr.get(0)));
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.FIVESCORE_LEX_t_f), Double.parseDouble(tmodelWtsStr.get(1)));
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.FIVESCORE_PHI_f_t), Double.parseDouble(tmodelWtsStr.get(2)));
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.FIVESCORE_LEX_f_t), Double.parseDouble(tmodelWtsStr.get(3)));
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.FIVESCORE_PHRASE_PENALTY), Double.parseDouble(tmodelWtsStr.get(4)));
     } else if (tmodelWtsStr.size() == 1) {
     weightConfig.setCount(
     makePair(PhraseTableScoresFeaturizer.PREFIX,
     FlatPhraseTable.ONESCORE_P_t_f), Double.parseDouble(tmodelWtsStr.get(0)));
     } else {
     throw new RuntimeException(String.format(
     "Unsupported weight count for translation model: %d",
     tmodelWtsStr.size()));
     }
     }
     return weightConfig;
     }*/
    private Scorer<String> getScorer(Counter<String> weightConfig) throws IOException {
        return ScorerFactory.factory(ScorerFactory.STATIC_SCORER, weightConfig);
    }

    private SearchHeuristic<IString, String> getHeuristic(IsolatedPhraseFeaturizer<IString, String> isolatedPhraseFeaturizer, Scorer<String> scorer) {
        return HeuristicFactory.factory(
                isolatedPhraseFeaturizer, scorer,
                withGaps ? HeuristicFactory.ISOLATED_DTU_FOREIGN_COVERAGE
                : HeuristicFactory.ISOLATED_PHRASE_FOREIGN_COVERAGE);
    }

    private RecombinationFilter<Hypothesis<IString, String>> getFilter(CombinedFeaturizer<IString, String> featurizer) {
        return RecombinationFilterFactory.factory(featurizer.getNestedFeaturizers(), featurizer instanceof MSDFeaturizer,
                recombinationHeuristic);
    }

    @Override
    public List<Translation> decode(List<String> phrase, PhraseTable phraseTable, List<String> featureNames, int nBest) {
        return decode(phrase, phraseTable, featureNames, nBest, beamCapacity);
    }

    @Override
    public List<Translation> decodeFast(List<String> phrase, PhraseTable phraseTable, List<String> featureNames, int nBest) {
        return decode(phrase, phraseTable, featureNames, nBest, 10);
    }

    private List<Translation> decode(List<String> phrase, PhraseTable phraseTable, List<String> featureNames, int nBest, int beamCapacity) {
        phraseTableWrapper.setPhraseTable(phraseTable,featureNames);
        Sequence<IString> foreign = new SimpleSequence<IString>(true,
                IStrings.toSyncIStringArray(phrase.toArray(new String[phrase.size()])));


        // do translation

        ConstrainedOutputSpace<IString, String> constrainedOutputSpace = null;

        int procid = 0;
        int lineNumber = 1;

        try {
        if (nBest == -1) {
            RichTranslation<IString, String> translation;
            translation = inferer.translate(
                    foreign,
                    lineNumber - 1,
                    constrainedOutputSpace,
                    (constrainedOutputSpace == null ? null : constrainedOutputSpace.getAllowableSequences()), beamCapacity);
            return Collections.singletonList(convert(translation, phraseTable.getForeignLanguage(), phraseTable.getTranslationLanguage()));
        } else {
            List<RichTranslation<IString, String>> translations = inferer.nbest(
                    foreign,
                    lineNumber - 1,
                    constrainedOutputSpace,
                    (constrainedOutputSpace == null ? null : constrainedOutputSpace.getAllowableSequences()), beamCapacity, nBest);
            List<Translation> monnetTrans = new ArrayList<Translation>(translations.size());
            for (RichTranslation<IString, String> richTranslation : translations) {
                monnetTrans.add(convert(richTranslation, phraseTable.getForeignLanguage(), phraseTable.getTranslationLanguage()));
            }
            return monnetTrans;
        }
        } catch(Exception x) {
        	Messages.translationFail(null,x);
        	return new ArrayList<Translation>(0);
        }
    }

    private Translation convert(RichTranslation<IString, String> stanfordTrans, Language srcLang, Language trgLang) {
        return new TranslationImpl(stanfordTrans, srcLang, trgLang);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage:\n\tPhrasalDecoder srcLang trgLang nBest");
            System.exit(-1);
        }
        Language srcLang = Language.get(args[0]);
        Language trgLang = Language.get(args[1]);
        int nBest = Integer.parseInt(args[2]);
        final ARPALanguageModelFactory lmf = new ARPALanguageModelFactory();
        final LanguageModel lm = lmf.getModel(trgLang);
        final PhrasalDecoderFactory pdf = new PhrasalDecoderFactory();
        final DecoderWeights weights = pdf.getDefaultWeights();
        final Decoder decoder = pdf.getDecoder(lm, weights);
        final BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
        final TranslationSourceFactory ptf = new MemoryMappedPhraseTableSourceFactory();
        final TranslationSource source = ptf.getSource(srcLang, trgLang);
        final TranslationPhraseChunker chunker = Services.get(TranslationPhraseChunkerFactory.class).getPhraseChunker(null, srcLang);
        String s;
        System.err.println("Ready:");
        while ((s = inReader.readLine()) != null) {
            if (s.matches("\\s*")) {
                continue;
            }
            final ChunkList chunks = chunker.chunk(new StringLabel(s, srcLang));
            PhraseTableImpl pt = new PhraseTableImpl(srcLang, trgLang, "std-table");
            for (Chunk chunk : chunks) {
                pt.addAll(source.candidates(chunk));
            }
            for (PhraseTableEntry pte : pt) {
                System.err.println(pte);
            }
            final List<Translation> translations = decoder.decode(Arrays.asList(FairlyGoodTokenizer.split(s)), pt, Arrays.asList(source.featureNames()),nBest);
            for (Translation translation : translations) {
                System.out.println(translation.getTargetLabel().asString());
                System.err.println("Score: " + translation.getScore());
                for (Feature feature : translation.getFeatures()) {
                    System.err.print(feature.name + " = " + feature.score + " ");
                }
                System.err.println();
            }
        }
    }
}
