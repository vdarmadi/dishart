package com.dish.ocr;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class MapAdapter<V extends Map<?, ? extends List<?>>> extends BaseAdapter {

	public static final int VIEW_TYPE_HEADER = 0;
	public static final int VIEW_TYPE_LISTITEM = 1;

	protected V data;
	protected int[] sectionsStart;
	protected Object[] sections;
	protected int count;

	public MapAdapter(V data) {
		this.data = data;
		onSetData();
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public Object getItem(int position) {
		int sectionIndex = getSectionForPosition(position);
		int innerIndex = position - sectionsStart[sectionIndex];
		if (innerIndex == 0) { // head
			return sections[sectionIndex];
		} else { // values
			return data.get(sections[sectionIndex]).get(innerIndex - 1);
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return Arrays.binarySearch(sectionsStart, position) < 0 ? VIEW_TYPE_LISTITEM
				: VIEW_TYPE_HEADER;
	}

	public int getPositionForSection(int section) {
		return sectionsStart[section];
	}

	public int getSectionForPosition(int position) {
		int section = Arrays.binarySearch(sectionsStart, position);
		return section < 0 ? -section - 2 : section;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == VIEW_TYPE_HEADER) {
			return getHeaderView(position, convertView, parent);
		} else {
			return getListItemView(position, convertView, parent);
		}
	}

	@Override
	public void notifyDataSetInvalidated() {
		data = null;
		onSetData();
		super.notifyDataSetInvalidated();
	}

	@Override
	public void notifyDataSetChanged() {
		onSetData();
		super.notifyDataSetChanged();
	}

	protected void onSetData() {
		if (data == null) {
			sectionsStart = null;
			sections = null;
			count = 0;
		} else {
			sectionsStart = new int[data.size()];
			sections = data.keySet().toArray(new Object[data.size()]);
			count = 0;
			int i = 0;
			for (List<?> v : data.values()) {
				sectionsStart[i] = count;
				i++;
				count += 1 + v.size();
			}
		}
	}

	protected abstract View getHeaderView(int position, View convertView, ViewGroup parent);

	protected abstract View getListItemView(int position, View convertView,	ViewGroup parent);
}