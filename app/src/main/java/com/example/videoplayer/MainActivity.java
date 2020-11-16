package com.example.videoplayer;


import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.pallycon.widevinelibrary.PallyconDrmException;
import com.pallycon.widevinelibrary.PallyconEventListener;
import com.pallycon.widevinelibrary.PallyconWVMSDK;
import com.pallycon.widevinelibrary.PallyconWVMSDKFactory;
import com.pallycon.widevinelibrary.UnAuthorizedDeviceException;

import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private SimpleExoPlayer player;
    private SimpleExoPlayerView simpleExoPlayerView;
    private DataSource.Factory mediaDataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private MediaSource mediaSource;
    private String userAgent;

    // TODO : must implement ExoPlayer.EventListener
    Player.EventListener playerEventListener = new Player.DefaultEventListener() {
        @Override
        public void onPlayerError(ExoPlaybackException e) {
            String errorString;
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                errorString = cause.toString();

            } else if (e.type == ExoPlaybackException.TYPE_SOURCE) {
                Exception cause = e.getSourceException();
                errorString = cause.toString();

            } else if (e.type == ExoPlaybackException.TYPE_UNEXPECTED) {
                Exception cause = e.getUnexpectedException();
                errorString = cause.toString();
            } else {
                errorString = e.toString();
            }

            Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show();
        }
    };

    // TODO : must implement PallyconEventListener
    private PallyconEventListener pallyconEventListener = new PallyconEventListener() {
        @Override
        public void onDrmKeysLoaded(Map<String, String> licenseInfo) {
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDrmKeysRestored() {
        }

        @Override
        public void onDrmKeysRemoved() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy pol = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(pol);

        simpleExoPlayerView = findViewById(R.id.player_view);
        simpleExoPlayerView.requestFocus();

        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        mediaDataSourceFactory = buildDataSourceFactory(false);
        trackSelector = new DefaultTrackSelector();
        initializePlayer();
    }

    private void initializePlayer() {
        // TODO: 1. initialize PallyconWVM SDK
        String siteId = "";
        String siteKey = "";
        PallyconWVMSDK WVMAgent = null;
        try {
            WVMAgent = PallyconWVMSDKFactory.getInstance(this);
            WVMAgent.init(this, null, siteId, siteKey);
            WVMAgent.setPallyconEventListener(pallyconEventListener);
        } catch (PallyconDrmException e) {
            e.printStackTrace();
        } catch (UnAuthorizedDeviceException e) {
            e.printStackTrace();
        }

        // TODO : 2.set content information
        UUID drmSchemeUuid = UUID.fromString((C.WIDEVINE_UUID).toString());
        Uri uri = Uri.parse("");
        String drmLicenseUrl = "http://license.pallycon.com/ri/licenseManager.do";
        String cid = "FIRANGI";
        String userId = "lpa";
        String ptoken = "";



        // TODO : 3.set drm session manager
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
        DefaultRenderersFactory renderersFactory = null;
        try {
            drmSessionManager = WVMAgent.createDrmSessionManagerByToken(drmSchemeUuid, drmLicenseUrl, uri, userId, cid, ptoken, false);
            renderersFactory = new DefaultRenderersFactory(this,drmSessionManager);
        } catch (PallyconDrmException e) {
            e.printStackTrace();
        }

        // player setting
        player = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector);
        player.addListener(playerEventListener);
        simpleExoPlayerView.setPlayer(player);
        player.setPlayWhenReady(true);
        mediaSource = buildMediaSource(uri, null);
        player.prepare(mediaSource);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @SuppressWarnings("unchecked")
    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), buildDataSourceFactory(false)).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), buildDataSourceFactory(false)).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    public DataSource.Factory buildDataSourceFactory(TransferListener listener) {
        DefaultDataSourceFactory upstreamFactory = new DefaultDataSourceFactory(this, listener, buildHttpDataSourceFactory(listener));
        return upstreamFactory;
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(TransferListener listener) {
        return new DefaultHttpDataSourceFactory(userAgent, listener);
    }
}
