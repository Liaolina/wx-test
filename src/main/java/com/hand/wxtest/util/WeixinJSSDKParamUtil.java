package com.hand.wxtest.util;

import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 关于微信JS-SDK相关的工具类
 */
@Component
public class WeixinJSSDKParamUtil {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    HttpRequestUtil httpRequestUtil;

    public String getJsapiTicket(String access_token){
        String url = WeixinConstants.GET_JSAPI_TICKET_URL.replace("ACCESS_TOKEN",access_token);
        JSONObject jsonObject = httpRequestUtil.httpsRequest(url,"GET",null);
        if (!jsonObject.isEmpty()&&"ok".equals(jsonObject.getString("errmsg"))){
            String jsapiTicket = jsonObject.getString("ticket");
            redisTemplate.opsForValue().set("jsapiTicket",jsapiTicket);
            return jsapiTicket;
        }
        return null;
    }

    //生成signature,并返回map（map里包含所需参数）
    public Map<String, String> sign(String jsapi_ticket, String url) {
        Map<String, String> ret = new HashMap<String, String>();
        String nonce_str = create_nonce_str();
        String timestamp = create_timestamp();
        String string1;
        String signature = "";

        //注意这里参数名必须全部小写，且必须有序
        string1 = "jsapi_ticket=" + jsapi_ticket +
                "&noncestr=" + nonce_str +
                "&timestamp=" + timestamp +
                "&url=" + url;
        System.out.println(string1);

        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(string1.getBytes("UTF-8"));
            signature = byteToHex(crypt.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        ret.put("url", url);
        ret.put("jsapi_ticket", jsapi_ticket);
        ret.put("nonceStr", nonce_str);
        ret.put("timestamp", timestamp);
        ret.put("signature", signature);

        return ret;
    }

    private String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    //获取随机字符
    private String create_nonce_str() {
        return UUID.randomUUID().toString();
    }
    //获取时间戳
    private String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
}
