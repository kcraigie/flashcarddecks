package com.kcraigie.flashcards;

import android.animation.LayoutTransition;
import android.app.Fragment;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class DeckListFragment extends Fragment {

	int m_oldSelectedPosition = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.deck_list_fragment, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if(savedInstanceState!=null) {
			m_oldSelectedPosition = savedInstanceState.getInt("old_selected_position", -1);
		}

		FCDB db = FCDB.getInstance(getActivity());
		java.util.ArrayList<java.util.Map<String,String>> al = new java.util.ArrayList<java.util.Map<String,String>>();
		for(FCDB.Deck deck: db.iterateDecks()) {
			Wrappers.DeckToMap dtm = new Wrappers.DeckToMap(deck);
			al.add(dtm);
		}

		final ListView lv = (ListView)getView().findViewById(R.id.deck_list_view);

		ListAdapter la = new SimpleAdapter(getActivity(), al, R.layout.deck_list_item,
						new String[] { "name" }, new int[] { android.R.id.text1 }) {
			@Override
			public View getView(final int position, final View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);

				final ImageView iv0 = (ImageView)v.findViewById(R.id.deck_list_item_edit);
				iv0.setTag(position);
				iv0.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int pos = (int)iv0.getTag();
						Wrappers.DeckToMap dtm = (Wrappers.DeckToMap)lv.getItemAtPosition(pos);
						FCDB.Deck deck = dtm.getDeck();
						editDeck(deck);
					}
				});

				final ImageView iv1 = (ImageView)v.findViewById(R.id.deck_list_item_play);
				iv1.setTag(position);
				iv1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int pos = (int) iv1.getTag();
						Wrappers.DeckToMap dtm = (Wrappers.DeckToMap) lv.getItemAtPosition(pos);
						FCDB.Deck deck = dtm.getDeck();
						playDeck(deck, false);
					}
				});

				final ImageView iv2 = (ImageView)v.findViewById(R.id.deck_list_item_shuffle);
				iv2.setTag(position);
				iv2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int pos = (int) iv1.getTag();
						Wrappers.DeckToMap dtm = (Wrappers.DeckToMap) lv.getItemAtPosition(pos);
						FCDB.Deck deck = dtm.getDeck();
						playDeck(deck, true);
					}
				});

//				// Tweak the layout transition because by default it doesn't look right
//				LayoutTransition lt = ((LinearLayout)v).getLayoutTransition();
//				lt.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
//				lt.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);

				// Restore selected item state on rotate et al
				if(m_oldSelectedPosition==position) {
					LinearLayout actionsLayout = (LinearLayout) v.findViewById(R.id.deck_list_item_actions);
					actionsLayout.setVisibility(View.VISIBLE);
				}

				return v;
			}
		};
		lv.setAdapter(la);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectAndExpandItem(position);
			}
		});

		Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.your_decks));
		toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.deck_list_menu);
		toolbar.getMenu().findItem(R.id.action_new_deck).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				editDeck(null);
				return false;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if(m_oldSelectedPosition!=-1) {
			outState.putInt("old_selected_position", m_oldSelectedPosition);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Update the list when returning to this fragment (eg. after adding a new deck)
		final ListView lv = (ListView)getView().findViewById(R.id.deck_list_view);
		SimpleAdapter sa = (SimpleAdapter)lv.getAdapter();
		sa.notifyDataSetChanged();
	}

	private void editDeck(FCDB.Deck deck) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		CardListFragment clf = new CardListFragment();
		if(deck!=null) {
			clf.setDeck(deck);
		}
		ft.replace(R.id.fragment_placeholder, clf, "CLF");
		ft.addToBackStack(null);
		ft.commitAllowingStateLoss();
	}

	private void playDeck(FCDB.Deck deck, boolean shouldShuffle) {
//		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		PlayDeckFragment pdf = new PlayDeckFragment();
		if(deck!=null) {
			pdf.setDeck(deck, shouldShuffle);
		}
		ft.replace(R.id.fragment_placeholder, pdf, "PDF");
		ft.addToBackStack(null);
		ft.commitAllowingStateLoss();
	}

	private void selectAndExpandItem(int position) {
		final ListView lv = (ListView)getView().findViewById(R.id.deck_list_view);

		if(m_oldSelectedPosition!=-1) {
			View lvi = lv.getChildAt(m_oldSelectedPosition-lv.getFirstVisiblePosition());
			LinearLayout actionsLayout = (LinearLayout) lvi.findViewById(R.id.deck_list_item_actions);
			actionsLayout.setVisibility(View.GONE);
		}
		if(m_oldSelectedPosition==position) {
			// This means contract
			position = -1;
		}
		if(position!=-1) {
			View lvi = lv.getChildAt(position-lv.getFirstVisiblePosition());
			LinearLayout actionsLayout = (LinearLayout) lvi.findViewById(R.id.deck_list_item_actions);
			actionsLayout.setVisibility(View.VISIBLE);
		}
		m_oldSelectedPosition = position;
	}
}
