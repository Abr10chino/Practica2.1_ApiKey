package com.universales.practica21.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class NoAuthService {
	
	@GetMapping("/hola")
	public ResponseEntity<?> holaMundo(){
		
		return ResponseEntity.ok("Hola Mundo!");
		
	}

}
