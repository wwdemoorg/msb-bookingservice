/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.acmeair.securityutils.SecurityUtils;
import com.acmeair.service.BookingService;

@Path("/")
public class BookingServiceREST {
    
    @Inject
    BookingService bs;
    
    @Inject
    private SecurityUtils secUtils;
    
    protected Logger logger =  Logger.getLogger(BookingServiceREST.class.getName());

    // TRACK MILES OPTIONS
    private static final Boolean TRACK_REWARD_MILES = Boolean.valueOf((System.getenv("TRACK_REWARD_MILES") == null) ? "false" : System.getenv("TRACK_REWARD_MILES"));
   
    // Default to amalgam8 default
    private static final String FLIGHT_SERVICE_LOC = ((System.getenv("FLIGHT_SERVICE") == null) ? "localhost:6379/flight" : System.getenv("FLIGHT_SERVICE"));
    private static final String GET_REWARD_PATH = "/getrewardmiles";
    
    private static final String CUSTOMER_SERVICE_LOC = ((System.getenv("CUSTOMER_SERVICE") == null) ? "localhost:6379/customer" : System.getenv("CUSTOMER_SERVICE"));
    private static final String UPDATE_REWARD_PATH = "/updateCustomerTotalMiles";
    
    private static final Boolean SECURE_USER_CALLS = Boolean.valueOf((System.getenv("SECURE_USER_CALLS") == null) ? "true" : System.getenv("SECURE_USER_CALLS"));
    private static final Boolean SECURE_SERVICE_CALLS = Boolean.valueOf((System.getenv("SECURE_SERVICE_CALLS") == null) ? "false" : System.getenv("SECURE_SERVICE_CALLS"));
    
    private static final String SERVICE_INVOCATION_HTTP  = "http";
    private static final String SERVICE_INVOCATION_JAXRS = "jaxrs";
    private static final String SERVICE_INVOCATION_JAXRS_NO_CACHE = "jaxrs_no_cache"; 
    private static final String SERVICE_INVOCATION_TYPE  = ((System.getenv("SERVICE_INVOCATION_TYPE") == null) ? SERVICE_INVOCATION_JAXRS : System.getenv("SERVICE_INVOCATION_TYPE"));
    
    private static WebTarget flightClientWebTarget = null;
    private static WebTarget customerClientWebTarget = null;

