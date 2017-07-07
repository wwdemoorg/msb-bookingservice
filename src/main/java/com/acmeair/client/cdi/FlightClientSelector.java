package com.acmeair.client.cdi;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import com.acmeair.client.FlightClient;

@SuppressWarnings("all")
public class FlightClientSelector{
    
    private static final String CLIENT_TYPE = (System.getenv("CLIENT_TYPE") == null) ? "jaxrs" : System.getenv("CLIENT_TYPE");
    
    @Inject
    @Any
    Instance<FlightClient> clients;        
   
    @Produces
    public FlightClient getCustomerClient(){
        
        Instance<FlightClient> found=clients.select(new ClientQualifier(CLIENT_TYPE));
        
        if (!found.isUnsatisfied() && !found.isAmbiguous()){
            return found.get();
        }       
        
        return null;
    }

    public static class ClientQualifier extends AnnotationLiteral<ClientType> implements ClientType { 
        
        private static final long serialVersionUID = 1L;
        private String value;

        public ClientQualifier(String value) {
            this.value=value;
        }
        public String value() { 
            return value; 
        }
    }
}
