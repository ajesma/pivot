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

import org.apache.pivot.wtk.media.Image;

/**
 * Accordion attribute listener interface.
 */
public interface AccordionAttributeListener {
    /**
     * Accordion attribute listener adapter.
     */
    public static class Adapter implements AccordionAttributeListener {
        @Override
        public void labelChanged(Accordion accordion, Component component, String previousLabel) {
        }

        @Override
        public void iconChanged(Accordion accordion, Component component, Image previousIcon) {
        }
    }

    /**
     * Called when a panel's label attribute has changed.
     *
     * @param accordion
     * @param component
     * @param previousLabel
     */
    public void labelChanged(Accordion accordion, Component component, String previousLabel);

    /**
     * Called when a panel's icon attribute has changed.
     *
     * @param accordion
     * @param component
     * @param previousIcon
     */
    public void iconChanged(Accordion accordion, Component component, Image previousIcon);
}
