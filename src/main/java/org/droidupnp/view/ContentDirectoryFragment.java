/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 * <p/>
 * This file is part of DroidUPNP.
 * <p/>
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.droidupnp.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import org.droidupnp.MainActivity;
import org.droidupnp.R;
import org.droidupnp.model.upnp.IDeviceDiscoveryObserver;
import org.droidupnp.model.upnp.didl.DIDLDevice;
import org.droidupnp.model.upnp.CallableContentDirectoryFilter;
import org.droidupnp.model.upnp.IContentDirectoryCommand;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.droidupnp.model.upnp.didl.IDIDLContainer;
import org.droidupnp.model.upnp.didl.IDIDLItem;
import org.droidupnp.model.upnp.didl.IDIDLObject;
import org.droidupnp.model.upnp.didl.IDIDLParentContainer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

public class ContentDirectoryFragment extends Fragment implements Observer {
    private static final String TAG = "ContentDirectoryFragme";

    private LinkedList<String> tree = null;
    private String currentID = null;
    private IUpnpDevice device;

    private boolean loading = true;
    int pastVisiblesItems, visibleItemCount, totalItemCount;

    private RecyclerView mRecyclerView;
    private ContentDirectoryRecyclerViewAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    private TextView mEmptyView;
    private IContentDirectoryCommand mContentDirectoryCommand;

    static final String STATE_CONTENTDIRECTORY = "contentDirectory";
    static final String STATE_TREE = "tree";
    static final String STATE_CURRENT = "current";

    private DeviceObserver deviceObserver;

    // newInstance constructor for creating fragment with arguments
    public static Fragment newInstance(int page, String title) {
        Fragment fragmentFirst = new ContentDirectoryFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        args.putString("someTitle", title);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MainActivity.setContentDirectoryFragment(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.browsing_list_fragment, container, false);
    }

