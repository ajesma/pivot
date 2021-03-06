/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pivot.wtk.media.drawing;

import java.awt.Paint;
import java.awt.geom.Line2D;

import org.apache.pivot.util.ListenerList;
import org.apache.pivot.wtk.Point;

/**
 * Shape representing a line.
 */
public class Line extends Shape2D {
    private static class LineListenerList extends ListenerList<LineListener>
        implements LineListener {
        public void endpointsChanged(Line line, int previousX1, int previousY1,
            int previousX2, int previousY2) {
            for (LineListener listener : this) {
                listener.endpointsChanged(line, previousX1, previousY1,
                    previousX2, previousY2);
            }
        }
    }

    private Line2D.Float line2D = new Line2D.Float();

    private LineListenerList lineListeners = new LineListenerList();

    public int getX1() {
        return (int)line2D.x1;
    }

    public void setX1(int x1) {
        setEndpoints(x1, getY1(), getX2(), getY2());
    }

    public int getY1() {
        return (int)line2D.y1;
    }

    public void setY1(int y1) {
        setEndpoints(getX1(), y1, getX2(), getY2());
    }

    public int getX2() {
        return (int)line2D.x2;
    }

    public void setX2(int x2) {
        setEndpoints(getX1(), getY1(), x2, getY2());
    }

    public int getY2() {
        return (int)line2D.y2;
    }

    public void setY2(int y2) {
        setEndpoints(getX1(), getY1(), getX2(), y2);
    }

    public Point getEndpoint1() {
        return new Point(getX1(), getY1());
    }

    public Point getEndpoint2() {
        return new Point(getX2(), getY2());
    }

    public void setEndpoints(int x1, int y1, int x2, int y2) {
        int previousX1 = (int)line2D.x1;
        int previousY1 = (int)line2D.y1;
        int previousX2 = (int)line2D.x2;
        int previousY2 = (int)line2D.y2;

        if (previousX1 != x1
            || previousY1 != y1
            || previousX2 != x2
            || previousY2 != y2) {
            line2D.x1 = x1;
            line2D.y1 = y1;
            line2D.x2 = x2;
            line2D.y2 = y2;
            invalidate();
            lineListeners.endpointsChanged(this, previousX1, previousY1,
                previousX2, previousY2);
        }
    }

    @Override
    public void setFill(Paint fill) {
        // Lines can't have a fill
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStroke(Paint stroke) {
        if (stroke == null) {
            // Lines must have a stroke
            throw new IllegalArgumentException();
        }

        super.setStroke(stroke);
    }

    @Override
    protected java.awt.Shape getShape2D() {
        return line2D;
    }

    public ListenerList<LineListener> getLineListeners() {
        return lineListeners;
    }
}
