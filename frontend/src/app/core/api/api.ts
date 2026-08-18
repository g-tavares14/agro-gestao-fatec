export const API = {
  base: '/api',
} as const;

export function apiUrl(path: string): string {
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${API.base}${suffix}`;
}
