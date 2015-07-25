/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.devbrackets.android.exomedia.builder;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.util.Log;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.listener.RendererBuilderCallback;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.chunk.VideoFormatSelectorUtil;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver.UtcTimingCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.ttml.TtmlParser;
import com.google.android.exoplayer.text.webvtt.WebvttParser;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

    /*
    A DashRendererBuilder
    copied from https://github.com/google/ExoPlayer/blob/master/demo/src/main/java/com/google/android/exoplayer/demo/player/DashRendererBuilder.java@r.1.4.1

    Changes are:
    * Uses com.devbrakets.android.exomedia.builder.RenderBuilder as a base class
        -the fields Context context, String userAgent, String url are not stored directly in the class
    * No DRM support
        -the field MediaDrmCallback drmCallback has been removed
         -MediaDrmCallback drmCallback has been removed as a parameter for the constructor
         -buildRenderers() has been modified to throw UnsupportedDrmException if content protection is present
    * Change references to DemoPlayer to com.devbrakets.android.exomedia.exoplayer.EMExoPlayer
        -change type
        -change the name of constants in buildRenderers()
     */

public class DashRendererBuilder extends RenderBuilder implements
        ManifestCallback<MediaPresentationDescription>, UtcTimingCallback {

    private static final String TAG = "DashRendererBuilder";

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 60;
    private static final int TEXT_BUFFER_SEGMENTS = 2;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private static final int SECURITY_LEVEL_UNKNOWN = -1;
    private static final int SECURITY_LEVEL_1 = 1;
    private static final int SECURITY_LEVEL_3 = 3;

    /**
     * Passthrough audio formats (encodings) in order of decreasing priority.
     */
    private static final int[] PASSTHROUGH_ENCODINGS_PRIORITY =
            new int[]{C.ENCODING_E_AC3, C.ENCODING_AC3};
    /**
     * Passthrough audio codecs corresponding to the encodings in
     * {@link #PASSTHROUGH_ENCODINGS_PRIORITY}.
     */
    private static final String[] PASSTHROUGH_CODECS_PRIORITY =
            new String[]{"ec-3", "ac-3"};

    private final AudioCapabilities audioCapabilities;

    private EMExoPlayer player;
    private RendererBuilderCallback callback;
    private ManifestFetcher<MediaPresentationDescription> manifestFetcher;
    private UriDataSource manifestDataSource;

    private MediaPresentationDescription manifest;
    private long elapsedRealtimeOffset;

    public DashRendererBuilder(Context context, String userAgent, String url, AudioCapabilities audioCapabilities) {
        super(context, userAgent, url);
        this.audioCapabilities = audioCapabilities;
    }

    @Override
    public void buildRenderers(EMExoPlayer player, RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;
        MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
        manifestDataSource = new DefaultUriDataSource(context, userAgent);
        manifestFetcher = new ManifestFetcher<>(uri, manifestDataSource, parser);
        manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    @Override
    public void onSingleManifest(MediaPresentationDescription manifest) {
        this.manifest = manifest;
        if (manifest.dynamic && manifest.utcTiming != null) {
            UtcTimingElementResolver.resolveTimingElement(manifestDataSource, manifest.utcTiming,
                    manifestFetcher.getManifestLoadTimestamp(), this);
        } else {
            buildRenderers();
        }
    }

    @Override
    public void onSingleManifestError(IOException e) {
        callback.onRenderersError(e);
    }

    @Override
    public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
        this.elapsedRealtimeOffset = elapsedRealtimeOffset;
        buildRenderers();
    }

    @Override
    public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
        Log.e(TAG, "Failed to resolve UtcTiming element [" + utcTiming + "]", e);
        // Be optimistic and continue in the hope that the device clock is correct.
        buildRenderers();
    }

    private void buildRenderers() {
        Period period = manifest.periods.get(0);
        Handler mainHandler = player.getMainHandler();
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);

        boolean hasContentProtection = false;
        int videoAdaptationSetIndex = period.getAdaptationSetIndex(AdaptationSet.TYPE_VIDEO);
        int audioAdaptationSetIndex = period.getAdaptationSetIndex(AdaptationSet.TYPE_AUDIO);
        AdaptationSet videoAdaptationSet = null;
        AdaptationSet audioAdaptationSet = null;
        if (videoAdaptationSetIndex != -1) {
            videoAdaptationSet = period.adaptationSets.get(videoAdaptationSetIndex);
            hasContentProtection |= videoAdaptationSet.hasContentProtection();
        }
        if (audioAdaptationSetIndex != -1) {
            audioAdaptationSet = period.adaptationSets.get(audioAdaptationSetIndex);
            hasContentProtection |= audioAdaptationSet.hasContentProtection();
        }

        // Fail if we have neither video or audio.
        if (videoAdaptationSet == null && audioAdaptationSet == null) {
            callback.onRenderersError(new IllegalStateException("No video or audio adaptation sets"));
            return;
        }

        // Check drm support if necessary.
        boolean filterHdContent = false;
        StreamingDrmSessionManager drmSessionManager = null;
        if (hasContentProtection) {
            callback.onRenderersError(
                    new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
            return;
        }

        // Determine which video representations we should use for playback.
        int[] videoRepresentationIndices = null;
        if (videoAdaptationSet != null) {
            try {
                videoRepresentationIndices = VideoFormatSelectorUtil.selectVideoFormatsForDefaultDisplay(
                        context, videoAdaptationSet.representations, null, filterHdContent);
            } catch (DecoderQueryException e) {
                callback.onRenderersError(e);
                return;
            }
        }

        // Build the video renderer.
        final MediaCodecVideoTrackRenderer videoRenderer;
        if (videoRepresentationIndices == null || videoRepresentationIndices.length == 0) {
            videoRenderer = null;
        } else {
            DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher,
                    videoAdaptationSetIndex, videoRepresentationIndices, videoDataSource,
                    new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset,
                    mainHandler, player);
            ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
                    VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, player,
                    EMExoPlayer.RENDER_VIDEO_INDEX);
            videoRenderer = new MediaCodecVideoTrackRenderer(videoSampleSource, drmSessionManager, true,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, mainHandler, player, 50);
        }

        // Build the audio chunk sources.
        List<ChunkSource> audioChunkSourceList = new ArrayList<>();
        List<String> audioTrackNameList = new ArrayList<>();
        if (audioAdaptationSet != null) {
            DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            FormatEvaluator audioEvaluator = new FormatEvaluator.FixedEvaluator();
            List<Representation> audioRepresentations = audioAdaptationSet.representations;
            List<String> codecs = new ArrayList<>();
            for (int i = 0; i < audioRepresentations.size(); i++) {
                Format format = audioRepresentations.get(i).format;
                audioTrackNameList.add(format.id + " (" + format.numChannels + "ch, " +
                        format.audioSamplingRate + "Hz)");
                audioChunkSourceList.add(new DashChunkSource(manifestFetcher, audioAdaptationSetIndex,
                        new int[]{i}, audioDataSource, audioEvaluator, LIVE_EDGE_LATENCY_MS,
                        elapsedRealtimeOffset, mainHandler, player));
                codecs.add(format.codecs);
            }

            if (audioCapabilities != null) {
                // If there are any passthrough audio encodings available, select the highest priority
                // supported format (e.g. E-AC-3) and remove other tracks.
                for (int i = 0; i < PASSTHROUGH_CODECS_PRIORITY.length; i++) {
                    String codec = PASSTHROUGH_CODECS_PRIORITY[i];
                    int encoding = PASSTHROUGH_ENCODINGS_PRIORITY[i];
                    if (codecs.indexOf(codec) == -1 || !audioCapabilities.supportsEncoding(encoding)) {
                        continue;
                    }

                    for (int j = audioRepresentations.size() - 1; j >= 0; j--) {
                        if (!audioRepresentations.get(j).format.codecs.equals(codec)) {
                            audioTrackNameList.remove(j);
                            audioChunkSourceList.remove(j);
                        }
                    }
                    break;
                }
            }
        }

        // Build the audio renderer.
        final String[] audioTrackNames;
        final MultiTrackChunkSource audioChunkSource;
        final TrackRenderer audioRenderer;
        if (audioChunkSourceList.isEmpty()) {
            audioTrackNames = null;
            audioChunkSource = null;
            audioRenderer = null;
        } else {
            audioTrackNames = new String[audioTrackNameList.size()];
            audioTrackNameList.toArray(audioTrackNames);
            audioChunkSource = new MultiTrackChunkSource(audioChunkSourceList);
            SampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                    AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, player,
                    EMExoPlayer.RENDER_AUDIO_INDEX);
            audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource, drmSessionManager, true,
                    mainHandler, player);
        }

        // Build the text chunk sources.
        DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        FormatEvaluator textEvaluator = new FormatEvaluator.FixedEvaluator();
        List<ChunkSource> textChunkSourceList = new ArrayList<>();
        List<String> textTrackNameList = new ArrayList<>();
        for (int i = 0; i < period.adaptationSets.size(); i++) {
            AdaptationSet adaptationSet = period.adaptationSets.get(i);
            if (adaptationSet.type == AdaptationSet.TYPE_TEXT) {
                List<Representation> representations = adaptationSet.representations;
                for (int j = 0; j < representations.size(); j++) {
                    Representation representation = representations.get(j);
                    textTrackNameList.add(representation.format.id);
                    textChunkSourceList.add(new DashChunkSource(manifestFetcher, i, new int[]{j},
                            textDataSource, textEvaluator, LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset,
                            mainHandler, player));
                }
            }
        }

        // Build the text renderers
        final String[] textTrackNames;
        final MultiTrackChunkSource textChunkSource;
        final TrackRenderer textRenderer;
        if (textChunkSourceList.isEmpty()) {
            textTrackNames = null;
            textChunkSource = null;
            textRenderer = null;
        } else {
            textTrackNames = new String[textTrackNameList.size()];
            textTrackNameList.toArray(textTrackNames);
            textChunkSource = new MultiTrackChunkSource(textChunkSourceList);
            SampleSource textSampleSource = new ChunkSampleSource(textChunkSource, loadControl,
                    TEXT_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, player,
                    EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX);
            textRenderer = new TextTrackRenderer(textSampleSource, player, mainHandler.getLooper(),
                    new TtmlParser(), new WebvttParser());
        }

        // Invoke the callback.
        String[][] trackNames = new String[EMExoPlayer.RENDER_COUNT][];
        trackNames[EMExoPlayer.RENDER_AUDIO_INDEX] = audioTrackNames;
        trackNames[EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX] = textTrackNames;

        MultiTrackChunkSource[] multiTrackChunkSources =
                new MultiTrackChunkSource[EMExoPlayer.RENDER_COUNT];
        multiTrackChunkSources[EMExoPlayer.RENDER_AUDIO_INDEX] = audioChunkSource;
        multiTrackChunkSources[EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX] = textChunkSource;

        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        renderers[EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX] = textRenderer;
        callback.onRenderers(trackNames, multiTrackChunkSources, renderers);
    }

    private static int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
        String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
        return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1 : securityLevelProperty
                .equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
    }

}
