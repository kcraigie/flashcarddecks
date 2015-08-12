package com.kcraigie.flashycards;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends android.support.v7.app.AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		if(savedInstanceState==null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_placeholder, new DeckListFragment(), "DLF");
			ft.commitAllowingStateLoss();
		}
	}
}
