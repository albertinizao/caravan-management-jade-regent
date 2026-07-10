import { fetchJson } from "@/services/http";
import type { CalendarDay, CalendarMonth } from "@/types/calendar";

export function getCalendarMonth(caravanId: string, year: number, month: number) {
  return fetchJson<CalendarMonth>(`/caravans/${caravanId}/calendar?year=${year}&month=${month}`);
}

export function getCalendarDay(caravanId: string, year: number, month: number, day: number) {
  return fetchJson<CalendarDay>(`/caravans/${caravanId}/calendar/day?year=${year}&month=${month}&day=${day}`);
}

export function setCalendarCurrentDate(caravanId: string, year: number, month: number, day: number) {
  return fetchJson<CalendarDay>(`/caravans/${caravanId}/calendar/current-date`, {
    method: "PUT",
    body: JSON.stringify({ year, month, day }),
  });
}

export function advanceCalendarDays(caravanId: string, days: number) {
  return fetchJson<CalendarDay>(`/caravans/${caravanId}/calendar/advance`, {
    method: "POST",
    body: JSON.stringify({ days }),
  });
}
