package com.florescer.product.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.enums.Watering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// equals e hashCode apenas pelo identificador. Compará-los por todos os campos,
// como o @Data faria, quebra o contrato de igualdade em entidade gerenciada:
// dois carregamentos da mesma linha em momentos diferentes deixariam de ser
// iguais assim que qualquer campo mudasse.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, length = 100)
	private String type;

	@Column(nullable = false, length = 500)
	private String description;

	/**
	 * Dinheiro em {@code BigDecimal}, com escala fixa no banco.
	 *
	 * <p>{@code Double} é ponto flutuante binário e não representa valores
	 * decimais exatamente: somar itens acumula erro e comparar por igualdade
	 * deixa de ser confiável. Com dinheiro isso vira diferença de centavos que
	 * ninguém consegue explicar.
	 */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "quantity_stock", nullable = false)
	private Integer quantityStock;

	@Column(name = "care_requirements", nullable = false, length = 500)
	private String careRequirements;

	@Column(nullable = false)
	private Boolean availability;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "image_path", nullable = false)
	private String imagePath;

	// Os campos abaixo respondem o que quem compra pergunta antes de fechar
	// negócio. Aceitam nulo porque foram acrescentados a uma tabela que já tinha
	// linhas: inventar altura ou luminosidade para as plantas antigas colocaria
	// informação falsa na vitrine. A API exige todos nos cadastros novos.

	/** Altura aproximada, em centímetros. A pergunta número um de quem compra. */
	@Column(name = "height_cm")
	private Integer heightCm;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Light light;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private Watering watering;

	/** Se pode conviver com gato ou cachorro. Segurança, não conveniência. */
	@Column(name = "pet_safe")
	private Boolean petSafe;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Environment environment;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Difficulty difficulty;

	/** Se o preço já inclui o vaso, ou se a planta vai só com o torrão. */
	@Column(name = "includes_pot")
	private Boolean includesPot;

	/** Quando o produto entrou no catálogo. Preenchido uma única vez. */
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Última alteração. Responde "isso mudou quando?" sem depender de log. */
	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	void aoCriar() {
		Instant agora = Instant.now();
		this.createdAt = agora;
		this.updatedAt = agora;
	}

	@PreUpdate
	void aoAtualizar() {
		this.updatedAt = Instant.now();
	}
}
