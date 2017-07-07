package com.acmeair.client.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.inject.Inject;

import com.acmeair.securityutils.SecurityUtils;

public class HTTPClient {
    
    @Inject
    private SecurityUtils secUtils;
    
    private static final Boolean SECURE_SERVICE_CALLS = Boolean.valueOf((System.getenv("SECURE_SERVICE_CALLS") == null) ? "false" : System.getenv("SECURE_SERVICE_CALLS"));

    protected String doHttpURLCall(HttpURLConnection conn, String urlParameters) {        
        
        StringBuffer response = new StringBuffer();
        
        try {

            DataOutputStream wr;
            wr = new DataOutputStream(conn.getOutputStream());
        
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            conn.disconnect(); 

            //  print result
        }  catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
            
        return response.toString();
    }

    protected HttpURLConnection createHttpURLConnection(String url, String urlParameters, String customerId, String path) {
        
        HttpURLConnection conn=null;
      
        try {
        
            URL obj = new URL(url);
            conn = (HttpURLConnection) obj.openConnection();

            //  add request header
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
        
            if (SECURE_SERVICE_CALLS) { 
            
                Date date= new Date();
                String sigBody = secUtils.buildHash(urlParameters);
                String signature = secUtils.buildHmac("POST",path,customerId,date.toString(),sigBody); 
            
                conn.setRequestProperty("acmeair-id", customerId);
                conn.setRequestProperty("acmeair-date", date.toString());
                conn.setRequestProperty("acmeair-sig-body", sigBody);    
                conn.setRequestProperty("acmeair-signature", signature);  
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return conn;
    }
    
    
}
