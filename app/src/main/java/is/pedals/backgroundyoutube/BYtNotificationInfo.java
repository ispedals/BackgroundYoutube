package is.pedals.backgroundyoutube;

/*
 * Copyright (C) 2015 Brian Wernick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.EMNotification;

/**
 Essentially a copy-paste of NotificationInfo with the position and duration fields added
 and the method getPercentage()
 */
class BYtNotificationInfo {
    private String title;
    private String content;

    private Bitmap largeImage;
    private Bitmap secondaryImage;

    @DrawableRes
    private int appIcon;
    private int notificationId;

    private boolean showNotifications;

    private PendingIntent pendingIntent;

    private EMNotification.NotificationMediaState mediaState;

    //a negative duration means that it is unknown and the position is meaningless
    private long position = -1, duration = -1;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLargeImage(@Nullable Bitmap largeImage) {
        this.largeImage = largeImage;
    }

    public void setSecondaryImage(@Nullable Bitmap secondaryImage) {
        this.secondaryImage = secondaryImage;
    }

    public void setAppIcon(@DrawableRes int appIcon) {
        this.appIcon = appIcon;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public void setShowNotifications(boolean showNotifications) {
        this.showNotifications = showNotifications;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public void setMediaState(@Nullable EMNotification.NotificationMediaState mediaState) {
        this.mediaState = mediaState;
    }

    public void setTime(long position, long duration) {
        this.position = position;
        this.duration = duration;
    }

    @NonNull
    public String getTitle() {
        return title != null ? title : "";
    }

    @NonNull
    public String getContent() {
        return content != null ? content : "";
    }

    @Nullable
    public Bitmap getLargeImage() {
        return largeImage;
    }

    @Nullable
    public Bitmap getSecondaryImage() {
        return secondaryImage;
    }

    @DrawableRes
    public int getAppIcon() {
        return appIcon;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public boolean getShowNotifications() {
        return showNotifications;
    }

    @Nullable
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Nullable
    public EMNotification.NotificationMediaState getMediaState() {
        return mediaState;
    }

    public long getPosition() {
        return position;
    }

    public long getDuration() {
        return duration;
    }

    public int getPercentage() {
        if (duration > 0) {
            return (int) (100 * position / duration);
        }
        return -1;
    }

}