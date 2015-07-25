package is.pedals.backgroundyoutube;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class MediaItem implements EMPlaylistManager.PlaylistItem {

    final String url;
    final String title;

    public MediaItem(String url, String title) {
        this.url = url;
        this.title = title;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public long getPlaylistId() {
        return 0;
    }

    @Override
    public boolean isAudio() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public String getMediaUrl() {
        return url;
    }

    @Override
    public String getDownloadedMediaUri() {
        return null;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getThumbnailUrl() {
        return "";
    }

    @Override
    public String getArtworkUrl() {
        return "";
    }
}
