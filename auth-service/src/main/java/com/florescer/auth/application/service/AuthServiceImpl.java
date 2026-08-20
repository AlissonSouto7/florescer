package com.florescer.auth.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.florescer.auth.exception.custom.TooManyAttemptsException;
import com.florescer.auth.infrastructure.ratelimit.FailedLoginTracker;
import com.florescer.auth.application.repository.RoleRepository;
import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.dto.LoginRequest;
import com.florescer.auth.domain.dto.LoginResponse;
import com.florescer.auth.domain.dto.RegisterRequest;
import com.florescer.auth.domain.dto.RegisterResponse;
import com.florescer.auth.domain.entity.Role;
import com.florescer.auth.domain.entity.User;
import com.florescer.auth.exception.custom.EmailAlreadyRegisteredException;
import com.florescer.auth.infrastructure.logging.SensitiveData;
import com.florescer.auth.infrastructure.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final SensitiveData sensitiveData;
	private final FailedLoginTracker failedLoginTracker;

	@Override
	public RegisterResponse register(@Valid RegisterRequest request) {
		// O pseudônimo permite acompanhar tentativas do mesmo e-mail sem gravar
		// o endereço. Ver SensitiveData.
		log.info("Tentativa de registro: subject={}", sensitiveData.pseudonymize(request.email()));

		checkAvailableEmail(request.email());
		User user = registerUser(request);

		log.info("Usuário registrado com sucesso: userId={}", user.getUserId());
		return new RegisterResponse(user.getUserId());
	}

	/**
	 * Entra na conta, com a contagem de erros por conta no caminho.
	 *
	 * <p>A ordem aqui é a parte que importa, e ela é deliberada: <b>a senha é
	 * verificada primeiro, e só quem erra é recusado pelo contador</b>. Quem
	 * acerta entra mesmo com o contador estourado.
	 *
	 * <p>Recusar antes de verificar seria mais barato, e foi assim que a versão
	 * anterior funcionava. O problema é que isso entrega a chave do painel a
	 * qualquer um: dez senhas erradas contra a conta da vendedora e ela fica de
	 * fora, sem ter feito nada, até a janela virar. Repetindo a cada janela,
	 * indefinidamente. Errar a senha de alguém não pode ser uma forma de
	 * trancar essa pessoa.
	 *
	 * <p>O contador zera no acerto: errar três vezes e lembrar a senha não pode
	 * deixar a conta a três erros do bloqueio pelo resto da janela.
	 */
	@Override
	public LoginResponse login(@Valid LoginRequest request) {

		String subject = sensitiveData.pseudonymize(request.email());
		log.info("Tentativa de login: subject={}", subject);

		boolean bloqueada = failedLoginTracker.isBlocked(request.email());

		try {
			String accessToken = authenticatesGeneratesToken(request);
			failedLoginTracker.recordSuccess(request.email());
			log.info("Login bem-sucedido: subject={}", subject);
			return new LoginResponse(accessToken);

		} catch (BadCredentialsException ex) {
			failedLoginTracker.recordFailure(request.email());
			log.warn("Falha no login: subject={}", subject);

			if (bloqueada) {
				// O log não traz o e-mail: o pseudônimo dá o rastro sem colocar
				// a lista de quem tem conta na loja dentro do arquivo de log.
				log.warn("Conta com excesso de erros de senha: subject={}", subject);
				throw new TooManyAttemptsException(failedLoginTracker.secondsUntilReset(request.email()));
			}

			throw new BadCredentialsException("Credenciais inválidas.");
		}
	}

	private String authenticatesGeneratesToken(LoginRequest request) {

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

		return jwtService.generateToken(authentication);
	}

	private void checkAvailableEmail(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			log.warn("Registro recusado, e-mail já em uso: subject={}", sensitiveData.pseudonymize(email));
			throw new EmailAlreadyRegisteredException();
		}
	}

	private User registerUser(@Valid RegisterRequest request) {
		Role roleBasic = roleRepository.findByName(Role.Values.BASIC.name());

		User user = new User(request.name(), request.email(), roleBasic);
		user.setPassword(passwordEncoder.encode(request.password()));

		return userRepository.save(user);
	}
}