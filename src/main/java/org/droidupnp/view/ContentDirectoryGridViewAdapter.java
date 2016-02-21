package org.droidupnp.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v7.appcompat.R;

import org.droidupnp.model.upnp.didl.IDIDLObject;
import org.w3c.dom.Text;

import java.util.ArrayList;

public class ContentDirectoryGridViewAdapter extends RecyclerView.Adapter<ContentDirectoryGridViewAdapter.ViewHolder> {
    //private String[] mDataset;
    ArrayList<DIDLObjectDisplay> mContent;
    ContentDirectoryFragment mFragment;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // each data item is just a string in this case
        public View mView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();

            IDIDLObject ob = mContent.get(position).getDIDLObject();

            mFragment.onListItemClick(ob);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ContentDirectoryGridViewAdapter(ContentDirectoryFragment frag) {
        mContent = new ArrayList<>();
        mFragment = frag;
    }

    public void updateDataset(ArrayList<DIDLObjectDisplay> content) {
        mContent = content;
        notifyDataSetChanged();
    };


    // Create new views (invoked by the layout manager)
    @Override
    public ContentDirectoryGridViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v;//= (//new TextView(parent.getContext());

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        v = inflater.inflate(org.droidupnp.R.layout.browsing_list_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        IDIDLObject obje = mContent.get(position).getDIDLObject();
        TextView text1 = (TextView)holder.mView.findViewById(org.droidupnp.R.id.text1);
        TextView text2 = (TextView)holder.mView.findViewById(org.droidupnp.R.id.text2);
        TextView text3 = (TextView)holder.mView.findViewById(org.droidupnp.R.id.text3);
        ImageView icon= (ImageView)holder.mView.findViewById(org.droidupnp.R.id.icon);

        icon.setImageResource(obje.getIcon());
        text1.setText(obje.getTitle());
        text2.setText((obje.getDescription()!=null) ? obje.getDescription() : "");
        text3.setText(obje.getCount());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mContent.size();
    }
}
