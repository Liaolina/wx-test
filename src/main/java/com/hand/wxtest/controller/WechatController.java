package com.hand.wxtest.controller;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.hand.wxtest.service.WechatService;
import com.hand.wxtest.util.CheckUtil;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
public class WechatController {
    @Autowired
    WechatService wechatService;

    /**
     * 服务器配置提交时的校验确认
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/wechat")
    public String login(HttpServletRequest request, HttpServletResponse response){
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        String echostr = request.getParameter("echostr");
        if(CheckUtil.checkSignature(signature,timestamp,nonce)){
            return echostr;
        }
        return null;
    }

    /**
     * 被动回复消息
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/wechat")
    public String answer(HttpServletRequest request,HttpServletResponse response){
        //设置编码
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            String msgXml = wechatService.returnMsg(request,response);
            return msgXml;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取access_Token
     */
    @GetMapping("/getAccessToken")
    public void getWxAccessToken(){
        String str = wechatService.getWxAccessToken();
        System.out.println(str);
    }

    /**
     * 自定义菜单创建
     * @return
     */
    @PostMapping("/createMenu")
    public String createMenu(){
        String url = " https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN";
        String result = wechatService.createMenu(url);
        return result;
    }

    /**
     * 群发消息
     * @param imageUrl
     * @param thumbUrl
     * @return
     */
    @PostMapping("/sendAllMsg")
    public String sendAllMsg(@RequestParam("imageUrl") MultipartFile imageUrl,@RequestParam("thumbUrl")MultipartFile thumbUrl){
        String msgId = wechatService.sendAllMsg(imageUrl,thumbUrl);
        return msgId;
    }

    /**
     * 查看群发消息的状态
     * @param msgId
     * @return
     */
    @GetMapping("/checkSendAllMsgStatus")
    public String checkSendAllMsgStatus(String msgId){
        String status = wechatService.getSenAllMsgStatus(msgId);
        if("SEND_SUCCESS".equals(status)){
            return "发送成功";
        }else if("DELETE".equals(status)){
            return "已删除";
        }else if("SENDING".equals(status)){
            return "发送中";
        }else if("SEND_FAIL".equals(status)){
            return "发送失败";
        }else {
            return "未知错误";
        }
    }

    /**
     * 测试用户网页授权的重定向
     * @param request
     * @param response
     */
    @GetMapping("/test")
    public void test(HttpServletRequest request,HttpServletResponse response){
        String code = request.getParameter("code");
        System.out.println(code);
    }

    /**
     * 发送模板消息
     * @return
     */
    @GetMapping("/sendTemplateMsg")
    public String sendTemplateMsg(){
        return wechatService.sendTemplateMsg();
    }

    /**
     * 获取JS-SDK所需的参数
     * @param url
     * @return
     */
    @GetMapping("/getJsSDKParam")
    public String getJSSDKParam(String url){
        Map<String,String> map = wechatService.getJSSDKParam(url);
        return JSONObject.fromObject(map).toString();
    }
}
