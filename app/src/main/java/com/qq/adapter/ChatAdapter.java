package com.qq.adapter;

import java.io.File;
import java.util.List;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.FinalHttp;

import com.qq.R;
import com.qq.activity.ImgPageActivity;
import com.qq.bean.Msg;
import com.qq.util.Const;
import com.qq.util.ExpressionUtil;
import com.qq.util.PreferencesUtils;
import com.qq.util.ToastUtil;
import com.qq.view.CircleImageView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


/**
 * from为收到的消息，to为自己的消息
 * @author baiyuliang
 *
 */
@SuppressLint("NewApi")
public class ChatAdapter extends BaseAdapter {
	private Context mContext;
	private List<Msg> list;
	private FinalBitmap finalImageLoader ;
	private FinalHttp fh;  
	AnimationDrawable anim;		
	String mheadpath,xgzheadpath;//个人头像路径
	boolean ismHeadExsits=false;
	boolean isxgzHeadExsits=false;

	public ChatAdapter(Context mContext, List<Msg> list) {
		super();
		this.mContext = mContext;
		this.list = list;
		finalImageLoader=FinalBitmap.create(mContext);
		finalImageLoader.configLoadingImage(R.drawable.location_default);
		fh=new FinalHttp();
		
	}

	public void setList(List<Msg> list) {
		this.list = list;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHodler hodler;
		if (convertView == null) {
			hodler = new ViewHodler();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_lv_item, null);
			hodler.rl_chat= (RelativeLayout) convertView.findViewById(R.id.rl_chat);//聊天布局
			//接收的消息
			hodler.fromIcon= (CircleImageView) convertView.findViewById(R.id.chatfrom_icon);//他人头像
			hodler.toIcon= (CircleImageView) convertView.findViewById(R.id.chatto_icon);//自己头像
			hodler.fromContainer = (LinearLayout) convertView.findViewById(R.id.chart_from_container);
			hodler.fromText = (TextView) convertView.findViewById(R.id.chatfrom_content);//文本
			hodler.fromImg= (ImageView) convertView.findViewById(R.id.chatfrom_img);//图片
			hodler.fromLocation= (ImageView) convertView.findViewById(R.id.chatfrom_location);//位置
			hodler.progress_load=(ProgressBar) convertView.findViewById(R.id.progress_load);//ProgressBar
			//发送的消息
			hodler.toContainer = (RelativeLayout) convertView.findViewById(R.id.chart_to_container);
			hodler.toText = (TextView) convertView.findViewById(R.id.chatto_content);//文本
			hodler.toImg = (ImageView) convertView.findViewById(R.id.chatto_img);//图片
			hodler.toLocation= (ImageView) convertView.findViewById(R.id.chatto_location);//位置
			//时间
			hodler.time = (TextView) convertView.findViewById(R.id.chat_time);
			
			convertView.setTag(hodler);
		} else {
			hodler = (ViewHodler) convertView.getTag();
		}
		
		final Msg msg=list.get(position);

		if (msg.getIsComing()== 0) {// 收到消息 from显示
			hodler.toContainer.setVisibility(View.GONE);//隐藏右侧布局
			hodler.fromContainer.setVisibility(View.VISIBLE);
			hodler.time.setText(msg.getDate());
			if(msg.getType().equals(Const.MSG_TYPE_TEXT)){//文本类型
				hodler.fromText.setVisibility(View.VISIBLE);//文本
				hodler.fromImg.setVisibility(View.GONE);//图片
				hodler.fromLocation.setVisibility(View.GONE);//位置
				hodler.progress_load.setVisibility(View.GONE);
				SpannableStringBuilder sb = ExpressionUtil.prase(mContext,hodler.fromText,msg.getContent());// 对内容做处理
				hodler.fromText.setText(sb);
				Linkify.addLinks(hodler.fromText,Linkify.ALL);//增加文本链接类型
			}else if(msg.getType().equals(Const.MSG_TYPE_IMG)){//图片类型
				hodler.fromText.setVisibility(View.GONE);//文本
				hodler.fromImg.setVisibility(View.VISIBLE);//图片
				hodler.fromLocation.setVisibility(View.GONE);//位置
				hodler.progress_load.setVisibility(View.GONE);
				finalImageLoader.display(hodler.fromImg, msg.getContent());//加载图片
			}else if(msg.getType().equals(Const.MSG_TYPE_LOCATION)){//位置类型
				hodler.fromText.setVisibility(View.GONE);//文本
				hodler.fromImg.setVisibility(View.GONE);//图片
				hodler.fromLocation.setVisibility(View.VISIBLE);//位置
				hodler.progress_load.setVisibility(View.GONE);
				String lat=msg.getContent();//经纬度
				if(TextUtils.isEmpty(lat)){
					lat="116.404,39.915";//北京
				}
				getImg(hodler.fromLocation, Const.LOCATION_URL_S+lat+"&markers=|"+lat+"&markerStyles=l,A,0xFF0000");//加载网络图片
			}
		} else {// 发送消息 to显示（目前发送消息只能发送文本类型，后期将会增加其它类型）
			hodler.toContainer.setVisibility(View.VISIBLE);
			hodler.fromContainer.setVisibility(View.GONE);
			hodler.time.setText(msg.getDate());
			if(msg.getType().equals(Const.MSG_TYPE_TEXT)){//文本类型
				hodler.toText.setVisibility(View.VISIBLE);//文本
				hodler.toImg.setVisibility(View.GONE);//图片
				hodler.toLocation.setVisibility(View.GONE);//位置
				SpannableStringBuilder sb = ExpressionUtil.prase(mContext,hodler.toText,msg.getContent());// 对内容做处理
				hodler.toText.setText(sb);
				Linkify.addLinks(hodler.toText,Linkify.ALL);
			}else if(msg.getType().equals(Const.MSG_TYPE_IMG)){//图片类型
				hodler.toText.setVisibility(View.GONE);//文本
				hodler.toImg.setVisibility(View.VISIBLE);//图片
				hodler.toLocation.setVisibility(View.GONE);//位置
				finalImageLoader.display(hodler.toImg, msg.getContent());//加载图片
			}else if(msg.getType().equals(Const.MSG_TYPE_LOCATION)){//位置类型
				hodler.toText.setVisibility(View.GONE);//文本
				hodler.toImg.setVisibility(View.GONE);//图片
				hodler.toLocation.setVisibility(View.VISIBLE);//位置
				String lat=msg.getContent();//经纬度
				if(TextUtils.isEmpty(lat)){
					lat="116.404,39.915";//北京
				}
				getImg(hodler.toLocation, Const.LOCATION_URL_S+lat+"&markers=|"+lat+"&markerStyles=l,A,0xFF0000");//加载网络图片
			}
		}

