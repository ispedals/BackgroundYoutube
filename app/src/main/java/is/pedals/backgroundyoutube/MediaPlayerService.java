package is.pedals.backgroundyoutube;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.service.EMPlaylistService;
import com.devbrackets.android.exomedia.util.MediaUtil;

//copied from exomediademo/service/AudioService.java
public class MediaPlayerService extends EMPlaylistService<MediaItem, PlaylistManager> {

    private static final int NOTIFICATION_ID = 1564; //Arbitrary
    private static final int FOREGROUND_REQUEST_CODE = 332; //Arbitrary
    private static final float AUDIO_DUCK_VOLUME = 0.1f;

    @Override
    protected String getAppName() {
        return getResources().getString(R.string.app_name);
    }

    /*
    this method is private in EMPlaylistService, be we need to call this in our overridden playAudioItem() implementation,
    so we just copied it
     */
    void initializeAudioPlayer() {
        if (audioPlayer != null) {
            audioPlayer.reset();
            return;
        }

        audioPlayer = new EMAudioPlayer(getApplicationContext());
        audioPlayer.setBus(getBus());
        audioPlayer.startProgressPoll(this);
        audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        audioPlayer.setOnPreparedListener(audioListener);
        audioPlayer.setOnCompletionListener(audioListener);
        audioPlayer.setOnErrorListener(audioListener);
    }

    @Override
    /*
    we need to
     */
    protected void playAudioItem() {
        stopVideoPlayback();
        initializeAudioPlayer();
        audioFocusHelper.requestFocus();

        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioPlayer.setDataSource(this, Uri.parse(currentPlaylistItem.getMediaUrl()), MediaUtil.MediaType.DASH);

        setMediaState(MediaState.PREPARING);
        setupAsForeground();

        audioPlayer.prepareAsync();

        wifiLock.acquire();

    }

    @Override
    protected int getNotificationId() {
        return NOTIFICATION_ID;
    }

    @Override
    protected float getAudioDuckVolume() {
        return AUDIO_DUCK_VOLUME;
    }

    @Override
    protected PlaylistManager getMediaPlaylistManager() {
        return App.getPlaylistManager();
    }

    @Override
    protected PendingIntent getNotificationClickPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        return PendingIntent.getActivity(getApplicationContext(), FOREGROUND_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    @Override
    protected Bitmap getDefaultLargeNotificationImage() {
        return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
    }

    @Nullable
    @Override
    protected Bitmap getDefaultLargeNotificationSecondaryImage() {
        return null;
    }

    @Override
    protected int getNotificationIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected int getLockScreenIconRes() {
        return R.mipmap.ic_launcher;
    }
}

