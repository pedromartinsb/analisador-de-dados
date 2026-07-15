package br.com.analisador.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.Sale;
import br.com.analisador.domain.SaleItem;

/**
 * {@code 003çID da VendaçItens da VendaçNome do Vendedor}
 *
 * <p>Itens delimitados por colchetes, separados por vírgula, cada item no
 * formato {@code ID-Quantidade-Preço}.
 */
public class SaleParser implements RecordParser {

    private static final int EXPECTED_FIELDS = 4;
    private static final int EXPECTED_ITEM_PARTS = 3;

    @Override
    public String prefix() {
        return "003";
    }

    @Override
    public DataRecord parse(String[] fields) {
        if (fields.length != EXPECTED_FIELDS) {
            throw new MalformedLineException(
                    "registro 003 esperado com %d campos, encontrado %d"
                            .formatted(EXPECTED_FIELDS, fields.length));
        }
        String saleId = fields[1];
        List<SaleItem> items = parseItems(fields[2]);
        String sellerName = fields[3];
        return new Sale(saleId, items, sellerName);
    }

    private List<SaleItem> parseItems(String raw) {
        if (raw.length() < 2 || raw.charAt(0) != '[' || raw.charAt(raw.length() - 1) != ']') {
            throw new MalformedLineException("itens da venda sem delimitadores '[' e ']'");
        }
        String inner = raw.substring(1, raw.length() - 1);
        if (inner.isEmpty()) {
            return List.of();
        }
        List<SaleItem> items = new ArrayList<>();
        for (String rawItem : inner.split(",", -1)) {
            items.add(parseItem(rawItem));
        }
        return items;
    }

    private SaleItem parseItem(String rawItem) {
        String[] parts = rawItem.split("-", -1);
        if (parts.length != EXPECTED_ITEM_PARTS) {
            throw new MalformedLineException(
                    "item de venda esperado no formato ID-Quantidade-Preço, recebido com %d parte(s)"
                            .formatted(parts.length));
        }
        String itemId = parts[0];
        int quantity = parseQuantity(parts[1]);
        BigDecimal price = parsePrice(parts[2]);
        return new SaleItem(itemId, quantity, price);
    }

    private int parseQuantity(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new MalformedLineException("quantidade de item inválida", e);
        }
    }

    private BigDecimal parsePrice(String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new MalformedLineException("preço de item inválido", e);
        }
    }
}
