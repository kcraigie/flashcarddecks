package com.kcraigie.flashycards;

import java.util.Iterator;
import java.util.Set;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class MainListFragment extends android.support.v4.app.Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_list_fragment, container);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		FlashyCardsDB db = FlashyCardsDB.getInstance(getActivity());
		java.util.ArrayList<java.util.Map<String,String>> al = new java.util.ArrayList<java.util.Map<String,String>>();
		for(FlashyCardsDB.FlashyDeck fd: db.iterateDecks()) {
			FlashyDeckWrapper fclw = new FlashyDeckWrapper(fd);
			al.add(fclw);
		}
		android.widget.ListAdapter la =
				new android.widget.SimpleAdapter(getActivity(), al, android.R.layout.simple_list_item_1,
						new String[] { "name" }, new int[] { android.R.id.text1 });
		ListView lv = (ListView)getView().findViewById(R.id.main_list_view);
		lv.setAdapter(la);

		Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
		toolbar.setTitle("Your Decks");
	}

	private class FlashyDeckWrapper extends java.util.AbstractMap<String,String> {
		private FlashyCardsDB.FlashyDeck m_fd;
		FlashyDeckWrapper(FlashyCardsDB.FlashyDeck fd) { m_fd = fd; }
		@Override
		public Set<java.util.Map.Entry<String, String>> entrySet() {
			java.util.AbstractSet<java.util.Map.Entry<String, String>> ret =
					new java.util.AbstractSet<java.util.Map.Entry<String,String>>() {
				@Override
				public Iterator<java.util.Map.Entry<String, String>> iterator() {
					java.util.ArrayList<java.util.Map.Entry<String, String>> al = new java.util.ArrayList<java.util.Map.Entry<String, String>>();
					al.add(new java.util.AbstractMap.SimpleEntry<String, String>("name", m_fd.getName()));
					return al.iterator();
				}
				@Override
				public int size() { return 1; }
			};
			return ret;
		}
	}

	private class FlashyCardWrapper extends java.util.AbstractMap<String,String> {
		private FlashyCardsDB.FlashyCard m_fc;
		FlashyCardWrapper(FlashyCardsDB.FlashyCard fc) { m_fc = fc; }
		@Override
		public Set<java.util.Map.Entry<String, String>> entrySet() {
			java.util.AbstractSet<java.util.Map.Entry<String, String>> ret =
					new java.util.AbstractSet<java.util.Map.Entry<String, String>>() {
				@Override
				public Iterator<java.util.Map.Entry<String, String>> iterator() {
					java.util.ArrayList<java.util.Map.Entry<String, String>> al = new java.util.ArrayList<java.util.Map.Entry<String, String>>();
					al.add(new java.util.AbstractMap.SimpleEntry<String, String>("front", m_fc.getFront()));
					al.add(new java.util.AbstractMap.SimpleEntry<String, String>("back", m_fc.getBack()));
					return al.iterator();
				}
				@Override
				public int size() { return 2; }
			};
			return ret;
		}
	}
}
