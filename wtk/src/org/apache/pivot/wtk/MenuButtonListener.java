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
package org.apache.pivot.wtk;

/**
 * Menu button listener interface.
 */
public interface MenuButtonListener {
    /**
     * Menu button listener adapter.
     */
    public static class Adapter implements MenuButtonListener {
        @Override
        public void menuChanged(MenuButton menuButton, Menu previousMenu) {
        }

        @Override
        public void repeatableChanged(MenuButton menuButton) {
        }
    }

    /**
     * Called when a menu button's menu has changed.
     *
     * @param menuButton
     * @param previousMenu
     */
    public void menuChanged(MenuButton menuButton, Menu previousMenu);

    /**
     * Called when a menu button's repeatable flag has changed.
     *
     * @param menuButton
     */
    public void repeatableChanged(MenuButton menuButton);
}
