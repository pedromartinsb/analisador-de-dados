package br.com.analisador.analysis;

import java.util.Optional;

/**
 * Resultado da análise de um arquivo. Ausência de dado (nenhuma venda,
 * nenhum vendedor) é modelada com {@link Optional} — a decisão de
 * apresentação ("N/A") pertence ao formatter, não a este tipo (D17, §8).
 */
public record AnalysisResult(
        long clientCount,
        long sellerCount,
        Optional<String> mostExpensiveSaleId,
        Optional<String> worstSellerName) {
}
