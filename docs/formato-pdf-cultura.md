# Formato do PDF de cultura e contrato JSON (Gemini)

Este documento descreve a ficha de exemplo [`alface.pdf`](alface.pdf) e o JSON que o backend deve obter do Gemini ao analisá-la. O modelo **não inventa** campo ausente: o que a planilha não informa sai `null` (escalares) ou `[]` (listas). Ambiguidades de classificação vão para `warnings`.

## A planilha `alface.pdf`

Fonte: **EMATER-DF** (Governo do Distrito Federal / SEAGRI-DF), documento gerado eletronicamente em 21/10/2025, página 1/1.

Título: **Custo de produção por hectare**.

Cabeçalho da ficha:

| Informação na planilha | Interpretação |
| --- | --- |
| Cultura: Alface (Aspersão) | `nomeCultura` = Alface; `sistemaIrrigacao` = Aspersão |
| Produtividade: 4.100,00 cx/5kg | `produtividadeEsperada` = `4100.00 cx/5kg` |
| (sem variedade) | `variedade` = `null` |
| Custo por hectare (não há talhão) | `areaHectares` = `null` |
| (sem data de plantio) | `dataPlantio` = `null` |
| CUSTO TOTAL | `custoTotal` = `44076.26` |
| CUSTO (UNIDADE DE COMERCIALIZAÇÃO) | `custoUnidadeComercializacao` = `10.75` |

A ficha original agrupa linhas em **INSUMOS** e **SERVIÇOS**. A extração **reclassifica** cada linha em um dos quatro grupos do sistema, pela natureza do item (não pelo título da seção da EMATER):

| Grupo no JSON | Critério | Exemplos nesta ficha |
| --- | --- | --- |
| `acoesMecanicas` | Serviço com unidade **h/m** | Aração, gradagem, rotoencanteirador |
| `acoesManuais` | Serviço com unidade **d/h** | Capina, adubação manual, transplantio, colheita, aplicação de agrotóxico, irrigação |
| `insumos` | Adubos, defensivos e mudas | 4-30-16, sulfato de zinco, ureia, cama de frango, agrotóxicos, mudas em bandeja |
| `outros` | Demais custos | Energia elétrica para irrigação |

Reclassificações obrigatórias nesta ficha (registrar em `warnings`):

- **Mudas** (Formação em bandejas de 200 células) aparece em SERVIÇOS; vai para `insumos`.
- **Energia elétrica p/ irrigação** aparece em INSUMOS; vai para `outros`.

Subtotais da ficha (conferência, não são campos do JSON): INSUMOS R$ 10.689,26; SERVIÇOS R$ 33.387,00; total R$ 44.076,26.

Observações impressas na planilha (copiar o sentido, sem completar o que não está escrito):

1. Espaçamento 0,25 m × 0,25 m, canteiros de 1,0 m de largura e 50 cm de carreador (66 canteiros de 100 m por hectare); 105.600 plantas por hectare.
2. Mudas com 5% de perdas: 554 bandejas de 200 células.
3. Rendimento de colheita, lavagem e acondicionamento: 50 caixas por dia-homem.
4. Em média 30% das plantas não são colhidas.

## Item de custo

Cada elemento das quatro listas tem exatamente:

| Campo | Tipo | Significado |
| --- | --- | --- |
| `descricao` | string | Texto da coluna Descrição |
| `unidade` | string | Texto da coluna Unidade (`h/m`, `d/h`, `sc/50kg`, `kg`, `t`, `L`, `und`, `kwh`, …) |
| `quantidade` | number | Coluna Quantidade |
| `valorUnitario` | number | Coluna Valor Unitário (R$) |
| `valorTotal` | number | Coluna Valor Total (R$) |

Números no JSON usam ponto decimal. Não arredondar de outro modo: copiar os valores impressos.

## Campos de cabeçalho

| Campo | Tipo | Regra |
| --- | --- | --- |
| `nomeCultura` | string | Nome da cultura, sem o sistema de irrigação entre parênteses |
| `variedade` | string \| null | `null` se a ficha não nomear variedade |
| `sistemaIrrigacao` | string \| null | Ex.: Aspersão; `null` se ausente |
| `areaHectares` | number \| null | `null` em ficha de **custo por hectare** (não há área do talhão) |
| `dataPlantio` | string \| null | `null` se a data não existir; se existir, ISO `YYYY-MM-DD` |
| `produtividadeEsperada` | string \| null | Valor e unidade como na ficha (ex.: `4100.00 cx/5kg`) |
| `custoTotal` | number \| null | CUSTO TOTAL |
| `custoUnidadeComercializacao` | number \| null | Custo por unidade de comercialização |
| `observacoes` | string \| null | Texto das observações da ficha; `null` se não houver |
| `warnings` | string[] | Avisos de extração (campo ausente, reclassificação, valor ilegível). Nunca omitir a chave; usar `[]` se não houver aviso |

## JSON esperado para `alface.pdf`

