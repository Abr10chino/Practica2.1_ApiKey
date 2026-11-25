package com.universales.practica21.util;

import java.security.SecureRandom;
import java.util.Base64;

public class ApiKeyOptions {

	public static String generarApiKey() {
		
		SecureRandom secureRandom = new SecureRandom();

	    byte[] apiKey = new byte[32];

	    secureRandom.nextBytes(apiKey);

	    return Base64.getUrlEncoder()
	            .withoutPadding()
	            .encodeToString(apiKey);
		
	}
	
}
