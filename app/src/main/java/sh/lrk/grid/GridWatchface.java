package sh.lrk.grid;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/*
   Copyright 2020 Lukas Fülling (lukas@k40s.net)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
public class GridWatchface extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MILLISECONDS.toMillis(30);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<GridWatchface.Engine> reference;

        public EngineHandler(GridWatchface.Engine reference) {
            this.reference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            GridWatchface.Engine engine = reference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler updateHandler = new EngineHandler(this);
        private Calendar calendar;
        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean tzrRegistered = false;
        private boolean muteMode;
        private float centerX;
        private float centerY;
        private boolean ambientMode;
        private boolean lowBitAmbient;
        private boolean burnInProtection;
        private Paint backgroundPaint;//TODO add property for this
        private Paint gridPaint; //TODO add property for this
        private TextPaint textPaint; //TODO add property for this
        private boolean use24h; //TODO add property for this
        private int gridOffset = 0;
        private float textSize = 48f;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(GridWatchface.this)
                    .setAcceptsTapEvents(true)
                    .build());

            calendar = Calendar.getInstance();

            initializeWatchFace();
        }

        private void initializeWatchFace() {
            /* Set defaults for colors */
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);
            gridPaint = new Paint();
            gridPaint.setColor(Color.BLUE);
            textPaint = new TextPaint();
            textPaint.setColor(Color.argb(200, 255, 255, 255));
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "TRON.TTF"));
        }

        @Override
        public void onDestroy() {
            updateHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            ambientMode = inAmbientMode;

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (ambientMode) {
                gridPaint.setColor(Color.rgb(50, 50, 50));
                gridPaint.setAntiAlias(false);
                gridPaint.clearShadowLayer();
                backgroundPaint.setColor(Color.BLACK);
                backgroundPaint.setAntiAlias(false);
                backgroundPaint.clearShadowLayer();
            } else {
                gridPaint.setColor(Color.BLUE);
                gridPaint.setAntiAlias(true);
                gridPaint.setShadowLayer(2f, 0, 0, Color.argb(255, 0, 0, 128));
                backgroundPaint.setColor(Color.BLACK);
                backgroundPaint.setAntiAlias(true);
                backgroundPaint.clearShadowLayer();
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode;
                gridPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            centerX = width / 2f;
            centerY = height / 2f;
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);
            drawBackground(canvas);
            drawGrid(canvas);
            drawTime(canvas);
        }

        private void drawBackground(Canvas canvas) {
            if (ambientMode && (lowBitAmbient || burnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawPaint(backgroundPaint);
            }
        }

        private void drawTime(Canvas canvas) {
            String dateFormatString = (use24h) ? "HH:mm" : "hh:mm a";
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatString);
            String timeText = simpleDateFormat.format(calendar.getTime());
            float textWidth = getTextWidth(timeText);
            int textX = Math.round(centerX - (textWidth / 2));
            int textY = Math.round(centerY + (textSize / 2));
            canvas.drawText(timeText, textX, textY, textPaint);
        }

        private float getTextWidth(String timeText) {
            float[] widths = new float[timeText.length()]; // create array to hold char widths
            textPaint.getTextWidths(timeText, widths); // get width of chars in text
            float textWidth = 0;
            for (float width : widths) {
                textWidth += width;
            }
            return textWidth;
        }

        private void drawGrid(Canvas canvas) {
            final int height = canvas.getHeight();
            final int width = canvas.getWidth();
            final int numLines = 10; // TODO: create preference


            float gridSpacing = ((float) height) / numLines;

            handleOffset(gridSpacing);

            final float gridLeft = gridOffset;
            final float gridTop = gridOffset;


            float lineStart;
            for (int i = 0; i < gridSpacing; i++) {
                lineStart = gridTop + i * gridSpacing;
                canvas.drawLine(gridLeft - gridSpacing, lineStart, (float) width + gridSpacing, lineStart, gridPaint);
                canvas.drawLine(lineStart, (float) height + gridSpacing, lineStart, gridTop - gridSpacing, gridPaint);
            }

        }

        private void handleOffset(float maxOffset) {
            if (gridOffset < maxOffset) {
                gridOffset++;
            } else if (gridOffset >= maxOffset) {
                gridOffset = 0;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (tzrRegistered) {
                return;
            }
            tzrRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GridWatchface.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!tzrRegistered) {
                return;
            }
            tzrRegistered = false;
            GridWatchface.this.unregisterReceiver(timeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #updateHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            updateHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !ambientMode;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
