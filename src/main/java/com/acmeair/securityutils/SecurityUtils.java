package com.acmeair.securityutils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;



public class SecurityUtils {
    
    protected Logger logger =  Logger.getLogger(SecurityUtils.class.getName());
    
    // TODO: Hardcode for now
    private final static String secretKey = "acmeairsecret128";
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String UTF8 = "UTF-8";

    // Generate simple JWT with login as the Subject 
    public String generateJWT(String customerid)    {
        String token = null;
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            token = JWT.create()
                    .withSubject(customerid)
                    .sign(algorithm);
        } catch (Exception exception) {
            exception.printStackTrace();
        } 
        return token;
    }
    
    public boolean validateJWT(String customerid, String jwtToken)    {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("validate : customerid " + customerid);
        }
        
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).build(); //Cache?
            
            DecodedJWT jwt = verifier.verify(jwtToken);
            return jwt.getSubject().equals(customerid);
            
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        
        return false;
    }
    
    public String buildHash(String data) 
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(SHA_256);
        md.update(data.getBytes(UTF8));
        byte[] digest = md.digest();
        return Base64.getEncoder().encodeToString( digest );
    }
    
    public String buildHmac(String method, String baseUri, String userId, String dateString, String sigBody)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        
        List<String> toHash = new ArrayList<String>();
        toHash.add(method);
        toHash.add(baseUri);
        toHash.add(userId);
        toHash.add(dateString);
        toHash.add(sigBody);
        
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secretKey.getBytes(UTF8), HMAC_ALGORITHM));

        for(String s: toHash){
            mac.update(s.getBytes(UTF8));
        }
        
        return Base64.getEncoder().encodeToString( mac.doFinal() );
    }
    
    public boolean verifyBodyHash(String body, String sigBody) {
        
        if (sigBody.isEmpty())
            throw new WebApplicationException("Invalid signature (sigBody)", Status.FORBIDDEN);

        if ( body == null || body.length() == 0) {
            throw new WebApplicationException("Invalid signature (body)", Status.FORBIDDEN);
        }

        try {
            String h_bodyHash = buildHash(body);
            if ( !sigBody.equals(h_bodyHash) ) {
                throw new WebApplicationException("Invalid signature (bodyHash)", Status.FORBIDDEN);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new WebApplicationException("Unable to generate hash", Status.FORBIDDEN);
        }

        return true;
    }
    
    public boolean verifyFullSignature(String method, 
            String baseUri, 
            String userId, 
            String dateString, 
            String sigBody,
            String signature) {
        try {
            String h_hmac = buildHmac(method,baseUri,userId,dateString,sigBody);
            
            if ( !signature.equals(h_hmac) ) {
                throw new WebApplicationException("Invalid signature (hmacCompare)", Status.FORBIDDEN);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new WebApplicationException("Invalid signature", Status.FORBIDDEN);
        }

        return true;
    }
}
