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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import org.droidupnp.MainActivity;
import org.droidupnp.R;
import org.droidupnp.model.cling.RendererState;
import org.droidupnp.model.upnp.ARendererState;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class RendererFragment extends Fragment implements Observer {
    private static final String TAG = "RendererFragment";

    private IUpnpDevice device;
    private ARendererState rendererState;
    private IRendererCommand rendererCommand;

    // NowPlaying Slide
    private ImageView mStopButton;
    private ImageView play_pauseButton;
    private ImageView mVolumeButton;

    // Settings Slide
    private SeekBar mProgressBar;
    private SeekBar mVolume;

    private TextView mDuration;
    private boolean mDurationRemaining;

    // newInstance constructor for creating fragment with arguments
    public static Fragment newInstance(int page, String title) {
        Fragment fragmentFirst = new RendererFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        args.putString("someTitle", title);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    public RendererFragment() {
        super();
        mDurationRemaining = true;
    }

    public void hide() {
        Activity a = getActivity();
        if (a == null)
            return;
    }

    public void show() {
        Activity a = getActivity();
        if (a == null)
            return;
        a.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        getFragmentManager().beginTransaction().show(this).commit();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Listen to renderer change
        if (MainActivity.upnpServiceController != null)
            MainActivity.upnpServiceController.addSelectedRendererObserver(this);
        else
            Log.w(TAG, "upnpServiceController was not ready !!!");

        // Initially hide renderer
        hide();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Call MainActivity Initialise Function
        this.init();
    }

    @Override
    public void onResume() {
        super.onResume();
        startControlPoint();

        if (rendererCommand != null)
            rendererCommand.resume();
    }

    @Override
    public void onPause() {
        device = null;
        if (rendererCommand != null)
            rendererCommand.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        MainActivity.upnpServiceController.delSelectedRendererObserver(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_renderer, container, false);
    }

    public void startControlPoint() {
        if (MainActivity.upnpServiceController.getSelectedRenderer() == null) {
            if (device != null) {
                Log.i(TAG, "Current renderer have been removed");
                device = null;

                final Activity a = getActivity();
                if (a == null)
                    return;

                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            hide();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return;
        }

        if (device == null || rendererState == null || rendererCommand == null
                || !device.equals(MainActivity.upnpServiceController.getSelectedRenderer())) {
            device = MainActivity.upnpServiceController.getSelectedRenderer();

            Log.i(TAG, "Renderer changed !!! " + MainActivity.upnpServiceController.getSelectedRenderer().getDisplayString());

            rendererState = MainActivity.factory.createRendererState();
            rendererCommand = MainActivity.factory.createRendererCommand(rendererState);

            if (rendererState == null || rendererCommand == null) {
                Log.e(TAG, "Fail to create renderer command and/or state");
                return;
            }

            rendererCommand.resume();

            rendererState.addObserver(this);
            rendererCommand.updateFull();
        }
        updateRenderer();
    }

    public void updateRenderer() {
        Log.v(TAG, "updateRenderer");

        if (rendererState != null) {
            final Activity a = getActivity();
            if (a == null)
                return;

            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        show();

                        RelativeLayout rendererBackground = (RelativeLayout) a.findViewById(R.id.renderer_background);
                        TextView title = (TextView) a.findViewById(R.id.title);
                        TextView artist = (TextView) a.findViewById(R.id.artist);
                        SeekBar seek = (SeekBar) a.findViewById(R.id.progressBar);
                        SeekBar volume = (SeekBar) a.findViewById(R.id.volume);
                        TextView durationElapse = (TextView) a.findViewById(R.id.trackDurationElapse);

                        if (title == null || artist == null || seek == null || mDuration == null || durationElapse == null)
                            return;

                        if (mDurationRemaining)
                            mDuration.setText(rendererState.getRemainingDuration());
                        else
                            mDuration.setText(rendererState.getDuration());

                        durationElapse.setText(rendererState.getPosition());

                        seek.setProgress(rendererState.getElapsedPercent());

                        title.setText(rendererState.getTitle());
                        artist.setText(rendererState.getArtist());

                        if (rendererState.getState() == RendererState.State.PLAY) {
                            play_pauseButton.setImageResource(R.drawable.pause);
                            play_pauseButton.setContentDescription(getResources().getString(R.string.pause));
                        } else {
                            play_pauseButton.setImageResource(R.drawable.play);
                            play_pauseButton.setContentDescription(getResources().getString(R.string.play));
                        }

                        if (rendererState.isMute())
                            mVolumeButton.setImageResource(R.drawable.volume_mute);
                        else
                            mVolumeButton.setImageResource(R.drawable.volume);

                        volume.setProgress(rendererState.getVolume());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Log.v(TAG, rendererState.toString());
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        startControlPoint();
    }

    private void init() {
        SetupButtons();
        SetupButtonListeners();
    }

    private void SetupButtons() {
        // Now_Playing Footer Buttons
        play_pauseButton = (ImageView) getActivity().findViewById(R.id.play_pauseButton);
        mVolumeButton = (ImageView) getActivity().findViewById(R.id.volumeIcon);
        mStopButton = (ImageView) getActivity().findViewById(R.id.stopButton);
        mProgressBar = (SeekBar) getActivity().findViewById(R.id.progressBar);
        mVolume = (SeekBar) getActivity().findViewById(R.id.volume);
    }

    public abstract class ButtonCallback implements Callable<Void> {
        protected View view = null;

        // Button action
        protected abstract void action();

        public ButtonCallback(View v) {
            this.view = v;
        }

        public Void call() throws java.lang.Exception {
            view.setBackgroundColor(getResources().getColor(R.color.blue_trans));
            action();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
            }, 100L);
            return null;
        }
    }

    public class PlayPauseCallback extends ButtonCallback {
        public PlayPauseCallback(View v) {
            super(v);
        }

        @Override
        protected void action() {
            if (rendererCommand != null)
                rendererCommand.commandToggle();
        }
    }

    public class StopCallback extends ButtonCallback {
        public StopCallback(View v) {
            super(v);
        }

        @Override
        protected void action() {
            if (rendererCommand != null)
                rendererCommand.commandStop();
        }
    }

    public class MuteCallback extends ButtonCallback {
        public MuteCallback(View v) {
            super(v);
        }

        @Override
        protected void action() {
            if (rendererCommand != null)
                rendererCommand.toggleMute();
        }
    }

    public class TimeSwitchCallback extends ButtonCallback {
        public TimeSwitchCallback(View v) {
            super(v);
        }

        @Override
        protected void action() {
            if (rendererState == null)
                return;

            mDurationRemaining = !mDurationRemaining;
            if (mDurationRemaining)
                mDuration.setText(rendererState.getRemainingDuration());
            else
                mDuration.setText(rendererState.getDuration());
        }
    }

    private void SetupButtonListeners() {
        if (play_pauseButton != null) {
            play_pauseButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        (new PlayPauseCallback(v)).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (mStopButton != null) {
            mStopButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        (new StopCallback(v)).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (mVolumeButton != null) {
            mVolumeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        (new MuteCallback(v)).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (rendererState == null)
                        return;

                    int position = seekBar.getProgress();

                    long t = (long) ((1.0 - ((double) seekBar.getMax() - position) / (seekBar.getMax())) * rendererState
                            .getDurationSeconds());
                    long h = t / 3600;
                    long m = (t - h * 3600) / 60;
                    long s = t - h * 3600 - m * 60;
                    String seek = formatTime(h, m, s);

                    Toast.makeText(getActivity().getApplicationContext(), "Seek to " + seek, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Seek to " + seek);
                    if (rendererCommand != null)
                        rendererCommand.commandSeek(seek);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }
            });
        }

        if (mVolume != null) {
            mVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(getActivity().getApplicationContext(), "Set mVolume to " + seekBar.getProgress(),
                            Toast.LENGTH_SHORT).show();

                    if (rendererCommand != null)
                        rendererCommand.setVolume(seekBar.getProgress());
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        mDuration = (TextView) getActivity().findViewById(R.id.trackDurationRemaining);
        if (mDuration != null) {
            mDuration.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        (new TimeSwitchCallback(v)).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private String formatTime(long h, long m, long s) {
        return ((h >= 10) ? "" + h : "0" + h) + ":" + ((m >= 10) ? "" + m : "0" + m) + ":"
                + ((s >= 10) ? "" + s : "0" + s);
    }
}
