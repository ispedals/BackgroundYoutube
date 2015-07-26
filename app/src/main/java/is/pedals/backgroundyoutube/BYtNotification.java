package is.pedals.backgroundyoutube;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.devbrackets.android.exomedia.EMNotification;
import com.devbrackets.android.exomedia.EMRemoteActions;

/*
Essentially a copy-paste of EMNotification with these differences:
    -uses our custom BYtNotificationInfo class
    -sets the position and duration to the BYtNotificationInfo in updateNotificationInformation()
    -uses our custom bigview in getBigNotification() where it also updates the progressbar
    -removed NotificationMediaState so that the original in EMNotification is used
 */
public class BYtNotification extends EMNotification {
    private Context context;
    private NotificationManager notificationManager;
    private BYtNotificationInfo notificationInfo = new BYtNotificationInfo();

    private Class<? extends Service> mediaServiceClass;
    private RemoteViews bigContent;

    public BYtNotification(Context context) {
        super(context);
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Sets weather notifications are shown when audio is playing or
     * ready for playback (e.g. paused).  The notification information
     * will need to be updated by calling {@link #setNotificationBaseInformation(int, int)}
     * and {@link #updateNotificationInformation(String, String, Bitmap, Bitmap)} and can be retrieved
     * with {@link #getNotification(android.app.PendingIntent)}
     *
     * @param enabled True if notifications should be shown
     */
    public void setNotificationsEnabled(boolean enabled) {
        if (enabled == notificationInfo.getShowNotifications()) {
            return;
        }

        notificationInfo.setShowNotifications(enabled);

        //Remove the notification when disabling
        if (!enabled) {
            notificationManager.cancel(notificationInfo.getNotificationId());
        }
    }

    /**
     * Sets the basic information for the notification that doesn't need to be updated.  To enable the big
     * notification you will need to use {@link #setNotificationBaseInformation(int, int, Class)} instead
     *
     * @param notificationId The ID to specify this notification
     * @param appIcon        The applications icon resource
     */
    public void setNotificationBaseInformation(int notificationId, @DrawableRes int appIcon) {
        setNotificationBaseInformation(notificationId, appIcon, null);
    }

    /**
     * Sets the basic information for the notification that doesn't need to be updated.  Additionally, when
     * the mediaServiceClass is set the big notification will send intents to that service to notify of
     * button clicks.  These intents will have an action from
     * <ul>
     * <li>{@link EMRemoteActions#ACTION_STOP}</li>
     * <li>{@link EMRemoteActions#ACTION_PLAY_PAUSE}</li>
     * <li>{@link EMRemoteActions#ACTION_PREVIOUS}</li>
     * <li>{@link EMRemoteActions#ACTION_NEXT}</li>
     * </ul>
     *
     * @param notificationId    The ID to specify this notification
     * @param appIcon           The applications icon resource
     * @param mediaServiceClass The class for the service to notify of big notification actions
     */
    public void setNotificationBaseInformation(int notificationId, @DrawableRes int appIcon, @Nullable Class<? extends Service> mediaServiceClass) {
        notificationInfo.setNotificationId(notificationId);
        notificationInfo.setAppIcon(appIcon);
        this.mediaServiceClass = mediaServiceClass;
    }

    /**
     * Sets the volatile information for the notification.  This information is expected to
     * change frequently.
     *
     * @param title                      The title to display for the notification (e.g. A song name)
     * @param content                    A short description or additional information for the notification (e.g. An artists name)
     * @param notificationImage          An image to display on the notification (e.g. Album artwork)
     * @param secondaryNotificationImage An image to display on the notification should be used to indicate playback type (e.g. Chromecast)
     */
    public void updateNotificationInformation(String title, String content, @Nullable Bitmap notificationImage, @Nullable Bitmap secondaryNotificationImage) {
        updateNotificationInformation(title, content, notificationImage, secondaryNotificationImage, null);
    }

    /**
     * Sets the volatile information for the notification.  This information is expected to
     * change frequently.
     *
     * @param title                      The title to display for the notification (e.g. A song name)
     * @param content                    A short description or additional information for the notification (e.g. An artists name)
     * @param notificationImage          An image to display on the notification (e.g. Album artwork)
     * @param secondaryNotificationImage An image to display on the notification should be used to indicate playback type (e.g. Chromecast)
     * @param notificationMediaState     The current media state for the expanded (big) notification
     */
    public void updateNotificationInformation(String title, String content, @Nullable Bitmap notificationImage, @Nullable Bitmap secondaryNotificationImage,
                                              @Nullable NotificationMediaState notificationMediaState) {
        //a negative duration means that the duration is unknown and the position is meaningless
        updateNotificationInformation(title, content, notificationImage, secondaryNotificationImage, null, -1, -1);

    }


    public void updateNotificationInformation(String title, String content, @Nullable Bitmap notificationImage, @Nullable Bitmap secondaryNotificationImage,
                                              @Nullable NotificationMediaState notificationMediaState, long position, long duration) {
        notificationInfo.setTitle(title);
        notificationInfo.setContent(content);
        notificationInfo.setLargeImage(notificationImage);
        notificationInfo.setSecondaryImage(secondaryNotificationImage);
        notificationInfo.setMediaState(notificationMediaState);
        notificationInfo.setTime(position, duration);

        if (notificationInfo.getShowNotifications()) {
            notificationManager.notify(notificationInfo.getNotificationId(), getNotification(notificationInfo.getPendingIntent()));
        }
    }

    /**
     * Returns a fully constructed notification to use when moving a service to the
     * foreground.  This should be called after the notification information is set with
     * {@link #setNotificationBaseInformation(int, int)} and {@link #updateNotificationInformation(String, String, Bitmap, Bitmap)}.
     *
     * @param pendingIntent The pending intent to use when the notification itself is clicked
     * @return The constructed notification
     */
    public Notification getNotification(@Nullable PendingIntent pendingIntent) {
        notificationInfo.setPendingIntent(pendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(notificationInfo.getTitle());
        builder.setContentText(notificationInfo.getContent());

        builder.setSmallIcon(notificationInfo.getAppIcon());
        builder.setLargeIcon(notificationInfo.getLargeImage());
        builder.setOngoing(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        //Set the notification category on lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        //Build the notification and set the expanded content view if there is a service to inform of clicks
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && mediaServiceClass != null) {
            notification.bigContentView = getBigNotification();
        }

        return notification;
    }

    /**
     * Creates the RemoteViews used for the expanded (big) notification
     *
     * @return The resulting RemoteViews
     */
    private RemoteViews getBigNotification() {
        if (bigContent == null) {
            bigContent = new RemoteViews(context.getPackageName(), R.layout.backgroundyoutube_big_notification_content);
            bigContent.setOnClickPendingIntent(R.id.backgroundyoutube_notification_close, createPendingIntent(EMRemoteActions.ACTION_STOP, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.backgroundyoutube_notification_playpause, createPendingIntent(EMRemoteActions.ACTION_PLAY_PAUSE, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.backgroundyoutube_notification_next, createPendingIntent(EMRemoteActions.ACTION_NEXT, mediaServiceClass));
            bigContent.setOnClickPendingIntent(R.id.backgroundyoutube_notification_prev, createPendingIntent(EMRemoteActions.ACTION_PREVIOUS, mediaServiceClass));
        }

        bigContent.setTextViewText(R.id.backgroundyoutube_notification_title, notificationInfo.getTitle());
        bigContent.setTextViewText(R.id.backgroundyoutube_notification_content_text, notificationInfo.getContent());
        bigContent.setBitmap(R.id.backgroundyoutube_notification_large_image, "setImageBitmap", notificationInfo.getLargeImage());
        bigContent.setBitmap(R.id.backgroundyoutube_notification_secondary_image, "setImageBitmap", notificationInfo.getSecondaryImage());

        //Makes sure the play/pause, next, and previous are displayed correctly
        if (notificationInfo.getMediaState() != null) {
            updateMediaState(bigContent);
        }

        int percentage = notificationInfo.getPercentage();
        if (percentage > -1) {
            bigContent.setProgressBar(R.id.progressbar, 100, percentage, false);
        }
        return bigContent;
    }

    /**
     * Updates the images for the play/pause, next, and previous buttons so that only valid ones are
     * displayed with the correct state.
     *
     * @param bigContent The RemoteViews to use to modify the state
     */
    private void updateMediaState(RemoteViews bigContent) {
        NotificationMediaState state = notificationInfo.getMediaState();
        if (bigContent == null || state == null) {
            return;
        }

        bigContent.setImageViewResource(R.id.backgroundyoutube_notification_playpause, state.isPlaying() ? com.devbrackets.android.exomedia.R.drawable.exomedia_notification_pause
                : com.devbrackets.android.exomedia.R.drawable.exomedia_notification_play);

        bigContent.setInt(R.id.backgroundyoutube_notification_prev, "setVisibility", state.isPreviousEnabled() ? View.VISIBLE : View.INVISIBLE);
        bigContent.setInt(R.id.backgroundyoutube_notification_next, "setVisibility", state.isNextEnabled() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Creates a PendingIntent for the given action to the specified service
     *
     * @param action       The action to use
     * @param serviceClass The service class to notify of intents
     * @return The resulting PendingIntent
     */
    private PendingIntent createPendingIntent(String action, Class<? extends Service> serviceClass) {
        Intent intent = new Intent(context, serviceClass);
        intent.setAction(action);

        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
