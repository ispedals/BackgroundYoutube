package is.pedals.backgroundyoutube;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int PLAYLIST_ID = 823;
    private static final String TAG = "BackgroundYoutube";
    private static final boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) {
            new StartPlaybackTask().execute("https://www.youtube.com/watch?v=sHRzUEA5YfY");
            finish();
            return;
        }
        Intent intent = getIntent();
        if (intent.getAction() == null || !intent.getAction().equals(Intent.ACTION_SEND)) {
            finish();
            return;
        }
        String info = intent.getExtras().getString(Intent.EXTRA_TEXT);
        new StartPlaybackTask().execute(info);
        finish();

    }

    private class StartPlaybackTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... infos) {
            String info = infos[0];
            if (info == null) {
                return null;
            }
            Pattern videoIDPattern = Pattern.compile("([a-zA-Z0-9_-]{11})$");
            Matcher matcher = videoIDPattern.matcher(info);
            if (!matcher.find()) {
                return null;
            }
            String videoID = matcher.group(1);
            OkHttpClient client = new OkHttpClient();
            try {
                Request request = new Request.Builder()
                        .url("https://www.youtube.com/get_video_info?video_id=" + videoID)
                        .build();
                Response response = client.newCall(request).execute();
                Map<String, String> data = new HashMap<>();
                for (String item : response.body().string().split("&")) {
                    String[] keyvals = item.split("=");
                    data.put(keyvals[0], keyvals.length == 2 ? URLDecoder.decode(keyvals[1], "utf-8") : null);
                }
                String dashmpd = data.get("dashmpd");
                if (dashmpd == null) {
                    return null;
                }
                //dashmpd is double encoded, so we need to decode it again
                String url = URLDecoder.decode(dashmpd, "utf-8");
                String title = data.get("title");
                PlaylistManager playlistManager = App.getPlaylistManager();
                playlistManager.setMediaServiceClass(MediaPlayerService.class);
                List<MediaItem> mediaItems = new LinkedList<>();
                MediaItem mediaItem = new MediaItem(url, title);
                mediaItems.add(mediaItem);
                playlistManager.setParameters(mediaItems, 0);
                playlistManager.setPlaylistId(PLAYLIST_ID);
                playlistManager.play(0, false);

            } catch (IOException e) {
                Log.e(TAG, "http", e);
                return null;
            } catch (Exception ex) {
                Log.e(TAG, "http", ex);
                return null;
            }
            return null;

        }

    }


}
