package com.yougi.sample.launchpadusb;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public class WorldShape {

    private Point worldPosition;
    private Orientation orientation;

    private final Shape shape;

    public WorldShape(Point initialPosition, Shape shape) {
        this.worldPosition = initialPosition;
        this.shape = shape;
        this.orientation = Orientation.NORMAL;
    }

    public void moveXPosition(int relativePosition) {
        if (relativePosition == 0) {
            return;
        }

        int shiftLocalPosition = 0;
        for (Point point : shape.getLocalChildPosition()) {
            if (relativePosition < 0) {
                shiftLocalPosition = Math.min(shiftLocalPosition, point.x);
            } else {
                shiftLocalPosition = Math.max(shiftLocalPosition, point.x);
            }
        }

        final int newPos = worldPosition.x + relativePosition;
        if (newPos < TetrisGameManager.MIN_X_WORLD_POSITION - shiftLocalPosition) {
            worldPosition.x = TetrisGameManager.MIN_X_WORLD_POSITION - shiftLocalPosition;
            return;
        }

        if (newPos > TetrisGameManager.MAX_X_WORLD_POSITION - shiftLocalPosition) {
            worldPosition.x = TetrisGameManager.MAX_X_WORLD_POSITION - shiftLocalPosition;
            return;
        }

        worldPosition.x = newPos;
    }

    public void moveYPosition(int relativePosition) {
        if (relativePosition == 0) {
            return;
        }

        int shiftLocalPosition = 0;
        for (Point point : shape.getLocalChildPosition()) {
            if (relativePosition < 0) {
                shiftLocalPosition = Math.min(shiftLocalPosition, point.y);
            } else {
                shiftLocalPosition = Math.max(shiftLocalPosition, point.y);
            }
        }


        final int newPos = worldPosition.y + relativePosition;
        if (newPos < TetrisGameManager.MIN_Y_WORLD_POSITION - shiftLocalPosition) {
            worldPosition.y = TetrisGameManager.MIN_Y_WORLD_POSITION - shiftLocalPosition;
            return;
        }

        if (newPos > TetrisGameManager.MAX_Y_WORLD_POSITION - shiftLocalPosition) {
            worldPosition.y = TetrisGameManager.MAX_Y_WORLD_POSITION - shiftLocalPosition;
            return;
        }

        worldPosition.y = newPos;
    }

    public List<Point> getListWorldPosition() {
        return getListPositionWithOffeset(worldPosition);
    }

    public List<Point> getListPositionWithOffeset(Point offset) {
        final List<Point> worldShapePosition = new ArrayList<>();

        for (Point localPoint : shape.getLocalChildPosition()) {
            Point p = new Point(localPoint);
            p.offset(offset.x, offset.y);
            worldShapePosition.add(p);
        }

        return worldShapePosition;
    }

    public Point getWorldPosition() {
        return worldPosition;
    }

    public Shape getShape() {
        return shape;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public enum Orientation {
        NORMAL,
        LEFT,
        BOTTOM,
        RIGHT
    }
}
