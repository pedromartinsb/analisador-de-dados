package br.com.analisador.analysis;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.analisador.domain.Client;
import br.com.analisador.domain.ParsedData;
import br.com.analisador.domain.Sale;
import br.com.analisador.domain.Seller;

/**
 * Agrega um {@link ParsedData} em um {@link AnalysisResult}. Puro, exceto
 * pelo {@code WARN} de venda descartada por integridade referencial (§11).
 *
 * <p>O cálculo de faturamento por vendedor parte da lista de {@link Seller}
 * (D14), não de um agrupamento sobre as vendas: um vendedor sem nenhuma
 * venda tem faturamento zero e precisa aparecer no resultado, o que um
 * {@code groupingBy} sobre {@link Sale} nunca produziria por construção —
 * ele simplesmente nunca veria esse vendedor.
 *
 * <p>A resolução de integridade referencial (D7) roda em duas passagens:
 * o conjunto de vendedores conhecidos é montado primeiro, a partir de
 * {@link ParsedData#sellers()}, e só então as vendas são filtradas contra
 * ele. Isso é necessário porque nada garante que os registros 001 precedam
 * os 003 no arquivo — o exemplo do enunciado ordena, mas isso não é
 * contrato do formato.
 *
 * <p>A referência de venda para vendedor é feita por <strong>nome</strong>,
 * não por CPF — é o único identificador que o registro 003 carrega
 * ({@code Nome do Vendedor}). Dois vendedores homônimos com CPFs diferentes
 * colidiriam nessa checagem; o formato do enunciado não oferece meio de
 * desambiguar isso.
 */
public class SalesAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(SalesAnalyzer.class);

    public AnalysisResult analyze(ParsedData data) {
        Objects.requireNonNull(data, "data");

        List<Seller> sellers = distinctByKey(data.sellers(), Seller::cpf);
        List<Client> clients = distinctByKey(data.clients(), Client::cnpj);
        List<Sale> validSales = discardSalesWithUnknownSeller(data.sales(), sellers);

        Optional<String> mostExpensiveSaleId = validSales.stream()
                .max(Comparator.comparing(Sale::total))
                .map(Sale::id);

        return new AnalysisResult(clients.size(), sellers.size(),
                mostExpensiveSaleId, worstSellerName(sellers, validSales));
    }

    /**
     * Faturamento por vendedor em uma única passagem sobre as vendas.
     *
     * <p>O mapa é semeado a partir da lista de {@link Seller} antes de
     * qualquer venda ser somada — é isso que faz o vendedor sem nenhuma
     * venda aparecer com faturamento zero e concorrer a pior vendedor
     * (D14). Um agrupamento sobre as vendas nunca o veria.
     *
     * <p>{@link LinkedHashMap} preserva a ordem de inserção, que é a ordem
     * dos registros 001 no arquivo; combinada com a estabilidade de
     * {@link java.util.stream.Stream#min}, garante o desempate por primeira
     * ocorrência (D12).
     */
    private Optional<String> worstSellerName(List<Seller> sellers, List<Sale> sales) {
        Map<String, BigDecimal> revenueBySeller = new LinkedHashMap<>();
        for (Seller seller : sellers) {
            revenueBySeller.putIfAbsent(seller.name(), BigDecimal.ZERO);
        }
        for (Sale sale : sales) {
            revenueBySeller.merge(sale.sellerName(), sale.total(), BigDecimal::add);
        }
        return revenueBySeller.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private List<Sale> discardSalesWithUnknownSeller(List<Sale> sales, List<Seller> sellers) {
        Set<String> knownSellerNames = new HashSet<>();
        for (Seller seller : sellers) {
            knownSellerNames.add(seller.name());
        }

        return sales.stream()
                .filter(sale -> {
                    boolean known = knownSellerNames.contains(sale.sellerName());
                    if (!known) {
                        log.warn("Venda {}: descartada, vendedor referenciado não consta nos registros 001",
                                sale.id());
                    }
                    return known;
                })
                .toList();
    }

    private <T> List<T> distinctByKey(List<T> items, Function<T, String> keyExtractor) {
        Set<String> seen = new HashSet<>();
        return items.stream()
                .filter(item -> seen.add(keyExtractor.apply(item)))
                .toList();
    }
}
