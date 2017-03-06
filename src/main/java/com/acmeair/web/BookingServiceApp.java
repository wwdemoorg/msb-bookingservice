package com.acmeair.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.acmeair.config.BookingConfiguration;
import com.acmeair.config.BookingLoaderREST;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")

public class BookingServiceApp extends Application {
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(BookingServiceREST.class,BookingConfiguration.class,BookingLoaderREST.class));
    }
}