    static {
        System.out.println("TRACK_REWARD_MILES: " + TRACK_REWARD_MILES);
        System.out.println("SECURE_USER_CALLS: " + SECURE_USER_CALLS); 
        System.out.println("SECURE_SERVICE_CALLS: " + SECURE_SERVICE_CALLS); 
        System.out.println("SERVICE_INVOCATION_TYPE: " + SERVICE_INVOCATION_TYPE); 
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Path("/bookflights")
    @Produces("text/plain")
    public /*BookingInfo*/ Response bookFlights(
            @FormParam("userid") String userid,
            @FormParam("toFlightId") String toFlightId,
            @FormParam("toFlightSegId") String toFlightSegId,
            @FormParam("retFlightId") String retFlightId,
            @FormParam("retFlightSegId") String retFlightSegId,
            @FormParam("oneWayFlight") boolean oneWay,
            @CookieParam("jwt_token") String jwtToken) {
        try {
            // make sure the user isn't trying to bookflights for someone else
            if (SECURE_USER_CALLS  && !secUtils.validateJWT(userid,jwtToken)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            
            String bookingIdTo = bs.bookFlight(userid, toFlightSegId, toFlightId);
            if (TRACK_REWARD_MILES) {
                updateRewardMiles(userid, toFlightSegId, true);
            }
            
            String bookingInfo = "";
            
            String bookingIdReturn = null;
            if (!oneWay) {
                bookingIdReturn = bs.bookFlight(userid, retFlightSegId, retFlightId);
                if (TRACK_REWARD_MILES) {
                    updateRewardMiles(userid, retFlightSegId, true);
                }               
                bookingInfo = "{\"oneWay\":false,\"returnBookingId\":\"" + bookingIdReturn + "\",\"departBookingId\":\"" + bookingIdTo + "\"}";
            } else {
                bookingInfo = "{\"oneWay\":true,\"departBookingId\":\"" + bookingIdTo + "\"}";
            }
            return Response.ok(bookingInfo).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
        
    @GET
    @Path("/bybookingnumber/{userid}/{number}")
    @Produces("text/plain")
    public Response getBookingByNumber(
            @PathParam("number") String number,
            @PathParam("userid") String userid,
            @CookieParam("jwt_token") String jwtToken) {
        try {
            //  make sure the user isn't trying to bookflights for someone else
            if(SECURE_USER_CALLS  && !secUtils.validateJWT(userid, jwtToken)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            return Response.ok(bs.getBooking(userid, number)).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GET
    @Path("/byuser/{user}")
    @Produces("text/plain")
    public Response getBookingsByUser(@PathParam("user") String user,@CookieParam("jwt_token") String jwtToken) {
        
        try {
            // make sure the user isn't trying to bookflights for someone else
            if (SECURE_USER_CALLS  && !secUtils.validateJWT(user,jwtToken)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            return  Response.ok(bs.getBookingsByUser(user).toString()).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Path("/cancelbooking")
    @Produces("text/plain")
    public Response cancelBookingsByNumber(
            @FormParam("number") String number,
            @FormParam("userid") String userid,
            @CookieParam("jwt_token") String jwtToken) {
        try {
            //   make sure the user isn't trying to bookflights for someone else
            if (SECURE_USER_CALLS  && !secUtils.validateJWT(userid,jwtToken)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
 
            if (TRACK_REWARD_MILES) {
                try {
                    JSONObject booking = (JSONObject) new JSONParser().parse(bs.getBooking(userid, number));
                    bs.cancelBooking(userid, number);
                    updateRewardMiles(userid, (String)booking.get("flightSegmentId"), false);
                }
                catch (RuntimeException re) {
                    // booking does not exist
                    if(logger.isLoggable(Level.FINE)){
                        logger.fine("booking : This booking does not exist: " + number);
                    }
                }
            }
            else {
                bs.cancelBooking(userid, number);
            }
            
            return Response.ok("booking " + number + " deleted.").build();
                    
        }
        catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GET
    public Response checkStatus() {
        return Response.ok("OK").build();     
    } 
    
    private void updateRewardMiles(String customerId, String flightSegId, boolean add) {
        
        if (SERVICE_INVOCATION_TYPE.equals(SERVICE_INVOCATION_HTTP)) {
            // TODO: Multi-threaded JAX-RS client works on Liberty in 17.0.0.1+, but does not seem to work on wildfly 10.1
            // out of the box, so adding this http call for now. Will investigate.
            // The JAX-RS client call below has the advantage of being asynchronous.
            
            // Set maxConnections - this seems to help with keepalives/running out of sockets with a high load.
            if (System.getProperty("http.maxConnections")==null) {
                System.setProperty("http.maxConnections", "50");
            }
                       
            String flightUrl = "http://"+ FLIGHT_SERVICE_LOC + GET_REWARD_PATH;
            String flightParameters="flightSegment=" + flightSegId;  
                
            HttpURLConnection flightConn = createHttpURLConnection(flightUrl, flightParameters, customerId, GET_REWARD_PATH);
            String output = doHttpURLCall(flightConn, flightParameters);                       
            Long milesLong = (Long)JSONValue.parse(output);
            String miles = milesLong.toString();
        
            if (!add) {
                miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
            }
                
            String customerUrl = "http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH + "/" + customerId;
            String customerParameters="miles=" + miles;
                
            HttpURLConnection customerConn = createHttpURLConnection(customerUrl, customerParameters, customerId, UPDATE_REWARD_PATH);
            doHttpURLCall(customerConn, customerParameters); 

        } else if (SERVICE_INVOCATION_TYPE.equals(SERVICE_INVOCATION_JAXRS)) {
        
            // Cache the WebTargets to avoid the huge creation overhead.
            if (flightClientWebTarget==null) {
                initializeFlightWebTarget();
            }
            if (customerClientWebTarget==null) {
                initializeCustomerWebTarget();
            }
            
            Form form = new Form("flightSegment", flightSegId);          
            Builder builder = createInvocationBuilder(flightClientWebTarget, form, customerId,GET_REWARD_PATH);      
            AsyncInvoker asyncInvoker = builder.async();
            
            // Call the flight service to get the miles for the flight segment.
            // Do this call asynchronously to return control back to the client.
            // Then call the customer service to update the total_miles
            asyncInvoker.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), new InvocationCallback<Response>() {
            
                @Override
                public void completed(Response response) {
                    
                    String miles = response.readEntity(String.class); 
                    if (!add) {
                        miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
                    }
  
                    Form form = new Form("miles", miles);
                    WebTarget customerClientWebTargetFinal = customerClientWebTarget.path(customerId);
                    Builder builder = createInvocationBuilder(customerClientWebTargetFinal, form, customerId, UPDATE_REWARD_PATH);     
                
                    builder.accept(MediaType.TEXT_PLAIN);       
                    Response res = builder.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
                    res.readEntity(String.class); 
                }
                
                @Override
                public void failed(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            
        } else  if (SERVICE_INVOCATION_TYPE.equals(SERVICE_INVOCATION_JAXRS_NO_CACHE)) {
                
            Form form = new Form("flightSegment", flightSegId);             
            Client flightClient = ClientBuilder.newClient();
            WebTarget wt = flightClient.target("http://"+ FLIGHT_SERVICE_LOC + GET_REWARD_PATH);
            
            Builder builder = createInvocationBuilder(wt, form, customerId,GET_REWARD_PATH);     
            AsyncInvoker asyncInvoker = builder.async();
            
            asyncInvoker.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), new InvocationCallback<Response>() {
            
                @Override
                public void completed(Response response) {
                    
                    flightClient.close();
                    
                    String miles = response.readEntity(String.class); 
                    if (!add) {
                        miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
                    }
                     
                    Client customerClient = ClientBuilder.newClient();
                    WebTarget wt = customerClient.target("http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH);                    
                    
                    Form form = new Form("miles", miles);
                    WebTarget customerClientWebTargetFinal = wt.path(customerId);
                    Builder builder = createInvocationBuilder(customerClientWebTargetFinal, form, customerId,UPDATE_REWARD_PATH);    
                    builder.accept(MediaType.TEXT_PLAIN);       
                    
                    Response res = builder.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
                    res.readEntity(String.class); 
                    
                    customerClient.close();
                }
                
                @Override
                public void failed(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });       
        } 
             
    }    
        
    private String doHttpURLCall(HttpURLConnection conn, String urlParameters) {        
        
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

    private HttpURLConnection createHttpURLConnection(String url, String urlParameters, String customerId, String path) {
        
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

    private Builder createInvocationBuilder(WebTarget target, Form form, String customerId, String path) {
        Builder builder = target.request();
        
        if (SECURE_SERVICE_CALLS) { 
            try {
                Date date= new Date();
                
                String body="";
                MultivaluedMap<String,String> map = form.asMap();
                
                for(String key : map.keySet())
                {
                    body=key+"=" + map.getFirst(key);
                }
    
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

    /** Set up JAX-RS Client/WebTarget. Recreating the WebTarget is painfully slow, so caching it here.
     * This works on Liberty 17.0.0.1+. If there is a better way to do this, please let me know!
    **/
    private static void initializeFlightWebTarget() {
            
        Client client = ClientBuilder.newClient();

        // liberty specific
        client.property("http.maxConnections", Integer.valueOf(50));
        client.property("thread.safe.client", Boolean.valueOf(true));
            
        flightClientWebTarget = client.target("http://"+ FLIGHT_SERVICE_LOC + GET_REWARD_PATH);
    }
    
    private static void initializeCustomerWebTarget() {
                     
        Client client = ClientBuilder.newClient();

        // liberty specific
        client.property("http.maxConnections", Integer.valueOf(50));
        client.property("thread.safe.client", Boolean.valueOf(true));
            
        customerClientWebTarget = client.target("http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH);
    }   
}