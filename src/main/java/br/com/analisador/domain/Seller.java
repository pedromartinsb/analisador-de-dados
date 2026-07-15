package br.com.analisador.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Registro de vendedor (prefixo 001): {@code 001çCPFçNomeçSalário}
 */
public record Seller(String cpf, String name, BigDecimal salary) implements DataRecord {

    public Seller {
        Objects.requireNonNull(cpf, "cpf");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(salary, "salary");
    }
}
