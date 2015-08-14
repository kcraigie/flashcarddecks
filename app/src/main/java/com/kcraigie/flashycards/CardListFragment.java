package com.kcraigie.flashycards;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class CardListFragment extends android.support.v4.app.Fragment {

    private FCDB.Deck m_deck;
    java.util.ArrayList<java.util.Map<String,String>> m_alCards = new java.util.ArrayList<java.util.Map<String,String>>();

    public void setDeck(FCDB.Deck deck) {
        m_deck = deck;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.card_list_fragment, container, false);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(m_deck!=null) {
            outState.putString("deck_id", m_deck.getID());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState!=null) {
            if(m_deck==null) {
                String deckID = savedInstanceState.getString("deck_id");
                if(deckID!=null) {
                    FCDB db = FCDB.getInstance(getActivity());
                    m_deck = db.findDeckByID(deckID);
                }
            }
        }

        // Populate initial list of cards
        if(m_deck!=null) {
            for (FCDB.Card card : m_deck.iterateCards()) {
                Wrappers.CardToMap ctm = new Wrappers.CardToMap(card);
                m_alCards.add(ctm);
            }
        }
        SimpleAdapter sa = new SimpleAdapter(getActivity(), m_alCards, R.layout.card_list_item,
                new String[] { "front / back" }, new int[] { android.R.id.text1 });
        ListView lv = (ListView)getView().findViewById(R.id.card_list_view);
        lv.setAdapter(sa);

        // TODO: Restore listview's scroll position after rotate

        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.card_list_menu);
        toolbar.getMenu().findItem(R.id.action_delete_deck).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                deleteDeck();
                return false;
            }
        });

        final EditText etDeckName = (EditText)getView().findViewById(R.id.edit_deck_name);
        final EditText etCardFront = (EditText)getView().findViewById(R.id.edit_card_front);
        final EditText etCardBack = (EditText)getView().findViewById(R.id.edit_card_back);

        toolbar.setTitle(getString(R.string.edit_deck));
        if(m_deck!=null) {
            etDeckName.setText(m_deck.getName());
        }
        etCardFront.setEnabled(true);

        etDeckName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                etCardFront.setEnabled(s.length() > 0);
            }
        });
        etDeckName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String deckName = etDeckName.getText().toString();
                    if (deckName.isEmpty()) {
                        etDeckName.setError(null);
                    } else {
                        toolbar.setTitle(deckName);
                        if (m_deck == null) {
                            FCDB db = FCDB.getInstance(getActivity());
                            m_deck = db.createDeck(deckName);
                        } else {
                            m_deck.renameDeck(deckName);
                        }
                    }
                }
            }
        });

        etCardFront.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Don't allow a card with only a back
                etCardBack.setEnabled(s.length() > 0);
            }
        });
        etCardFront.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) getView().findViewById(R.id.edit_card_front)).setHint(R.string.hint_card_front);
                    getView().findViewById(R.id.edit_card_back).setVisibility(View.VISIBLE);
                }
            }
        });

        TextView.OnEditorActionListener oeal = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String frontText = etCardFront.getText().toString().trim();
                    String backText = etCardBack.getText().toString().trim();
                    FCDB.Card card = m_deck.createCard(frontText, backText);
                    if(card!=null) {
                        Wrappers.CardToMap ctm = new Wrappers.CardToMap(card);
                        m_alCards.add(ctm);
                        ListView lv = (ListView)getView().findViewById(R.id.card_list_view);
                        SimpleAdapter sa = (SimpleAdapter)lv.getAdapter();
                        etCardFront.setText("");
                        etCardBack.setText("");
                        sa.notifyDataSetChanged();
//                        lv.smoothScrollToPosition(lv.getCount()-1);
                        etCardFront.requestFocus();
                        return true;
                    }
                }
                return false;
            }
        };
        etCardFront.setOnEditorActionListener(oeal);
        etCardBack.setOnEditorActionListener(oeal);
    }

    private void deleteDeck() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
//        builder.setTitle(getString(R.string.confirm_delete));
        builder.setMessage(getString(R.string.confirm_delete_deck));
        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FCDB db = FCDB.getInstance(getActivity());
                db.deleteDeck(m_deck);
                getFragmentManager().popBackStackImmediate();
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.show();
    }

}
