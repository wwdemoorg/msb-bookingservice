package com.acmeair.client;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

//import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;

import com.acmeair.securityutils.SecurityUtils;

@Stateless
@LocalBean
public class FlightClient {

    WebTarget flightTarget;

    
    // Default to amalgam8 default
    private static final String FLIGHT_SERVICE_LOC = ((System.getenv("FLIGHT_SERVICE") == null) ? "localhost:6379/flight" : System.getenv("FLIGHT_SERVICE"));
    private static final String GET_REWARD_PATH = "/getrewardmiles";
    
      
    private static final Boolean SECURE_SERVICE_CALLS = Boolean.valueOf((System.getenv("SECURE_SERVICE_CALLS") == null) ? "false" : System.getenv("SECURE_SERVICE_CALLS"));

    @Inject
    private SecurityUtils secUtils;
        
    @PostConstruct
    public void init(){

        Client flightClient = ClientBuilder.newClient();
        flightTarget =  flightClient.target("http://"+ FLIGHT_SERVICE_LOC +GET_REWARD_PATH);
 
    }
    
    
    public String makeCall (String flightSegId, String customerId, boolean add) {
        
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
    
    private Builder createInvocationBuilder(WebTarget target, Form form, String customerId, String path) {
        
        Builder builder = target.request(MediaType.APPLICATION_JSON_TYPE);
        
        if (SECURE_SERVICE_CALLS) { 
            try {
                Date date= new Date();
                
                String body="";
                MultivaluedMap<String,String> map = form.asMap();
                
                for(String key : map.keySet())
                {
                    body=body+key+"=" + map.getFirst(key) + "&";
                }
                body=body.substring(0,body.length()-1);
                
                String sigBody = secUtils.buildHash(body);
                String signature = secUtils.buildHmac("POST",path,customerId,date.toString(),sigBody); 
        
                builder.header("acmeair-id", customerId);
                builder.header("acmeair-date", date.toString());
                builder.header("acmeair-sig-body", sigBody);    
                builder.header("acmeair-signature", signature); 
            
            } catch (Exception e) {
                e.printStackTrace();
            }  
        }
        
        return builder;
    }

    
}
