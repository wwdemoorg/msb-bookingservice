package com.acmeair.config;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.acmeair.service.BookingService;


@Path("/config")
public class BookingConfiguration {
    	
	Logger logger = Logger.getLogger(BookingConfiguration.class.getName());

	@Inject
    BookingService bookingService;
	
    public BookingConfiguration() {
        super();
    }
	    
	@GET
	@Path("/countBookings")
	@Produces("application/json")
	public Response countBookings() {
		try {
			String count = bookingService.count().toString();			
			return Response.ok(count).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	@GET
	@Path("/activeDataService")
	@Produces("application/json")
	public Response getActiveDataServiceInfo() {
		try {		
			logger.fine("Get active Data Service info");
			return  Response.ok(bookingService.getServiceType()).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok("Unknown").build();
		}
	}
		
	@GET
	@Path("/runtime")
	@Produces("application/json")
	public ArrayList<ServiceData> getRuntimeInfo() {
		try {
			logger.fine("Getting Runtime info");
			ArrayList<ServiceData> list = new ArrayList<ServiceData>();
			ServiceData data = new ServiceData();
			data.name = "Runtime";
			data.description = "Java";			
			list.add(data);
				
			data = new ServiceData();
			data.name = "Version";
			data.description = System.getProperty("java.version");			
			list.add(data);
				
			data = new ServiceData();
			data.name = "Vendor";
			data.description = System.getProperty("java.vendor");			
			list.add(data);
				
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

		
	class ServiceData {
		public String name = "";
		public String description = "";
	}			
}
