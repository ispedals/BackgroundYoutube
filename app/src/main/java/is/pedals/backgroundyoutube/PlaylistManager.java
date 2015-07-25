package is.pedals.backgroundyoutube;

import android.app.Application;
import android.app.Service;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class PlaylistManager extends EMPlaylistManager<MediaItem> {

    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return MediaPlayerService.class;
    }
}