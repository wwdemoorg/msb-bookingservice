/*******************************************************************************
* Copyright (c) 2017 IBM Corp.
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

package com.acmeair.config;

import com.acmeair.service.BookingService;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/config")
public class BookingConfiguration {

  Logger logger = Logger.getLogger(BookingConfiguration.class.getName());

  @Inject
  BookingService bookingService;

  public BookingConfiguration() {
    super();
  }

  /**
   *  Get numbers of bookings in the db.
   */
  @GET
  @Path("/countBookings")
  @Produces("application/json")
  public Response countBookings() {
    try {
      String count = bookingService.count().toString();
      return Response.ok(count).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.ok(-1).build();
    }
  }

  /**
   *  Get active db impl.
   */
  @GET
  @Path("/activeDataService")
  @Produces("application/json")
  public Response getActiveDataServiceInfo() {
    try {
      logger.fine("Get active Data Service info");
      return Response.ok(bookingService.getServiceType()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.ok("Unknown").build();
    }
  }

  /**
   *  Get runtime info.
   */
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
