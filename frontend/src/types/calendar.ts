export type CalendarEventCategory = "CANONICAL" | "BIRTHDAY" | "ASTRONOMICAL" | "CUSTOM";

export interface GolarionDate {
  year: number;
  month: number;
  monthName: string;
  day: number;
  dayOfWeek: string;
  dayOfWeekAbbreviation: string;
}

export interface CalendarEvent {
  id: number | null;
  name: string;
  scope: string | null;
  description: string | null;
  category: CalendarEventCategory | string;
  secret: boolean;
}

export interface WeatherPeriod {
  precipitation: string | null;
  windStrength: string | null;
  temperatureC: number | null;
  temperatureF: number | null;
}

export interface WeatherSnapshot {
  midnightToDawn: WeatherPeriod | null;
  dawnToNoon: WeatherPeriod | null;
  noonToDusk: WeatherPeriod | null;
  duskToMidnight: WeatherPeriod | null;
}

export interface CalendarDay {
  date: GolarionDate;
  isCurrentDay: boolean;
  isInCurrentMonth: boolean;
  canonicalEvents: CalendarEvent[];
  customEvents: CalendarEvent[];
  weather: WeatherSnapshot | null;
}

export interface CalendarMonth {
  caravanId: string;
  currentDate: GolarionDate;
  displayYear: number;
  displayMonth: number;
  displayMonthName: string;
  weekDayHeaders: string[];
  days: CalendarDay[];
}
