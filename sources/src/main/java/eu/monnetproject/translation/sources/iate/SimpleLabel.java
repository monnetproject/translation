package eu.monnetproject.translation.sources.iate;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.Tokenizer;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class SimpleLabel implements Label {
    private final String value;
    private final Language language;

    public SimpleLabel(String value, Language language, Tokenizer tokenizer) {
        final List<String> tokens = tokenizer.tokenize(value);
        final StringBuilder sb = new StringBuilder();
        for(String tk : tokens) {
            sb.append(tk).append(" ");
        }
        this.value = sb.toString().trim();
        this.language = language;
    }
    
    @Override
    public String asString() {
        return value;
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleLabel other = (SimpleLabel) obj;
        if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
            return false;
        }
        if (this.language != other.language && (this.language == null || !this.language.equals(other.language))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.value != null ? this.value.hashCode() : 0);
        hash = 59 * hash + (this.language != null ? this.language.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"@" + language;
    }
    
    
}
