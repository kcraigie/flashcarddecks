package com.kcraigie.flashcards;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

public class FCDB extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
	
	public FCDB(Context context) {
		super(context, "flashcards", null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE decks (id TEXT PRIMARY KEY, name TEXT, last_accessed INTEGER);");
        db.execSQL("CREATE TABLE cards (deck_id TEXT, id TEXT PRIMARY KEY, sequence INTEGER, front TEXT, back TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	private java.util.Random theRandom = new java.util.Random();
	private synchronized String generateID() {
		byte[] randomBytes = new byte[20];
		theRandom.nextBytes(randomBytes);
		String ret = Base64.encodeToString(randomBytes, Base64.NO_PADDING|Base64.NO_WRAP);
		return ret;
	}
	
	public class Deck {
		private String m_id;
		private String m_name;
		public Deck(Cursor cur) {
			m_id = cur.getString(cur.getColumnIndex("id"));
			m_name = cur.getString(cur.getColumnIndex("name"));
		}
		public String getID() { return m_id; }
		public String getName() { return m_name; }
		
		public Iterable<Card> iterateCards() {
			SQLiteDatabase db = getDB();
			ContentValues cv = new ContentValues();
			cv.put("last_accessed", System.currentTimeMillis()/1000L);
			db.update("decks", cv, "id=?", new String[]{getID()});
			final Cursor cur = db.query("cards", null, "deck_id=?", new String[] { getID() }, null, null, "sequence", null);
			Iterable<Card> fcs = null;
			if(cur!=null) {
				fcs = new Iterable<Card>() {
					public Iterator<Card> iterator() {
						// TODO: Leaking cursor?
						return new CardIterator(cur);
					}
				};
			}
			return fcs;
		}
		
		public Card createCard(String front, String back) {
			// Gotta have at least a non-empty front
			if(front==null || front.isEmpty()) {
				return null;
			}
			SQLiteDatabase db = getDB();
			Card card = null;
			ContentValues cv = new ContentValues();
			cv.put("deck_id", getID());
			String fcID = generateID();
			cv.put("id", fcID);
			cv.put("sequence", "SELECT max(sequence) FROM cards)+1");
			cv.put("front", front);
			if(back!=null && !back.isEmpty()) {
				cv.put("back", back);
			}
			long cardRowID = db.insert("cards", null, cv);
			if(cardRowID!=-1) {
				Cursor cur = db.query("cards", null, "ROWID=?", new String[] { Long.toString(cardRowID) }, null, null, null, "1");
				if(cur!=null) {
					if(cur.moveToNext()) {
						card = new Card(cur);
					}
					cur.close();
				}
			}
			return card;
		}
		
		public void renameDeck(String newName) {
			SQLiteDatabase db = getDB();
			ContentValues cv = new ContentValues();
			cv.put("name", newName);
			if(db.update("decks", cv, "id=?", new String[] { getID() } )>0) {
				m_name = newName;
			}
		}
	}
	
	public class Card {
		private String m_deckID;
		private String m_id;
		private int m_sequence;
		private String m_front;
		private String m_back;
		public Card(Cursor cur) {
			m_deckID = cur.getString(cur.getColumnIndex("deck_id"));
			m_id = cur.getString(cur.getColumnIndex("id"));
			m_sequence = cur.getInt(cur.getColumnIndex("sequence"));
			m_front = cur.getString(cur.getColumnIndex("front"));
			m_back = cur.getString(cur.getColumnIndex("back"));
		}
		public String getDeckID() { return m_deckID; }
		public String getID() { return m_id; }
		public int getSequence() { return m_sequence; }
		public String getFront() { return m_front; }
		public String getBack() { return m_back; }

		public void setFrontAndBack(String newFront, String newBack) {
			SQLiteDatabase db = getDB();
			ContentValues cv = new ContentValues();
			cv.put("front", newFront);
			cv.put("back", newBack);
			if(db.update("cards", cv, "id=?", new String[] { getID() } )>0) {
				m_front = newFront;
				m_back = newBack;
			}
		}
	}
	
	public abstract class MyBaseIterator<T> implements Iterator<T>, Closeable {
		protected Cursor m_cursor;
		protected boolean m_hasNext;

		public MyBaseIterator(Cursor cursor) {
			m_cursor = cursor;
			m_hasNext = m_cursor.moveToNext();
		}

		@Override
		public boolean hasNext() {
			return m_hasNext;
		}
		
		@Override
		public void remove() {
			// TODO Support this?
		}

		@Override
		public void close() throws IOException {
			m_cursor.close();
		}
	}
	
	public class DeckIterator extends MyBaseIterator<Deck> {
		public DeckIterator(Cursor cursor) { super(cursor); }

		@Override
		public Deck next() {
			Deck deck= null;
			if(m_hasNext) {
				deck = new Deck(m_cursor);
				m_hasNext = m_cursor.moveToNext();
			}
			return deck;
		}
	}
	
	public class CardIterator extends MyBaseIterator<Card> {
		public CardIterator(Cursor cursor) { super(cursor); }

		@Override
		public Card next() {
			Card card = null;
			if(m_hasNext) {
				card = new Card(m_cursor);
				m_hasNext = m_cursor.moveToNext();
			}
			return card;
		}
	}

	private SQLiteDatabase theDB;
	private synchronized SQLiteDatabase getDB() {
		if(theDB==null) {
			theDB = getWritableDatabase();
		}
		return theDB;
	}

	public Iterable<Deck> iterateDecks() {
		Iterable<Deck> fcls = null;
		SQLiteDatabase db = getDB();
		if(db!=null) {
			final Cursor cur = db.query("decks", null, null, null, null, null, "last_accessed DESC", null);
			if(cur!=null) {
				fcls = new Iterable<Deck>() {
					public Iterator<Deck> iterator() {
						// TODO: Leaking cursor?
						return new DeckIterator(cur);
					}
				};
			}
		}
		return fcls;
	}

	public Deck createDeck(String name) {
		Log.d(getClass().toString(), "Creating deck with name: '" + name + "'");
		SQLiteDatabase db = getDB();
		Deck deck = null;
		ContentValues cv = new ContentValues();
		String fclID = generateID();
		cv.put("id", fclID);
		cv.put("name", name);
		cv.put("last_accessed", System.currentTimeMillis()/1000L);
		long deckRowID = db.insert("decks", null, cv);
		if(deckRowID!=-1) {
			Cursor cur = db.query("decks", null, "ROWID=?", new String[] { Long.toString(deckRowID) }, null, null, null, "1");
			if(cur!=null) {
				if(cur.moveToNext()) {
					deck = new Deck(cur);
				}
				cur.close();
			}
		}
		return deck;
	}

	public void deleteDeck(Deck deck) {
		Log.d(getClass().toString(), "Deleting deck with name: '" + deck.getName() + "' and ID: '" + deck.getID() + "'");
		SQLiteDatabase db = getDB();
		db.delete("decks", "id=?", new String [] { deck.getID() });
		db.delete("cards", "deck_id=?", new String [] { deck.getID() });
	}

	public void deleteCard(Card card) {
		Log.d(getClass().toString(), "Deleting card with front: '" + card.getFront() + "' and back: '" + (card.getBack() != null ? card.getBack() : "") + "' and ID: '" + card.getID() + "'");
		SQLiteDatabase db = getDB();
		db.delete("cards", "id=?", new String [] { card.getID() });
	}

	public Deck findDeckByID(String id) {
		SQLiteDatabase db = getDB();
		Deck deck = null;
		Cursor cur = db.query("decks", null, "id=?", new String[]{id}, null, null, null, "1");
		if(cur!=null) {
			if(cur.moveToNext()) {
				deck = new Deck(cur);
				Log.d(getClass().toString(), "Found deck with name: '" + deck.getName() + "' by ID: '" + deck.getID() + "'");
			}
			cur.close();
		} else {
			Log.d(getClass().toString(), "Couldn't find deck by ID: '" + id + "'");
		}
		return deck;
	}

	public Card findCardByID(String id) {
		SQLiteDatabase db = getDB();
		Card card = null;
		Cursor cur = db.query("cards", null, "id=?", new String[]{id}, null, null, null, "1");
		if(cur!=null) {
			if(cur.moveToNext()) {
				card = new Card(cur);
				Log.d(getClass().toString(), "Found card with front: '" + card.getFront() + "' and back: '" + card.getBack() + "' by ID: '" + card.getID() + "'");
			}
			cur.close();
		} else {
			Log.d(getClass().toString(), "Couldn't find card by ID: '" + id + "'");
		}
		return card;
	}

	public Card[] findCardsByIDs(String[] ids) {
		// Build a string with a bunch of question marks for the WHERE clause,
		// and also populate a HashMap with ids-to-indices, so we can sort the result
		HashMap<String,Integer> hm = new HashMap<>(ids.length);
		StringBuilder result = new StringBuilder("(");
		for(int i=0;i<ids.length;i++) {
			if(i>0) {
				result.append(",");
			}
			result.append("?");
			hm.put(ids[i], i);
		}
		result.append(")");

		SQLiteDatabase db = getDB();
		Cursor cur = db.query("cards", null, "id IN " + result, ids, null, null, null, null);
		Card[] ret = new Card[ids.length];
		if(cur!=null) {
			// TODO: Handle the case of not enough cards found?
			while (cur.moveToNext()) {
				Card card = new Card(cur);
				// TODO: Handle the case of a null index?
				Integer index = hm.get(card.getID());
				ret[index] = card;
				Log.d(getClass().toString(), "Found card #" + index + " with front: '" + card.getFront() + "' and back: '" + card.getBack() + "' by ID: '" + card.getID() + "'");
			}
			cur.close();
		}
		return ret;
	}

	private static FCDB theInstance = null;
	public static synchronized FCDB getInstance(Context context) {
		if(theInstance==null) {
			theInstance = new FCDB(context);
		}
		return theInstance;
	}
}
