package com.agrogestao.gemini;

import com.agrogestao.config.GeminiProperties;
import com.agrogestao.exception.BadGatewayException;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoogleGenAiCropExtractor implements GeminiCropExtractor {

    private static final Logger log = LoggerFactory.getLogger(GoogleGenAiCropExtractor.class);

    static final String PROMPT = """
            Você extrai dados de fichas de custo de produção agrícola em PDF (formato EMATER-DF e semelhantes).
            Responda APENAS com um JSON válido, sem markdown.

            Regras obrigatórias:
            - Nunca invente campos ausentes. O que a planilha não informar deve sair null (escalares) ou [] (listas).
            - Não infira variedade, data de plantio, área do talhão nem produtividade que não estejam escritas.
            - areaHectares deve ser null em ficha de "custo por hectare" (sem área do talhão).
            - dataPlantio, se existir, no formato ISO YYYY-MM-DD; caso contrário null.
            - produtividadeEsperada deve preservar valor e unidade como na ficha (ex.: "4100.00 cx/5kg").
            - nomeCultura é o nome da cultura SEM o sistema de irrigação entre parênteses.
            - sistemaIrrigacao é o texto entre parênteses ou o sistema informado; null se ausente.
            - Números no JSON usam ponto decimal. Copie os valores impressos; não recálcule nem arredonde de outro modo.
            - Se um valor numérico estiver ilegível, omita o item da lista e explique em warnings.
            - Se a soma dos itens divergir do total impresso, avise em warnings. Não "corrija" a planilha.
            - warnings nunca deve ser omitido; use [] se não houver aviso. Inclua avisos de campo ausente e de reclassificação.

            Classifique CADA linha em um dos quatro grupos pela natureza do item (não pelo título da seção da EMATER):
            - acoesMecanicas: serviços com unidade h/m (aração, gradagem, rotoencanteirador, etc.).
            - acoesManuais: serviços com unidade d/h (capina, adubação manual, transplantio, colheita, aplicação, irrigação, etc.).
            - insumos: adubos, defensivos e mudas.
            - outros: demais custos (energia elétrica para irrigação, etc.).
            Reclassificações obrigatórias:
            - Mudas (mesmo que apareçam em SERVIÇOS) vão para insumos; registre em warnings.
            - Energia elétrica (mesmo que apareça em INSUMOS) vai para outros; registre em warnings.
            Os quatro grupos devem existir sempre, mesmo que vazios.

            Cada item das listas tem exatamente:
            - descricao (texto da coluna Descrição)
            - unidade (h/m, d/h, sc/50kg, kg, t, L, und, kwh, ...)
            - quantidade (número)
            - valorUnitario (R$)
            - valorTotal (R$)

            Campos de cabeçalho:
            - nomeCultura, variedade, sistemaIrrigacao, areaHectares, dataPlantio,
              produtividadeEsperada, custoTotal, custoUnidadeComercializacao, observacoes, warnings.
            observacoes: copie o sentido das observações impressas, sem completar o que não está escrito.
            """;

    private final Client client;
    private final String model;
    private final Schema responseSchema;

    public GoogleGenAiCropExtractor(GeminiProperties properties) {
        this.client = Client.builder().apiKey(properties.getApiKey()).build();
        this.model = properties.getModel();
        this.responseSchema = buildSchema();
    }

    @Override
    public String extractJson(byte[] pdfContent) {
        try {
            Content content = Content.fromParts(
                    Part.fromBytes(pdfContent, "application/pdf"),
                    Part.fromText(PROMPT)
            );
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(responseSchema)
                    .temperature(0.1f)
                    .build();
            GenerateContentResponse response = client.models.generateContent(model, content, config);
            String text = response == null ? null : response.text();
            if (text == null || text.isBlank()) {
                throw new BadGatewayException("Não foi possível analisar o PDF. Tente novamente mais tarde.");
            }
            return text;
        } catch (BadGatewayException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Falha na extração de cultura via Gemini");
            throw new BadGatewayException("Não foi possível analisar o PDF. Tente novamente mais tarde.");
        }
    }

    private static Schema buildSchema() {
        Schema item = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(itemProperties())
                .required(List.of("descricao", "unidade", "quantidade", "valorUnitario", "valorTotal"))
                .build();
        Schema itemArray = Schema.builder()
                .type(Type.Known.ARRAY)
                .items(item)
                .build();
        Schema stringOrNull = Schema.builder().type(Type.Known.STRING).nullable(true).build();
        Schema numberOrNull = Schema.builder().type(Type.Known.NUMBER).nullable(true).build();
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("nomeCultura", stringOrNull);
        properties.put("variedade", stringOrNull);
        properties.put("sistemaIrrigacao", stringOrNull);
        properties.put("areaHectares", numberOrNull);
        properties.put("dataPlantio", stringOrNull);
        properties.put("produtividadeEsperada", stringOrNull);
        properties.put("custoTotal", numberOrNull);
        properties.put("custoUnidadeComercializacao", numberOrNull);
        properties.put("observacoes", stringOrNull);
        properties.put("warnings", Schema.builder()
                .type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.STRING).build())
                .build());
        properties.put("acoesMecanicas", itemArray);
        properties.put("acoesManuais", itemArray);
        properties.put("insumos", itemArray);
        properties.put("outros", itemArray);
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of(
                        "nomeCultura",
                        "variedade",
                        "sistemaIrrigacao",
                        "areaHectares",
                        "dataPlantio",
                        "produtividadeEsperada",
                        "custoTotal",
                        "custoUnidadeComercializacao",
                        "observacoes",
                        "warnings",
                        "acoesMecanicas",
                        "acoesManuais",
                        "insumos",
                        "outros"
                ))
                .build();
    }

    private static Map<String, Schema> itemProperties() {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("descricao", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        properties.put("unidade", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        properties.put("quantidade", Schema.builder().type(Type.Known.NUMBER).nullable(true).build());
        properties.put("valorUnitario", Schema.builder().type(Type.Known.NUMBER).nullable(true).build());
        properties.put("valorTotal", Schema.builder().type(Type.Known.NUMBER).nullable(true).build());
        return properties;
    }
}
