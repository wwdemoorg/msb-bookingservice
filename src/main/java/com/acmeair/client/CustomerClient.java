package com.acmeair.client;

public interface CustomerClient {
    
    static final String CUSTOMER_SERVICE_LOC = ((System.getenv("CUSTOMER_SERVICE") == null) ? "localhost:6379/customer" : System.getenv("CUSTOMER_SERVICE"));
    static final String UPDATE_REWARD_PATH = "/updateCustomerTotalMiles";
      
    abstract public void updateTotalMiles(String customerId, String miles);
}
