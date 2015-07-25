package is.pedals.backgroundyoutube;

import android.app.Application;

public class App extends Application {

    private static App application;
    private static PlaylistManager playlistManager;

    public static PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public static App getApplication() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        playlistManager = new PlaylistManager();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        application = null;
        playlistManager = null;
    }
}