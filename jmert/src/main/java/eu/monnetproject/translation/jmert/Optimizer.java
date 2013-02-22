package eu.monnetproject.translation.jmert;

import eu.monnetproject.translation.Feature;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Optimizer {

	double[] optimizeFeatures(
			List<Collection<JMertTranslation>> nBests, Feature[] initWeights,
			int nIters, Set<String> unused);

}