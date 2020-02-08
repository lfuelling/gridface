package sh.lrk.grid;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.HashSet;

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

        preferences.getStringSet(KEY_ACTIVE_DIRECTION, new HashSet<>());

        //FIXME implement grid direction handling!

        float lineStart;
        for (int i = 0; i < gridSpacing; i++) {
            lineStart = gridOffset + i * gridSpacing;

            float hStartX = gridOffset - gridSpacing;
            float hStartY = lineStart;
            float hStopX = (float) width + gridSpacing;
            float hStopY = lineStart;
            canvas.drawLine(hStartX, hStartY, hStopX, hStopY, gridPaint);

            float vStartX = lineStart;
            float vStartY = (float) height + gridSpacing;
            float vStopX = lineStart;
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
