package com.lmgy.redirect.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.lmgy.redirect.R;
import com.lmgy.redirect.bean.HostData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by lmgy on 15/8/2019
 */
public class HostSettingAdapter extends RecyclerView.Adapter<HostSettingAdapter.HostSettingViewHolder> implements MultiSelectMode<String> {

    private Context mContext;
    private List<String> mSelectedIds;
    private AsyncListDiffer<HostData> mDiffer;
//    private List<HostData> hostDataList;
//    private final OnListInteractionListener mListener;


    private DiffUtil.ItemCallback<HostData> diffCallback = new DiffUtil.ItemCallback<HostData>() {
        @Override
        public boolean areItemsTheSame(@NonNull HostData oldItem, @NonNull HostData newItem) {
            return oldItem.getHostName().equals(newItem.getHostName()) && oldItem.getIpAddress().equals(newItem.getIpAddress());
        }

        @Override
        public boolean areContentsTheSame(@NonNull HostData oldItem, @NonNull HostData newItem) {
            return oldItem.getHostName().equals(newItem.getHostName()) && oldItem.getIpAddress().equals(newItem.getIpAddress()) && oldItem.getType() == newItem.getType();
        }
    };

    public HostSettingAdapter(Context context, List<HostData> hostDataList) {
        this.mContext = context;
        mDiffer = new AsyncListDiffer<>(this, diffCallback);
        mSelectedIds = new ArrayList<>();
        Log.e("asd", "HostSettingAdapter = " + hostDataList.size());
        setHostDataList(hostDataList);
    }

    public void setHostDataList(List<HostData> mHostDataList) {
        Log.e("asd", "setHostDataList");
        mDiffer.submitList(mHostDataList);
    }

    @NotNull
    @Override
    public HostSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.e("asd", "onCreateViewHolder");
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_list, parent, false);
        return new HostSettingViewHolder(view);
    }

    public interface OnListInteractionListener {

        void onItemClick(HostData item);

        void onItemLongClick();
    }

    @Override
    public void onBindViewHolder(HostSettingViewHolder holder, int position) {
        Log.e("asd", "onBindViewHolder");
        holder.setData(getItem(position), position);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public HostData getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    public void setSelectedIds(List<String> selectedIds) {
        this.mSelectedIds = selectedIds;
        notifyDataSetChanged();
    }

    @NotNull
    @Override
    public List<String> getSelectedItemsId() {
        return mSelectedIds;
    }


    @Override
    public void addSelectedItemId(String selectedItemId) {
        mSelectedIds.add(selectedItemId);
    }

    @Override
    public void removeSelectedItemId(String selectedItemId) {
        mSelectedIds.remove(selectedItemId);
    }

    @Override
    public void removeAllSelected() {
        mSelectedIds.clear();
    }

    @Override
    public int getSelectedItemsCount() {
        return mSelectedIds.size();
    }

    @Override
    public boolean isSomethingSelected() {
        return mDiffer.getCurrentList().size() == mSelectedIds.size();
    }

    class HostSettingViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private FrameLayout rootView;

        HostSettingViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            rootView = itemView.findViewById(R.id.root_view);
        }

        private void setData(final HostData hostData, int position) {
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    mListener.onItemClick(hostData);
                }
            });

            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
//                    mListener.onItemLongClick();
                    return true;
                }
            });
            String text = hostData.getIpAddress()
                    + "   " + hostData.getHostName()
                    + "   " + hostData.getRemark();
            if (!hostData.getType())
                text = "#" + text;
            title.setText(text);
            if (mSelectedIds.contains(String.valueOf(position))) {
                rootView.setForeground(new ColorDrawable(ContextCompat.getColor(mContext, R.color.colorControlActivated)));
            } else {
                rootView.setForeground(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.transparent)));
            }
        }
    }
}
