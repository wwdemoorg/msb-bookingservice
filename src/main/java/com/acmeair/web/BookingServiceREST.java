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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.acmeair.service.BookingService;
import io.jsonwebtoken.Jwts;

@Path("/")
public class BookingServiceREST {
	
    @Inject
    BookingService bs;
    
	protected Logger logger =  Logger.getLogger(BookingServiceREST.class.getName());
	
	// TODO: Use actual shared keys instead of this secret 
    private static final String secretKey = "secret";
	
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
			
			String bookingInfo = "";
			
			String bookingIdReturn = null;
			if (!oneWay) {
				bookingIdReturn = bs.bookFlight(userid, retFlightSegId, retFlightId);
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
		    
			bs.cancelBooking(userid, number);
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
}