package org.droidupnp.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import org.droidupnp.model.upnp.didl.IDIDLObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class ContentDirectoryRecyclerViewAdapter extends RecyclerView.Adapter<ContentDirectoryRecyclerViewAdapter.ViewHolder> {

    ArrayList<DIDLObjectDisplay> mContent;
    ContentDirectoryFragment mFragment;
    private LruCache<String, Bitmap> mMemoryCache;
    private boolean mGridMode = false;

    private static final int IMAGE_FADE_ANIMATION_DURATION = 400;
    private static final float MAX_CACHE_SIZE = 1.0f / 8.0f;
    private static final String TAG = "ContDirGridViewAdp";

    public void setGridMode(boolean gridMode) {
        mGridMode = gridMode;
    }

    private void initCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = (int) (maxMemory * MAX_CACHE_SIZE);

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // each data item is just a string in this case
        private View mView;

        private DownloadImageTask mDownloadImageTask;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            v.setSelected(false);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            v.setSelected(true);

            IDIDLObject ob = mContent.get(position).getDIDLObject();
            mFragment.onListItemClick(ob);
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getLayoutPosition();
            IDIDLObject ob = mContent.get(position).getDIDLObject();
            return mFragment.onItemLongClick(ob);
        }

        public View getView() {
            return mView;
        }

        public void setView(View view) {
            mView = view;
        }

        public DownloadImageTask getDownloadImageTask() {
            return mDownloadImageTask;
        }

        public void setDownloadImageTask(DownloadImageTask downloadImageTask) {
            mDownloadImageTask = downloadImageTask;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ContentDirectoryRecyclerViewAdapter(ContentDirectoryFragment fragment, boolean gridMode) {
        mContent = new ArrayList<>();
        mFragment = fragment;
        mGridMode = gridMode;
        initCache();
    }

    public void updateDataset(ArrayList<DIDLObjectDisplay> content) {
        mContent = content;
        notifyDataSetChanged();
    }

    public void addDataset(ArrayList<DIDLObjectDisplay> content) {
        int positionStart = content.size();
        mContent.addAll(content);
        notifyItemRangeInserted(positionStart, content.size());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ContentDirectoryRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                             int viewType) {
        // create a new view
        View v;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (mGridMode) {
            v = inflater.inflate(org.droidupnp.R.layout.browsing_grid_item, parent, false);
        } else {
            v = inflater.inflate(org.droidupnp.R.layout.browsing_list_item, parent, false);
        }

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public int getItemViewType(int position) {
        return mGridMode ? 1 : 0;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        IDIDLObject obje = mContent.get(position).getDIDLObject();

        holder.getView().setSelected(false);
        TextView text1 = (TextView) holder.getView().findViewById(org.droidupnp.R.id.text1);
        TextView text2 = (TextView) holder.getView().findViewById(org.droidupnp.R.id.text2);
        TextView text3 = (TextView) holder.getView().findViewById(org.droidupnp.R.id.text3);
        ImageView imageView = (ImageView) holder.getView().findViewById(org.droidupnp.R.id.icon);

        if (obje.getIcon() instanceof Integer) {
            imageView.setImageResource((Integer) obje.getIcon());
            imageView.setScaleType(ImageView.ScaleType.CENTER);
        } else if (obje.getIcon() instanceof URI) {
            imageView.setTag(obje.getIcon().toString());
            DownloadImageTask downloadImageTask = new DownloadImageTask(imageView, obje.getIcon().toString());
            downloadImageTask.execute();
            holder.setDownloadImageTask(downloadImageTask);
        } else
            imageView.setImageResource(android.R.color.transparent);

        text1.setText(obje.getTitle());
        text2.setText((obje.getDescription() != null) ? obje.getDescription() : "");
        text3.setText(obje.getCount());
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);

        // cancel image request if the view is recycled before the image loading has been completed
        if (holder.getDownloadImageTask() != null) {
            holder.getDownloadImageTask().cancel(true);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mContent.size();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
        ImageView imageView;
        String url;

        public DownloadImageTask(ImageView imageView, String url) {
            this.imageView = imageView;
            this.url = url;
            imageView.setImageResource(android.R.color.transparent);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                Bitmap b = getBitmapFromMemCache(url);

                if (b != null) {
                    return b;
                }

                b = BitmapFactory.decodeStream(new java.net.URL(url).openStream());
                b = Bitmap.createScaledBitmap(b, 200, 200, true);
                addBitmapToMemoryCache(url, b);
                return b;
            } catch (IOException e) {
                Log.e(TAG, "IO Error during image fetch: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && imageView.getTag().equals(url)) {
                imageView.setImageBitmap(result);

                Animation a = new AlphaAnimation(0.00f, 1.00f);
                a.setDuration(IMAGE_FADE_ANIMATION_DURATION);
                a.setAnimationListener(new Animation.AnimationListener() {

                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                        imageView.setVisibility(View.VISIBLE);
                    }
                });

                imageView.startAnimation(a);
            }
        }
    }
}
