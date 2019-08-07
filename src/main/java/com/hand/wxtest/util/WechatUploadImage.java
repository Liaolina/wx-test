package com.hand.wxtest.util;

import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 上传文件的请求
 */
@Component
public class WechatUploadImage {

    /**
     *
     * @param urlStr 请求的接口url
     * @param imagePath 上传的文件
     * @return
     */
    public JSONObject wxUploadImage(String urlStr, MultipartFile imagePath){
        String result = "";
        String BOUNDARY = "-------------123821742118716";//分隔符
        HttpURLConnection conn = null;
        JSONObject jsonObject = null;
        try {
            URL url = new URL(urlStr);
            MultipartFile file = imagePath;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            OutputStream out = new DataOutputStream(conn.getOutputStream());

            String inputName = "media";
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            if (fileName.endsWith(".png")) {
                contentType = "image/png";
            }
            if (contentType == null || contentType.equals("")) {
                contentType = "application/octet-stream";
            }

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
            stringBuffer.append(
                    "Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + fileName + "\"\r\n");
            stringBuffer.append("Content-Type:" + contentType + "\r\n\r\n");
            out.write(stringBuffer.toString().getBytes());
            DataInputStream in = new DataInputStream(file.getInputStream());
            int bytes = 0;
            byte[] bufferOut = new byte[1024];
            while ((bytes = in.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            in.close();
            byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();

            // 读取返回数据
            StringBuffer strBuf2 = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                strBuf2.append(line).append("\n");
            }
            result = strBuf2.toString();
            reader.close();
            reader = null;
            jsonObject = JSONObject.fromObject(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
        return jsonObject;
    }
}
