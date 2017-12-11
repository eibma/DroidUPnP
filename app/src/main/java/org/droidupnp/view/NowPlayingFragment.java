package org.droidupnp.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.droidupnp.R;

/**
 * Fragment that contains the renderer on top and the playlist below
 */
public class NowPlayingFragment extends Fragment {

    public static Fragment newInstance(int page, String title) {
        Fragment fragment = new NowPlayingFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        args.putString("someTitle", title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Fragment rendererFragment =  new RendererFragment();
        Fragment playlistFragment = new PlaylistFragment();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_nowplaying, container, false);

        // add child fragments
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.nowPlaying, rendererFragment);
        transaction.add(R.id.nowPlaying, playlistFragment);
        transaction.commit();
        return view;
    }
}
