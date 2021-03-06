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
package org.apache.pivot.demos.million;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.serialization.CSVSerializer;
import org.apache.pivot.serialization.SerializationException;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.ApplicationContext;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.ListButton;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TableViewSortListener;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.content.TableViewRowComparator;
import org.apache.pivot.wtkx.WTKXSerializer;

public class LargeData implements Application {
    private class LoadDataCallback implements Runnable {
        private class AddRowsCallback implements Runnable {
            private ArrayList<Object> page;

            public AddRowsCallback(ArrayList<Object> page) {
                this.page = page;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                List<Object> tableData = (List<Object>)tableView.getTableData();
                for (Object item : page) {
                    tableData.add(item);
                }
            }
        }

        private URL fileURL;

        public LoadDataCallback(URL fileURL) {
            this.fileURL = fileURL;
        }

        @Override
        public void run() {
            Exception fault = null;

            long t0 = System.currentTimeMillis();

            int i = 0;
            try {
                InputStream inputStream = null;

                try {
                    inputStream = fileURL.openStream();

                    CSVSerializer csvSerializer = new CSVSerializer();
                    csvSerializer.getKeys().add("c0");
                    csvSerializer.getKeys().add("c1");
                    csvSerializer.getKeys().add("c2");
                    csvSerializer.getKeys().add("c3");

                    CSVSerializer.StreamIterator streamIterator = csvSerializer.getStreamIterator(inputStream);

                    ArrayList<Object> page = new ArrayList<Object>(pageSize);
                    while (streamIterator.hasNext()
                        && !abort) {
                        Object item = streamIterator.next();
                        if (item != null) {
                            page.add(item);
                        }
                        i++;

                        if (!streamIterator.hasNext()
                            || page.getLength() == pageSize) {
                            ApplicationContext.queueCallback(new AddRowsCallback(page));
                            page = new ArrayList<Object>(pageSize);
                        }
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch(IOException exception) {
                fault = exception;
            } catch(SerializationException exception) {
                fault = exception;
            }

            long t1 = System.currentTimeMillis();

            final String status;
            if (abort) {
                status = "Aborted";
            } else if (fault != null) {
                status = fault.toString();
            } else {
                status = "Read " + i + " rows in " + (t1 - t0) + "ms";
            }

            ApplicationContext.queueCallback(new Runnable() {
                @Override
                public void run() {
                    statusLabel.setText(status);
                    loadDataButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                }
            });
        }
    }

    private String basePath = null;

    private Window window = null;
    private ListButton fileListButton = null;
    private PushButton loadDataButton = null;
    private PushButton cancelButton = null;
    private Label statusLabel = null;
    private TableView tableView = null;

    private CSVSerializer csvSerializer;
    private int pageSize = 0;

    private volatile boolean abort = false;

    private static final String BASE_PATH_KEY = "basePath";

    public LargeData() {
        csvSerializer = new CSVSerializer();
        csvSerializer.getKeys().add("c0");
        csvSerializer.getKeys().add("c1");
        csvSerializer.getKeys().add("c2");
        csvSerializer.getKeys().add("c3");
    }

    @Override
    public void startup(Display display, Map<String, String> properties)
        throws Exception {
        basePath = properties.get(BASE_PATH_KEY);
        if (basePath == null) {
            throw new IllegalArgumentException(BASE_PATH_KEY + " is required.");
        }

        WTKXSerializer wtkxSerializer = new WTKXSerializer();
        window = (Window)wtkxSerializer.readObject(this, "large_data.wtkx");
        fileListButton = (ListButton)wtkxSerializer.get("fileListButton");
        loadDataButton = (PushButton)wtkxSerializer.get("loadDataButton");
        cancelButton = (PushButton)wtkxSerializer.get("cancelButton");
        statusLabel = (Label)wtkxSerializer.get("statusLabel");
        tableView = (TableView)wtkxSerializer.get("tableView");

        loadDataButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                loadDataButton.setEnabled(false);
                cancelButton.setEnabled(true);

                loadData();
            }
        });

        cancelButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                abort = true;

                loadDataButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        });

        tableView.getTableViewSortListeners().add(new TableViewSortListener.Adapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void sortChanged(TableView tableView) {
                List<Object> tableData = (List<Object>)tableView.getTableData();

                long startTime = System.currentTimeMillis();
                tableData.setComparator(new TableViewRowComparator(tableView));
                long endTime = System.currentTimeMillis();

                statusLabel.setText("Data sorted in " + (endTime - startTime) + " ms.");
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

    private void loadData() {
        abort = false;

        int index = fileListButton.getSelectedIndex();
        int capacity = (int)Math.pow(10, index + 1);
        tableView.setTableData(new ArrayList<Object>(capacity));

        pageSize = Math.max(capacity / 1000, 100);

        String fileName = (String)fileListButton.getSelectedItem();

        URL origin = ApplicationContext.getOrigin();

        URL fileURL = null;
        try {
            fileURL = new URL(origin, basePath + "/" + fileName);
        } catch(MalformedURLException exception) {
            System.err.println(exception.getMessage());
        }

        if (fileURL != null) {
            statusLabel.setText("Loading " + fileURL);

            LoadDataCallback callback = new LoadDataCallback(fileURL);
            Thread thread = new Thread(callback);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    public static void main(String[] args) {
        DesktopApplicationContext.main(LargeData.class, args);
    }
}
