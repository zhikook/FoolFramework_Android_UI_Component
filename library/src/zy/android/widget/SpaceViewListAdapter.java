/*******************************************************************************
 * Copyright 2013, 2014 David Lau.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package zy.android.widget;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

import java.util.ArrayList;

import zy.fool.widget.FoolListView;


/**
 * 考虑到Pull成对出现，如果在底部或者顶部的
 * 发生事件怎么处理？
 * 
 * ListAdapter used when a FoolListView has header views. This ListAdapter
 * wraps another one and also keeps track of the header views and their
 * associated data objects.
 *<p>This is intended as a base class; you will probably not need to
 * use this class directly in your own code.
 */
public class SpaceViewListAdapter implements WrapperListAdapter, Filterable {

    private final ListAdapter mAdapter;

    // These two ArrayList are assumed to NOT be null.
    // They are indeed created when declared in FoolView and then shared.
    ArrayList<FoolListView.FixedViewInfo> mAbovePulledViewInfos;
    ArrayList<FoolListView.FixedViewInfo> mBelowPulledViewInfos;

    // Used as a placeholder in case the provided info views are indeed null.
    // Currently only used by some CTS tests, which may be removed.
    static final ArrayList<FoolListView.FixedViewInfo> EMPTY_INFO_LIST =
        new ArrayList<FoolListView.FixedViewInfo>();

    boolean mAreAllFixedViewsSelectable;

    private final boolean mIsFilterable;

    public SpaceViewListAdapter(ArrayList<FoolListView.FixedViewInfo> AbovePulledViewInfos,
                                 ArrayList<FoolListView.FixedViewInfo> BelowPulledViewInfos,
                                 ListAdapter adapter) {
        mAdapter = adapter;
        mIsFilterable = adapter instanceof Filterable;

        if (AbovePulledViewInfos == null) {
            mAbovePulledViewInfos = EMPTY_INFO_LIST;
        } else {
            mAbovePulledViewInfos = AbovePulledViewInfos;
        }

        if (BelowPulledViewInfos == null) {
            mBelowPulledViewInfos = EMPTY_INFO_LIST;
        } else {
            mBelowPulledViewInfos = BelowPulledViewInfos;
        }

        mAreAllFixedViewsSelectable =
                areAllListInfosSelectable(mAbovePulledViewInfos)
                && areAllListInfosSelectable(mBelowPulledViewInfos);
    }

    public int getAboversCount() {
        return mAbovePulledViewInfos.size();
    }

    public int getBelowersCount() {
        return mBelowPulledViewInfos.size();
    }

    public boolean isEmpty() {
        return (mAdapter == null || mAdapter.isEmpty())
	        && getBelowersCount() + getAboversCount() == 0;
    }

    private boolean areAllListInfosSelectable(ArrayList<FoolListView.FixedViewInfo> infos) {
        if (infos != null) {
            for (FoolListView.FixedViewInfo info : infos) {
                if (!info.isSelectable) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean removeAbover(View v) {
        for (int i = 0; i < mAbovePulledViewInfos.size(); i++) {
        	FoolListView.FixedViewInfo info = mAbovePulledViewInfos.get(i);
            if (info.view == v) {
                mAbovePulledViewInfos.remove(i);

                mAreAllFixedViewsSelectable =
                        areAllListInfosSelectable(mAbovePulledViewInfos)
                        && areAllListInfosSelectable(mBelowPulledViewInfos);

                return true;
            }
        }

        return false;
    }

    public boolean removeBelower(View v) {
        for (int i = 0; i < mBelowPulledViewInfos.size(); i++) {
        	FoolListView.FixedViewInfo info = mBelowPulledViewInfos.get(i);
            if (info.view == v) {
                mBelowPulledViewInfos.remove(i);

                mAreAllFixedViewsSelectable =
                        areAllListInfosSelectable(mAbovePulledViewInfos)
                        && areAllListInfosSelectable(mBelowPulledViewInfos);

                return true;
            }
        }

        return false;
    }

    public int getCount() {
        if (mAdapter != null) {
            return getBelowersCount() + getAboversCount() + mAdapter.getCount();
        } else {
            return getBelowersCount() + getAboversCount();
        }
    }

    public boolean areAllItemsEnabled() {
        if (mAdapter != null) {
            return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
        } else {
            return true;
        }
    }

    public boolean isEnabled(int position) {
        // Header (negative positions will throw an IndexOutOfBoundsException)
        int numHeaders = getAboversCount();
        if (position < numHeaders) {
            return mAbovePulledViewInfos.get(position).isSelectable;
        }

        // Adapter
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.isEnabled(adjPosition);
            }
        }

        // Footer (off-limits positions will throw an IndexOutOfBoundsException)
        return mBelowPulledViewInfos.get(adjPosition - adapterCount).isSelectable;
    }

    public Object getItem(int position) {
        // Header (negative positions will throw an IndexOutOfBoundsException)
        int numHeaders = getAboversCount();
        if (position < numHeaders) {
            return mAbovePulledViewInfos.get(position).data;
        }

        // Adapter
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItem(adjPosition);
            }
        }

        // Footer (off-limits positions will throw an IndexOutOfBoundsException)
        return mBelowPulledViewInfos.get(adjPosition - adapterCount).data;
    }

    public long getItemId(int position) {
        int numHeaders = getAboversCount();
        if (mAdapter != null && position >= numHeaders) {
            int adjPosition = position - numHeaders;
            int adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItemId(adjPosition);
            }
        }
        return -1;
    }

    public boolean hasStableIds() {
        if (mAdapter != null) {
            return mAdapter.hasStableIds();
        }
        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        // Header (negative positions will throw an IndexOutOfBoundsException)
        int numHeaders = getAboversCount();
        if (position < numHeaders) {
            return mAbovePulledViewInfos.get(position).view;
        }

        // Adapter
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getView(adjPosition, convertView, parent);
            }
        }

        // Footer (off-limits positions will throw an IndexOutOfBoundsException)
        return mBelowPulledViewInfos.get(adjPosition - adapterCount).view;
    }

    public int getItemViewType(int position) {
        int numHeaders = getAboversCount();
        if (mAdapter != null && position >= numHeaders) {
            int adjPosition = position - numHeaders;
            int adapterCount = mAdapter.getCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItemViewType(adjPosition);
            }
        }

        return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    public int getViewTypeCount() {
        if (mAdapter != null) {
            return mAdapter.getViewTypeCount();
        }
        return 1;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(observer);
        }
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(observer);
        }
    }

    public Filter getFilter() {
        if (mIsFilterable) {
            return ((Filterable) mAdapter).getFilter();
        }
        return null;
    }
    
    public ListAdapter getWrappedAdapter() {
        return mAdapter;
    }
}
