package com.florescer.auth.application.domain;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Entity
@Table(name = "tb_users")
@Data
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID userId;
	
	private String name;
	
	@Email
	private String email;
	
	private String password;
	
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
	private Set<Role> roles; 
    
} 