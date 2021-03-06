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
package org.apache.pivot.wtk.content;

import org.apache.pivot.beans.BeanAdapter;
import org.apache.pivot.collections.Dictionary;
import org.apache.pivot.collections.HashMap;
import org.apache.pivot.collections.List;
import org.apache.pivot.util.ListenerList;
import org.apache.pivot.util.Vote;
import org.apache.pivot.wtk.ApplicationContext;
import org.apache.pivot.wtk.Bounds;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.ComponentKeyListener;
import org.apache.pivot.wtk.ComponentListener;
import org.apache.pivot.wtk.Container;
import org.apache.pivot.wtk.ContainerMouseListener;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.Keyboard;
import org.apache.pivot.wtk.Mouse;
import org.apache.pivot.wtk.Point;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TableViewListener;
import org.apache.pivot.wtk.TableViewRowListener;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.WindowStateListener;

/**
 * Default table view cell editor.
 */
public class TableViewCellEditor implements TableView.RowEditor {
    /**
     * Responsible for repositioning the popup when the table view's size changes.
     */
    private ComponentListener componentListener = new ComponentListener.Adapter() {
        @Override
        public void sizeChanged(Component component, int previousWidth, int previousHeight) {
            ApplicationContext.queueCallback(new Runnable() {
                @Override
                public void run() {
                    reposition();
                }
            });
        }

        @Override
        public void locationChanged(Component component, int previousX, int previousY) {
            ApplicationContext.queueCallback(new Runnable() {
                @Override
                public void run() {
                    reposition();
                }
            });
        }
    };

    /**
     * Responsible for cancelling the edit if any relevant changes are made to
     * the table view while we're editing.
     */
    private TableViewListener tableViewListener = new TableViewListener.Adapter() {
        @Override
        public void tableDataChanged(TableView tableView, List<?> previousTableData) {
            cancelEdit();
        }

        @Override
        public void rowEditorChanged(TableView tableView, TableView.RowEditor previousRowEditor) {
            cancelEdit();
        }
    };

    /**
     * Responsible for cancelling the edit if any changes are made to
     * the table data while we're editing.
     */
    private TableViewRowListener tableViewRowListener = new TableViewRowListener.Adapter() {
        @Override
        public void rowInserted(TableView tableView, int rowIndex) {
            cancelEdit();
        }

        @Override
        public void rowsRemoved(TableView tableView, int rowIndex, int count) {
            cancelEdit();
        }

        @Override
        public void rowUpdated(TableView tableView, int rowIndex) {
            cancelEdit();
        }

        @Override
        public void rowsSorted(TableView tableView) {
            cancelEdit();
        }
    };

    /**
     * Responsible for saving or cancelling the edit based on the user pressing
     * the <tt>ENTER</tt> key or the <tt>ESCAPE</tt> key, respectively.
     */
    private ComponentKeyListener textInputKeyHandler = new ComponentKeyListener.Adapter() {
        @Override
        public boolean keyPressed(Component component, int keyCode, Keyboard.KeyLocation keyLocation) {
            if (keyCode == Keyboard.KeyCode.ENTER) {
                saveChanges();
            } else if (keyCode == Keyboard.KeyCode.ESCAPE) {
                cancelEdit();
            }

            return false;
        }
    };

    /**
     * Responsible for "edit initialization" and "edit finalization" tasks when
     * the edit popup is opened and closed, respectively.
     */
    private WindowStateListener popupWindowStateHandler = new WindowStateListener.Adapter() {
        @Override
        public void windowOpened(Window window) {
            Display display = window.getDisplay();
            display.getContainerMouseListeners().add(displayMouseHandler);

            tableView.getComponentListeners().add(componentListener);
            tableView.getTableViewListeners().add(tableViewListener);
            tableView.getTableViewRowListeners().add(tableViewRowListener);
        }

        @Override
        public void windowClosed(Window window, Display display, Window owner) {
            // Clean up
            display.getContainerMouseListeners().remove(displayMouseHandler);

            tableView.getComponentListeners().remove(componentListener);
            tableView.getTableViewListeners().remove(tableViewListener);
            tableView.getTableViewRowListeners().remove(tableViewRowListener);

            // Move the owner to front
            owner.moveToFront();

            // Free memory
            tableView = null;
            textInput = null;
            popup = null;
        }
    };

    /**
     * Responsible for closing the popup whenever the user clicks outside the
     * bounds of the popup.
     */
    private ContainerMouseListener displayMouseHandler = new ContainerMouseListener.Adapter() {
        @Override
        public boolean mouseDown(Container container, Mouse.Button button, int x, int y) {
            Display display = (Display)container;
            Window window = (Window)display.getComponentAt(x, y);

            boolean consumed = false;
            if (popup != window) {
                consumed = !saveChanges();
            }

            return consumed;
        }

        @Override
        public boolean mouseWheel(Container container, Mouse.ScrollType scrollType,
            int scrollAmount, int wheelRotation, int x, int y) {
            return true;
        }
    };

