package com.acmeair.client.jaxrs.ejb.impl;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.acmeair.client.FlightClient;
import com.acmeair.client.cdi.ClientType;
import com.acmeair.client.jaxrs.JAXRSClient;

@ClientType("jaxrs-ejb")
@Stateless
public class JAXRSEJBFlightClient extends JAXRSClient implements FlightClient {

    private WebTarget flightTarget;
            
    static {
        System.out.println("Using JAXRSEJBFlightClient");
        System.out.println("SECURE_SERVICE_CALLS: " + SECURE_SERVICE_CALLS); 
    }
    
    @PostConstruct
    public void init(){
        Client flightClient = ClientBuilder.newClient();
        flightTarget =  flightClient.target("http://"+ FLIGHT_SERVICE_LOC +GET_REWARD_PATH);
    }
        
    public String getRewardMiles(String customerId, String flightSegId, boolean add) {
        
        Form form = new Form("flightSegment", flightSegId);          
        Builder builder = createInvocationBuilder(flightTarget, form, customerId,GET_REWARD_PATH);      
        Response res = builder.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
       
                               
        JsonObject jsonObject =res.readEntity(JsonObject.class);       
        Long milesLong = jsonObject.getJsonNumber("miles").longValue();
        String miles = milesLong.toString(); 
                
                
        if (!add) {
            miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
        }

        return miles;
    } 
    
   
    
}
