package com.sismics.docs.core.util;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// Modify according to https://ai.youdao.com/DOCSIRMA/html/trans/api/wdfy/index.html
public class TranslationUtil {
    private static Logger logger = LoggerFactory.getLogger(TranslationUtil.class);

    private static final String YOUDAO_URL_UPLOAD = "https://openapi.youdao.com/file_trans/upload";

    private static final String YOUDAO_URL_QUERY = "https://openapi.youdao.com/file_trans/query";

    private static final String YOUDAO_URL_DOWNLOAD = "https://openapi.youdao.com/file_trans/download";

    private static final String APP_KEY = "1533a744ec72c0b0";

    private static final String APP_SECRET = "dkD1vrpDbsZ20QLQ1MRZidfaHhUIzOiL";

    public static void main(String[] args) throws IOException {
        String fileName = "RISC-V中文手册.pdf";
        String[] parts = fileName.split("\\.");
        if (parts.length == 1) {
            return;
        }
        String fileType = parts[parts.length - 1];

        String flownumber = upload("/mnt/f/pdf/" + fileName,
                fileName, fileType, "zh-CHS",
                "en");
        System.out.println(flownumber);
        int status = 1;
        while (status > 0 && status != 4) {
            status = query(flownumber);
            System.out.println("status: " + status);
            try {
                Thread.sleep(3000); // 1000 毫秒 = 1 秒
            } catch (InterruptedException e) {
                return;
            }
        }
        String tempName = download(flownumber, "/mnt/f/translate/");
        System.out.println(tempName);
    }

    public static String upload(String filePath, String fileName, String fileType, String langFrom,
                                String langTo) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        String q = loadAsBase64(filePath);
        String salt = String.valueOf(System.currentTimeMillis());
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = APP_KEY + truncate(q) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("q", q);
        params.put("fileName", fileName);
        params.put("fileType", fileType);
        params.put("langFrom", langFrom);
        params.put("langTo", langTo);
        params.put("appKey", APP_KEY);
        params.put("salt", salt);
        params.put("curtime", curtime);
        params.put("sign", sign);
        params.put("docType", "json");
        params.put("signType", "v3");
        String result = requestForHttp(YOUDAO_URL_UPLOAD, params);
        /** 处理结果 */
        JSONObject jsonObject = new JSONObject(result);
        String flownumber = jsonObject.getString("flownumber");
        String errorCode = jsonObject.getString("errorCode");
        if (!errorCode.equals("0")) {
            throw new RuntimeException();
        }
        return flownumber;
    }

    public static int query(String flownumber) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        String salt = String.valueOf(System.currentTimeMillis());
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = APP_KEY + truncate(flownumber) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("flownumber", flownumber);
        params.put("appKey", APP_KEY);
        params.put("salt", salt);
        params.put("curtime", curtime);
        params.put("sign", sign);
        params.put("docType", "json");
        params.put("signType", "v3");
        String result = requestForHttp(YOUDAO_URL_QUERY, params);
        /** 处理结果 */
        System.out.println(result);
        JSONObject jsonObject = new JSONObject(result);
        int status = jsonObject.getInt("status");
        String errorCode = jsonObject.getString("errorCode");
        if (!errorCode.equals("0")) {
            throw new RuntimeException();
        }
        return status;
    }

    public static String download(String flownumber, String saveDir) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        String salt = String.valueOf(System.currentTimeMillis());
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = APP_KEY + truncate(flownumber) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("flownumber", flownumber);
        params.put("downloadFileType", "pdf");
        params.put("appKey", APP_KEY);
        params.put("salt", salt);
        params.put("curtime", curtime);
        params.put("sign", sign);
        params.put("docType", "json");
        params.put("signType", "v3");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(YOUDAO_URL_DOWNLOAD);
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key, value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
        String fileName = UUID.randomUUID() + ".pdf";
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                // 确保保存目录存在
                File saveFile = new File(saveDir + fileName);
                saveFile.getParentFile().mkdirs();

                // 将文件流写入本地文件
                try (InputStream inputStream = httpEntity.getContent();
                     FileOutputStream outputStream = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                EntityUtils.consume(httpEntity);
            }
            return fileName;
        }
    }

    public static String requestForHttp(String url, Map<String, String> params) throws IOException {
        String result = "";

        /** 创建HttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

        /** httpPost */
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key, value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            result = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                System.out.println("## release resouce error ##" + e);
            }
        }
        return result;
    }

    /**
     * 生成加密字段
     */
    public static String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String loadAsBase64(String imgFile) {// 将文件转化为字节数组字符串，并对其进行Base64编码处理

        File file = new File(imgFile);
        if (!file.exists()) {
            System.out.println("文件不存在");
            return null;
        }
        InputStream in = null;
        byte[] data = null;
        // 读取文件字节数组
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 对字节数组Base64编码
        return Base64.getEncoder().encodeToString(data);// 返回Base64编码过的字节数组字符串
    }

    public static String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        String result;
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }
}