		// 文本点击
		hodler.fromText.setOnClickListener(new onClick(position,msg));
		hodler.fromText.setOnLongClickListener(new onLongCilck(position));
		
		hodler.toText.setOnClickListener(new onClick(position,msg));
		hodler.toText.setOnLongClickListener(new onLongCilck(position));
		//图片点击
		hodler.fromImg.setOnClickListener(new onClick(position,msg));
		hodler.fromImg.setOnLongClickListener(new onLongCilck(position));
		hodler.toImg.setOnClickListener(new onClick(position,msg));
		hodler.toImg.setOnLongClickListener(new onLongCilck(position));
		//位置
		hodler.fromLocation.setOnClickListener(new onClick(position,msg));
		hodler.fromLocation.setOnLongClickListener(new onLongCilck(position));
		hodler.toLocation.setOnClickListener(new onClick(position,msg));
		hodler.toLocation.setOnLongClickListener(new onLongCilck(position));
		
		return convertView;
	}
	
	void getImg(ImageView iv,String path){
		if(!TextUtils.isEmpty(path)){
			finalImageLoader.display(iv, path);
		}else{
			iv.setImageResource(R.drawable.ic_launcher);
		}
	}


	class ViewHodler {
		RelativeLayout rl_chat;
		CircleImageView fromIcon, toIcon;
		ImageView fromImg,fromLocation,toImg,toLocation;
		TextView fromText, toText, time;
		LinearLayout fromContainer;
		RelativeLayout toContainer;
		ProgressBar progress_load;
	}

	/**
	 * 屏蔽listitem的所有事件
	 * */
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}
	
	/**
	 * 点击监听
	 * @author 白玉梁
	 *
	 */
	class onClick implements OnClickListener{
		int position;
		Msg msg;
		public onClick(int position,Msg msg){
			this.position=position;
			this.msg=msg;
		}
		@Override
		public void onClick(View arg0) {
			String content=msg.getContent();
				if(msg.getType().equals(Const.MSG_TYPE_IMG)){//图片
					Intent intentImg=new Intent(mContext, ImgPageActivity.class);
					intentImg.putExtra("url", content);
					mContext.startActivity(intentImg);
				}else if(msg.getType().equals(Const.MSG_TYPE_LOCATION)){//位置
					String address=PreferencesUtils.getSharePreStr(mContext, "location_adr_detail");//详细地址
					if(TextUtils.isEmpty(address)){
						address="无法获取当前位置";
					}
					Toast.makeText(mContext,address, Toast.LENGTH_LONG).show();
					String lat=PreferencesUtils.getSharePreStr(mContext, "location_my");//经纬度
					if(TextUtils.isEmpty(lat)){
						lat="116.404,39.915";//北京
					}
					Intent intentMap=new Intent(mContext, ImgPageActivity.class);
					intentMap.putExtra("url", Const.LOCATION_URL_L+lat+"&markers=|"+lat+"&markerStyles=l,A,0xFF0000");
					mContext.startActivity(intentMap);
				}else{
					
				}
		}
		
	}
	
	/**
	 * 长按监听
	 * @author 白玉梁
	 *
	 */
	class onLongCilck implements OnLongClickListener{
		int position;
		public onLongCilck(int position){
			this.position=position;
		}
		@Override
		public boolean onLongClick(View arg0) {
			Intent intent=new Intent(Const.ACTION_MSG_OPER);
			intent.putExtra("type", 1);
			intent.putExtra("position", position);
			mContext.sendBroadcast(intent);
			return true;
		}
	}

}
