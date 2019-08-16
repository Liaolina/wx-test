package com.hand.wxtest.service;

import com.hand.wxtest.bean.*;
import com.hand.wxtest.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WechatServiceImpl implements WechatService{

    @Autowired
    private WeixinCommenUtil weixinCommenUtil;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    WeixinMessageUtil weixinMessageUtil;
    @Autowired
    HttpRequestUtil httpRequestUtil;
    @Autowired
    WechatUploadImage wechatUploadImage;
    @Autowired
    WeixinJSSDKParamUtil weixinJSSDKParamUtil;

    /**
     * 获取Access_Token
     * @return
     */
    @Scheduled(initialDelay = 1000,fixedDelay = 7000*1000)
    public String getWxAccessToken(){
        try {
            String token = weixinCommenUtil.getToken(WeixinConstants.APPID,WeixinConstants.APPSECRET).getAccess_token();
            stringRedisTemplate.opsForValue().set("accessToken",token,7000*1000, TimeUnit.SECONDS);
            String jsapiTicket = weixinJSSDKParamUtil.getJsapiTicket(token);
            return "获取到的微信access_token为"+token+"\n"+"获取到的微信JSSDK jsapi_ticket为"+jsapiTicket;
        } catch (Exception e) {
            e.printStackTrace();
            this.getWxAccessToken();
            return "failed";
        }
    }
    /**
     * 被动消息回复
     * @param request
     * @param response
     * @return
     */
    @Override
    public String returnMsg(HttpServletRequest request, HttpServletResponse response) {
        String respMessage = null;//返回给微信的消息，默认为null
        try {
            String respContent = null;//返回的文本消息
            String mediaId = "";
            //xml解析
            Map<String,String> map = weixinMessageUtil.parseXml(request);

            String fromUserName = map.get("FromUserName");//发送方账号
            String toUserName = map.get("ToUserName");//接收方账号
            String msgType = map.get("MsgType");//消息类型
            String content = map.get("Content");//内容
            TextMessage textMessage = new TextMessage();
            textMessage.setToUserName(fromUserName);
            textMessage.setFromUserName(toUserName);
            textMessage.setCreateTime(new Date().getTime());
            textMessage.setMsgType(weixinMessageUtil.RESP_MESSAGE_TYPE_TEXT);
            if(msgType.equals(weixinMessageUtil.REQ_MESSAGE_TYPE_TEXT)){
                if("1".equals(content)){
                    respContent = "文本消息：text1";
                }
                else if("2".equals(content)){
                    respContent = "文本消息：text2";
                }
                else{
                    respContent = "请按照规则提问噢！";
                }
                textMessage.setContent(respContent);
                respMessage = weixinMessageUtil.textMessageToXml(textMessage);
            }
            else if(msgType.equals(weixinMessageUtil.REQ_MESSAGE_TYPE_VOICE)){
                respContent = "语音暂时无法回复";
                textMessage.setContent(respContent);
                respMessage = weixinMessageUtil.textMessageToXml(textMessage);
            }
            else if(msgType.equals(weixinMessageUtil.REQ_MESSAGE_TYPE_IMAGE)){
                mediaId = map.get("MediaId");
                ImageMessage imageMessage = new ImageMessage();
                Image image = new Image();
                imageMessage.setToUserName(fromUserName);
                imageMessage.setFromUserName(toUserName);
                imageMessage.setCreateTime(new Date().getTime());
                imageMessage.setMsgType(weixinMessageUtil.RESP_MESSAGE_TYPE_IMAGE);
                image.setMediaId(mediaId);
                imageMessage.setImage(image);
                respMessage = weixinMessageUtil.imageMessageToXml(imageMessage);
            }
            else if(msgType.equals(weixinMessageUtil.REQ_MESSAGE_TYPE_EVENT)){
                NewsMessage newsMessage = new NewsMessage();
                newsMessage.setToUserName(fromUserName);
                newsMessage.setFromUserName(toUserName);
                newsMessage.setCreateTime(new Date().getTime());
                if("CLICK".equals(map.get("Event")))
                {
                    if("V1001_TODAY_MUSIC".equals(map.get("EventKey")))
                    {
                        newsMessage.setMsgType(weixinMessageUtil.RESP_MESSAGE_TYPE_NEWS);
                        newsMessage.setArticleCount(1);
                        List<Article> articles = new ArrayList<>();
                        Article article = new Article();
                        article.setTitle("今日音乐");
                        article.setDescription("函数时隔三年再次登上舞台，泪奔！！！");
                        article.setPicUrl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1565005742493&di=6da44cda015ccd972214ecf954cccdd3&imgtype=0&src=http%3A%2F%2Fwww.zhuyin.com%2Ffpic%2F2542.jpg");
                        article.setUrl("https://music.163.com/#/artist?id=127043");
                        articles.add(article);
                        newsMessage.setArticles(articles);
                        respMessage = weixinMessageUtil.newsMessageToXml(newsMessage);
                    }
                    if("V1001_GOOD".equals(map.get("EventKey"))){
                        respContent = "谢谢您的支持！我们会继续努力的";
                        textMessage.setContent(respContent);
                        respMessage = weixinMessageUtil.textMessageToXml(textMessage);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("系统出错");
            respMessage = null;
        }
        finally {
            if(null == respMessage){
                respMessage = "";
            }
        }
        return  respMessage;
    }

    /**
     * 自定义菜单创建
     * @param url
     * @return
     */
    @Override
    public String createMenu(String url) {
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken");
        String requestUrl = url.replace("ACCESS_TOKEN",accessToken);
        List<Button> buttonList = new ArrayList<>();
        Button button1 = new Button();
        button1.setType("click");
        button1.setName("今日歌曲");
        button1.setKey("V1001_TODAY_MUSIC");
        buttonList.add(button1);

        Button button2 = new Button();
        button2.setName("菜单");
        List<Button> subButtonList = new ArrayList<>();
        Button subbutton1 = new Button();
        subbutton1.setType("view");
        subbutton1.setName("搜索");
//        subbutton1.setUrl("http://www.soso.com/");
        String urlAuth = WeixinConstants.WEB_AUTH_URL.replace("APPID",WeixinConstants.APPID)
                                                     .replace("REDIRECT_URI","https%3a%2f%2f2j6c073284.qicp.vip%2fwechatTest%2ftest")
                                                     .replace("SCOPE","snsapi_userinfo")
                                                     .replace("STATE","A");
        subbutton1.setUrl(urlAuth);
        subButtonList.add(subbutton1);
        Button subbutton2 = new Button();
        subbutton2.setType("miniprogram");
        subbutton2.setName("wxa");
        subbutton2.setUrl("http://mp.weixin.qq.com");
        subbutton2.setAppid("wx286b93c14bbf93aa");
        subbutton2.setPagepath("pages/lunar/index");
        subButtonList.add(subbutton2);
        Button subbutton3 = new Button();
        subbutton3.setType("click");
        subbutton3.setName("赞一下我们");
        subbutton3.setKey("V1001_GOOD");
        subButtonList.add(subbutton3);
        button2.setSub_button(subButtonList);
        buttonList.add(button2);

        JSONArray jsonArray = new JSONArray();
        String request = "{    \"button\": "+jsonArray.fromObject(buttonList).toString()+"}";
        System.out.println(request);
        JSONObject json = httpRequestUtil.httpsRequest(requestUrl,"POST",request);
        if (json!=null){
            int errcode = Integer.parseInt(json.getString("errcode"));
            if(errcode == 0){
                return "create success";
            }
            else {
                String errmsg = json.getString("errmsg");
                return "create failed："+errmsg;
            }
        }else {
            return "获取token失败 errcode:{} errmsg:{}";
        }
    }

    /**
     * 群发消息
     * @param imageUrl
     * @param thumbUrl
     * @return
     */
    @Override
    public String sendAllMsg(MultipartFile imageUrl,MultipartFile thumbUrl) {
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken");
        String urlImgStr = WeixinConstants.UPLOAD_IMG_URL.replace("ACCESS_TOKEN",accessToken);
        String urlNewsStr = WeixinConstants.UPLOAD_NEWS_URL.replace("ACCESS_TOKEN",accessToken);
        String urlSendAll = WeixinConstants.SEND_ALL_URL.replace("ACCESS_TOKEN",accessToken);
        String urlAddMat = WeixinConstants.ADD_MATERIAL_URL.replace("ACCESS_TOKEN",accessToken).replace("TYPE","thumb");
        String urlSendAllByOpenid = WeixinConstants.SEND_NEWS_BY_OPENID_URL.replace("ACCESS_TOKEN",accessToken);
        String urlGetUserList = WeixinConstants.GET_USER_LIST_URL.replace("ACCESS_TOKEN",accessToken).replace("NEXT_OPENID","");
        //1.上传图文消息中的图片并获取Url
        JSONObject jsonObject = wechatUploadImage.wxUploadImage(urlImgStr,imageUrl);
        String url = jsonObject.getString("url");
        //2.上传图文消息素材
        //新增临时素材
        JSONObject jsonAdd = wechatUploadImage.wxUploadImage(urlAddMat,thumbUrl);
        String thumbMediaId = jsonAdd.getString("thumb_media_id");

        String content = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "\t<head>\n" +
                "\t\t<meta charset=\"utf-8\">\n" +
                "\t\t<title>aaa</title>\n" +
                "\t</head>\n" +
                "\t<body>\n" +
                "\t\t<h1>asdasdadasdasdasdasdas</h1>\n" +
                "\t\t<p>sdfhaskldfhkashfkhasdkfhksdfhkshfklhskfhksdfh</p>\n" +
                "\t\t<img src=\""+url+"\"/>\n" +
                "\t</body>\n" +
                "</html>\n";
        Map<String,Object> map = new HashMap<>();
        List<SendAllArticle> list = new ArrayList<>();
        SendAllArticle article1 = new SendAllArticle();
        SendAllArticle article2 = new SendAllArticle();
        article1.setThumb_media_id(thumbMediaId);
        article1.setAuthor("Kryna");
        article1.setTitle("What a happy day");
        article1.setContent_source_url("www.qq.com");
        article1.setContent(content);
        article1.setDigest("");
        article1.setShow_cover_pic(1);
        article1.setNeed_open_comment(1);
        article1.setOnly_fans_can_comment(1);
        list.add(article1);

        article2.setThumb_media_id(thumbMediaId);
        article2.setAuthor("Kryna");
        article2.setTitle("What a happy day");
        article2.setContent_source_url("www.qq.com");
        article2.setContent(content);
        article2.setDigest("");
        article2.setShow_cover_pic(0);
        article2.setNeed_open_comment(1);
        article2.setOnly_fans_can_comment(1);
        list.add(article2);
        map.put("articles",list);
        String request = JSONObject.fromObject(map).toString();
        JSONObject json = httpRequestUtil.httpsRequest(urlNewsStr,"POST",request);
        String mediaId = json.getString("media_id");
        //3.根据openid进行群发
        //拉取用户列表获取openid
        JSONObject jsonUserList = httpRequestUtil.httpsRequest(urlGetUserList,"GET",null);
        System.out.println(jsonUserList.toString());
        JSONArray str = jsonUserList.getJSONObject("data").getJSONArray("openid");
        for (int i=0;i<str.size();i++){
            stringRedisTemplate.opsForValue().set("openId"+i,str.getString(i));
        }
//        String requestItem = "{\n" +
//                "   \"touser\":[\n" +
//                "    \"oUjOOwqdnHcXIS_B-BaSmVKoY_k4\",\n" +
//                "    \"oUjOOwgEmV0Jx6dl4KkG0CXvw53g\"\n" +
//                "   ],\n" +
//                "   \"mpnews\":{\n" +
//                "      \"media_id\":\""+mediaId+"\"\n" +
//                "   },\n" +
//                "    \"msgtype\":\"mpnews\"，\n" +
//                "    \"send_ignore_reprint\":0\n" +
//                "}";
        String requestItem = "{\n" +
                "   \"touser\":[\n" +
                "    \"oUjOOwqdnHcXIS_B-BaSmVKoY_k4\",\n" +
                "    \"oUjOOwgEmV0Jx6dl4KkG0CXvw53g\"\n" +
                "   ],\n" +
                "    \"msgtype\": \"text\",\n" +
                "    \"text\": { \"content\": \"hello from boxer.\"}\n" +
                "}";
        JSONObject jsonSend = httpRequestUtil.httpsRequest(urlSendAllByOpenid,"POST",requestItem);
        System.out.println("----"+requestItem);
        System.out.println(jsonSend.toString());
        String msgId = jsonSend.getString("msg_id");
        return msgId;
    }

    /**
     * 查看群发消息状态
     * @param msgId
     * @return
     */
    @Override
    public String getSenAllMsgStatus(String msgId) {
        //4.查询群发状态
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken");
        String urlCheckSendSts = WeixinConstants.CHECK_SEND_ALL_STS_URL.replace("ACCESS_TOKEN",accessToken);
        String requestCheck = "{\n" +
                "   \"msg_id\": \""+msgId+"\"\n" +
                "}";
        JSONObject jsonCheck = httpRequestUtil.httpsRequest(urlCheckSendSts,"POST",requestCheck);
        String msgStatus = jsonCheck.getString("msg_status");
        return msgStatus;
    }

    /**
     * 发送模板消息
     * @return
     */
    @Override
    public String sendTemplateMsg() {
        String accessToken = stringRedisTemplate.opsForValue().get("accessToken");
        String setInduUrl = WeixinConstants.SET_INDUSTRY_URL.replace("ACCESS_TOKEN",accessToken);
        String getTempIdUrl = WeixinConstants.GET_TEMPLATE_ID_URL.replace("ACCESS_TOKEN",accessToken);
        String sendTempMagUrl = WeixinConstants.SEND_TEMPLATE_MSG_URL.replace("ACCESS_TOKEN",accessToken);
        String postInduData = "{\n" +
                "    \"industry_id1\":\"1\",\n" +
                "    \"industry_id2\":\"4\"\n" +
                "}";
        //设置所属行业
        JSONObject setInduJson = httpRequestUtil.httpsRequest(setInduUrl,"POST",postInduData);
        //行业一个月只能修改一次
        if(Integer.parseInt(setInduJson.getString("errcode"))==0 || Integer.parseInt(setInduJson.getString("errcode"))==43100){
            String getTempIdData = "{\n" +
                    "    \"template_id_short\":\"TM00015\"\n" +
                    " }";
            //获取模板ID
            JSONObject getTempIdJson = httpRequestUtil.httpsRequest(getTempIdUrl,"POST",getTempIdData);
            if(Integer.parseInt(getTempIdJson.getString("errcode"))==0){
                String tempId = getTempIdJson.getString("template_id");
                String sendTempMsgData = "{\n" +
                        "    \"touser\":\"oUjOOwqdnHcXIS_B-BaSmVKoY_k4\",\n" +
                        "    \"template_id\":\""+tempId+"\",\n" +
                        "    \"url\":\"http://www.baidu.com\",\n" +
                        "    \"data\":{\n" +
                        "        \"first\":{\n" +
                        "            \"value\":\"我们已收到您的货款，开始为您打包商品，请耐心等待:\",\n" +
                        "            \"color\":\"#173177\"\n" +
                        "        },\n" +
                        "        \"orderMoneySum\":{\n" +
                        "            \"value\":\"150元\",\n" +
                        "            \"color\":\"#173177\"\n" +
                        "        },\n" +
                        "        \"orderProductName\":{\n" +
                        "            \"value\":\"针织衫\",\n" +
                        "            \"color\":\"#173177\"\n" +
                        "        },\n" +
                        "        \"keyword3\":{\n" +
                        "            \"value\":\"2014年9月22日\",\n" +
                        "            \"color\":\"#173177\"\n" +
                        "        },\n" +
                        "        \"remark\":{\n" +
                        "            \"value\":\"如有问题请致电400-828-1878或直接在微信留言，小易将第一时间为您服务！！\",\n" +
                        "            \"color\":\"#173177\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";
                //发送模板消息
                JSONObject senTempMsgJson = httpRequestUtil.httpsRequest(sendTempMagUrl,"POST",sendTempMsgData);
                System.out.println(senTempMsgJson.toString());
                if(Integer.parseInt(senTempMsgJson.getString("errcode"))==0){
                    return "发送成功";
                }
                else{
                    return "模板消息发送失败";
                }
            }
            else {
                return "获取模板ID出错";
            }
        }
        return "设置行业出错";
    }

    @Override
    public Map<String, String> getJSSDKParam(String url) {
        String jsapiTicket = stringRedisTemplate.opsForValue().get("jsapiTicket");
        Map<String,String> map = weixinJSSDKParamUtil.sign(jsapiTicket,url);
        return map;
    }
}
