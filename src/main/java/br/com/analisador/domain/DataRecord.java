package br.com.analisador.domain;

/**
 * Registro de dados do arquivo de entrada. O conjunto de implementações é
 * fechado por design: os três layouts (001, 002, 003) são os únicos que o
 * enunciado define. Adicionar um novo tipo de registro exige editar esta
 * cláusula {@code permits}, o que força a revisão de todo consumidor
 * exaustivo (parser, análise, relatório).
 */
public sealed interface DataRecord permits Seller, Client, Sale {
}
