package com.oymotion.gforcedev.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.oymotion.gforcedev.R;
import com.oymotion.gforcedev.global.OymotionApplication;


/**
 * ToastUtil
 * @author MouMou
 */


public class ToastUtil {
	private static Toast toast;
	private static Toast customToast;

	public static void showToast(String text){
		if (customToast !=null){
			customToast.cancel();
			customToast = null;
		}
		if(toast==null){
			toast = Toast.makeText(OymotionApplication.context, text,Toast.LENGTH_SHORT);
		}else {
			toast.setText(text);//if the toast is not null,change the text soon
		}
		toast.show();
	}

	public static void showCenterToast(String msg) {
		if (toast != null){
			toast.cancel();
			toast = null;
		}
		if (customToast == null) {
			LayoutInflater inflater = (LayoutInflater) OymotionApplication.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.view_toast_custom,null);
			customToast = new Toast(OymotionApplication.context);
			customToast.setGravity(Gravity.CENTER,0,0);
			customToast.setDuration(Toast.LENGTH_LONG);
			customToast.setView(view);
		}
		TextView textView = (TextView) customToast.getView().findViewById(R.id.tv_message);
		textView.setText(msg);
		customToast.show();
	}
	public static void showCenterToast(String title, String msg) {
		if (toast != null){
			toast.cancel();
			toast = null;
		}
		if (customToast == null) {
			LayoutInflater inflater = (LayoutInflater) OymotionApplication.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.view_toast_custom,null);
			customToast = new Toast(OymotionApplication.context);
			customToast.setGravity(Gravity.CENTER,0,0);
			customToast.setDuration(Toast.LENGTH_LONG);
			customToast.setView(view);
		}
		TextView tv_message = (TextView) customToast.getView().findViewById(R.id.tv_message);
		tv_message.setText(msg);
		TextView tv_title = (TextView) customToast.getView().findViewById(R.id.tv_title);
		tv_title.setText(title);
		customToast.getView().findViewById(R.id.tv_title).setVisibility(View.VISIBLE);
		customToast.getView().findViewById(R.id.iv_tag).setVisibility(View.GONE);
		customToast.show();
	}
	public static void showCenterToast(int res, String msg) {
		if (toast != null){
			toast.cancel();
			toast = null;
		}
		if (customToast == null) {
			LayoutInflater inflater = (LayoutInflater) OymotionApplication.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.view_toast_custom,null);
			customToast = new Toast(OymotionApplication.context);
			customToast.setGravity(Gravity.CENTER,0,0);
			customToast.setDuration(Toast.LENGTH_LONG);
			customToast.setView(view);
		}
		TextView tv_message = (TextView) customToast.getView().findViewById(R.id.tv_message);
		tv_message.setText(msg);
		ImageView iv_tag = (ImageView) customToast.getView().findViewById(R.id.iv_tag);
		customToast.getView().findViewById(R.id.tv_title).setVisibility(View.VISIBLE);
		iv_tag.setImageResource(res);
		customToast.getView().findViewById(R.id.tv_title).setVisibility(View.GONE);
		customToast.show();
	}
}
