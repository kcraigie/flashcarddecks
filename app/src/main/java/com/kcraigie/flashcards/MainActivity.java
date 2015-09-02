package com.kcraigie.flashcards;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

//		// TODO: This mitigates layout shifts when toggling fullscreen mode,
//		// TODO: but it's not perfect...
//        if(Build.VERSION.SDK_INT >= 16) {
//            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
////            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
////            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
//            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
//            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
//        }

		final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				if(getFragmentManager().getBackStackEntryCount()>0) {
					toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
				} else {
					toolbar.setNavigationIcon(null);
				}
			}
		});

		if(savedInstanceState==null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_placeholder, new DeckListFragment(), "DLF");
			ft.commitAllowingStateLoss();
		}
	}
}
