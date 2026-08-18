export type CropStatus =
  | 'PLANEJADA'
  | 'PLANTADA'
  | 'EM_DESENVOLVIMENTO'
  | 'COLHIDA'
  | 'ENCERRADA';

export type ItemCategory = 'ACAO_MECANICA' | 'ACAO_MANUAL' | 'INSUMO' | 'OUTRO';

export type FinanceType = 'RECEITA' | 'DESPESA';

export type PlannedGroupKey = 'acoesMecanicas' | 'acoesManuais' | 'insumos' | 'outros';

export interface Property {
  id: string;
  name: string;
  city?: string | null;
  state?: string | null;
  totalAreaHa?: number | null;
  description?: string | null;
}

export interface PropertyPayload {
  name: string;
  city?: string | null;
  state?: string | null;
  totalAreaHa?: number | null;
  description?: string | null;
}

export interface PlannedItem {
  id?: string;
  description: string;
  unit?: string | null;
  quantity?: number | null;
  unitValue?: number | null;
  totalValue?: number | null;
  category?: ItemCategory;
}

export interface Crop {
  id: string;
  propertyId?: string | null;
  propertyName?: string | null;
  name: string;
  variety?: string | null;
  irrigationSystem?: string | null;
  areaHa?: number | null;
  plantingDate?: string | null;
  expectedHarvestDate?: string | null;
  status: CropStatus;
  expectedYield?: string | null;
  notes?: string | null;
  acoesMecanicas?: PlannedItem[];
  acoesManuais?: PlannedItem[];
  insumos?: PlannedItem[];
  outros?: PlannedItem[];
}

export interface CropPayload {
  propertyId: string;
  name: string;
  variety?: string | null;
  irrigationSystem?: string | null;
  areaHa?: number | null;
  plantingDate?: string | null;
  expectedHarvestDate?: string | null;
  status: CropStatus;
  expectedYield?: string | null;
  notes?: string | null;
}

export interface CropCyclePayload {
  status: CropStatus;
  plantingDate?: string | null;
  expectedHarvestDate?: string | null;
  notes?: string | null;
}

export interface PdfExtracted {
  name?: string | null;
  variety?: string | null;
  irrigationSystem?: string | null;
  areaHa?: number | null;
  plantingDate?: string | null;
  expectedYield?: string | null;
  notes?: string | null;
  acoesMecanicas?: PlannedItem[];
  acoesManuais?: PlannedItem[];
  insumos?: PlannedItem[];
  outros?: PlannedItem[];
}

export interface PdfAnalyzeResponse {
  analysisId: string;
  extracted?: PdfExtracted | null;
  warnings?: string[] | null;
}

export interface PdfConfirmPayload {
  analysisId: string;
  propertyId: string;
  name: string;
  variety?: string | null;
  irrigationSystem?: string | null;
  areaHa?: number | null;
  plantingDate?: string | null;
  expectedHarvestDate?: string | null;
  status: CropStatus;
  expectedYield?: string | null;
  notes?: string | null;
  acoesMecanicas: PlannedItem[];
  acoesManuais: PlannedItem[];
  insumos: PlannedItem[];
  outros: PlannedItem[];
  importCosts: boolean;
}

export interface ProductionCost {
  id: string;
  cropId?: string | null;
  description: string;
  category: ItemCategory;
  amount: number;
  date: string;
}

export interface ProductionCostPayload {
  description: string;
  category: ItemCategory;
  amount: number;
  date: string;
}

export interface FieldLog {
  id: string;
  propertyId: string;
  cropId?: string | null;
  cropName?: string | null;
  date: string;
  activity: string;
  notes?: string | null;
}

export interface FieldLogPayload {
  propertyId: string;
  cropId?: string | null;
  date: string;
  activity: string;
  notes?: string | null;
}

export interface FinanceEntry {
  id: string;
  propertyId: string;
  cropId?: string | null;
  cropName?: string | null;
  type: FinanceType;
  category?: string | null;
  description: string;
  amount: number;
  date: string;
}

export interface FinancePayload {
  propertyId: string;
  cropId?: string | null;
  type: FinanceType;
  category?: string | null;
  description: string;
  amount: number;
  date: string;
}

