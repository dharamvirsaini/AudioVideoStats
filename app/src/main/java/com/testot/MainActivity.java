package com.testot;

import android.Manifest;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Session.ReconnectionListener;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Publisher.PublisherListener, Subscriber.VideoListener, View.OnClickListener, Session.SignalListener, ReconnectionListener{

    private static final String TAG = "screen-sharing " + MainActivity.class.getSimpleName();

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;

    private LinearLayout mPublisherViewContainer;
    private RelativeLayout mParent;
    private LinearLayout mSubscriberViewContainer;
    private Subscriber mSubscriber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
Button share = (Button) findViewById(R.id.Camera);
        Button camera = (Button) findViewById(R.id.two);
        share.setOnClickListener(this);
        camera.setOnClickListener(this);

        mPublisherViewContainer = (LinearLayout) findViewById(R.id.ll_start_call_publisher_view);
        mSubscriberViewContainer = (LinearLayout) findViewById(R.id.ll_start_call_subscriber_view);
        mParent = (RelativeLayout) findViewById(R.id.parent);

        requestPermissions();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");

        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    private void attachSubscriberView(Subscriber subscriber) {
        showLog("attachSubscriberView");
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        //dettachSubscriberView(subscriber);
        mSubscriberViewContainer.removeAllViews();
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onPause");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            mSession = new Session(MainActivity.this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.setReconnectionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());


        mPublisher = new Publisher(MainActivity.this, "publisher");
        mPublisher.setPublisherListener(this);
        //mPublisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeCamera);
        mPublisher.setAudioFallbackEnabled(false);
       // mPublisherViewContainer.removeAllViews();
        mPublisherViewContainer.addView(mPublisher.getView());
       // mPublisher.startPreview();
       mSession.publish(mPublisher);

        AudioManager.OnAudioFocusChangeListener afChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            System.out.println("AUDIOFOCUS_LOSS_TRANSIENT");
                            // Pause playback
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            System.out.println("AUDIOFOCUS_GAIN");
                            // Resume playback
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            System.out.println("AUDIOFOCUS_LOSS");

                        }
                    }
                };
    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

        mSession = null;
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        System.out.println("onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        //finish();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        if(mSubscriber != null)
        mSession.unsubscribe(mSubscriber);
        subscribeToStream(stream);
    }

    private void subscribeToStream(Stream stream) {
        showLog("subscribeToStream");
        mSubscriber = new Subscriber(MainActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }

    void showLog(String s) {
        Log.w("Varshit Jain", s);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        mPublisherViewContainer.removeAllViews();
        showLog("onStreamDropped");
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        showLog("onStreamCreated");

        if (mSubscriber == null) {
            subscribeToStream(stream);
        }
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Publisher Session error. See the logcat please.", Toast.LENGTH_LONG).show();
       // finish();
    }

    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }

    void A() {
        ScreensharingCapturer screenCapturer = new ScreensharingCapturer(MainActivity.this);
        mSession.unpublish(mPublisher);
        mPublisher = new Publisher(MainActivity.this, "a", screenCapturer);
        mPublisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);
        mPublisher.setAudioFallbackEnabled(false);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.removeAllViews();
        mPublisherViewContainer.addView(mPublisher.getView());
        mSession.publish(mPublisher);
    }

    void B() {
        mSession.unpublish(mPublisher);
        mPublisher = new Publisher(MainActivity.this, "publisher");
        mPublisher.setPublisherListener(this);
        mPublisher.setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeCamera);
        mPublisher.setAudioFallbackEnabled(false);
        mPublisherViewContainer.removeAllViews();
        mPublisherViewContainer.addView(mPublisher.getView());

        mSession.publish(mPublisher);
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        showLog("onVideoDataReceived");
        attachSubscriberView(mSubscriber);
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }

    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.Camera:
            {
                ((ScreensharingCapturer)mPublisher.getCapturer()).swapCamera(1);
               // public static Bitmap getScreenShot(View view) {
               // View screenView = view.getRootView();
               // mSession.
              //  mPublisher.getCapture
               // return bitmap;

               // mSession.sendSignal("chat", "Hello from saini");
                //A();
                break;
            }

            case R.id.two:
            {
                B();
                break;
            }
        }
    }

    public  void store(Bitmap bm){
        try {
            // image naming and path  to include sd card  appending name you choose for file
            //Utils.toastL(getApplicationContext(),"ScreenShot Taken");
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + "my" + Integer.toString((int)Math.random()) + ".jpg";

            // create bitmap screen capture


            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bm.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            // openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    @Override
    public void onSignalReceived(Session session, String s, String s1, Connection connection) {
        System.out.println("msg received is " + s1);
    }

    @Override
    public void onReconnecting(Session session) {
        System.out.println("onReconnecting");
    }

    @Override
    public void onReconnected(Session session) {
        System.out.println("onReconnected");
    }
}