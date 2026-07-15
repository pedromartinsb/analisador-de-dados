package br.com.analisador.domain;

import java.util.Objects;

/**
 * Registro de cliente (prefixo 002): {@code 002çCNPJçNomeçSegmento de Negócio}
 */
public record Client(String cnpj, String name, String businessSegment) implements DataRecord {

    public Client {
        Objects.requireNonNull(cnpj, "cnpj");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(businessSegment, "businessSegment");
    }
}
