package com.tapshield.android.ui.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.tapshield.android.R;
import com.tapshield.android.ui.fragment.BaseDestinationPickFragment.DestinationPickListener;
import com.tapshield.android.ui.fragment.ContactDestinationPickFragment;
import com.tapshield.android.ui.fragment.PlaceDestinationPickFragment;

public class PickDestinationActivity extends Activity implements DestinationPickListener {

	private AlertDialog mModeDialog;
	private String mDestination;
	
	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setTabs();
		mModeDialog = getModeDialog();
	}
	
	private void setTabs() {
		ActionBar actionBar = getActionBar();

		Tab places = actionBar.newTab()
				.setIcon(R.drawable.ic_pin)
				.setTabListener(
						new TabListener<PlaceDestinationPickFragment>(
								this,
								"places",
								PlaceDestinationPickFragment.class));
		
		Tab contacts = actionBar.newTab()
				.setIcon(R.drawable.ic_actionbar_people)
				.setTabListener(
						new TabListener<ContactDestinationPickFragment>(
								this,
								"contacts",
								ContactDestinationPickFragment.class));
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.addTab(places);
		actionBar.addTab(contacts);
	}
	
	private AlertDialog getModeDialog() {
		return new AlertDialog.Builder(this)
				.setTitle(R.string.ts_fragment_pickdestination_dialog_mode_title)
				.setMessage(R.string.ts_fragment_pickdestination_dialog_mode_message)
				.setPositiveButton(R.string.ts_fragment_pickdestination_dialog_mode_button_driving,
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startRouteSelection(PickRouteActivity.MODE_DRIVING, mDestination);
							}
						})
				.setNeutralButton(R.string.ts_fragment_pickdestination_dialog_mode_button_walking,
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startRouteSelection(PickRouteActivity.MODE_WALKING, mDestination);
							}
						})
				.setNegativeButton(R.string.ts_common_cancel, null)
				.create();
	}
	
	@Override
	public void onDestinationPick(String destination) {
		mDestination = destination;
		mModeDialog.show();
	}
	
	private void startRouteSelection(final int mode, final String destination) {
		Intent pickRoute = new Intent(this, PickRouteActivity.class);
		pickRoute.putExtra(PickRouteActivity.EXTRA_MODE, mode);
		pickRoute.putExtra(PickRouteActivity.EXTRA_DESTINATION, destination);
		startActivity(pickRoute);
	}

	//from http://developer.android.com/guide/topics/ui/actionbar.html
    //as of april 24 2014
	private class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private Fragment mFragment;
	    private final Activity mActivity;
	    private final String mTag;
	    private final Class<T> mClass;

	    /** Constructor used each time a new tab is created.
	      * @param activity  The host Activity, used to instantiate the fragment
	      * @param tag  The identifier tag for the fragment
	      * @param clz  The fragment's Class, used to instantiate the fragment
	      */
	    public TabListener(Activity activity, String tag, Class<T> cls) {
	        mActivity = activity;
	        mTag = tag;
	        mClass = cls;
	    }

	    /* The following are each of the ActionBar.TabListener callbacks */

	    public void onTabSelected(Tab tab, FragmentTransaction ft) {
	        // Check if the fragment is already initialized
	        if (mFragment == null) {
	            // If not, instantiate and add it to the activity
	            mFragment = Fragment.instantiate(mActivity, mClass.getName());
	            ft.add(android.R.id.content, mFragment, mTag);
	        } else {
	            // If it exists, simply attach it in order to show it
	            ft.attach(mFragment);
	        }
	    }

	    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	        if (mFragment != null) {
	            // Detach the fragment, because another one is being attached
	            ft.detach(mFragment);
	        }
	    }

	    public void onTabReselected(Tab tab, FragmentTransaction ft) {
	        // User selected the already selected tab. Usually do nothing.
	    }
	}
}
