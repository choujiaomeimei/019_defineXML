package com.stat.web.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

public class DownLoadUtils {

    public static void download(HttpServletResponse response, String filePath, String fileName){
        try {
            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName,"UTF-8"));
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
            writeBytes(is, response.getOutputStream());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void writeBytes(InputStream is, OutputStream os) {
        try {
            byte[] buf = new byte[1024];
            int len = 0;
            while((len = is.read(buf))!=-1)
            {
                os.write(buf,0,len);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
