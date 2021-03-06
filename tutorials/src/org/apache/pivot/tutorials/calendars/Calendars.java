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
package org.apache.pivot.tutorials.calendars;

import org.apache.pivot.collections.Map;
import org.apache.pivot.util.CalendarDate;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.Calendar;
import org.apache.pivot.wtk.CalendarButton;
import org.apache.pivot.wtk.CalendarButtonSelectionListener;
import org.apache.pivot.wtk.CalendarSelectionListener;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtkx.WTKXSerializer;

public class Calendars implements Application {
    private Window window = null;
    private Calendar calendar = null;
    private CalendarButton calendarButton = null;
    private Label selectedDateLabel = null;

    private boolean updatingSelectedDate = false;

    @Override
    public void startup(Display display, Map<String, String> properties)
        throws Exception {
        WTKXSerializer wtkxSerializer = new WTKXSerializer();
        window = (Window)wtkxSerializer.readObject(this, "calendars.wtkx");
        calendar = (Calendar)wtkxSerializer.get("calendar");
        calendarButton = (CalendarButton)wtkxSerializer.get("calendarButton");
        selectedDateLabel = (Label)wtkxSerializer.get("selectedDateLabel");

        calendar.getCalendarSelectionListeners().add(new CalendarSelectionListener() {
            @Override
            public void selectedDateChanged(Calendar calendar, CalendarDate previousSelectedDate) {
                updateSelectedDate(calendar.getSelectedDate());
            }
        });

        calendarButton.getCalendarButtonSelectionListeners().add(new CalendarButtonSelectionListener() {
            @Override
            public void selectedDateChanged(CalendarButton calendarButton, CalendarDate previousSelectedDate) {
                updateSelectedDate(calendarButton.getSelectedDate());
            }
        });

        window.open(display);
    }

    @Override
    public boolean shutdown(boolean optional) {
        if (window != null) {
            window.close();
        }

        return false;
    }

    @Override
    public void suspend() {
    }

    @Override
    public void resume() {
    }

    private void updateSelectedDate(CalendarDate selectedDate) {
        if (!updatingSelectedDate) {
            updatingSelectedDate = true;

            calendar.setSelectedDate(selectedDate);
            calendar.setYear(selectedDate.year);
            calendar.setMonth(selectedDate.month);

            calendarButton.setSelectedDate(selectedDate);

            selectedDateLabel.setText(selectedDate.toString());

            updatingSelectedDate = false;
        }
    }

    public static void main(String[] args) {
        DesktopApplicationContext.main(Calendars.class, args);
    }
}
