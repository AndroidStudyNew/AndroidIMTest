package com.qq.listener;

import java.util.Date;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.packet.Message;

import com.qq.QQApplication;
import com.qq.R;
import com.qq.bean.Msg;
import com.qq.bean.Session;
import com.qq.db.ChatMsgDao;
import com.qq.db.SessionDao;
import com.qq.service.MsfService;
import com.qq.util.Const;
import com.qq.util.PreferencesUtils;
import com.qq.util.ToastUtil;
import com.qq.util.XmppUtil;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;


/**
 * @author baiyuliang
 */

@SuppressWarnings("static-access")
public class MsgListener implements MessageListener{
	
	private MsfService context;
	private NotificationManager mNotificationManager;
	
	
	private Notification mNotification;
	private KeyguardManager mKeyguardManager = null;
	
	private boolean isShowNotice=false;
	
	private ChatMsgDao msgDao;
	private SessionDao sessionDao;
	
	public MsgListener(MsfService context,NotificationManager mNotificationManager){
		this.context=context;
		this.mNotificationManager=mNotificationManager;
		mKeyguardManager = (KeyguardManager) context.getSystemService(context.KEYGUARD_SERVICE);    
		sessionDao=new SessionDao(context);
		msgDao=new ChatMsgDao(context);
	}
	
	@Override
	public void processMessage(Chat arg0, Message message) {
		try {
			String msgBody = message.getBody();
			if (TextUtils.isEmpty(msgBody))
				return;
			//接收者卍发送者卍消息类型卍消息内容卍发送时间
			String[] msgs=msgBody.split(Const.SPLIT);
			String to=msgs[0];//接收者,当然是自己
			String from=msgs[1];//发送者，谁给你发的消息
			String msgtype=msgs[2];//消息类型
			String msgcontent=msgs[3];//消息内容
			String msgtime=msgs[4];//消息时间
			
			Session session=new Session();
			session.setFrom(from);
			session.setTo(to);
			session.setNotReadCount("");//未读消息数量
			session.setTime(msgtime);
			
			if(msgtype.equals(Const.MSG_TYPE_ADD_FRIEND)){//添加好友的请求
				session.setType(msgtype);
				session.setContent(msgcontent);
				session.setIsdispose("0");
				sessionDao.insertSession(session);
			}else	if(msgtype.equals(Const.MSG_TYPE_ADD_FRIEND_SUCCESS)){//对方同意添加好友的请求
				session.setType(Const.MSG_TYPE_TEXT);
				session.setContent("我们已经是好友了，快来和我聊天吧！");
				sessionDao.insertSession(session);
				//发送广播更新好友列表
				 Intent intent=new Intent(Const.ACTION_FRIENDS_ONLINE_STATUS_CHANGE);
	        	 context.sendBroadcast(intent);
			}else if(msgtype.equals(Const.MSG_TYPE_TEXT)){//文本类型
				Msg msg=new Msg();
				msg.setToUser(to);
				msg.setFromUser(from);
				msg.setIsComing(0);
				msg.setContent(msgcontent);
				msg.setDate(msgtime);
				msg.setIsReaded("0");
				msg.setType(msgtype);
				msgDao.insert(msg);
				sendNewMsg(msg);
				
				session.setType(Const.MSG_TYPE_TEXT);
				session.setContent(msgcontent);
				if(sessionDao.isContent(from, to)){//判断最近联系人列表是否已存在记录
					sessionDao.updateSession(session);
				}else{
					sessionDao.insertSession(session);
				}
			}else if(msgtype.equals(Const.MSG_TYPE_IMG)){
				Msg msg=new Msg();
				msg.setToUser(to);
				msg.setFromUser(from);
				msg.setIsComing(0);
				msg.setContent(msgcontent);
				msg.setDate(msgtime);
				msg.setIsReaded("0");
				msg.setType(msgtype);
				msgDao.insert(msg);
				sendNewMsg(msg);
				
				session.setType(Const.MSG_TYPE_TEXT);
				session.setContent("[图片]");
				if(sessionDao.isContent(from, to)){
					sessionDao.updateSession(session);
				}else{
					sessionDao.insertSession(session);
				}
			}else if(msgtype.equals(Const.MSG_TYPE_LOCATION)){//位置
				Msg msg=new Msg();
				msg.setToUser(to);
				msg.setFromUser(from);
				msg.setIsComing(0);
				msg.setContent(msgcontent);
				msg.setDate(msgtime);
				msg.setIsReaded("0");
				msg.setType(msgtype);
				msgDao.insert(msg);
				sendNewMsg(msg);
				
				session.setType(Const.MSG_TYPE_TEXT);
				session.setContent("[位置]");
				if(sessionDao.isContent(from, to)){
					sessionDao.updateSession(session);
				}else{
					sessionDao.insertSession(session);
				}
			}
			
			Intent intent=new Intent(Const.ACTION_ADDFRIEND);//发送广播，通知消息界面更新
			context.sendBroadcast(intent);
			
			showNotice(session.getFrom()+":"+session.getContent());
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	void sendNewMsg(Msg msg){
		Intent intent=new Intent(Const.ACTION_NEW_MSG);//发送广播到聊天界面
		Bundle b=new Bundle();
		b.putSerializable("msg", msg);
		intent.putExtra("msg", b);
		context.sendBroadcast(intent);
	}
	
	@SuppressWarnings("deprecation")
	public void showNotice(String content) {
		// 更新通知栏
		CharSequence tickerText = content;
//		mNotification = new Notification(R.drawable.ic_notice, tickerText, System.currentTimeMillis());

		// API11之后才支持
		mNotification = new Notification.Builder(context)
				.setSmallIcon(R.drawable.ic_notice) // 设置状态栏中的小图片，尺寸一般建议在24×24，这个图片同样也是在下拉状态栏中所显示，如果在那里需要更换更大的图片，可以使用setLargeIcon(Bitmap
				.setTicker(tickerText)// 设置在status
				// bar上显示的提示文字
				.setContentTitle("setContentTitle : 您有新消息")// 设置在下拉status
				// bar后Activity，本例子中的NotififyMessage的TextView中显示的标题
//				.setContentText(sentence)// TextView中显示的详细内容
//				.setContentIntent(contentIntent) // 关联PendingIntent
				.setNumber(1) // 在TextView的右方显示的数字，可放大图片看，在最右侧。这个number同时也起到一个序列号的左右，如果多个触发多个通知（同一ID），可以指定显示哪一个。
				.getNotification(); // 需要注意build()是在API level

		mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		if(PreferencesUtils.getSharePreBoolean(context, Const.MSG_IS_VOICE)){
			// 设置默认声音
			mNotification.defaults |= Notification.DEFAULT_SOUND;
		}
		if(PreferencesUtils.getSharePreBoolean(context, Const.MSG_IS_VIBRATE)){
			// 设定震动(需加VIBRATE权限)
			mNotification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		// LED灯
		mNotification.defaults |= Notification.DEFAULT_LIGHTS;
		mNotification.ledARGB = 0xff00ff00;
		mNotification.ledOnMS = 500;
		mNotification.ledOffMS = 1000;
		mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
//		mNotification.setLatestEventInfo(context, "新消息", tickerText, null);

		mNotificationManager.notify(Const.NOTIFY_ID, mNotification);// 通知
	}
	
 }

