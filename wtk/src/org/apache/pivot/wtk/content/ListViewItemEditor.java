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
import org.apache.pivot.wtk.Insets;
import org.apache.pivot.wtk.Keyboard;
import org.apache.pivot.wtk.ListView;
import org.apache.pivot.wtk.ListViewItemListener;
import org.apache.pivot.wtk.ListViewListener;
import org.apache.pivot.wtk.Mouse;
import org.apache.pivot.wtk.Point;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.WindowStateListener;

/**
 * Default list view item editor.
 */
public class ListViewItemEditor implements ListView.ItemEditor {
    private ListView listView = null;
    private int index = -1;

    private TextInput textInput = null;
    private Window popup = null;

    private ItemEditorListenerList itemEditorListeners = new ItemEditorListenerList();

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

    private ListViewListener listViewListener = new ListViewListener.Adapter() {
        @Override
        public void listDataChanged(ListView listView, List<?> previousListData) {
            cancelEdit();
        }

        @Override
        public void itemEditorChanged(ListView listView, ListView.ItemEditor previousItemEditor) {
            cancelEdit();
        }
    };

    private ListViewItemListener listViewItemListener = new ListViewItemListener.Adapter() {
        @Override
        public void itemInserted(ListView listView, int index) {
            cancelEdit();
        }

        @Override
        public void itemsRemoved(ListView listView, int index, int count) {
            cancelEdit();
        }

        @Override
        public void itemUpdated(ListView listView, int index) {
            cancelEdit();
        }

        @Override
        public void itemsSorted(ListView listView) {
            cancelEdit();
        }
    };

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

    private WindowStateListener popupWindowStateHandler = new WindowStateListener.Adapter() {
        @Override
        public void windowOpened(Window window) {
            Display display = window.getDisplay();
            display.getContainerMouseListeners().add(displayMouseHandler);

            listView.getComponentListeners().add(componentListener);
            listView.getListViewListeners().add(listViewListener);
            listView.getListViewItemListeners().add(listViewItemListener);
        }

        @Override
        public void windowClosed(Window window, Display display, Window owner) {
            display.getContainerMouseListeners().remove(displayMouseHandler);

            listView.getComponentListeners().remove(componentListener);
            listView.getListViewListeners().remove(listViewListener);
            listView.getListViewItemListeners().remove(listViewItemListener);

            owner.moveToFront();

            listView = null;
            index = -1;
            textInput = null;
            popup = null;
        }
    };

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void editItem(ListView listView, int index) {
        if (this.listView != null) {
            throw new IllegalStateException("Currently editing.");
        }

        Vote vote = itemEditorListeners.previewEditItem(this, listView, index);

        if (vote == Vote.APPROVE) {
            this.listView = listView;
            this.index = index;

            // Get the data being edited
            List<?> listData = listView.getListData();
            ListItem listItem = (ListItem)listData.get(index);


            textInput = new TextInput();
            textInput.setText(listItem.getText());
            textInput.getComponentKeyListeners().add(textInputKeyHandler);

            // Create and open the popup
            popup = new Window(textInput);
            popup.getWindowStateListeners().add(popupWindowStateHandler);
            popup.open(listView.getWindow());
            reposition();

            textInput.selectAll();
            textInput.requestFocus();

            itemEditorListeners.itemEditing(this, listView, index);
        } else if (vote == Vote.DENY) {
            itemEditorListeners.editItemVetoed(this, vote);
        }
    }

    /**
     * Repositions the popup to be located over the item being edited.
     */
    private void reposition() {
        // Get the data being edited
        List<?> listData = listView.getListData();
        ListItem listItem = (ListItem)listData.get(index);

        // Get the item bounds
        Bounds itemBounds = listView.getItemBounds(index);
        int itemIndent = listView.getItemIndent();
        itemBounds = new Bounds(itemBounds.x + itemIndent, itemBounds.y,
            itemBounds.width - itemIndent, itemBounds.height);

        // Render the item data
        ListViewItemRenderer itemRenderer = (ListViewItemRenderer)listView.getItemRenderer();
        itemRenderer.render(listItem, index, listView, false, false, false, false);
        itemRenderer.setSize(itemBounds.width, itemBounds.height);

        // Calculate the text bounds
        Bounds textBounds = itemRenderer.getTextBounds();

        // Calculate the bounds of what we're editing
        Insets padding = (Insets)textInput.getStyles().get("padding");
        Bounds editBounds = new Bounds(itemBounds.x + textBounds.x - (padding.left + 1),
            itemBounds.y, itemBounds.width - textBounds.x + (padding.left + 1),
            itemBounds.height);

        // Scroll to make the text as visible as possible
        listView.scrollAreaToVisible(editBounds.x, editBounds.y,
            textBounds.width + padding.left + 1, editBounds.height);

        // Constrain the bounds by what is visible through Viewport ancestors
        editBounds = listView.getVisibleArea(editBounds);
        Point displayCoordinates = listView.mapPointToAncestor(listView.getDisplay(),
            editBounds.x, editBounds.y);

        textInput.setPreferredWidth(editBounds.width);
        popup.setLocation(displayCoordinates.x, displayCoordinates.y
            + (editBounds.height - textInput.getPreferredHeight(-1)) / 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditing() {
        return (listView != null);
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
        ListView listView = this.listView;
        int index = this.index;

        // Preview the changes
        String text = textInput.getText();
        Vote vote = itemEditorListeners.previewSaveChanges(this, listView, index, text);

        boolean saved = false;
        if (vote == Vote.APPROVE) {
            List<Object> listData = (List<Object>)listView.getListData();
            ListItem listItem = (ListItem)listData.get(index);

            // Update the item data
            listItem.setText(text);

            // Notifying the parent will close the popup
            if (listData.getComparator() == null) {
                listData.update(index, listItem);
            } else {
                listData.remove(index, 1);
                listData.add(listItem);

                // Re-select the item, and make sure it's visible
                index = listData.indexOf(listItem);
                listView.setSelectedIndex(index);
                listView.scrollAreaToVisible(listView.getItemBounds(index));
            }

            itemEditorListeners.changesSaved(this, listView, index);
            saved = true;
        } else if (vote == Vote.DENY) {
            itemEditorListeners.saveChangesVetoed(this, vote);
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
        ListView listView = this.listView;
        int index = this.index;

        popup.close();

        itemEditorListeners.editCancelled(this, listView, index);
    }

    @Override
    public ListenerList<ListView.ItemEditorListener> getItemEditorListeners() {
        return itemEditorListeners;
    }
}
