package com.florescer.auth.domain.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "tb_users")
@Getter
@Setter
@NoArgsConstructor
// equals e hashCode apenas pelo identificador. Compará-los por todos os campos
// percorreria também o Set de papéis, e faria dois carregamentos da mesma linha
// deixarem de ser iguais assim que qualquer campo mudasse.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id")
	@EqualsAndHashCode.Include
	private UUID userId;

	@Column(nullable = false, length = 50)
	private String name;

	/**
	 * Único no banco, e não só verificado em código.
	 *
	 * <p>A checagem anterior consultava antes de gravar, o que deixa uma janela:
	 * duas requisições simultâneas com o mesmo endereço passam as duas e criam
	 * duas contas.
	 */
	@Column(nullable = false, unique = true)
	@ToString.Exclude
	private String email;

	/**
	 * Fora do {@code toString} de propósito.
	 *
	 * <p>O hash apareceu no log de um teste porque a entidade era anotada com
	 * {@code @Data}, que imprime todos os campos. Hash de BCrypt não é senha em
	 * texto puro, mas é material para ataque offline: quem o obtém tenta quebrar
	 * sem tocar no sistema, sem rate limit e sem deixar rastro.
	 */
	@Column(nullable = false, length = 60)
	@ToString.Exclude
	private String password;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "tb_users_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	@ToString.Exclude
	private Set<Role> roles = new HashSet<>();

	public User(String name, String email, Role role) {
		this.name = name;
		this.email = email;
		this.roles.add(role);
	}
}
