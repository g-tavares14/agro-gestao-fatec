import {
  CROP_STATUSES,
  CropStatus,
  FINANCE_TYPES,
  FinanceType,
  ITEM_CATEGORIES,
  ItemCategory,
} from './models';

export function cropStatusLabel(status: CropStatus | string | null | undefined): string {
  return CROP_STATUSES.find((item) => item.value === status)?.label ?? status ?? '—';
}

export function itemCategoryLabel(category: ItemCategory | string | null | undefined): string {
  return ITEM_CATEGORIES.find((item) => item.value === category)?.label ?? category ?? '—';
}

export function financeTypeLabel(type: FinanceType | string | null | undefined): string {
  return FINANCE_TYPES.find((item) => item.value === type)?.label ?? type ?? '—';
}

export function locationLabel(city?: string | null, state?: string | null): string {
  const parts = [city?.trim(), state?.trim()].filter((part): part is string => !!part);
  return parts.length > 0 ? parts.join(' · ') : 'Local não informado';
}