export interface FinanceStatement {
  receitaBruta: number;
  custosProducao: number;
  outrasDespesas: number;
  resultado: number;
}

export interface DocumentFile {
  id: string;
  propertyId: string;
  cropId?: string | null;
  cropName?: string | null;
  originalName: string;
  contentType?: string | null;
  sizeBytes: number;
  kind?: string | null;
  createdAt?: string | null;
}

export interface DashboardProperty {
  id: string;
  name: string;
  city?: string | null;
  state?: string | null;
  totalAreaHa?: number | null;
  activeCrops: number;
}

export interface DashboardCrop {
  id: string;
  name: string;
  status: CropStatus;
  areaHa?: number | null;
  propertyName: string;
}

export interface DashboardKpis {
  receitaMes: number;
  despesaMes: number;
  resultadoMes: number;
  culturasAtivas: number;
  areaCultivadaHa: number;
}

export interface DashboardResponse {
  properties: DashboardProperty[];
  activeCrops: DashboardCrop[];
  kpis: DashboardKpis;
}

export const CROP_STATUSES: { value: CropStatus; label: string }[] = [
  { value: 'PLANEJADA', label: 'Planejada' },
  { value: 'PLANTADA', label: 'Plantada' },
  { value: 'EM_DESENVOLVIMENTO', label: 'Em desenvolvimento' },
  { value: 'COLHIDA', label: 'Colhida' },
  { value: 'ENCERRADA', label: 'Encerrada' },
];

export const ITEM_CATEGORIES: { value: ItemCategory; label: string }[] = [
  { value: 'ACAO_MECANICA', label: 'Ação mecânica' },
  { value: 'ACAO_MANUAL', label: 'Ação manual' },
  { value: 'INSUMO', label: 'Insumo' },
  { value: 'OUTRO', label: 'Outro' },
];

export const PLANNED_GROUPS: {
  key: PlannedGroupKey;
  category: ItemCategory;
  title: string;
}[] = [
  { key: 'acoesMecanicas', category: 'ACAO_MECANICA', title: 'Ações mecânicas' },
  { key: 'acoesManuais', category: 'ACAO_MANUAL', title: 'Ações manuais' },
  { key: 'insumos', category: 'INSUMO', title: 'Insumos' },
  { key: 'outros', category: 'OUTRO', title: 'Outros' },
];

export const FINANCE_TYPES: { value: FinanceType; label: string }[] = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
];

export const BRAZILIAN_STATES: { uf: string; name: string }[] = [
  { uf: 'AC', name: 'Acre' },
  { uf: 'AL', name: 'Alagoas' },
  { uf: 'AP', name: 'Amapá' },
  { uf: 'AM', name: 'Amazonas' },
  { uf: 'BA', name: 'Bahia' },
  { uf: 'CE', name: 'Ceará' },
  { uf: 'DF', name: 'Distrito Federal' },
  { uf: 'ES', name: 'Espírito Santo' },
  { uf: 'GO', name: 'Goiás' },
  { uf: 'MA', name: 'Maranhão' },
  { uf: 'MT', name: 'Mato Grosso' },
  { uf: 'MS', name: 'Mato Grosso do Sul' },
  { uf: 'MG', name: 'Minas Gerais' },
  { uf: 'PA', name: 'Pará' },
  { uf: 'PB', name: 'Paraíba' },
  { uf: 'PR', name: 'Paraná' },
  { uf: 'PE', name: 'Pernambuco' },
  { uf: 'PI', name: 'Piauí' },
  { uf: 'RJ', name: 'Rio de Janeiro' },
  { uf: 'RN', name: 'Rio Grande do Norte' },
  { uf: 'RS', name: 'Rio Grande do Sul' },
  { uf: 'RO', name: 'Rondônia' },
  { uf: 'RR', name: 'Roraima' },
  { uf: 'SC', name: 'Santa Catarina' },
  { uf: 'SP', name: 'São Paulo' },
  { uf: 'SE', name: 'Sergipe' },
  { uf: 'TO', name: 'Tocantins' },
];
