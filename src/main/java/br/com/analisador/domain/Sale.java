package br.com.analisador.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Registro de venda (prefixo 003): {@code 003çID da VendaçItens da VendaçNome do Vendedor}
 */
public record Sale(String id, List<SaleItem> items, String sellerName) implements DataRecord {

    public Sale {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(sellerName, "sellerName");
        items = List.copyOf(items);
    }

    public BigDecimal total() {
        return items.stream()
                .map(SaleItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
