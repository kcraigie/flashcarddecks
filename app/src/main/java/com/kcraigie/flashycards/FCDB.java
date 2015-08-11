package com.kcraigie.flashycards;

import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

public class FCDB extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
	
	public FCDB(Context context) {
		super(context, "flashy_cards", null, DB_VERSION);
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
			db.update("decks", cv, "id=?", new String[] { getID() });
			final Cursor cur = db.query("cards", null, "deck_id=?", new String[] { getID() }, null, null, "sequence", null);
			Iterable<Card> fcs = null;
			if(cur!=null) {
				fcs = new Iterable<Card>() {
					public Iterator<Card> iterator() {
						return new FlashyCardIterator(cur);
					}
				};
			}
			return fcs;
		}
		
		public Card createCard(String front, String back) {
			SQLiteDatabase db = getDB();
			Card card = null;
			ContentValues cv = new ContentValues();
			cv.put("deck_id", getID());
			String fcID = generateID();
			cv.put("id", fcID);
			cv.put("sequence", "SELECT max(sequence) FROM cards)+1");
			if(front!=null) {
				cv.put("front", front);
			}
			if(back!=null) {
				cv.put("back", back);
			}
			long cardRowID = db.insert("cards", null, cv);
			if(cardRowID!=-1) {
				Cursor cur = db.query("cards", null, "ROWID=?", new String[] { Long.toString(cardRowID) }, null, null, null, "1");
				if(cur!=null && cur.moveToNext()) {
					card = new Card(cur);
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
	
	public abstract class MyBaseIterator<T> implements Iterator<T> {
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
	
	public class FlashyCardIterator extends MyBaseIterator<Card> {
		public FlashyCardIterator(Cursor cursor) { super(cursor); }

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
						return new DeckIterator(cur);
					}
				};
			}
		}
		return fcls;
	}

	public Deck createDeck(String name) {
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
			if(cur!=null && cur.moveToNext()) {
				deck = new Deck(cur);
			}
		}
		return deck;
	}

	private static FCDB theInstance = null;
	public static synchronized FCDB getInstance(Context context) {
		if(theInstance==null) {
			theInstance = new FCDB(context);
		}
		return theInstance;
	}
}
