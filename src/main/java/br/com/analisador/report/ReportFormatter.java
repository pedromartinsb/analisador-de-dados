package br.com.analisador.report;

import br.com.analisador.analysis.AnalysisResult;

/**
 * Converte um {@link AnalysisResult} no texto de saída, nas quatro linhas e
 * na ordem exatas exigidas pelo enunciado. Lógica pura — sem I/O — para
 * permitir teste sem tocar disco.
 *
 * <p>{@code Optional.empty()} é renderizado como {@code "N/A"} aqui, não no
 * domínio (D17): o domínio expressa ausência de dado, a decisão de como
 * apresentar essa ausência é responsabilidade de apresentação.
 *
 * <p>Cada linha termina em {@code \n}, incluindo a última — convenção comum
 * para arquivos de texto, evita a ambiguidade de um arquivo sem newline
 * final.
 */
public class ReportFormatter {

    private static final String NOT_AVAILABLE = "N/A";

    public String format(AnalysisResult result) {
        return "Quantidade de clientes: " + result.clientCount() + "\n"
                + "Quantidade de vendedores: " + result.sellerCount() + "\n"
                + "ID da venda mais cara: " + result.mostExpensiveSaleId().orElse(NOT_AVAILABLE) + "\n"
                + "Pior vendedor (menor volume de vendas): " + result.worstSellerName().orElse(NOT_AVAILABLE) + "\n";
    }
}
