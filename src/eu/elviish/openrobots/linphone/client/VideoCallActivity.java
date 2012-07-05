package eu.elviish.openrobots.linphone.client;

import org.linphone.LinphoneManager;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.CallManager;
import org.linphone.LinphoneUtils;
import org.linphone.R;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.Log;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * @author Sylvain Berfini
 */
public class VideoCallActivity extends Activity implements LinphoneOnCallStateChangedListener, OnClickListener {
	private LinphoneChatRoom chatRoom;
	private WakeLock mWakeLock;
	private SurfaceView mVideoView, mPreviewVideoView;
	private AndroidVideoWindowImpl androidVideoWindowImpl;
	
	private Handler mHandler = new Handler();
	
	private String adapterSIPAddress;
	private static final String forward = "z", back = "s", rotateLeft = "q", rotateRight = "d", stop = "p";
	
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!LinphoneManager.isInstanciated()) {
			Log.e("No service running: avoid crash by finishing ", getClass()
					.getName());
			
			finish();
			return;
		}

		setContentView(R.layout.video);
		LinphoneManager.addListener(this);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		adapterSIPAddress = prefs.getString(getString(R.string.pref_adapter_ip_key), null);
				
		mVideoView = (SurfaceView) findViewById(R.id.videoSurface);
		SurfaceView captureView = (SurfaceView) findViewById(R.id.videoPreviewSurface);
		captureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		fixZOrder(mVideoView, captureView);
		
		androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, captureView);
		androidVideoWindowImpl.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {
			public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				LinphoneManager.getLc().setVideoWindow(vw);
				mVideoView = surface;
			}

			public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				Log.d("VIDEO WINDOW destroyed!\n");
				LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull(); 
				if (lc != null) {
					lc.setVideoWindow(null);
				}
			}

			public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
				LinphoneManager.getLc().setPreviewWindow(null);
			}

			public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
				mPreviewVideoView = surface;
				LinphoneManager.getLc().setPreviewWindow(mPreviewVideoView);
			}
		});
		androidVideoWindowImpl.init();
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, Log.TAG);
		mWakeLock.acquire();
		
		// Force front camera if available
		int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
		videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
		LinphoneManager.getLc().setVideoDevice(videoDeviceId);
		CallManager.getInstance().updateCall();
		if (mPreviewVideoView != null)
			LinphoneManager.getLc().setPreviewWindow(mPreviewVideoView);
		
		initChatRoom();
		initLayout();
	}
	
	private void initChatRoom() {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (adapterSIPAddress != null && lc != null)
			chatRoom = lc.createChatRoom(adapterSIPAddress);
	}
	
	private void initLayout() {
		findViewById(R.id.forward).setOnClickListener(this);
		findViewById(R.id.back).setOnClickListener(this);
		findViewById(R.id.stop).setOnClickListener(this);
		findViewById(R.id.rotateLeft).setOnClickListener(this);
		findViewById(R.id.rotateRight).setOnClickListener(this);
	}
	
	private void terminateVideoCall(LinphoneCore lc) {
		if (lc.getCallsNb() > 0) {
			lc.terminateCall(lc.getCalls()[0]);
		}
	}
	
	private void startVideoCall(LinphoneCore lc) {
		if (adapterSIPAddress == null)
			return;
		
		try {
			lc.invite(adapterSIPAddress);
			LinphoneManager.getInstance().sendStaticImage(true);
		} catch (LinphoneCoreException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onCallStateChanged(LinphoneCall currentCall, State state, String message) {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) {
			/* we are certainly exiting, ignore then. */
			return;
		}
		
		if (state == State.CallIncomingEarlyMedia) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mVideoView.setVisibility(View.VISIBLE);
				}
			});
		} else if (state == State.Error) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(VideoCallActivity.this, getString(R.string.call_error).replace("%s", ""), Toast.LENGTH_LONG).show();
				}
			});
		}
	}
	
	void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.forward:
			if (chatRoom != null) {
				chatRoom.sendMessage(forward);
			}
			break;
			
		case R.id.back:
			if (chatRoom != null) {
				chatRoom.sendMessage(back);
			}
			
		case R.id.rotateLeft:
			if (chatRoom != null) {
				chatRoom.sendMessage(rotateLeft);
			}
			break;
			
		case R.id.rotateRight:
			if (chatRoom != null) {
				chatRoom.sendMessage(rotateRight);
			}
			break;
			
		case R.id.stop:
			if (chatRoom != null) {
				chatRoom.sendMessage(stop);
			}
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (LinphoneUtils.onKeyVolumeAdjust(keyCode)) return true;
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onResume() {		
		super.onResume();
		
		if (mVideoView != null)
			((GLSurfaceView) mVideoView).onResume();
		
		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
			}
		}		
		
		startVideoCall(LinphoneManager.getLc());
		LinphoneManager.getLc().getCurrentCall().enableCamera(true);
	}
	
	@Override
	protected void onDestroy() {
		if (androidVideoWindowImpl != null) { 
			// Prevent linphone from crashing if correspondent hang up while you are rotating
			androidVideoWindowImpl.release();
		}
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {		
		synchronized (androidVideoWindowImpl) {
			/*
			 * this call will destroy native opengl renderer which is used by
			 * androidVideoWindowImpl
			 */
			LinphoneManager.getLc().setVideoWindow(null);
		}
		
		if (mWakeLock.isHeld())
			mWakeLock.release();
		
		super.onPause();
		
		if (mVideoView != null)
			((GLSurfaceView) mVideoView).onPause();
		
		terminateVideoCall(LinphoneManager.getLc());
	}
}
