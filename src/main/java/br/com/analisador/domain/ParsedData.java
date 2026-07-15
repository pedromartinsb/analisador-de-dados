package br.com.analisador.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resultado da leitura completa de um arquivo: todos os registros, na ordem
 * em que apareceram. A ordem é preservada de propósito — a resolução de
 * integridade referencial (D7, camada de análise) e o desempate por
 * primeira ocorrência (D12) dependem dela.
 */
public record ParsedData(List<DataRecord> records) {

    public ParsedData {
        Objects.requireNonNull(records, "records");
        records = List.copyOf(records);
    }

    public List<Seller> sellers() {
        return filter(Seller.class);
    }

    public List<Client> clients() {
        return filter(Client.class);
    }

    public List<Sale> sales() {
        return filter(Sale.class);
    }

    private <T extends DataRecord> List<T> filter(Class<T> type) {
        return records.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
