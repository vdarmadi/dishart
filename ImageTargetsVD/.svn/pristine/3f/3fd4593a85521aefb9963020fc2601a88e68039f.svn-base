package com.qualcomm.QCARSamples.ImageTargets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class CustomView extends View {
	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 600;
	private static final int MAX_FRAME_HEIGHT = 400;

	public CustomView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CustomView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomView(Context context) {
		super(context);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		Path outerShape = new Path();
		// add rect covering the whole view area
		outerShape.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
		// add "selection" rect;

		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		int w = metrics.widthPixels;
		int h = metrics.heightPixels;

		Point screenResolution = new Point(w, h);

		int width = screenResolution.x * 3 / 4;
		if (width < MIN_FRAME_WIDTH) {
			width = MIN_FRAME_WIDTH;
		} else if (width > MAX_FRAME_WIDTH) {
			width = MAX_FRAME_WIDTH;
		}
		int height = screenResolution.y * 3 / 4;
		if (height < MIN_FRAME_HEIGHT) {
			height = MIN_FRAME_HEIGHT;
		} else if (height > MAX_FRAME_HEIGHT) {
			height = MAX_FRAME_HEIGHT;
		}
		int leftOffset = (screenResolution.x - width) / 2;
		int topOffset = (screenResolution.y - height) / 2;
		int tolerancy = 125;
		// RectF framingRect = new RectF(leftOffset, topOffset, leftOffset + width, topOffset + height);
		
		DebugLog.LOGD("screenResolution " + screenResolution.y);
		RectF framingRect = new RectF(0, (screenResolution.y / 2) - tolerancy, 
				w, (screenResolution.y / 2) + tolerancy);

		RectF inner = framingRect;
		outerShape.addRect(inner, Path.Direction.CW);
		// set the fill rule so inner area will not be painted
		outerShape.setFillType(Path.FillType.EVEN_ODD);

		// set up paints
		Paint outerPaint = new Paint();
		outerPaint.setColor(Color.TRANSPARENT);

		Paint borderPaint = new Paint();
		borderPaint.setARGB(255, 255, 128, 0);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(4);
		canvas.drawPath(outerShape, outerPaint);
		canvas.drawRect(inner, borderPaint);
	}
}
