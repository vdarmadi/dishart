package com.qualcomm.QCARSamples.ImageTargets;

import android.graphics.Bitmap;

/** A support class encapsulating the info for one dish*/
public class Dish
{
    private String name;
    private String ingredients;
    private String ratingAvg;
    private String ratingTotal;
    private String priceNormal;
    private String priceDiscount;
    private String targetId;
    private Bitmap thumb;
    private String dishUrl;

    public Dish()
    {

    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIngredients() {
		return ingredients;
	}

	public void setIngredients(String ingredients) {
		this.ingredients = ingredients;
	}

	public String getRatingAvg() {
		return ratingAvg;
	}

	public void setRatingAvg(String ratingAvg) {
		this.ratingAvg = ratingAvg;
	}

	public String getRatingTotal() {
		return ratingTotal;
	}

	public void setRatingTotal(String ratingTotal) {
		this.ratingTotal = ratingTotal;
	}

	public String getPriceNormal() {
		return priceNormal;
	}

	public void setPriceNormal(String priceNormal) {
		this.priceNormal = priceNormal;
	}

	public String getPriceDiscount() {
		return priceDiscount;
	}

	public void setPriceDiscount(String priceDiscount) {
		this.priceDiscount = priceDiscount;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public Bitmap getThumb() {
		return thumb;
	}

	public void setThumb(Bitmap thumb) {
		this.thumb = thumb;
	}

	public String getDishUrl() {
		return dishUrl;
	}

	public void setDishUrl(String dishUrl) {
		this.dishUrl = dishUrl;
	}

	public void recycle()
    {
        // Cleans the Thumb bitmap variable
        thumb.recycle();
        thumb = null;
    }
}