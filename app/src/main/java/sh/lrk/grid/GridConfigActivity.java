package sh.lrk.grid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;

import java.util.HashSet;
import java.util.Set;

import static sh.lrk.grid.GridWatchface.KEY_ACTIVE_DIRECTION;
import static sh.lrk.grid.GridWatchface.KEY_USE_24H;
import static sh.lrk.grid.GridWatchface.PREFERENCES_NAME;
import static sh.lrk.grid.GridWatchface.UPDATE_RATE_MS;

/**
 * Copied from the codelabs example, modified. So it's Apache2.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class GridConfigActivity extends Activity {
    private static final String TAG = "ConfigActivity";

    private SharedPreferences preferences;
    private ImageButton gridDirectionRight;
    private ImageButton gridDirectionLeft;
    private ImageButton gridDirectionUp;
    private ImageButton gridDirectionDown;
    private ImageButton ampmToggle;
    private GridThread gridThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        initBackground();

        ampmToggle = findViewById(R.id.ampm_toggle);
        updateCheckboxState();
        ampmToggle.setOnClickListener(v -> toggleAmpm());

        gridDirectionRight = findViewById(R.id.grid_direction_right);
        gridDirectionRight.setOnClickListener(v -> toggleGrid(GridDirection.RIGHT));
        gridDirectionLeft = findViewById(R.id.grid_direction_left);
        gridDirectionLeft.setOnClickListener(v -> toggleGrid(GridDirection.LEFT));
        gridDirectionUp = findViewById(R.id.grid_direction_up);
        gridDirectionUp.setOnClickListener(v -> toggleGrid(GridDirection.UP));
        gridDirectionDown = findViewById(R.id.grid_direction_down);
        gridDirectionDown.setOnClickListener(v -> toggleGrid(GridDirection.DOWN));
    }

    private void toggleGrid(GridDirection direction) {
        Set<String> directions = preferences.getStringSet(KEY_ACTIVE_DIRECTION, new HashSet<>());
        switch (direction) {
            case UP:
                if (directions.contains(GridDirection.UP.name())) {
                    directions.remove(GridDirection.UP.name());
                } else {
                    directions.remove(GridDirection.DOWN.name());
                    directions.add(GridDirection.UP.name());
                }
                break;
            case DOWN:
                if (directions.contains(GridDirection.DOWN.name())) {
                    directions.remove(GridDirection.DOWN.name());
                } else {
                    directions.remove(GridDirection.UP.name());
                    directions.add(GridDirection.DOWN.name());
                }
                break;
            case LEFT:
                if (directions.contains(GridDirection.LEFT.name())) {
                    directions.remove(GridDirection.LEFT.name());
                } else {
                    directions.remove(GridDirection.RIGHT.name());
                    directions.add(GridDirection.LEFT.name());
                }
                break;
            case RIGHT:
                if (directions.contains(GridDirection.RIGHT.name())) {
                    directions.remove(GridDirection.RIGHT.name());
                } else {
                    directions.remove(GridDirection.LEFT.name());
                    directions.add(GridDirection.RIGHT.name());
                }
                break;
            default:
                break;
        }
        preferences.edit().putStringSet(KEY_ACTIVE_DIRECTION, directions).apply();
        updateGridControlState();
    }

    private void updateGridControlState() {
        Set<String> activeDirections = preferences.getStringSet(KEY_ACTIVE_DIRECTION, new HashSet<>());

        if (activeDirections.contains(GridDirection.RIGHT.name())) {
            gridDirectionRight.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_right_blue_24dp));
        } else {
            gridDirectionRight.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp));
        }

        if (activeDirections.contains(GridDirection.LEFT.name())) {
            gridDirectionLeft.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_left_blue_24dp));
        } else {
            gridDirectionLeft.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_left_white_24dp));
        }

        if (activeDirections.contains(GridDirection.DOWN.name())) {
            gridDirectionDown.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_down_blue_24dp));
        } else {
            gridDirectionDown.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_down_white_24dp));
        }

        if (activeDirections.contains(GridDirection.UP.name())) {
            gridDirectionUp.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_up_blue_24dp));
        } else {
            gridDirectionUp.setImageDrawable(getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp));
        }
    }

    private void toggleAmpm() {
        boolean previousState = preferences.getBoolean(KEY_USE_24H, true);
        preferences.edit().putBoolean(KEY_USE_24H, !previousState).apply();
        updateCheckboxState();
    }

    private void updateCheckboxState() {
        boolean use24h = preferences.getBoolean(KEY_USE_24H, true);
        if (use24h) {
            ampmToggle.setImageDrawable(getDrawable(R.drawable.ic_check_box_white_24dp));
        } else {
            ampmToggle.setImageDrawable(getDrawable(R.drawable.ic_check_box_outline_blank_white_24dp));
        }
    }

    private class GridThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private boolean run;
        private GridPainter gridPainter;

        GridThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
            run = true;
            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.BLUE);
            gridPainter = new GridPainter(gridPaint, preferences);
        }

        void quit() {
            this.run = false;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            while (run) {
                try {
                    canvas = surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        if (canvas != null) {
                            gridPainter.drawGrid(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    Thread.sleep(UPDATE_RATE_MS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "GridThread was interrupted!", e);
                }
            }
        }
    }

    private void initBackground() {
        SurfaceView surface = findViewById(R.id.watch_face_background);
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                gridThread = new GridThread(surface.getHolder());
                gridThread.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                gridThread = new GridThread(surface.getHolder());
                gridThread.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                gridThread.quit();
            }
        });
    }

}

