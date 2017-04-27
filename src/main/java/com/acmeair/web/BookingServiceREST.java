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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.acmeair.service.BookingService;
import io.jsonwebtoken.Jwts;

@Path("/")
public class BookingServiceREST {
	
    @Inject
    BookingService bs;
    
	protected Logger logger =  Logger.getLogger(BookingServiceREST.class.getName());
	
	// TODO: Use actual shared keys instead of this secret 
    private static final String secretKey = "secret";
    
    // TRACK MILES OPTIONS
    private static final Boolean TRACK_REWARD_MILES = Boolean.valueOf((System.getenv("TRACK_REWARD_MILES") == null) ? "false" : System.getenv("TRACK_REWARD_MILES"));
   
    // Default to amalgam8 default
    private static final String FLIGHT_SERVICE_LOC = ((System.getenv("FLIGHT_SERVICE") == null) ? "localhost:6379/flight" : System.getenv("FLIGHT_SERVICE"));
    private static final String GET_REWARD_PATH = "/getrewardmiles";
    
    private static final String CUSTOMER_SERVICE_LOC = ((System.getenv("CUSTOMER_SERVICE") == null) ? "localhost:6379/customer" : System.getenv("CUSTOMER_SERVICE"));
    private static final String UPDATE_REWARD_PATH = "/updateCustomerTotalMiles";

    
    private static WebTarget flightClientWebTarget = null;
    private static WebTarget customerClientWebTarget = null;

    static {
        System.out.println("TRACK_REWARD_MILES: " + TRACK_REWARD_MILES);
                
        // Set up JAX-RS Client. Recreating the WebTarget is painfully slow, so caching it here.
        // This works on Libertty 17.0.0.1+
        // If there is a better way to do this, please let me know!
        Client flightClient = ClientBuilder.newClient();
        flightClient.property("http.maxConnections", Integer.valueOf(50));
        flightClient.property("thread.safe.client", Boolean.valueOf(true));
        flightClientWebTarget = flightClient.target("http://"+ FLIGHT_SERVICE_LOC + GET_REWARD_PATH);
        
        Client customerClient = ClientBuilder.newClient();
        customerClient.property("http.maxConnections", Integer.valueOf(50));
        customerClient.property("thread.safe.client", Boolean.valueOf(true));
        customerClientWebTarget = customerClient.target("http://"+ CUSTOMER_SERVICE_LOC + UPDATE_REWARD_PATH);
    }
    
    
	
	private boolean validateJWT(String customerid, String jwtToken)    {    
        if(logger.isLoggable(Level.FINE)){
            logger.fine("validate : customerid " + customerid);
        }
                
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(jwtToken).getBody().getSubject().equals(customerid);
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
            if (!validateJWT(userid,jwtToken)) {
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
            if (!validateJWT(userid, jwtToken)) {
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
            if (!validateJWT(user,jwtToken)) {
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
            if (!validateJWT(userid,jwtToken)) {
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
	
	private void updateRewardMiles(String customerId, String flightSegId, boolean add) {
	    // Cached the WebTarget above to avoid the huge creation overhead.
	    // Call the flight service to get the miles for the flight segment.
	    //   Do this call asynchronously to return control back to the client.
	    // Then call the customer service to update the total_miles
	    
	    Form form = new Form();
	    form.param("flightSegment", flightSegId);
	     
	    Builder builder = flightClientWebTarget.request();
	    AsyncInvoker asyncInvoker = builder.async();
	    asyncInvoker.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), new InvocationCallback<Response>() {
	        
	        @Override
	        public void completed(Response response) {
	            String miles = response.readEntity(String.class); 
	            if (!add) {
	                miles = ((Integer)(Integer.parseInt(miles) * -1)).toString();
	            }
	                                        
	            Form form = new Form();
	            form.param("miles", miles);
	                
	            WebTarget customerClientWebTargetFinal = customerClientWebTarget.path(customerId);
	                
	            Builder builder = customerClientWebTargetFinal.request();
	            builder.accept(MediaType.TEXT_PLAIN);       
	            Response res = builder.post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
	            res.readEntity(String.class); 
	        }
	         
	        @Override
	        public void failed(Throwable throwable) {
	            throwable.printStackTrace();
	        }
	    });
	}	
	
	@GET
	public Response checkStatus() {
	    return Response.ok("OK").build();
	    
	}  
}