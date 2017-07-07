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

import com.acmeair.client.cdi.ClientType;
import com.acmeair.client.CustomerClient;
import com.acmeair.client.jaxrs.JAXRSClient;

@ClientType("jaxrs-ejb")
@Stateless
public class JAXRSEJBCustomerClient extends JAXRSClient implements CustomerClient {

    private WebTarget customerTargetBase;   
        
    static {
        System.out.println("Using JAXRSEJBCustomerClient");
        System.out.println("SECURE_SERVICE_CALLS: " + SECURE_SERVICE_CALLS); 
    }
    
    @PostConstruct
    public void init(){
        Client customerClient = ClientBuilder.newClient();
        customerTargetBase =  customerClient.target("http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH);     
    }
    
    public void updateTotalMiles(String customerId, String miles) {
        Form form = new Form("miles", miles);
        WebTarget customerClientWebTargetFinal = customerTargetBase.path(customerId);
        Builder builder = createInvocationBuilder(customerClientWebTargetFinal, form, customerId, UPDATE_REWARD_PATH);     
            
        builder.accept(MediaType.TEXT_PLAIN);       
        Response res = builder.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
                
        res.readEntity(JsonObject.class);                        
    }    
}
