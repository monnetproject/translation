package eu.monnetproject.translation.sources.ewn;


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.Feature;
import eu.monnetproject.translation.Label;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;


public class PhraseTableImpl implements PhraseTable {

	private final Collection<String> underlying;
	private final Language foreignLanguage, translationLanguage;
	private final String name;
	private final String sourceLabel;



	public PhraseTableImpl(List<String> underlying, String sourceLabel, Language foreignLanguage, Language translationLanguage, String name) {
		this.underlying = underlying;
		this.foreignLanguage = foreignLanguage;
		this.translationLanguage = translationLanguage;
		this.name = name;
		this.sourceLabel = sourceLabel;
	}

	public PhraseTableImpl(Set<String> underlying, String sourceLabel, Language foreignLanguage, Language translationLanguage, String name) {
		this.underlying = underlying;
		this.foreignLanguage = foreignLanguage;
		this.translationLanguage = translationLanguage;
		this.name = name;
		this.sourceLabel = sourceLabel;
	}


	@Override
	public Language getForeignLanguage() {
		return foreignLanguage;
	}

	private int longestForeignPhase = -1;

	@Override
	public int getLongestForeignPhrase() {
		if(longestForeignPhase >= 0) {
			return longestForeignPhase;
		}
		for(String translation : underlying) {
			final String[] elems = translation.split("\\s+");
			if(elems.length > longestForeignPhase) {
				longestForeignPhase = elems.length;
			}
		}
		return longestForeignPhase;
	}

	@Override
	public Language getTranslationLanguage() {
		return translationLanguage;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Iterator<PhraseTableEntry> iterator() {
		final Iterator<String> iterator = underlying.iterator();
		return new Iterator<PhraseTableEntry>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public PhraseTableEntry next() {
				final String n = iterator.next();
				return new PhraseTableEntry() {

					@Override
					public Label getForeign() {
						return new Label() {

							@Override
							public String asString() {
								return sourceLabel;
							}

							@Override
							public Language getLanguage() {
								return foreignLanguage;
							}
						};
					}

					@Override
					public Label getTranslation() {
						return new Label() {

							@Override
							public String asString() {
								return n;
							}

							@Override
							public Language getLanguage() {
								return translationLanguage;
							}
						};
					}

					@Override
					public Feature[] getFeatures() {
						//applying a constant feature score of 1 to each translation candidate							
						Feature[] IATEFeatures = new Feature[1]; 					
						IATEFeatures[0] = new Feature("inEWN", 1.0);
						return IATEFeatures;
					}

					@Override
					public double getApproxScore() {
						return 0.0;
					}

				};
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}

