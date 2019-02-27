package com.tsy.sdk.social.sina;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WbAuthListener;
import com.sina.weibo.sdk.auth.WbConnectErrorMessage;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.share.WbShareCallback;
import com.sina.weibo.sdk.share.WbShareHandler;
import com.tsy.sdk.social.PlatformConfig;
import com.tsy.sdk.social.SSOHandler;
import com.tsy.sdk.social.listener.AuthListener;
import com.tsy.sdk.social.listener.ShareListener;
import com.tsy.sdk.social.share_media.IShareMedia;
import com.tsy.sdk.social.share_media.ShareTextImageMedia;
import com.tsy.sdk.social.util.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 新浪微博 第三方Hnadler
 * Created by tsy on 16/9/18.
 */
public class SinaWBHandler extends SSOHandler {

    private Context mContext;

    private AuthInfo mAuthInfo;
    private SsoHandler mSsoHandler;
    private WbShareHandler mWeiboShareHandler;

    private PlatformConfig.SinaWB mConfig;
    private AuthListener mAuthListener;
    private ShareListener mShareListener;

    private static String REDIRECT_URL = "https://api.weibo.com/oauth2/default.html";// 应用的回调页 要和微博开放平台的回调地址一致
    private final String SCOPE = "";

    /**
     * 设置微博 REDIRECT_URL
     * @param redirctUrl
     */
    public static void setRedirctUrl(String redirctUrl) {
        REDIRECT_URL = redirctUrl;
    }

    @Override
    public void onCreate(Context context, PlatformConfig.Platform config) {
        this.mContext = context;
        this.mConfig = (PlatformConfig.SinaWB) config;
        this.mAuthInfo = new AuthInfo(mContext, mConfig.appKey, REDIRECT_URL, SCOPE);
    }

    @Override
    public void authorize(Activity activity, AuthListener authListener) {
        WbSdk.install(activity, mAuthInfo);
        this.mAuthListener = authListener;
        this.mSsoHandler = new SsoHandler(activity);
        mSsoHandler.authorize(new WbAuthListener() {

            @Override
            public void onSuccess(Oauth2AccessToken oauth2AccessToken) {
                if(oauth2AccessToken.isSessionValid()) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("uid", oauth2AccessToken.getUid());
                    map.put("access_token", oauth2AccessToken.getToken());
                    map.put("refresh_token", oauth2AccessToken.getRefreshToken());
                    map.put("expire_time", "" + oauth2AccessToken.getExpiresTime());

                    mAuthListener.onComplete(mConfig.getName(), map);
                } else {
                    String errmsg = "errmsg=accessToken is not SessionValid";
                    LogUtils.e(errmsg);
                    mAuthListener.onError(mConfig.getName(), errmsg);
                }
            }

            @Override
            public void cancel() {
                mAuthListener.onCancel(mConfig.getName());
            }

            @Override
            public void onFailure(WbConnectErrorMessage wbConnectErrorMessage) {
                String errmsg = "errCode = " + wbConnectErrorMessage.getErrorCode() + " || errMsg = " + wbConnectErrorMessage.getErrorMessage();
                LogUtils.e(errmsg);
                mAuthListener.onError(mConfig.getName(), errmsg);
            }
        });
    }

    @Override
    public void share(Activity activity, IShareMedia shareMedia, ShareListener shareListener) {
        WbSdk.install(activity, mAuthInfo);
        this.mShareListener = shareListener;
        this.mSsoHandler = new SsoHandler(activity);
        this.mWeiboShareHandler = new WbShareHandler(activity);
        this.mWeiboShareHandler.registerApp();
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();

        if(shareMedia instanceof ShareTextImageMedia) {       //文字图片分享
            ShareTextImageMedia shareTextImageMedia = (ShareTextImageMedia) shareMedia;

            if(shareTextImageMedia.getText().length() > 0) {
                TextObject textObject = new TextObject();
                textObject.text = shareTextImageMedia.getText();
                weiboMessage.textObject = textObject;
            }

            if(shareTextImageMedia.getImage() != null) {
                ImageObject imageObject = new ImageObject();
                imageObject.setImageObject(shareTextImageMedia.getImage());
                weiboMessage.imageObject = imageObject;
            }
        } else {
            if(this.mShareListener != null) {
                this.mShareListener.onError(this.mConfig.getName(), "weibo is not support this shareMedia");
            }
            return ;
        }

        mWeiboShareHandler.shareMessage(weiboMessage, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    public void onNewIntent(Intent intent, WbShareCallback callback) {
        if(mWeiboShareHandler != null) {
            mWeiboShareHandler.doResultIntent(intent, callback);
        }
    }

    public void onWbShareSuccess() {
        if(this.mShareListener != null) {
            this.mShareListener.onComplete(this.mConfig.getName());
        }
    }

    public void onWbShareCancel() {
        if(this.mShareListener != null) {
            this.mShareListener.onCancel(this.mConfig.getName());
        }
    }

    public void onWbShareFail() {
        if(this.mShareListener != null) {
            this.mShareListener.onError(this.mConfig.getName(), "sina share failed");
        }
    }
}
