package edu.sfsu.cs.orange.ocr;

import android.os.Parcel;
import android.os.Parcelable;

public class Restaurant implements Parcelable {
	String id;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	String name;
	double latitude;
	double longitude;
	String source;
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeString(id);
		arg0.writeString(name);
		arg0.writeString(source);
		arg0.writeDouble(latitude);
		arg0.writeDouble(longitude);
	}
	
	public static final Parcelable.Creator<Restaurant> CREATOR = new Parcelable.Creator<Restaurant>() {
        public Restaurant createFromParcel(Parcel in) {
            return new Restaurant(in);
        }

        public Restaurant[] newArray(int size) {
            return new Restaurant[size];
        }
    };
    
    private Restaurant(Parcel in) {
    	id = in.readString();
    	name = in.readString();
    	source = in.readString();
    	latitude = in.readDouble();
    	longitude = in.readDouble();
    }
	public Restaurant() {
		super();
	}    
}
