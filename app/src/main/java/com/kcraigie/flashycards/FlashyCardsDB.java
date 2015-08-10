package com.kcraigie.flashycards;

import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

public class FlashyCardsDB extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
	
	public FlashyCardsDB(Context context) {
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
		String ret = Base64.encodeToString(randomBytes, Base64.NO_PADDING);
		return ret;
	}
	
	public class FlashyDeck {
		private String m_id;
		private String m_name;
		public FlashyDeck(Cursor cur) {
			m_id = cur.getString(cur.getColumnIndex("id"));
			m_name = cur.getString(cur.getColumnIndex("name"));
		}
		public String getID() { return m_id; }
		public String getName() { return m_name; }
		
		public Iterable<FlashyCard> iterateCards() {
			SQLiteDatabase db = getDB();
			ContentValues cv = new ContentValues();
			cv.put("last_accessed", System.currentTimeMillis()/1000L);
			db.update("decks", cv, "id=?", new String[] { getID() });
			final Cursor cur = db.query("cards", null, "deck_id=?", new String[] { getID() }, null, null, "sequence", null);
			Iterable<FlashyCard> fcs = null;
			if(cur!=null) {
				fcs = new Iterable<FlashyCard>() {
					public Iterator<FlashyCard> iterator() {
						return new FlashyCardIterator(cur);
					}
				};
			}
			return fcs;
		}
		
		public FlashyCard createCard(String front, String back) {
			SQLiteDatabase db = getDB();
			FlashyCard fc = null;
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
			long fcRowID = db.insert("cards", null, cv);
			if(fcRowID!=-1) {
				Cursor cur = db.query("cards", null, "ROWID=", new String[] { Long.toString(fcRowID) }, null, null, null, "1");
				if(cur!=null && cur.moveToNext()) {
					fc = new FlashyCard(cur);
				}
			}
			return fc;
		}
		
		public void renameDeck(String newName) {
			SQLiteDatabase db = getDB();
			ContentValues cv = new ContentValues();
			cv.put("name", newName);
			if(db.update("decks", cv, "WHERE id=?", new String[] { getID() } )>0) {
				m_name = newName;
			}
		}
	}
	
	public class FlashyCard {
		private String m_deckID;
		private String m_id;
		private int m_sequence;
		private String m_front;
		private String m_back;
		public FlashyCard(Cursor cur) {
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
			if(db.update("cards", cv, "WHERE id=?", new String[] { getID() } )>0) {
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
	
	public class FlashyDeckIterator extends MyBaseIterator<FlashyDeck> {
		public FlashyDeckIterator(Cursor cursor) { super(cursor); }

		@Override
		public FlashyDeck next() {
			FlashyDeck fd= null;
			if(m_hasNext) {
				fd = new FlashyDeck(m_cursor);
				m_hasNext = m_cursor.moveToNext();
			}
			return fd;
		}
	}
	
	public class FlashyCardIterator extends MyBaseIterator<FlashyCard> {
		public FlashyCardIterator(Cursor cursor) { super(cursor); }

		@Override
		public FlashyCard next() {
			FlashyCard fc = null;
			if(m_hasNext) {
				fc = new FlashyCard(m_cursor);
				m_hasNext = m_cursor.moveToNext();
			}
			return fc;
		}
	}

	private SQLiteDatabase theDB;
	private synchronized SQLiteDatabase getDB() {
		if(theDB==null) {
			theDB = getWritableDatabase();
		}
		return theDB;
	}

	public Iterable<FlashyDeck> iterateDecks() {
		Iterable<FlashyDeck> fcls = null;
		SQLiteDatabase db = getDB();
		if(db!=null) {
			final Cursor cur = db.query("decks", null, null, null, null, null, "last_accessed", null);
			if(cur!=null) {
				fcls = new Iterable<FlashyDeck>() {
					public Iterator<FlashyDeck> iterator() {
						return new FlashyDeckIterator(cur);
					}
				};
			}
		}
		return fcls;
	}
	
	public FlashyDeck createDeck(String name) {
		SQLiteDatabase db = getDB();
		FlashyDeck fd = null;
		ContentValues cv = new ContentValues();
		String fclID = generateID();
		cv.put("id", fclID);
		cv.put("name", name);
		cv.put("last_accessed", System.currentTimeMillis()/1000L);
		long fcRowID = db.insert("decks", null, cv);
		if(fcRowID!=-1) {
			Cursor cur = db.query("decks", null, "ROWID=", new String[] { Long.toString(fcRowID) }, null, null, null, "1");
			if(cur!=null && cur.moveToNext()) {
				fd = new FlashyDeck(cur);
			}
		}
		return fd;
	}

	private static FlashyCardsDB theInstance = null;
	public static synchronized FlashyCardsDB getInstance(Context context) {
		if(theInstance==null) {
			theInstance = new FlashyCardsDB(context);
		}
		return theInstance;
	}
}
