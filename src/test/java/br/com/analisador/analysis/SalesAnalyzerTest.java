package br.com.analisador.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.analisador.domain.Client;
import br.com.analisador.domain.ParsedData;
import br.com.analisador.domain.Sale;
import br.com.analisador.domain.SaleItem;
import br.com.analisador.domain.Seller;

class SalesAnalyzerTest {

    private final SalesAnalyzer analyzer = new SalesAnalyzer();

    @Test
    void caso14_deveReproduzirOOraculoDoEnunciado() {
        Seller pedro = new Seller("1234567891234", "Pedro", new BigDecimal("50000"));
        Seller paulo = new Seller("3245678865434", "Paulo", new BigDecimal("40000.99"));
        Client jose = new Client("2345675434544345", "Jose da Silva", "Rural");
        Client eduardo = new Client("2345675433444345", "Eduardo Pereira", "Rural");

        Sale saleDoPedro = new Sale("10", List.of(
                new SaleItem("1", 10, new BigDecimal("100")),
                new SaleItem("2", 30, new BigDecimal("2.50")),
                new SaleItem("3", 40, new BigDecimal("3.10"))
        ), "Pedro");
        Sale saleDoPaulo = new Sale("08", List.of(
                new SaleItem("1", 34, new BigDecimal("10")),
                new SaleItem("2", 33, new BigDecimal("1.50")),
                new SaleItem("3", 40, new BigDecimal("0.10"))
        ), "Paulo");

        ParsedData data = new ParsedData(List.of(pedro, paulo, jose, eduardo, saleDoPedro, saleDoPaulo));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals(2, result.clientCount());
        assertEquals(2, result.sellerCount());
        assertEquals("10", result.mostExpensiveSaleId().orElseThrow());
        assertEquals("Paulo", result.worstSellerName().orElseThrow());
    }

    @Test
    void caso15_deveDescartarVendaDeVendedorInexistente() {
        Seller pedro = new Seller("1", "Pedro", new BigDecimal("1000"));
        Sale vendaValida = new Sale("1", List.of(new SaleItem("i", 1, new BigDecimal("500"))), "Pedro");
        Sale vendaFantasma = new Sale("2", List.of(new SaleItem("i", 1, new BigDecimal("9999"))), "Fantasma");

        ParsedData data = new ParsedData(List.of(pedro, vendaValida, vendaFantasma));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals("1", result.mostExpensiveSaleId().orElseThrow(),
                "a venda de maior valor pertence a um vendedor inexistente e deve ser descartada");
        assertEquals("Pedro", result.worstSellerName().orElseThrow());
    }

    @Test
    void caso16_semVendas_deveDevolverVendaMaisCaraVazia() {
        Seller pedro = new Seller("1", "Pedro", new BigDecimal("1000"));
        ParsedData data = new ParsedData(List.of(pedro));

        AnalysisResult result = analyzer.analyze(data);

        assertTrue(result.mostExpensiveSaleId().isEmpty());
    }

    @Test
    void caso17_semVendedores_devePiorVendedorVazio() {
        ParsedData data = new ParsedData(List.of());

        AnalysisResult result = analyzer.analyze(data);

        assertTrue(result.worstSellerName().isEmpty());
    }

    @Test
    void caso18_empateNaVendaMaisCara_deveVencerAPrimeiraDoArquivo() {
        Seller pedro = new Seller("1", "Pedro", new BigDecimal("1000"));
        Sale primeira = new Sale("A", List.of(new SaleItem("i", 1, new BigDecimal("500"))), "Pedro");
        Sale segunda = new Sale("B", List.of(new SaleItem("i", 1, new BigDecimal("500"))), "Pedro");

        ParsedData data = new ParsedData(List.of(pedro, primeira, segunda));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals("A", result.mostExpensiveSaleId().orElseThrow());
    }

    @Test
    void caso19_empateNoPiorVendedor_deveVencerOPrimeiroDoArquivo() {
        Seller primeiro = new Seller("1", "Ana", new BigDecimal("1000"));
        Seller segundo = new Seller("2", "Bruno", new BigDecimal("1000"));
        Sale vendaAna = new Sale("1", List.of(new SaleItem("i", 1, new BigDecimal("100"))), "Ana");
        Sale vendaBruno = new Sale("2", List.of(new SaleItem("i", 1, new BigDecimal("100"))), "Bruno");

        ParsedData data = new ParsedData(List.of(primeiro, segundo, vendaAna, vendaBruno));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals("Ana", result.worstSellerName().orElseThrow());
    }

    @Test
    void caso20_ordemNoArquivoNaoDeveAfetarAResolucaoReferencial() {
        // Venda aparece antes do vendedor na lista de records — simula um
        // arquivo em que 003 precede 001, algo que o formato não proíbe.
        Sale venda = new Sale("1", List.of(new SaleItem("i", 1, new BigDecimal("500"))), "Pedro");
        Seller pedro = new Seller("1", "Pedro", new BigDecimal("1000"));

        ParsedData data = new ParsedData(List.of(venda, pedro));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals("1", result.mostExpensiveSaleId().orElseThrow(),
                "a venda não deve ser descartada só porque o vendedor aparece depois no arquivo");
    }

    @Test
    void caso21_vendedorSemVendas_deveEntrarComFaturamentoZeroEDeveSerOPior() {
        Seller comVenda = new Seller("1", "Pedro", new BigDecimal("1000"));
        Seller semVenda = new Seller("2", "Susana", new BigDecimal("1000"));
        Sale venda = new Sale("1", List.of(new SaleItem("i", 1, new BigDecimal("500"))), "Pedro");

        ParsedData data = new ParsedData(List.of(comVenda, semVenda, venda));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals(2, result.sellerCount(),
                "vendedor sem venda continua fazendo parte do universo de vendedores");
        assertEquals("Susana", result.worstSellerName().orElseThrow());
    }

    @Test
    void caso22_clientesDuplicadosPeloMesmoCnpjDevemContarUmaVez() {
        Client primeiro = new Client("123", "Jose da Silva", "Rural");
        Client duplicado = new Client("123", "Jose da Silva", "Rural");

        ParsedData data = new ParsedData(List.of(primeiro, duplicado));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals(1, result.clientCount());
    }

    @Test
    void caso22b_vendedoresDuplicadosPeloMesmoCpfDevemContarUmaVez() {
        Seller primeiro = new Seller("123", "Pedro", new BigDecimal("50000"));
        Seller duplicado = new Seller("123", "Pedro", new BigDecimal("50000"));

        ParsedData data = new ParsedData(List.of(primeiro, duplicado));

        AnalysisResult result = analyzer.analyze(data);

        assertEquals(1, result.sellerCount());
    }
}
