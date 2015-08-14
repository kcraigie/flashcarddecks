package com.kcraigie.flashycards;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class PlayDeckFragment extends Fragment {
    private FCDB.Deck m_deck;

    public void setDeck(FCDB.Deck deck) {
        m_deck = deck;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.play_deck_fragment, container, false);

        // Get root layout
        LinearLayout layout = (LinearLayout)rootView.findViewById(R.id.play_deck_layout);
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                toggleHideyBar();
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

//        final View decorView = getActivity().getWindow().getDecorView();
//        decorView.setOnSystemUiVisibilityChangeListener(
//            new View.OnSystemUiVisibilityChangeListener() {
//                @Override
//                public void onSystemUiVisibilityChange(int i) {
//                    int height = decorView.getHeight();
//                    Log.i(getClass().toString(), "Current height: " + height);
//                }
//            });

        if(savedInstanceState!=null) {
            if(m_deck==null) {
                String deckID = savedInstanceState.getString("deck_id");
                if(deckID!=null) {
                    FCDB db = FCDB.getInstance(getActivity());
                    m_deck = db.findDeckByID(deckID);
                }
            }
        }

//        // Populate initial list of cards
//        if(m_deck!=null) {
//            for (FCDB.Card card : m_deck.iterateCards()) {
//                Wrappers.CardToMap ctm = new Wrappers.CardToMap(card);
//                m_alCards.add(ctm);
//            }
//        }
//        SimpleAdapter sa = new SimpleAdapter(getActivity(), m_alCards, R.layout.card_list_item,
//                new String[] { "front / back" }, new int[] { android.R.id.text1 });
//        ListView lv = (ListView)getView().findViewById(R.id.card_list_view);
//        lv.setAdapter(sa);

        // TODO: Restore listview's scroll position after rotate

        final Toolbar toolbar = (Toolbar)getActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
//        toolbar.inflateMenu(R.menu.card_list_menu);

        toolbar.setTitle(getString(R.string.playing_deck, (m_deck!=null?m_deck.getName():"?")));
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
}
