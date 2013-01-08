package eu.monnetproject.translation.phrasal.jmert;

import java.util.Arrays;

import eu.monnetproject.translation.Feature;

public class JMertTranslation {

    public final Feature[] features;
    public final double score;
    
    // A temporary variable (bad practice but for efficiency it is here)
    public double offset;

    public JMertTranslation(Feature[] features, double score) {
        this.features = features;
        this.score = score;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JMertTranslation other = (JMertTranslation) obj;
        if (!Arrays.deepEquals(this.features, other.features)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Arrays.deepHashCode(this.features);
        hash = 41 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return "JMertTranslation{" + "features=" + Arrays.toString(features) + ", score=" + score + '}';
    }
    
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < features.length; i++) {
            sb.append(features[i].score).append(",");
        }
        sb.append(score);
        return sb.toString();
    }
    
}