    /**
     * This update the search visibility depending on current content directory capabilities
     */
    public void updateSearchVisibility() {
        final Activity a = getActivity();
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MainActivity.setSearchVisibility(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public class DeviceObserver implements IDeviceDiscoveryObserver {
        ContentDirectoryFragment cdf;

        public DeviceObserver(ContentDirectoryFragment cdf) {
            this.cdf = cdf;
        }

        @Override
        public void addedDevice(IUpnpDevice device) {
            if (MainActivity.upnpServiceController.getSelectedContentDirectory() == null)
                cdf.update();
        }

        @Override
        public void removedDevice(IUpnpDevice device) {
            if (MainActivity.upnpServiceController.getSelectedContentDirectory() == null)
                cdf.update();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFocusable(true);
        mRecyclerView.setClickable(true);
        mRecyclerView.setFocusableInTouchMode(true);

        deviceObserver = new DeviceObserver(this);
        MainActivity.upnpServiceController.getContentDirectoryDiscovery().addObserver(deviceObserver);

        // Listen to content directory change
        if (MainActivity.upnpServiceController != null)
            MainActivity.upnpServiceController.addSelectedContentDirectoryObserver(this);
        else
            Log.w(TAG, "upnpServiceController was not ready !!!");

        if (savedInstanceState != null
                && savedInstanceState.getStringArray(STATE_TREE) != null
                && MainActivity.upnpServiceController.getSelectedContentDirectory() != null
                && 0 == MainActivity.upnpServiceController.getSelectedContentDirectory().getUID()
                .compareTo(savedInstanceState.getString(STATE_CONTENTDIRECTORY))) {
            Log.i(TAG, "Restore previews state");

            // Content directory is still the same => reload context
            tree = new LinkedList<>(Arrays.asList(savedInstanceState.getStringArray(STATE_TREE)));
            currentID = savedInstanceState.getString(STATE_CURRENT);

            device = MainActivity.upnpServiceController.getSelectedContentDirectory();
            mContentDirectoryCommand = MainActivity.factory.createContentDirectoryCommand();
        }

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) //check for scroll down
                {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (loading) {
                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            loading = false;
                            Log.v("...", "Last Item Wow !");

                            if (tree != null && tree.size() > 0) {
                                String parentID = (tree.size() > 0) ? tree.getLast() : null;
                                Log.i(TAG, "Browse, currentID : " + currentID + ", parentID : " + parentID);
                                mContentDirectoryCommand.continueBrowse(currentID, parentID, new AdditionalContentCallback());
                            }
                        }
                    }
                }
            }
        });

        Log.d(TAG, "Force refresh");
        refresh();
    }

    public boolean onItemLongClick(IDIDLObject didl) {
        Log.v(TAG, "On long-click event");

        if (didl instanceof IDIDLItem) {
            IDIDLItem ididlItem = (IDIDLItem) didl;
            final Activity a = getActivity();
            final Intent intent = new Intent(Intent.ACTION_VIEW);

            Uri uri = Uri.parse(ididlItem.getURI());
            intent.setDataAndType(uri, didl.getDataType());

            try {
                a.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getActivity(), R.string.failed_action, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.no_action_available, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MainActivity.upnpServiceController.delSelectedContentDirectoryObserver(this);
        MainActivity.upnpServiceController.getContentDirectoryDiscovery().removeObserver(deviceObserver);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyView = new TextView(view.getContext());
        mRecyclerView = (RecyclerView) this.getView().findViewById(R.id.gridView);
        mRecyclerView.setHasFixedSize(true);

        final ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mLayoutManager = new GridLayoutManager(view.getContext(), view.getWidth() / 250);
                mRecyclerView.setLayoutManager(mLayoutManager);

            }
        });

        // specify an adapter (see also next example)
        mAdapter = new ContentDirectoryRecyclerViewAdapter(this, true);
        mRecyclerView.setAdapter(mAdapter);

        view.setBackgroundColor(getResources().getColor(R.color.grey));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "Save instance state");

        if (MainActivity.upnpServiceController.getSelectedContentDirectory() == null)
            return;

        savedInstanceState.putString(STATE_CONTENTDIRECTORY, MainActivity.upnpServiceController.getSelectedContentDirectory()
                .getUID());

        if (tree != null) {
            String[] arrayTree = new String[tree.size()];
            int i = 0;
            for (String s : tree)
                arrayTree[i++] = s;

            savedInstanceState.putStringArray(STATE_TREE, arrayTree);
            savedInstanceState.putString(STATE_CURRENT, currentID);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public Boolean goBack() {
        if (tree == null || tree.isEmpty()) {
            if (MainActivity.upnpServiceController.getSelectedContentDirectory() != null) {
                // Back on device root, unselect device
                MainActivity.upnpServiceController.setSelectedContentDirectory(null);
                return false;
            } else {
                // Already at the upper level
                return true;
            }
        } else {
            Log.d(TAG, "Go back in browsing");
            currentID = tree.pop();
            update();
            return false;
        }
    }

    public void printCurrentContentDirectoryInfo() {
        Log.i(TAG, "Device : " + MainActivity.upnpServiceController.getSelectedContentDirectory().getDisplayString());
        MainActivity.upnpServiceController.getSelectedContentDirectory().printService();
    }

    public class RefreshCallback implements Callable<Void> {
        public Void call() throws java.lang.Exception {
            final Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //TODO: manual pull to refresh
                            mAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return null;
        }
    }

    public class AdditionalContentCallback extends ContentCallback {
        public Void call() throws java.lang.Exception {
            final Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mAdapter.addDataset(content);
                            loading = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return null;
        }
    }

    public class ContentCallback extends RefreshCallback {
        protected ArrayList<DIDLObjectDisplay> content;

        public ContentCallback() {
            this.content = new ArrayList<>();
        }

        public void setContent(ArrayList<DIDLObjectDisplay> content) {
            this.content = content;
        }

        public Void call() throws java.lang.Exception {
            final Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mAdapter.updateDataset(content);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return null;
        }
    }

    public void setEmptyText(CharSequence text) {
        //TODO: no empty text
    }

    public synchronized void refresh() {
        Log.d(TAG, "refresh");

        setEmptyText(getString(R.string.loading));

        final Activity a = getActivity();
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Update search visibility
        updateSearchVisibility();

        if (MainActivity.upnpServiceController.getSelectedContentDirectory() == null) {
            // List here the content directory devices
            setEmptyText(getString(R.string.device_list_empty));

            if (device != null) {
                Log.i(TAG, "Current content directory have been removed");
                device = null;
                tree = null;
            }

            // Fill with the content directory list
            final Collection<IUpnpDevice> upnpDevices = MainActivity.upnpServiceController.getServiceListener()
                    .getFilteredDeviceList(new CallableContentDirectoryFilter());

            ArrayList<DIDLObjectDisplay> list = new ArrayList<DIDLObjectDisplay>();
            for (IUpnpDevice upnpDevice : upnpDevices)
                list.add(new DIDLObjectDisplay(new DIDLDevice(upnpDevice)));

            try {
                ContentCallback cc = new ContentCallback();
                cc.setContent(list);
                cc.call();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        Log.i(TAG, "device " + device + " device " + ((device != null) ? device.getDisplayString() : ""));
        Log.i(TAG, "mContentDirectoryCommand : " + mContentDirectoryCommand);

        mContentDirectoryCommand = MainActivity.factory.createContentDirectoryCommand();
        if (mContentDirectoryCommand == null)
            return; // Can't do anything if upnp not ready

        if (device == null || !device.equals(MainActivity.upnpServiceController.getSelectedContentDirectory())) {
            device = MainActivity.upnpServiceController.getSelectedContentDirectory();

            Log.i(TAG, "Content directory changed !!! "
                    + MainActivity.upnpServiceController.getSelectedContentDirectory().getDisplayString());

            tree = new LinkedList<String>();

            Log.i(TAG, "Browse root of a new device");
            mContentDirectoryCommand.browse("0", null, new ContentCallback());
        } else {
            if (tree != null && tree.size() > 0) {
                String parentID = (tree.size() > 0) ? tree.getLast() : null;
                Log.i(TAG, "Browse, currentID : " + currentID + ", parentID : " + parentID);
                mContentDirectoryCommand.browse(currentID, parentID, new ContentCallback());
            } else {
                Log.i(TAG, "Browse root");
                mContentDirectoryCommand.browse("0", null, new ContentCallback());
            }
        }
    }

    public void onListItemClick(IDIDLObject didl) {
        try {
            if (didl instanceof DIDLDevice) {
                MainActivity.upnpServiceController.setSelectedContentDirectory(((DIDLDevice) didl).getDevice(), false);

                // Refresh display
                refresh();
            } else if (didl instanceof IDIDLContainer) {
                // Update position
                if (didl instanceof IDIDLParentContainer) {
                    currentID = tree.pop();
                } else {
                    currentID = didl.getId();
                    String parentID = didl.getParentID();
                    tree.push(parentID);
                }

                // Refresh display
                refresh();
            } else if (didl instanceof IDIDLItem) {
                // Launch item
                launchURI((IDIDLItem) didl);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to finish action after item click");
            e.printStackTrace();
        }
    }

    private void launchURI(final IDIDLItem uri) {
        if (MainActivity.upnpServiceController.getSelectedRenderer() == null) {
            // No renderer selected yet, open a popup to select one
            final Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RendererDialog rendererDialog = new RendererDialog();
                            rendererDialog.setCallback(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    launchURIRenderer(uri);
                                    return null;
                                }
                            });
                            rendererDialog.show(getActivity().getFragmentManager(), "RendererDialog");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } else {
            // Renderer available, go for it
            launchURIRenderer(uri);
        }
    }

    private void launchURIRenderer(IDIDLItem uri) {
        IRendererCommand rendererCommand = MainActivity.factory.createRendererCommand(MainActivity.factory.createRendererState());
        rendererCommand.launchItem(uri);
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.i(TAG, "ContentDirectory have changed");
        update();
    }

    public void update() {
        final Activity a = getActivity();
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        }
    }

    public void setGridView(boolean gridView) {
        mAdapter.setGridMode(gridView);
        if (gridView) {
            mLayoutManager = new GridLayoutManager(this.getView().getContext(), this.getView().getWidth() / 250);
            mRecyclerView.setLayoutManager(mLayoutManager);
        } else {
            mLayoutManager = new LinearLayoutManager(this.getView().getContext());
            mRecyclerView.setLayoutManager(mLayoutManager);
        }
    }
}
