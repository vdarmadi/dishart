package com.dish.ocr;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class MenuData implements Parcelable {
	private String section;
	
	private ArrayList<String> items;

	public MenuData() {}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public ArrayList<String> getItems() {
		return items;
	}

	public void setItems(ArrayList<String> items) {
		this.items = items;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeString(section);
		arg0.writeStringList(items);
	}
	
	public static final Parcelable.Creator<MenuData> CREATOR = new Parcelable.Creator<MenuData>() {
        public MenuData createFromParcel(Parcel in) {
            return new MenuData(in);
        }

        public MenuData[] newArray(int size) {
            return new MenuData[size];
        }
    };
    
    private MenuData(Parcel in) {
    	section = in.readString();
    	if (items == null) {
    		items = new ArrayList();
    	}
    	in.readStringList(items);
    }
}