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

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.acmeair.client.CustomerClient;
import com.acmeair.client.FlightClient;
import com.acmeair.securityutils.SecurityUtils;
import com.acmeair.service.BookingService;

@Path("/")
public class BookingServiceREST {
    
    @Inject
    BookingService bs;
    
    @Inject
    private SecurityUtils secUtils;
    
    @Inject
    private FlightClient flightClient;
    
    @Inject
    private CustomerClient customerClient; 
    
    protected Logger logger =  Logger.getLogger(BookingServiceREST.class.getName());

    // TRACK MILES OPTIONS
    private static final Boolean TRACK_REWARD_MILES = Boolean.valueOf((System.getenv("TRACK_REWARD_MILES") == null) ? "false" : System.getenv("TRACK_REWARD_MILES"));
    private static final Boolean SECURE_USER_CALLS = Boolean.valueOf((System.getenv("SECURE_USER_CALLS") == null) ? "true" : System.getenv("SECURE_USER_CALLS"));
           
    static {
        System.out.println("TRACK_REWARD_MILES: " + TRACK_REWARD_MILES);
        System.out.println("SECURE_USER_CALLS: " + SECURE_USER_CALLS); 
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
                    JsonReader jsonReader = Json.createReader(new StringReader(bs.getBooking(userid, number)));
                    JsonObject booking = jsonReader.readObject();
                    jsonReader.close(); 
                    
                    bs.cancelBooking(userid, number);
                    updateRewardMiles(userid, booking.getString("flightSegmentId"), false);
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
        String miles = flightClient.getRewardMiles(customerId, flightSegId, add);
        customerClient.updateTotalMiles(customerId, miles);                
    }    
}