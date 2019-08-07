package com.hand.wxtest.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public interface WechatService {
    public String getWxAccessToken();
    public String returnMsg(HttpServletRequest request, HttpServletResponse response);//被动回复
    public String createMenu(String url);//创建菜单
    public String sendAllMsg(MultipartFile imageUrl,MultipartFile thumbUrl);//群发消息
    public String getSenAllMsgStatus(String msgId);//查询群发消息的状态
}
