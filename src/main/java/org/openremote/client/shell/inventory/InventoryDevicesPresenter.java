/*
 * Copyright 2015, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.openremote.client.shell.inventory;

import jsinterop.annotations.JsType;
import org.openremote.client.event.InventoryManagerOpenEvent;
import org.openremote.client.shared.AbstractPresenter;
import org.openremote.client.shared.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsType
public class InventoryDevicesPresenter extends AbstractPresenter<View> {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryDevicesPresenter.class);

    public String[] devices = new String[0];

    public InventoryDevicesPresenter(View view) {
        super(view);
    }

    public void openManager() {
        dispatch(new InventoryManagerOpenEvent());
    }

}
