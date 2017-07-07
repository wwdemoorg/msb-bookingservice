package com.acmeair.client.http.impl;

import java.io.StringReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.acmeair.client.FlightClient;
import com.acmeair.client.cdi.ClientType;
import com.acmeair.client.http.HTTPClient;

@ClientType("jaxrs-ejb")
public class HTTPFlightClient extends HTTPClient implements FlightClient {
               
    static {
        System.out.println("Using HTTPFlightClient");
    }
    
    public String getRewardMiles(String customerId, String flightSegId, boolean add) {
        // Set maxConnections - this seems to help with keepalives/running out of sockets with a high load.
        if (System.getProperty("http.maxConnections")==null) {
            System.setProperty("http.maxConnections", "50");
        }
                   
        String flightUrl = "http://"+ FLIGHT_SERVICE_LOC + GET_REWARD_PATH;
        String flightParameters="flightSegment=" + flightSegId;  
            
        HttpURLConnection flightConn = createHttpURLConnection(flightUrl, flightParameters, customerId, GET_REWARD_PATH);
        String output = doHttpURLCall(flightConn, flightParameters);  
        
        JsonReader jsonReader = Json.createReader(new StringReader(output));
        JsonObject milesObject = jsonReader.readObject();
        jsonReader.close();
        
        Long milesLong = milesObject.getJsonNumber("miles").longValue();
        String miles = milesLong.toString();

        if (!add) {
            miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
        }
        
        return miles;
    } 
}
