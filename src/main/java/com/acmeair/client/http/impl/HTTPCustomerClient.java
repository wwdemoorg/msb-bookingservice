package com.acmeair.client.http.impl;
import java.net.HttpURLConnection;

import com.acmeair.client.CustomerClient;
import com.acmeair.client.cdi.ClientType;
import com.acmeair.client.http.HTTPClient;

@ClientType("http")
public class HTTPCustomerClient extends HTTPClient implements CustomerClient { 
    
    static {
        System.out.println("Using HTTPCustomerClient");
    }
    
    public void updateTotalMiles(String customerId, String miles) {
        String customerUrl = "http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH + "/" + customerId;
        String customerParameters="miles=" + miles;
            
        HttpURLConnection customerConn = createHttpURLConnection(customerUrl, customerParameters, customerId, UPDATE_REWARD_PATH);
        doHttpURLCall(customerConn, customerParameters);                    
    }
}
