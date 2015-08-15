package com.kcraigie.flashycards;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlayDeckFragment extends Fragment {
    private FCDB.Deck m_deck;

    public void setDeck(FCDB.Deck deck, boolean shuffle) {
        m_deck = deck;
        if(shuffle) {

            // TODO: Implement shuffling (will need to persist shuffled indices for rotate)

        }
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
        PlayDeckAdapter pda = new PlayDeckAdapter(getFragmentManager(), m_deck);
        vp.setAdapter(pda);

        // Set up detection of single tap
        GestureDetector.SimpleOnGestureListener sogl = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleHideyBar();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                // TODO: Implement card flipping
                Toast.makeText(getActivity(), "TODO: IMPLEMENT CARD FLIPPING", Toast.LENGTH_LONG).show();

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

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    public void toggleHideyBar() {
        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);

        // TODO: Fix this so it's not so juddery and janky and weird

        if (isImmersiveModeEnabled) {
            Log.i(getClass().toString(), "Turning immersive mode mode off...");
            Animation animFadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            animFadeIn.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            animFadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    toolbar.setVisibility(View.VISIBLE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            toolbar.startAnimation(animFadeIn);
        } else {
            Log.i(getClass().toString(), "Turning immersive mode mode on...");
            Animation animFadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            animFadeOut.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            animFadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    toolbar.setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            toolbar.startAnimation(animFadeOut);
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if(Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if(Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if(Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private class PlayDeckAdapter extends FragmentStatePagerAdapter {
        ArrayList<FCDB.Card> m_alCards;

        public PlayDeckAdapter(FragmentManager fm, FCDB.Deck deck) {
            super(fm);

            m_alCards = new ArrayList<FCDB.Card>();
            for(FCDB.Card card: m_deck.iterateCards()) {
                m_alCards.add(card);
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

        public void setCard(FCDB.Card card) {
            m_card = card;
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
            }

            View v = inflater.inflate(R.layout.play_card_fragment, container, false);

            // Just a simple fragment with a single TextView in the middle
            View tv = v.findViewById(android.R.id.text1);
            ((TextView)tv).setText(m_card.getFront());

            return v;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            if(m_card!=null) {
                outState.putString("card_id", m_card.getID());
            }
        }
    }
}
