package com.kcraigie.flashcards;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class PlayDeckFragment extends Fragment {
    FCDB.Deck m_deck;
    boolean m_shouldShuffle;

    public void setDeck(FCDB.Deck deck, boolean shouldShuffle) {
        m_deck = deck;
        m_shouldShuffle = shouldShuffle;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(savedInstanceState!=null) {
            if(m_deck==null) {
                String deckID = savedInstanceState.getString("deck_id");
                if(deckID!=null) {
                    FCDB db = FCDB.getInstance(getActivity());
                    m_deck = db.findDeckByID(deckID);
                }
            }
        }

        View rootView = inflater.inflate(R.layout.play_deck_fragment, container, false);
        rootView.setCameraDistance(rootView.getCameraDistance() * 10);

        final ViewPager vp = (ViewPager)rootView.findViewById(R.id.pager);
        final PlayDeckAdapter pda = new PlayDeckAdapter(getFragmentManager(), m_deck, m_shouldShuffle);
        vp.setAdapter(pda);

        // Set up detection of single tap
        GestureDetector.SimpleOnGestureListener sogl = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                int currentPosition = vp.getCurrentItem();
                PlayCardFragment pcf = (PlayCardFragment) pda.instantiateItem(vp, currentPosition);
                if(pcf!=null) {
                    pcf.flipCard();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }
        };
        final GestureDetectorCompat gdc = new GestureDetectorCompat(getActivity(), sogl);
        vp.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                gdc.onTouchEvent(event);
                return false;
            }
        });

        return rootView;
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

        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.setTitle(getString(R.string.playing_deck, (m_deck != null ? m_deck.getName() : "?")));
    }

    class PlayDeckAdapter extends FragmentStatePagerAdapter {
        ArrayList<FCDB.Card> m_alCards;

        public PlayDeckAdapter(FragmentManager fm, FCDB.Deck deck, boolean shouldShuffle) {
            super(fm);

            m_alCards = new ArrayList<FCDB.Card>();
            for(FCDB.Card card: m_deck.iterateCards()) {
                m_alCards.add(card);
            }
            if(shouldShuffle) {
                Collections.shuffle(m_alCards);
            }
        }

        @Override
        public Fragment getItem(int position) {
            FCDB.Card card = m_alCards.get(position);
            PlayCardFragment f = null;
            if(card!=null) {
                f = new PlayCardFragment();
                f.setCard(card);
            }
            return f;
        }

        @Override
        public int getCount() {
            return m_alCards.size();
        }
    }

    static public class PlayCardFragment extends Fragment {
        FCDB.Card m_card;
        boolean m_showingBack;

        public void setCard(FCDB.Card card) {
            m_card = card;
        }

        public void flipCard() {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            CardFragment cf = new CardFragment();
            if(m_showingBack) {
                cf.setCardText(m_card.getFront());
                ft.setCustomAnimations(R.animator.card_flip_left_in, R.animator.card_flip_left_out);
            } else {
                cf.setCardText(m_card.getBack());
                ft.setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out);
            }
            ft.replace(R.id.card_fragment_placeholder, cf);
            ft.commitAllowingStateLoss();
            m_showingBack = !m_showingBack;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if(savedInstanceState!=null) {
                if(m_card==null) {
                    String cardID = savedInstanceState.getString("card_id");
                    if(cardID!=null) {
                        FCDB db = FCDB.getInstance(getActivity());
                        m_card = db.findCardByID(cardID);
                    }
                }
                m_showingBack = savedInstanceState.getBoolean("showing_back");
            }

            View rootView = inflater.inflate(R.layout.play_card_fragment, container, false);

            if(savedInstanceState==null) {
                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                CardFragment cf = new CardFragment();
                cf.setCardText(m_card.getFront());
                ft.replace(R.id.card_fragment_placeholder, cf);
                ft.commitAllowingStateLoss();
            }

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            if(m_card!=null) {
                outState.putString("card_id", m_card.getID());
                if(m_showingBack) {
                    outState.putBoolean("showing_back", m_showingBack);
                }
            }
        }
    }

    static public class CardFragment extends Fragment {
        String m_cardText;

        public void setCardText(String cardText) {
            m_cardText = cardText;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if(savedInstanceState!=null) {
                if (m_cardText == null) {
                    m_cardText = savedInstanceState.getString("card_text");
                }
            }

            View rootView = inflater.inflate(R.layout.card_fragment, container, false);

            View tv1 = rootView.findViewById(android.R.id.text1);
            ((TextView)tv1).setText(m_cardText);

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            if(m_cardText!=null) {
                outState.putString("card_text", m_cardText);
            }
        }
    }
}