```json
{
  "nomeCultura": "Alface",
  "variedade": null,
  "sistemaIrrigacao": "Aspersão",
  "areaHectares": null,
  "dataPlantio": null,
  "produtividadeEsperada": "4100.00 cx/5kg",
  "custoTotal": 44076.26,
  "custoUnidadeComercializacao": 10.75,
  "observacoes": "Espaçamento de 0,25 m x 0,25 m em canteiros de 1,0 m de largura e 50 cm de carreador (66 canteiros de 100 m por hectare). Total de 105.600 plantas por hectare. Necessidade de mudas considerando 5% de perdas: 554 bandejas de 200 células. Rendimento de colheita, lavagem e acondicionamento: 50 caixas por dia-homem. Em média 30% das plantas não são colhidas.",
  "warnings": [
    "variedade não informada na planilha",
    "dataPlantio não informada na planilha",
    "areaHectares nula: ficha de custo por hectare, sem área do talhão",
    "Mudas (Formação em bandejas de 200 células) reclassificado de SERVIÇOS para insumos",
    "Energia elétrica p/ irrigação reclassificado de INSUMOS para outros"
  ],
  "acoesMecanicas": [
    {
      "descricao": "Preparo do solo (Aração)",
      "unidade": "h/m",
      "quantidade": 3.0,
      "valorUnitario": 260.0,
      "valorTotal": 780.0
    },
    {
      "descricao": "Preparo do solo (Gradagem)",
      "unidade": "h/m",
      "quantidade": 2.0,
      "valorUnitario": 260.0,
      "valorTotal": 520.0
    },
    {
      "descricao": "Preparo de solo (Lev. cant.c/ rotoencanteirador)",
      "unidade": "h/m",
      "quantidade": 4.0,
      "valorUnitario": 260.0,
      "valorTotal": 1040.0
    }
  ],
  "acoesManuais": [
    {
      "descricao": "Adubação (Manual de cobertura)",
      "unidade": "d/h",
      "quantidade": 3.0,
      "valorUnitario": 110.0,
      "valorTotal": 330.0
    },
    {
      "descricao": "Adubos (Distribuição manual)",
      "unidade": "d/h",
      "quantidade": 4.0,
      "valorUnitario": 110.0,
      "valorTotal": 440.0
    },
    {
      "descricao": "Agrotóxico (Aplicação)",
      "unidade": "d/h",
      "quantidade": 5.0,
      "valorUnitario": 110.0,
      "valorTotal": 550.0
    },
    {
      "descricao": "Capina (Manual)",
      "unidade": "d/h",
      "quantidade": 60.0,
      "valorUnitario": 110.0,
      "valorTotal": 6600.0
    },
    {
      "descricao": "Colheita/Classificação/Acondicionamento",
      "unidade": "d/h",
      "quantidade": 82.0,
      "valorUnitario": 110.0,
      "valorTotal": 9020.0
    },
    {
      "descricao": "Irrigação (Aspersão)",
      "unidade": "d/h",
      "quantidade": 2.0,
      "valorUnitario": 110.0,
      "valorTotal": 220.0
    },
    {
      "descricao": "Irrigação (Montagem do sistema)",
      "unidade": "d/h",
      "quantidade": 2.0,
      "valorUnitario": 110.0,
      "valorTotal": 220.0
    },
    {
      "descricao": "Transplantio",
      "unidade": "d/h",
      "quantidade": 21.0,
      "valorUnitario": 110.0,
      "valorTotal": 2310.0
    }
  ],
  "insumos": [
    {
      "descricao": "Adubo mineral 4-30-16",
      "unidade": "sc/50kg",
      "quantidade": 20.0,
      "valorUnitario": 243.35,
      "valorTotal": 4867.0
    },
    {
      "descricao": "Adubo mineral Sulfato de zinco",
      "unidade": "kg",
      "quantidade": 20.0,
      "valorUnitario": 8.78,
      "valorTotal": 175.59
    },
    {
      "descricao": "Adubo mineral Uréia",
      "unidade": "sc/50kg",
      "quantidade": 2.0,
      "valorUnitario": 241.78,
      "valorTotal": 483.56
    },
    {
      "descricao": "Adubo orgânico (Cama de frango)",
      "unidade": "t",
      "quantidade": 10.0,
      "valorUnitario": 350.0,
      "valorTotal": 3500.0
    },
    {
      "descricao": "Agrotóxico (Azadiractina 12g/l)",
      "unidade": "L",
      "quantidade": 1.5,
      "valorUnitario": 284.26,
      "valorTotal": 426.39
    },
    {
      "descricao": "Agrotóxico (Imidacloprido 700 g/kg)",
      "unidade": "kg",
      "quantidade": 0.3,
      "valorUnitario": 112.0,
      "valorTotal": 33.6
    },
    {
      "descricao": "Agrotóxico (Iprodiona 500 g/l)",
      "unidade": "L",
      "quantidade": 3.0,
      "valorUnitario": 197.74,
      "valorTotal": 593.22
    },
    {
      "descricao": "Mudas (Formação em bandejas de 200 células)",
      "unidade": "und",
      "quantidade": 554.0,
      "valorUnitario": 20.5,
      "valorTotal": 11357.0
    }
  ],
  "outros": [
    {
      "descricao": "Energia elétrica p/ irrigação",
      "unidade": "kwh",
      "quantidade": 642.0,
      "valorUnitario": 0.95,
      "valorTotal": 609.9
    }
  ]
}
```

## Regras para outras fichas

- Manter os quatro grupos sempre presentes, mesmo que vazios.
- Classificar pela unidade e pelo tipo do item, não pelo título da seção da EMATER.
- Não inferir variedade, data de plantio, área ou produtividade que não estejam escritas.
- Se um valor numérico estiver ilegível, omitir o item da lista e explicar em `warnings`.
- Não recalcular totais para “corrigir” a planilha; usar os números impressos e, se a soma divergir, avisar em `warnings`.
