package com.universales.practica21.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.universales.practica21.entity.HistoryLog;
import com.universales.practica21.repository.HistoryLogRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter{
	
	private final HistoryLogRepository historyLogRepository;
	
	public ApiKeyFilter(HistoryLogRepository historyLogRepository) {
		
		this.historyLogRepository = historyLogRepository;
		
	}
	
	@Override
	protected void doFilterInternal(
		
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
			
			
	)throws ServletException, IOException {
		
		String path = request.getRequestURI();
		
		if(path == null || path.isBlank()) {
			mensajeError(response, HttpServletResponse.SC_NOT_FOUND,  "La ruta de la peticion esta vacia");
			return;
		}
		
		if(path.startsWith("/api/noauth/") || path.startsWith("/noauth/")) {
			filterChain.doFilter(request, response);
            return;
		}
		
		String apiKey = request.getHeader("X-API-KEY");
		if(apiKey == null || apiKey.isBlank()) {
			
			apiKey = request.getParameter("X-API-KEY");
			
			if(apiKey == null || apiKey.isBlank()) {
				
				mensajeError(response, HttpServletResponse.SC_UNAUTHORIZED, "No envio el apiKey");
				return;
				
			}
		}
		
		Optional<HistoryLog> logOpt = historyLogRepository.findByApiKey(apiKey);
		
		if(logOpt.isEmpty()) {
			
			mensajeError(response, HttpServletResponse.SC_UNAUTHORIZED, "ApiKey no valida");
			return;
			
		}
		
		HistoryLog log = logOpt.get();
		
		if (log.getCreatedAt() == null) {
		    mensajeError(response, HttpServletResponse.SC_UNAUTHORIZED, "La apiKey no tiene fecha de creacion");
		    return;
		}
		
		LocalDateTime tiempoExpirado = log.getCreatedAt().plusMinutes(1);
		
		if(LocalDateTime.now().isAfter(tiempoExpirado)) {
			mensajeError(response, HttpServletResponse.SC_UNAUTHORIZED, "La apiKey ha expirado");
		    return;
		}
		
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				log.getPerson() != null ? log.getPerson().getUsername() : "apiKeyUser",
				null,
				List.of()
		);
		
		SecurityContextHolder.getContext().setAuthentication(auth);
		filterChain.doFilter(request, response);
		
	}
	
	private void mensajeError(
	        HttpServletResponse response,
	        int status,
	        String mensaje
	) throws IOException {
	    response.setStatus(status);
	    response.setContentType("text/plain");
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().write("Error(" + status + "): " + mensaje);
	}

}
