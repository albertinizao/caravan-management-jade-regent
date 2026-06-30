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
    const body = await response.text();

    if (body) {
      let message = body;

      try {
        const parsed = JSON.parse(body) as { error?: string; message?: string };
        message = parsed.error ?? parsed.message ?? body;
      } catch {
        message = body;
      }

      throw new Error(message);
    }

    throw new Error(`Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const body = await response.text();

  if (!body) {
    return undefined as T;
  }

  return JSON.parse(body) as T;
}
