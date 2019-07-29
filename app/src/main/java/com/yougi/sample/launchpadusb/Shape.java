package com.yougi.sample.launchpadusb;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Shape {
    private final List<Point> localChildPosition;
    private final String name;
    private final int minSpawnRanomValue;
    private final int maxSpawnRanomValue;


    public Shape(List<Point> localChildPosition, String name, int minSpawnRanomValue, int maxSpawnRanomValue) {
        this.localChildPosition = localChildPosition;
        this.name = name;
        this.minSpawnRanomValue = minSpawnRanomValue;
        this.maxSpawnRanomValue = maxSpawnRanomValue;
    }

    public List<Point> getLocalChildPosition() {
        return new ArrayList<>(localChildPosition);
    }

    public String getName() {
        return name;
    }

    public int getMaxSpawnRanomValue() {
        return maxSpawnRanomValue;
    }

    public int getMinSpawnRanomValue() {
        return minSpawnRanomValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shape shape = (Shape) o;
        return Objects.equals(name, shape.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }
}
