package br.com.analisador.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.analisador.analysis.AnalysisResult;

class ReportFormatterTest {

    private final ReportFormatter formatter = new ReportFormatter();

    @Test
    void caso23_deveFormatarAsQuatroLinhasNaOrdemEsperada() {
        AnalysisResult result = new AnalysisResult(2, 2, Optional.of("10"), Optional.of("Paulo"));

        String expected = "Quantidade de clientes: 2\n"
                + "Quantidade de vendedores: 2\n"
                + "ID da venda mais cara: 10\n"
                + "Pior vendedor (menor volume de vendas): Paulo\n";

        assertEquals(expected, formatter.format(result));
    }

    @Test
    void caso24_devolveNADeOptionalVazio() {
        AnalysisResult result = new AnalysisResult(0, 0, Optional.empty(), Optional.empty());

        String expected = "Quantidade de clientes: 0\n"
                + "Quantidade de vendedores: 0\n"
                + "ID da venda mais cara: N/A\n"
                + "Pior vendedor (menor volume de vendas): N/A\n";

        assertEquals(expected, formatter.format(result));
    }
}
