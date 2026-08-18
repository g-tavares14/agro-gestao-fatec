import {
  Crop,
  CropStatus,
  ItemCategory,
  PdfExtracted,
  PlannedGroupKey,
  PlannedItem,
  PLANNED_GROUPS,
} from '../../../shared/models';
import { toNumber } from '../../../shared/http-utils';

const CATEGORY_BY_GROUP: Record<PlannedGroupKey, ItemCategory> = {
  acoesMecanicas: 'ACAO_MECANICA',
  acoesManuais: 'ACAO_MANUAL',
  insumos: 'INSUMO',
  outros: 'OUTRO',
};

export function normalizeCrop(raw: Crop & { property?: { id?: string; name?: string } }): Crop {
  return {
    ...raw,
    propertyId: raw.propertyId ?? raw.property?.id ?? null,
    propertyName: raw.propertyName ?? raw.property?.name ?? null,
    status: raw.status ?? 'PLANEJADA',
  };
}

export function plannedItemsOf(crop: Crop, key: PlannedGroupKey): PlannedItem[] {
  return (crop[key] ?? []).map((item) => normalizePlannedItem(item, CATEGORY_BY_GROUP[key]));
}

export function normalizePlannedItem(
  raw: unknown,
  fallbackCategory?: ItemCategory,
): PlannedItem {
  const item = (raw ?? {}) as Record<string, unknown>;
  const category = (item['category'] as ItemCategory | undefined) ?? fallbackCategory;
  return {
    id: typeof item['id'] === 'string' ? item['id'] : undefined,
    description: String(item['description'] ?? ''),
    unit: (item['unit'] as string | null | undefined) ?? null,
    quantity: toNumber(item['quantity']),
    unitValue: toNumber(item['unitValue']),
    totalValue: toNumber(item['totalValue']),
    category,
  };
}

export function mapExtractedCrop(extracted: PdfExtracted | null | undefined): {
  name: string;
  variety: string;
  irrigationSystem: string;
  areaHa: number | null;
  plantingDate: string;
  expectedHarvestDate: string;
  status: CropStatus;
  expectedYield: string;
  notes: string;
  groups: Record<PlannedGroupKey, PlannedItem[]>;
} {
  const source = extracted ?? {};
  const groups = {} as Record<PlannedGroupKey, PlannedItem[]>;
  for (const group of PLANNED_GROUPS) {
    groups[group.key] = (source[group.key] ?? []).map((item) =>
      normalizePlannedItem(item, group.category),
    );
  }

  return {
    name: String(source.name ?? ''),
    variety: String(source.variety ?? ''),
    irrigationSystem: String(source.irrigationSystem ?? ''),
    areaHa: toNumber(source.areaHa),
    plantingDate: String(source.plantingDate ?? ''),
    expectedHarvestDate: '',
    status: 'PLANEJADA',
    expectedYield: String(source.expectedYield ?? ''),
    notes: String(source.notes ?? ''),
    groups,
  };
}
