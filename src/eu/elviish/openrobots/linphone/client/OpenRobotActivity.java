package eu.elviish.openrobots.linphone.client;

import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferenceManager;
import org.linphone.LinphoneService;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.R;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCore;
import org.linphone.core.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Sylvain Berfini
 */
public class OpenRobotActivity extends Activity implements LinphoneOnCallStateChangedListener {
    private static final int video_activity = 100;
	private OrientationEventListener mOrientationHelper;
	private int mAlwaysChangingPhoneAngle = -1;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!LinphoneManager.isInstanciated()) {
			Log.e("No service running: avoid crash by starting the launcher", this.getClass().getName());

			finish();
			startActivity(getIntent().setClass(this, LauncherActivity.class));
			return;
		}
		setContentView(R.layout.main);

		LinphonePreferenceManager.getInstance(this);		
		LinphoneManager.addListener(this);
		
		LinphoneManager.getLc().enableVideo(true, true);
		
		findViewById(R.id.call).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startVideoActivity();
			}
		});
		findViewById(R.id.settings).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setClass(OpenRobotActivity.this, PreferencesActivity.class);
				startActivity(intent);
			}
		});
		findViewById(R.id.exit).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(new Intent(Intent.ACTION_MAIN).setClass(OpenRobotActivity.this, LinphoneService.class));
				finish();
			}
		});
	}

	@Override
	public void onCallStateChanged(LinphoneCall call, State state, String message) {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) {
			/* we are certainly exiting, ignore then.*/
			return;
		}
	}
	
	private void startVideoActivity() {
		startOrientationSensor();
		startActivityForResult(new Intent().setClass(OpenRobotActivity.this, VideoCallActivity.class), video_activity);
		LinphoneManager.getInstance().routeAudioToSpeaker();
	}
	
	/**
	 * Register a sensor to track phoneOrientation changes
	 */
	private synchronized void startOrientationSensor() {
		if (mOrientationHelper == null) {
			mOrientationHelper = new LocalOrientationEventListener(this);
		}
		mOrientationHelper.enable();
	}
	
	private class LocalOrientationEventListener extends OrientationEventListener {
		public LocalOrientationEventListener(Context context) {
			super(context);
		}
		
		@Override
		public void onOrientationChanged(final int o) {
			if (o == OrientationEventListener.ORIENTATION_UNKNOWN) return;

			int degrees = 270;
			if (o < 45 || o >315) degrees=0;
			else if (o<135) degrees=90;
			else if (o<225) degrees=180;

			if (mAlwaysChangingPhoneAngle == degrees) return;
			mAlwaysChangingPhoneAngle = degrees;

			Log.d("Phone orientation changed to ", degrees);
			int rotation = (360 - degrees) % 360;
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			if (lc != null){
				lc.setDeviceRotation(rotation);
				LinphoneCall currentCall = lc.getCurrentCall();
				if (currentCall != null && currentCall.cameraEnabled() && currentCall.getCurrentParamsCopy().getVideoEnabled()) {
					lc.updateCall(currentCall, null);
				}
			}
		}
	}
}
