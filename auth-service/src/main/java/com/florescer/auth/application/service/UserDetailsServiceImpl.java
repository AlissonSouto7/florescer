package com.florescer.auth.application.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.exception.custom.EmailNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
	
	private final UserRepository repository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return repository.findByEmail(email)
				.map(UserAuthenticated::new)
				// Sem o endereço na mensagem: ela é registrada em log e devolvida
				// pelo handler, então carregar o valor o espalharia nos dois.
				.orElseThrow(() -> new EmailNotFoundException("Credenciais inválidas."));
	}
}