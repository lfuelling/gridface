package sh.lrk.grid;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.HashSet;
import java.util.Set;

import static sh.lrk.grid.GridConfigActivity.DEF_VALUES;
import static sh.lrk.grid.GridWatchface.KEY_ACTIVE_DIRECTION;

class GridPainter {
    private int gridOffset = 0;
    private Paint gridPaint;
    private SharedPreferences preferences;

    GridPainter(Paint gridPaint, SharedPreferences preferences) {
        this.gridPaint = gridPaint;
        this.preferences = preferences;
    }

    void drawGrid(Canvas canvas) {
        drawBackgroundLayer(canvas);
        drawGridLayer(canvas);
    }

    private void drawGridLayer(Canvas canvas) {
        final int height = canvas.getHeight();
        final int width = canvas.getWidth();

        int numLines = 10;
        float gridSpacing = ((float) height) / numLines;

        handleOffset(gridSpacing);

        Set<String> directions = preferences.getStringSet(KEY_ACTIVE_DIRECTION, DEF_VALUES);

        //FIXME implement grid direction handling!

        float lineStart;
        for (int i = 0; i < gridSpacing; i++) {
            lineStart = i * gridSpacing;

            float hStartX = gridOffset - gridSpacing;
            float hStartY = ((directions.contains(GridDirection.DOWN.name())) ? gridOffset :
                    (directions.contains(GridDirection.UP.name())) ? -gridOffset : 0) + lineStart;
            float hStopX = (float) width + gridSpacing;
            float hStopY = ((directions.contains(GridDirection.DOWN.name())) ? gridOffset :
                    (directions.contains(GridDirection.UP.name())) ? -gridOffset : 0) + lineStart;
            canvas.drawLine(hStartX, hStartY, hStopX, hStopY, gridPaint);

            float vStartX = ((directions.contains(GridDirection.RIGHT.name())) ? gridOffset :
                    (directions.contains(GridDirection.LEFT.name())) ? -gridOffset : 0) + lineStart;
            float vStartY = (float) height + gridSpacing;
            float vStopX = ((directions.contains(GridDirection.RIGHT.name())) ? gridOffset :
                    (directions.contains(GridDirection.LEFT.name())) ? -gridOffset : 0) + lineStart;
            float vStopY = gridOffset - gridSpacing;
            canvas.drawLine(vStartX, vStartY, vStopX, vStopY, gridPaint);
        }
    }

    private void drawBackgroundLayer(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
    }

    private void handleOffset(float maxOffset) {
        if (gridOffset < maxOffset - 1) {
            gridOffset++;
        } else if (gridOffset >= maxOffset - 1) {
            gridOffset = 0;
        }
    }
}
