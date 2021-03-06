
package com.kcraigie.flashcarddecks;

import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.Set;

public class Wrappers {

    public static class DeckToMap extends java.util.AbstractMap<String,String> {

        private FCDB.Deck m_deck;

        DeckToMap(FCDB.Deck deck) {
            m_deck = deck;
        }

        @NonNull
        @Override
            public Set<Entry<String, String>> entrySet() {
            java.util.AbstractSet<java.util.Map.Entry<String, String>> ret =
                new java.util.AbstractSet<java.util.Map.Entry<String,String>>() {
                @NonNull
                @Override
                public Iterator<Entry<String, String>> iterator() {
                    java.util.ArrayList<java.util.Map.Entry<String, String>> al = new java.util.ArrayList<>();
                    al.add(new java.util.AbstractMap.SimpleEntry<>("name", m_deck.getName()));
                    return al.iterator();
                }
                @Override
                public int size() { return 1; }
            };
            return ret;
        }

        FCDB.Deck getDeck() {
            return m_deck;
        }
    }

	public static class CardToMap extends java.util.AbstractMap<String,String> {

		private FCDB.Card m_card;

		CardToMap(FCDB.Card card) {
            m_card = card;
        }

		@NonNull
        @Override
            public Set<java.util.Map.Entry<String, String>> entrySet() {
			java.util.AbstractSet<java.util.Map.Entry<String, String>> ret =
                new java.util.AbstractSet<java.util.Map.Entry<String, String>>() {
				@NonNull
                @Override
				public Iterator<java.util.Map.Entry<String, String>> iterator() {
					java.util.ArrayList<java.util.Map.Entry<String, String>> al = new java.util.ArrayList<>();
                    String cardFront = m_card.getFront();
                    String cardBack = m_card.getBack();
                    String cardFrontSlashBack = cardFront;
                    if(cardBack!=null) {
                        cardFrontSlashBack += " / " + cardBack;
                    }
					al.add(new java.util.AbstractMap.SimpleEntry<>("front", cardFront));
					al.add(new java.util.AbstractMap.SimpleEntry<>("back", cardBack));
					al.add(new java.util.AbstractMap.SimpleEntry<>("front / back", cardFrontSlashBack));
					return al.iterator();
				}
				@Override
				public int size() { return 2; }
			};
			return ret;
		}

        FCDB.Card getCard() {
            return m_card;
        }
	}

}
