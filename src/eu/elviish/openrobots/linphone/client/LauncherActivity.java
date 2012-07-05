package eu.elviish.openrobots.linphone.client;

import org.linphone.LinphoneLauncherActivity;
import org.linphone.LinphoneService;

import android.content.Intent;
import android.os.Bundle;

/**
 * @author Sylvain Berfini
 */
public class LauncherActivity extends LinphoneLauncherActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PreferencesActivity.initializeIfNeeded(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onServiceReady() {
		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(OpenRobotActivity.class);
		
		startActivity(new Intent()
		.setClass(this, OpenRobotActivity.class)
		.setData(getIntent().getData()));

		finish();
	}
}
