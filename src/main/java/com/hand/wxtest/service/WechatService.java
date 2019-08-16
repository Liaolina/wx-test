package com.hand.wxtest.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Map;

public interface WechatService {
    String getWxAccessToken();
    String returnMsg(HttpServletRequest request, HttpServletResponse response);//被动回复
    String createMenu(String url);//创建菜单
    String sendAllMsg(MultipartFile imageUrl,MultipartFile thumbUrl);//群发消息
    String getSenAllMsgStatus(String msgId);//查询群发消息的状态
    String sendTemplateMsg();//发送模板消息
    Map<String,String>  getJSSDKParam(String url);//获取JS-SDK所需的参数
}
