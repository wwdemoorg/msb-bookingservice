package com.acmeair.client;

public interface FlightClient {
    
    // Default to amalgam8 default
    static final String FLIGHT_SERVICE_LOC = ((System.getenv("FLIGHT_SERVICE") == null) ? "localhost:6379/flight" : System.getenv("FLIGHT_SERVICE"));
    static final String GET_REWARD_PATH = "/getrewardmiles";
       
    abstract public String getRewardMiles(String customerId, String flightSegId, boolean add);
}
