/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 * <p>
 * This file is part of DroidUPNP.
 * <p>
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.droidupnp.view;

import org.droidupnp.MainActivity;
import org.droidupnp.model.upnp.IUpnpDevice;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.Observable;
import java.util.Observer;

public class RendererDeviceFragment extends UpnpDeviceListFragment implements Observer {

    protected static final String TAG = "RendererDeviceFragment";

    public RendererDeviceFragment() {
        super();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.upnpServiceController.getRendererDiscovery().addObserver(this);
        MainActivity.upnpServiceController.addSelectedRendererObserver(this);
        Log.d(TAG, "onActivityCreated");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MainActivity.upnpServiceController.getRendererDiscovery().removeObserver(this);
        MainActivity.upnpServiceController.delSelectedRendererObserver(this);
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected boolean isSelected(IUpnpDevice device) {
        if (MainActivity.upnpServiceController != null && MainActivity.upnpServiceController.getSelectedRenderer() != null)
            return device.equals(MainActivity.upnpServiceController.getSelectedRenderer());

        return false;
    }

    @Override
    protected void select(IUpnpDevice device) {
        select(device, false);
    }

    @Override
    protected void select(IUpnpDevice device, boolean force) {
        MainActivity.upnpServiceController.setSelectedRenderer(device, force);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        select(list.getItem(position).getDevice());
        Log.d(TAG, "Set renderer to " + list.getItem(position));
    }

    @Override
    public void update(Observable observable, Object o) {
        Activity a = getActivity();
        if (a == null)
            return;

        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IUpnpDevice device = MainActivity.upnpServiceController.getSelectedRenderer();
                if (device == null) {
                    // Uncheck device
                    getListView().clearChoices();
                    list.notifyDataSetChanged();
                } else {
                    addedDevice(device);
                }
            }
        });
    }
}