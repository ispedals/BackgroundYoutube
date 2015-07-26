package is.pedals.backgroundyoutube;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class MediaItem implements EMPlaylistManager.PlaylistItem {

    private final String url;
    private final String title;
    private final String thumbnail;
    private final String id;

    public MediaItem(String url, String title, String thumbnail, String id) {
        this.url = url;
        this.title = title;
        this.thumbnail = thumbnail;
        this.id = id;
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
        return thumbnail;
    }

    @Override
    public String getArtworkUrl() {
        return "";
    }

    public String getVideoId() {
        return id;
    }
}
