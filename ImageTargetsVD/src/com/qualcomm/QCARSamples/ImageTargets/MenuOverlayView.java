package com.qualcomm.QCARSamples.ImageTargets;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;

/** Custom View with Menu Overlay Data */
public class MenuOverlayView extends RelativeLayout
{
    public MenuOverlayView(Context context)
    {
        this(context, null);
    }


    public MenuOverlayView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }


    public MenuOverlayView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        inflateLayout(context);

    }

    /** Inflates the Custom View Layout */
    private void inflateLayout(Context context)
    {

        final LayoutInflater inflater = LayoutInflater.from(context);

        // Generates the layout for the view
        inflater.inflate(R.layout.bitmap_layout, this, true);
    }


    /** Sets menu name in view */
    public void setDishName(String dishName)
    {
    	// TextView tv = (TextView) findViewById(R.id.custom_view_dishname);
    	// tv.setText(dishName);
    }

    /** Sets ingredients in View */
    public void setIngredients(String ingredients)
    {
    	// TextView tv = (TextView) findViewById(R.id.custom_view_ingredients);
    	// tv.setText(ingredients);
    }


    /** Sets dish Price in View */
    public void setDishPrice(String dishPrice)
    {
    	// TextView tv = (TextView) findViewById(R.id.custom_view_dish_price);
    	// tv.setText(getContext().getString(R.string.string_$) + dishPrice);
    }


    /** Sets Dish Number of Ratings in View */
    public void setDishRatingCount(String ratingCount)
    {
    	// TextView tv = (TextView) findViewById(R.id.custom_view_rating_text);
    	// tv.setText(getContext().getString(R.string.string_openParentheses)
    	// + ratingCount + getContext().getString(R.string.string_ratings)
    	// + getContext().getString(R.string.string_closeParentheses));
    }

    /** Sets Dish Special Price in View */
    public void setYourPrice(String yourPrice)
    {
    	// TextView tv = (TextView) findViewById(R.id.badge_price_value);
    	// tv.setText(getContext().getString(R.string.string_$) + yourPrice);
    }


    /** Sets Dish picture in View from a bitmap */
    public void setDishPictureViewFromBitmap(Bitmap dishPicture)
    {
        ImageView iv = (ImageView) findViewById(R.id.custom_view_menu_pict);
        iv.setImageBitmap(dishPicture);
    }


    /** Sets Dish Rating in View */
    public void setRating(String rating)
    {
        RatingBar rb = (RatingBar) findViewById(R.id.custom_view_rating);
        rb.setRating(Float.parseFloat(rating));
    }
}
