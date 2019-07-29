package com.yougi.sample.launchpadusb;

import android.graphics.Point;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yougi.launchpadusb.ControlRightPad;
import com.yougi.launchpadusb.ControlTopPad;
import com.yougi.launchpadusb.LaunchPadConnection;
import com.yougi.launchpadusb.PadColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TetrisGameManager implements LaunchPadConnection.OnReceiveLaunchPadListener {

    public static final int MIN_X_WORLD_POSITION = 0;
    public static final int MAX_X_WORLD_POSITION = 7;

    public static final int MIN_Y_WORLD_POSITION = 0;
    public static final int MAX_Y_WORLD_POSITION = 7;

    private final LaunchPadConnection launchPadConnection;

    private final Handler handler;

    private int nbMoveRight;
    private int nbMoveLeft;

    @Nullable
    private GameThread gameThread;

    private List<Shape> availableShapes;

    public TetrisGameManager(LaunchPadConnection launchPadConnection) {
        this.launchPadConnection = launchPadConnection;
        this.handler = new Handler();

        initAvailableShapes();
    }

    public void startGame() {
        if (gameThread != null) {
            throw new IllegalStateException("Cannot call startGame when a game already running...");
        }

        gameThread = new GameThread();
        launchPadConnection.registerOnReceiveLaunchPadEvents(this);

        final int duration = 1000;
        launchPadAnimation(duration);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gameThread.start();
            }
        }, duration + 500);
    }

    public void stopGame() {
        if (gameThread == null) {
            throw new IllegalStateException("Cannot call stopGame when a game not running...");
        }

        launchPadConnection.unregisterOnReceiveLaunchPadEvents(TetrisGameManager.this);

        gameThread.stopThread();
        gameThread = null;

        final int duration = 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                launchPadAnimation(duration);
            }
        }, 1000);
    }

    @Override
    public void OnReceiveTopControlEvent(ControlTopPad controlTopPad, boolean isDown) {
        if (isDown) {
            if (ControlTopPad.ARROW_LEFT == controlTopPad) {
                nbMoveLeft++;
            } else if (ControlTopPad.ARROW_RIGHT == controlTopPad) {
                nbMoveRight++;
            }
        }
    }

    @Override
    public void OnReceiveRightControlEvent(ControlRightPad controlRightPad, boolean isDown) {
        //DO NOTHING
    }

    @Override
    public void OnReceiveMainPadEvent(int padId, boolean isDown) {
        //DO NOTHING
    }

    private void initAvailableShapes() {
        availableShapes = new ArrayList<>();

//        List<Point> shapePoints = new ArrayList<>();
//        shapePoints.add(new Point(0, 0));
//        Shape shape = new Shape(shapePoints, "Simple");
//        availableShapes.add(shape);

        List<Point> shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, 1));
        shapePoints.add(new Point(-1, 0));
        shapePoints.add(new Point(-1, 1));
        Shape shape = new Shape(shapePoints, "Cube", 1, 7);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(-1, 0));
        shapePoints.add(new Point(1, 0));
        shape = new Shape(shapePoints, "TReverse", 1, 6);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(0, -2));
        shapePoints.add(new Point(1, 0));
        shape = new Shape(shapePoints, "L", 0, 6);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(0, -2));
        shapePoints.add(new Point(-1, 0));
        shape = new Shape(shapePoints, "LReverse", 1, 7);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(0, -2));
        shapePoints.add(new Point(0, -3));
        shape = new Shape(shapePoints, "I", 0, 7);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(-1, -1));
        shapePoints.add(new Point(1, 0));
        shape = new Shape(shapePoints, "Z", 1, 6);
        availableShapes.add(shape);

        shapePoints = new ArrayList<>();
        shapePoints.add(new Point(0, 0));
        shapePoints.add(new Point(0, -1));
        shapePoints.add(new Point(1, -1));
        shapePoints.add(new Point(-1, 0));
        shape = new Shape(shapePoints, "ZReverse", 1, 6);
        availableShapes.add(shape);
    }

    private void launchPadAnimation(final int duration) {
        handler.post(new Runnable() {

            private int yPosIndex = 0;

            @Override
            public void run() {
                for (int i = 0; i <= MAX_X_WORLD_POSITION; i++) {
                    if (yPosIndex > 0) {
                        launchPadConnection.disablePad((yPosIndex - 1) * (MAX_Y_WORLD_POSITION + 1) + i);
                    }

                    if (yPosIndex < 8) {
                        launchPadConnection.enablePad(yPosIndex * (MAX_Y_WORLD_POSITION + 1) + i,
                                PadColor.Red.POWER3, PadColor.Green.POWER3);
                    }
                }

                if (yPosIndex < 8) {
                    yPosIndex++;
                    handler.postDelayed(this, duration / (MAX_Y_WORLD_POSITION + 1));
                }
            }
        });
    }

    private class GameThread extends Thread {

        /**
         * The refresh interval in milliseconds.
         */
        private final long REFRESH_INTERVAL = 50;

        private final long REFRESH_INTERVAL_MOVE_Y_POSITION = 1000;

        private boolean isRunning;

        private boolean isInterrupted;

        @Nullable
        private WorldShape currentWorldShape;

        private long lastMoveCurrentShape;

        private boolean[][] occupedPads = new boolean[8][8];

        private final Random random = new Random();

        @Override
        public synchronized void start() {
            isInterrupted = false;
            super.start();
        }

        private boolean currentShapeCanMoveToPosition(Point destPosition){
            if(currentWorldShape == null) {
                return false;
            }

            List<Point> points = currentWorldShape.getListPositionWithOffeset(destPosition);
            for (Point point : points) {
                if (point.x < MIN_X_WORLD_POSITION || point.x > MAX_X_WORLD_POSITION
                        || point.y < MIN_Y_WORLD_POSITION || point.y > MAX_Y_WORLD_POSITION) {
                    return false;
                }

                if(occupedPads[point.y][point.x]){
                    return false;
                }
            }

            return true;
        }

        @Override
        public void run() {
            isRunning = true;
            lastMoveCurrentShape = System.currentTimeMillis();

            super.run();
            while (!isInterrupted() && !isInterrupted) {
                try {
                    Thread.sleep(REFRESH_INTERVAL);
                    if (isRunning) {
                        long currentTime = System.currentTimeMillis();
                        boolean forceUpdatePad = false;

                        if (currentWorldShape == null && currentTime - lastMoveCurrentShape >= REFRESH_INTERVAL_MOVE_Y_POSITION) {
                            final int indexShape = random.nextInt(availableShapes.size());
                            final Shape selectedShape = availableShapes.get(indexShape);
                            final int spawnColumn = selectedShape.getMinSpawnRanomValue()
                                    + random.nextInt(selectedShape.getMaxSpawnRanomValue()
                                    - selectedShape.getMinSpawnRanomValue() + 1);

                            currentWorldShape = new WorldShape(new Point(spawnColumn, MIN_Y_WORLD_POSITION), selectedShape);
                            lastMoveCurrentShape = currentTime;
                            forceUpdatePad = true;
                        }

                        if (currentWorldShape != null) {
                            Point oldPosition = new Point(currentWorldShape.getWorldPosition());

                            // X MOVE
                            if (nbMoveRight != 0 || nbMoveLeft != 0) {
                                int relativeMove = nbMoveRight - nbMoveLeft;
                                currentWorldShape.moveXPosition(relativeMove);

                                nbMoveRight = 0;
                                nbMoveLeft = 0;
                            }

                            // Y MOVE
                            boolean hasYPosMoved = false;
                            if (currentTime - lastMoveCurrentShape >= REFRESH_INTERVAL_MOVE_Y_POSITION) {
                                lastMoveCurrentShape = currentTime;
                                currentWorldShape.moveYPosition(1);
                                hasYPosMoved = true;
                            }

                            // Update Position
                            Point newPosition = currentWorldShape.getWorldPosition();
                            if (forceUpdatePad || !oldPosition.equals(newPosition)) {
                                //clear Pad
                                for (Point point : currentWorldShape.getListPositionWithOffeset(oldPosition)) {
                                    if (point.x >= MIN_X_WORLD_POSITION && point.x <= MAX_X_WORLD_POSITION
                                            && point.y >= MIN_Y_WORLD_POSITION && point.y <= MAX_Y_WORLD_POSITION) {
                                        launchPadConnection.disablePad(point.y * 8 + point.x);
                                    }
                                }

                                //Light newPad
                                for (Point point : currentWorldShape.getListWorldPosition()) {
                                    if (point.x >= MIN_X_WORLD_POSITION && point.x <= MAX_X_WORLD_POSITION
                                            && point.y >= MIN_Y_WORLD_POSITION && point.y <= MAX_Y_WORLD_POSITION) {
                                        launchPadConnection.enablePad(point.y * 8 + point.x, PadColor.Red.DISABLE,
                                                PadColor.Green.POWER3);
                                    }
                                }
                            }

                            if (hasYPosMoved) {
                                final List<Point> listWorldPosition = currentWorldShape.getListWorldPosition();
                                boolean needToFixShape = false;
                                for (Point point : listWorldPosition) {
                                    if (point.y == MAX_Y_WORLD_POSITION || (point.x >= MIN_X_WORLD_POSITION
                                            && point.x <= MAX_X_WORLD_POSITION && point.y >= MIN_Y_WORLD_POSITION
                                            && point.y <= MAX_Y_WORLD_POSITION && occupedPads[point.y + 1][point.x])) {
                                        needToFixShape = true;
                                        break;
                                    }
                                }

                                //Log.d("YOUGI", "run() called : ");
                                //for (Point point : listWorldPosition) {
                                //    Log.d("YOUGI", "listWorldPosition : " + point);
                                //}

                                if (needToFixShape) {
                                    Log.d("YOUGI", "run() called newPosition : " + newPosition);
                                    if (newPosition.y <= MIN_Y_WORLD_POSITION) {
                                        stopGame();
                                        return;
                                    }

                                    List<Integer> yPosAffected = new ArrayList<>();
                                    for (Point point : listWorldPosition) {
                                        occupedPads[point.y][point.x] = true;
                                        if (!yPosAffected.contains(point.y)) {
                                            yPosAffected.add(point.y);
                                        }
                                    }
                                    Collections.sort(yPosAffected);

                                    // for (int i = 0; i < 8; i++) {
                                    //     StringBuilder sb = new StringBuilder("run: i : " + i + " [");
                                    //     for (int j = 0; j < 8; j++) {
                                    //         sb.append(occupedPads[i][j] + ",");
                                    //     }
                                    //     Log.d("YOUGI", sb.toString() + "]");
                                    // }

                                    currentWorldShape = null;

                                    // Line completed
                                    boolean needToRefreshPads = false;
                                    for (Integer yPos : yPosAffected) {
                                        boolean lineCompleted = true;
                                        for (boolean b : occupedPads[yPos]) {
                                            if (!b) {
                                                lineCompleted = false;
                                                break;
                                            }
                                        }

                                        if (lineCompleted) {
                                            for (int i = yPos; i >= 1; i--) {
                                                occupedPads[i] = occupedPads[i - 1];
                                            }
                                            needToRefreshPads = true;
                                        }
                                    }

                                    if (needToRefreshPads) {
                                        for (int i = 0; i <= MAX_Y_WORLD_POSITION; i++) {
                                            for (int j = 0; j <= MAX_X_WORLD_POSITION; j++) {
                                                if (occupedPads[i][j]) {
                                                    launchPadConnection.enablePad(i * 8 + j, PadColor.Red.DISABLE,
                                                            PadColor.Green.POWER3);
                                                } else {
                                                    launchPadConnection.disablePad(i * 8 + j);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    isInterrupted = true;
                }
            }
        }

        void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        void stopThread() {
            setIsRunning(false);
            isInterrupted = true;
            interrupt();
        }
    }

}
