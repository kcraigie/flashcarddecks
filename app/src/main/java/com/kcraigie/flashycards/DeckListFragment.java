package com.kcraigie.flashycards;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class DeckListFragment extends android.support.v4.app.Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.deck_list_fragment, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		FCDB db = FCDB.getInstance(getActivity());
		java.util.ArrayList<java.util.Map<String,String>> al = new java.util.ArrayList<java.util.Map<String,String>>();
		for(FCDB.Deck deck: db.iterateDecks()) {
			Wrappers.DeckToMap dtm = new Wrappers.DeckToMap(deck);
			al.add(dtm);
		}
		android.widget.ListAdapter la =
				new android.widget.SimpleAdapter(getActivity(), al, android.R.layout.simple_list_item_1,
						new String[] { "name" }, new int[] { android.R.id.text1 });
		final ListView lv = (ListView)getView().findViewById(R.id.deck_list_view);
		lv.setAdapter(la);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Wrappers.DeckToMap dtm = (Wrappers.DeckToMap)lv.getItemAtPosition(position);
				FCDB.Deck deck = dtm.getDeck();
				openDeck(deck);
			}
		});

		Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.your_decks));
		toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.deck_list_menu);
		toolbar.getMenu().findItem(R.id.action_new_deck).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openDeck(null);
				return false;
			}
		});
	}

	private void openDeck(FCDB.Deck deck) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
//				ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		CardListFragment clf = new CardListFragment();
		if(deck!=null) {
			clf.setDeck(deck);
		}
		ft.replace(R.id.fragment_placeholder, clf, "CLF");
		ft.addToBackStack(null);
		ft.commitAllowingStateLoss();
	}

}