    private TableView tableView = null;
    private int rowIndex = -1;
    private int columnIndex = -1;

    private TextInput textInput = null;
    private Window popup = null;

    private RowEditorListenerList rowEditorListeners = new RowEditorListenerList();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void editRow(TableView tableView, int rowIndex, int columnIndex) {
        if (isEditing()) {
            throw new IllegalStateException("Currently editing.");
        }

        boolean isReadOnly = false;
        String columnName = tableView.getColumns().get(columnIndex).getName();

        // Get the row data, represented as a Dictionary
        Object tableRow = tableView.getTableData().get(rowIndex);
        Dictionary<String, Object> rowData;
        if (tableRow instanceof Dictionary<?, ?>) {
            rowData = (Dictionary<String, Object>)tableRow;
        } else {
            BeanAdapter beanAdapter = new BeanAdapter(tableRow);
            isReadOnly = beanAdapter.isReadOnly(columnName);
            rowData = beanAdapter;
        }

        if (!isReadOnly) {
            Vote vote = rowEditorListeners.previewEditRow(this, tableView, rowIndex, columnIndex);

            if (vote == Vote.APPROVE) {
                this.tableView = tableView;
                this.rowIndex = rowIndex;
                this.columnIndex = columnIndex;

                // Get the data being edited
                Object cellData = rowData.get(columnName);

                // Create the text input
                textInput = new TextInput();
                textInput.setText(cellData == null ? "" : cellData.toString());
                textInput.getComponentKeyListeners().add(textInputKeyHandler);

                // Create and open the popup
                popup = new Window(textInput);
                popup.getWindowStateListeners().add(popupWindowStateHandler);
                popup.open(tableView.getWindow());
                reposition();

                textInput.selectAll();
                textInput.requestFocus();

                rowEditorListeners.rowEditing(this, tableView, rowIndex, columnIndex);
            } else if (vote == Vote.DENY) {
                rowEditorListeners.editRowVetoed(this, vote);
            }
        }
    }

    /**
     * Repositions the popup to be located over the row being edited.
     */
    private void reposition() {
        // Get the cell bounds
        Bounds cellBounds = tableView.getCellBounds(rowIndex, columnIndex);
        tableView.scrollAreaToVisible(cellBounds);
        cellBounds = tableView.getVisibleArea(cellBounds);
        Point displayCoordinates = tableView.mapPointToAncestor(tableView.getDisplay(),
            cellBounds.x, cellBounds.y);

        // Position the popup/editor to fit over the cell bounds
        textInput.setPreferredWidth(cellBounds.width);
        popup.setLocation(displayCoordinates.x, displayCoordinates.y
            + (cellBounds.height - textInput.getPreferredHeight(-1)) / 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditing() {
        return (tableView != null);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean saveChanges() {
        if (!isEditing()) {
            throw new IllegalStateException();
        }

        // Save local reference to members variables before they get cleared
        TableView tableView = this.tableView;
        int rowIndex = this.rowIndex;
        int columnIndex = this.columnIndex;

        // Get the changes
        String text = textInput.getText();
        String columnName = tableView.getColumns().get(columnIndex).getName();

        // Preview the changes
        HashMap<String, Object> changes = new HashMap<String, Object>();
        changes.put(columnName, text);
        Vote vote = rowEditorListeners.previewSaveChanges(this, tableView, rowIndex,
            columnIndex, changes);

        boolean saved = false;
        if (vote == Vote.APPROVE) {
            List<Object> tableData = (List<Object>)tableView.getTableData();

            // Get the row data, represented as a Dictionary
            Object tableRow = tableData.get(rowIndex);
            Dictionary<String, Object> rowData;
            if (tableRow instanceof Dictionary<?, ?>) {
                rowData = (Dictionary<String, Object>)tableRow;
            } else {
                rowData = new BeanAdapter(tableRow);
            }

            // Update the cell data
            rowData.put(columnName, text);

            // Notifying the parent will close the popup
            if (tableData.getComparator() == null) {
                tableData.update(rowIndex, tableRow);
            } else {
                tableData.remove(rowIndex, 1);
                tableData.add(tableRow);

                // Re-select the row, and make sure it's visible
                rowIndex = tableData.indexOf(tableRow);
                tableView.setSelectedIndex(rowIndex);
                tableView.scrollAreaToVisible(tableView.getRowBounds(rowIndex));
            }

            rowEditorListeners.changesSaved(this, tableView, rowIndex, columnIndex);
            saved = true;
        } else if (vote == Vote.DENY) {
            rowEditorListeners.saveChangesVetoed(this, vote);
        }

        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            throw new IllegalStateException();
        }

        // Save local reference to members variables before they get cleared
        TableView tableView = this.tableView;
        int rowIndex = this.rowIndex;
        int columnIndex = this.columnIndex;

        popup.close();

        rowEditorListeners.editCancelled(this, tableView, rowIndex, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListenerList<TableView.RowEditorListener> getRowEditorListeners() {
        return rowEditorListeners;
    }
}
