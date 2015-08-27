package com.kcraigie.flashcards;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
            View v1 = getView().findViewById(R.id.frame1);
            View v2 = getView().findViewById(R.id.frame2);

            AnimatorSet as0 = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.card_flip_down_out);
            if(m_showingBack) {
                as0.setTarget(v2);
            } else {
                as0.setTarget(v1);
            }

            AnimatorSet as1 = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.card_flip_down_in);
            if(m_showingBack) {
                as1.setTarget(v1);
            } else {
                as1.setTarget(v2);
            }

            m_showingBack = !m_showingBack;

            AnimatorSet as = new AnimatorSet();
            as.playTogether(as0, as1);
            as.start();
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

            View tv1 = rootView.findViewById(android.R.id.text1);
            ((TextView)tv1).setText(m_card.getFront());

            View tv2 = rootView.findViewById(android.R.id.text2);
            ((TextView)tv2).setText(m_card.getBack());

            if(m_showingBack) {
                View f1 = rootView.findViewById(R.id.frame1);
                f1.setRotationX(-90.0f);
            } else {
                View f2 = rootView.findViewById(R.id.frame2);
                f2.setRotationX(90.0f);
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
}
