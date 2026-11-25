package com.universales.practica21.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.universales.practica21.dto.RequestLoginDto;
import com.universales.practica21.dto.ResponseApiKeyDto;
import com.universales.practica21.entity.HistoryLog;
import com.universales.practica21.entity.Person;
import com.universales.practica21.repository.HistoryLogRepository;
import com.universales.practica21.repository.PersonRepository;
import com.universales.practica21.util.ApiKeyOptions;

@RestController
@RequestMapping("/noauth")
@CrossOrigin
public class AuthService {

	private final PersonRepository pRepository;
	private final HistoryLogRepository hRepository;
	
	public AuthService(PersonRepository pRepository, HistoryLogRepository hRepository) {
		this.pRepository = pRepository;
		this.hRepository = hRepository;
	}
	
	@PostMapping("/apiKey")
	public ResponseEntity<?> login(
			
			@RequestBody RequestLoginDto personDto
			
	){
		
		try {
			
			Optional<Person> personOpt = pRepository.findByUsername(personDto.getUserName());
			
			if(personOpt.isPresent()) {
				
				Person person = personOpt.get();
				
				if(person.getPassword() == null || person.getPassword().isEmpty()) {
					
					return ResponseEntity
							.status(HttpStatus.BAD_REQUEST)
							.body("Usuario con falta de credenciales");
					
				}
				
				if(person.getPassword().equals(personDto.getPassword())) {
					
					String apiKey;
					
					do {
						
						apiKey = ApiKeyOptions.generarApiKey();
						
					}while(hRepository.existsByApiKey(apiKey));
					
					HistoryLog history = new HistoryLog();
					
					history.setApiKey(apiKey);
					history.setCreatedAt(LocalDateTime.now());
					history.setPerson(person);
					
					hRepository.save(history);
					
					ResponseApiKeyDto responseApiKey = new ResponseApiKeyDto();
					
					responseApiKey.setApiKey(apiKey);
					
					return ResponseEntity.ok(responseApiKey);
					
				}
				
			}
			
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.body("Usuario o Contraseña Invalidos");
			
		}catch(Exception e) {
			
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error en el servidor:"+e.getMessage());
			
		}
		
	}
	
}
