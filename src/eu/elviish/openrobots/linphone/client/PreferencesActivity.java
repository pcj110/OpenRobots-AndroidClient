package eu.elviish.openrobots.linphone.client;

import org.linphone.LinphoneException;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneManager.LinphoneConfigException;
import org.linphone.R;
import org.linphone.core.LinphoneCore;
import org.linphone.core.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
	
	public static void initializeIfNeeded(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean initialized = pref.getBoolean(context.getString(R.string.preferences_initialized), false);
		if (!initialized) {
			SharedPreferences.Editor editor = pref.edit();
			editor.putBoolean(context.getString(R.string.preferences_initialized), true);
	
			/* Audio */
			editor.putBoolean(context.getString(R.string.pref_codec_speex16_key), true);
			editor.putBoolean(context.getString(R.string.pref_codec_speex8_key), true);
			editor.putBoolean(context.getString(R.string.pref_codec_ilbc_key), true);
			editor.putBoolean(context.getString(R.string.pref_codec_g722_key), true);
			editor.putBoolean(context.getString(R.string.pref_codec_pcmu_key), true);
			editor.putBoolean(context.getString(R.string.pref_codec_pcma_key), true);
	
			/* Video */
			editor.putBoolean(context.getString(R.string.pref_video_enable_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_use_front_camera_key), false);
			editor.putBoolean(context.getString(R.string.pref_video_initiate_call_with_video_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_automatically_share_my_video_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_automatically_accept_video_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_codec_vp8_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_codec_h264_key), true);
			editor.putBoolean(context.getString(R.string.pref_video_codec_mpeg4_key), true);
			
			/* Network */
			editor.putBoolean(context.getString(R.string.pref_transport_tls_key), true);
			editor.putBoolean(context.getString(R.string.pref_transport_tcp_key), false);
			editor.putBoolean(context.getString(R.string.pref_transport_udp_key), false);
			
			/* Other */ 
			editor.putBoolean(context.getString(R.string.pref_autostart_key), false);
			editor.putBoolean(context.getString(R.string.pref_debug_key), true);
			editor.putString(context.getString(R.string.pref_media_encryption_key), context.getString(R.string.pref_media_encryption_key_srtp));
			
			editor.commit();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		if (!isFinishing()) return;

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		
		if (lc != null && (lc.isInComingInvitePending() || lc.isIncall())) {
			Log.w("Call in progress => settings not applied");
			return;
		}

		try {
			LinphoneManager.getInstance().initFromConf();
			lc.setVideoPolicy(LinphoneManager.getInstance().isAutoInitiateVideoCalls(), LinphoneManager.getInstance().isAutoAcceptCamera());
		} catch (LinphoneException e) {
			if (! (e instanceof LinphoneConfigException)) {
				Log.e(e, "Cannot update config");
				return;
			}
		}
	}
}
