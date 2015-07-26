package is.pedals.backgroundyoutube;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.devbrackets.android.exomedia.service.EMPlaylistService;

//copied from exomediademo/service/AudioService.java
public class MediaPlayerService extends EMPlaylistService<MediaItem, PlaylistManager> {

    public static final String PLAY_IN_YOUTUBE = "playinyoutube";
    private static final String TAG = "MediaPlayerService";
    private static final int NOTIFICATION_ID = 1564; //Arbitrary
    private static final int FOREGROUND_REQUEST_CODE = 332; //Arbitrary
    private static final float AUDIO_DUCK_VOLUME = 0.1f;

    private Bitmap largeNotificationImage;

    @Override
    protected String getAppName() {
        return getResources().getString(R.string.app_name);
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
        intent.setAction(PLAY_IN_YOUTUBE);
        return PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Nullable
    @Override
    protected Bitmap getLargeNotificationImage() {
        return largeNotificationImage;
    }

    @Override
    protected Bitmap getDefaultLargeNotificationImage() {
        return null;
    }

    @Nullable
    @Override
    protected Bitmap getDefaultLargeNotificationSecondaryImage() {
        return null;
    }

    @Override
    protected void updateLargeNotificationImage(int size, MediaItem playlistItem) {
        Glide.with(getApplicationContext())
                .load(playlistItem.getThumbnailUrl())
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(size, size) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        largeNotificationImage = bitmap;
                        onLargeNotificationImageUpdated();
                    }
                });
    }

    @Override
    protected int getNotificationIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected int getLockScreenIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    //necessary as there is currently a bug in EMPlaylistService
    protected void mediaItemChanged(MediaItem currentItem) {
        currentMediaType = getMediaPlaylistManager().getCurrentItemType();

        //Validates that the currentPlaylistItem is for the currentItem
        if (!getMediaPlaylistManager().isPlayingItem(currentPlaylistItem)) {
            Log.d(TAG, "forcing currentPlaylistItem update");
            currentPlaylistItem = getMediaPlaylistManager().getCurrentItem();
        }

        //Starts the notification loading
        /*
        EMPlaylistService has the condition:
            !currentItem.getThumbnailUrl().equals(currentPlaylistItem.getThumbnailUrl())
        but for the first played item currentItem == currentPlaylistItem, which means the
        image isn't update
        */
        if (currentPlaylistItem != null) {
            int size = getResources().getDimensionPixelSize(com.devbrackets.android.exomedia.R.dimen.exomedia_big_notification_height);
            updateLargeNotificationImage(size, currentPlaylistItem);
        }

        //Starts the lock screen loading
        /*
        EMPlaylistService has the condition:
            !currentItem.getArtworkUrl().equalsIgnoreCase(currentPlaylistItem.getArtworkUrl())
        but for the first played item currentItem == currentPlaylistItem, which means the
        image isn't updated
        */
        if (currentPlaylistItem != null) {
            updateLockScreenArtwork(currentPlaylistItem);
        }

        postPlaylistItemChanged();
    }
}

