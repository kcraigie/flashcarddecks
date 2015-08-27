package com.kcraigie.flashcards;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

public class MainActivity extends android.support.v7.app.AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// TODO: This mitigates layout shifts when toggling fullscreen mode,
		// TODO: but it's not perfect...
        if(Build.VERSION.SDK_INT >= 16) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
//            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }

		if(savedInstanceState==null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_placeholder, new DeckListFragment(), "DLF");
			ft.commitAllowingStateLoss();
		}
	}
}
