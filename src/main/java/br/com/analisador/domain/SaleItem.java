package br.com.analisador.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item de uma venda, no formato {@code ID-Quantidade-Preço} dentro dos
 * colchetes do registro 003. Quantidade é contagem (int); preço é valor
 * monetário ({@link BigDecimal}, D2 — nunca {@code double}).
 */
public record SaleItem(String id, int quantity, BigDecimal price) {

    public SaleItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(price, "price");
    }

    public BigDecimal total() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
