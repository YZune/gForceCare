package com.oymotion.gforcedev.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.oymotion.gforcedev.R;
import com.oymotion.gforcedev.global.OymotionApplication;
import com.oymotion.gforcedev.utils.ToastUtil;

public class LoadingDialog extends Dialog {

	private Context mContext;
	private ImageView icon;
	private String contentString = "loading...";
	private TextView content;
	private boolean isOnLoading = true;
	private IsBackPress isBackPress;

	private Animation animation = null;

	public interface IsBackPress{
		void closePage();
	}

	public void setIsBackPress(IsBackPress press){
		isBackPress = press;
	}

	public LoadingDialog(Context context) {
		super(context, R.style.normal_dialog);
		this.mContext = context;
	}

	private void initWidget() {
		this.icon = ((ImageView) findViewById(R.id.dialog_wait_icon));
		this.content = ((TextView) findViewById(R.id.dialog_wait_content));
	}

	private void init() {
		animation = AnimationUtils.loadAnimation(mContext,
				R.anim.loading_animation);
	}

	private void setAnimation() {
		this.icon.startAnimation(this.animation);
	}

	public void setContent(String paramString) {
		this.contentString = paramString;
		if (this.content != null)
			this.content.setText(this.contentString);
	}

	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.dialog_wait);
		initWidget();
		init();
	}

	public void show() {
		super.show();
		setAnimation();
		this.content.setText(this.contentString);
	}

	@Override
	public void onBackPressed() {
		isBackPress.closePage();
		super.onBackPressed();
	}
}
