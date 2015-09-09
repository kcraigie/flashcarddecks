package com.kcraigie.flashcarddecks;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
    FCDB.Card[] m_cards;

    public void setDeck(FCDB.Deck deck, boolean shouldShuffle) {
        m_deck = deck;

        // Pull out all the cards here and turn them into an array...
        // TODO: Is this the most efficient way to do this?
        ArrayList<FCDB.Card> al = new ArrayList<>();
        FCDB.CardIterator ci = m_deck.iterateCards();
        if(ci!=null) {
            while(ci.hasNext()) {
                FCDB.Card card = ci.next();
                al.add(card);
            }
            ci.close();
        }
        if(shouldShuffle) {
            Collections.shuffle(al);
        }
        m_cards = al.toArray(new FCDB.Card[al.size()]);

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
            if(m_cards==null) {
                String cardIDs = savedInstanceState.getString("card_ids");
                if(cardIDs!=null) {
                    String[] cardIDs2 = cardIDs.split(",");
                    FCDB db = FCDB.getInstance(getActivity());
                    m_cards = db.findCardsByIDs(cardIDs2);
                }
            }
        }

        View rootView = inflater.inflate(R.layout.play_deck_fragment, container, false);

        final ViewPager vp = (ViewPager)rootView.findViewById(R.id.pager);
        vp.setId(R.id.pager);
        final PlayDeckAdapter pda = new PlayDeckAdapter(getFragmentManager(), m_cards);
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
        if(m_cards!=null) {
            StringBuilder result = new StringBuilder();
            for(int i=0;i<m_cards.length;i++) {
                if(i>0) {
                    result.append(",");
                }
                result.append(m_cards[i].getID());
            }
            outState.putString("card_ids", result.toString());
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
        FCDB.Card[] m_cards;

        public PlayDeckAdapter(FragmentManager fm, FCDB.Card[] cards) {
            super(fm);
            m_cards = cards;
        }

        @Override
        public Fragment getItem(int position) {
            // TODO: Check bounds?
            FCDB.Card card = m_cards[position];
            PlayCardFragment f = null;
            if(card!=null) {
                f = new PlayCardFragment();
                f.setCard(card);
            }
            return f;
        }

        @Override
        public int getCount() {
            return m_cards.length;
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
                cf.setCardText(m_card.getFront(), false);
                ft.setCustomAnimations(R.animator.card_flip_left_in, R.animator.card_flip_left_out);
            } else {
                cf.setCardText(m_card.getBack(), true);
                ft.setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out);
            }
            ft.replace(R.id.card_fragment_placeholder, cf);
            try {
                ft.commitAllowingStateLoss();
                m_showingBack = !m_showingBack;
            } catch(IllegalStateException e) {
                Log.w(getClass().toString(), "Caught IllegalStateException when trying to flip card, assuming activity is finishing...");
            }
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
                cf.setCardText(m_card.getFront(), false);
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
        boolean m_invertColors;

        public void setCardText(String cardText, boolean invertColors) {
            m_cardText = cardText;
            m_invertColors = invertColors;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if(savedInstanceState!=null) {
                if (m_cardText == null) {
                    m_cardText = savedInstanceState.getString("card_text");
                }
                m_invertColors = savedInstanceState.getBoolean("invert_colors", false);
            }

            View rootView = inflater.inflate(R.layout.card_fragment, container, false);

            TextView tv = (TextView)rootView.findViewById(android.R.id.text1);

            if(m_invertColors) {
                Drawable bg = rootView.getBackground();
                //To generate negative image
                float[] colorMatrix_Negative = {
                        -1.0f, 0, 0, 0, 255, //red
                        0, -1.0f, 0, 0, 255, //green
                        0, 0, -1.0f, 0, 255, //blue
                        0, 0, 0, 1.0f, 0 //alpha
                };
                ColorFilter colorFilter_Negative = new ColorMatrixColorFilter(colorMatrix_Negative);
                bg.setColorFilter(colorFilter_Negative);
                rootView.setBackground(bg);

                int argb = tv.getCurrentTextColor();
                argb = ~argb;
                // TODO: Reset alpha value?
//                argb |= 0xff000000;
                tv.setTextColor(argb);
            }

            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

            // TODO: Set text size to be large... this doesn't work well, need a better solution
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, (Math.min(dm.widthPixels, dm.heightPixels) / 6.0f));

            tv.setText(m_cardText);

            // Pull camera away from card so rotation looks better (otherwise card appears to hit camera)
            rootView.setCameraDistance(10 * dm.widthPixels * dm.density);

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            if(m_cardText!=null) {
                outState.putString("card_text", m_cardText);
                outState.putBoolean("invert_colors", m_invertColors);
            }
        }
    }
}
