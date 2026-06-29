const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "/api";

export { apiBaseUrl };

export async function fetchJson<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${input}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  return (await response.json()) as T;
}
