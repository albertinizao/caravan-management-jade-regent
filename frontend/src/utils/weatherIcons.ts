import precipitationFogIcon from "@/assets/weather/precipitation-fog.png";
import precipitationRainHeavyIcon from "@/assets/weather/precipitation-rain-heavy.png";
import precipitationRainLightIcon from "@/assets/weather/precipitation-rain-light.png";
import precipitationRainMediumIcon from "@/assets/weather/precipitation-rain-medium.png";
import precipitationSnowHeavyIcon from "@/assets/weather/precipitation-snow-heavy.png";
import precipitationSnowLightIcon from "@/assets/weather/precipitation-snow-light.png";
import precipitationSnowMediumIcon from "@/assets/weather/precipitation-snow-medium.png";
import precipitationThunderstormIcon from "@/assets/weather/precipitation-thunderstorm.png";
import sunIcon from "@/assets/weather/sun.png";
import tempColdWeatherIcon from "@/assets/weather/temp-cold-weather.png";
import tempExtremeColdIcon from "@/assets/weather/temp-extreme-cold.png";
import tempExtremeHeatIcon from "@/assets/weather/temp-extreme-heat.png";
import tempHeatIcon from "@/assets/weather/temp-heat.png";
import tempSevereColdIcon from "@/assets/weather/temp-severe-cold.png";
import tempSevereHeatIcon from "@/assets/weather/temp-severe-heat.png";
import windLightIcon from "@/assets/weather/wind-light.png";
import windModerateIcon from "@/assets/weather/wind-moderate.png";
import windSevereIcon from "@/assets/weather/wind-severe.png";
import windStrongIcon from "@/assets/weather/wind-strong.png";
import windWindstormIcon from "@/assets/weather/wind-windstorm.png";

export interface WeatherIconAsset {
  src: string;
  alt: string;
}

const precipitationIcons: Record<string, WeatherIconAsset> = {
  LIGHT_FOG: { src: precipitationFogIcon, alt: "Niebla ligera" },
  MEDIUM_FOG: { src: precipitationFogIcon, alt: "Niebla moderada" },
  HEAVY_FOG: { src: precipitationFogIcon, alt: "Niebla intensa" },
  LIGHT_SNOW: { src: precipitationSnowLightIcon, alt: "Nieve ligera" },
  MEDIUM_SNOW: { src: precipitationSnowMediumIcon, alt: "Nieve moderada" },
  HEAVY_SNOW: { src: precipitationSnowHeavyIcon, alt: "Nieve intensa" },
  LIGHT_RAIN: { src: precipitationRainLightIcon, alt: "Lluvia ligera" },
  RAIN: { src: precipitationRainMediumIcon, alt: "Lluvia moderada" },
  HEAVY_RAIN: { src: precipitationRainHeavyIcon, alt: "Lluvia intensa" },
  THUNDERSTORM: { src: precipitationThunderstormIcon, alt: "Tormenta eléctrica" },
  NONE: { src: sunIcon, alt: "Sol" },
};

const windIcons: Record<string, WeatherIconAsset> = {
  LIGHT: { src: windLightIcon, alt: "Viento ligero" },
  MODERATE: { src: windModerateIcon, alt: "Viento moderado" },
  STRONG: { src: windStrongIcon, alt: "Viento fuerte" },
  SEVERE: { src: windSevereIcon, alt: "Viento severo" },
  WINDSTORM: { src: windWindstormIcon, alt: "Vendaval" },
};

function buildTemperatureRisk(
  src: string,
  alt: string,
  label: string,
  temperatureF: number,
): WeatherIconAsset & { label: string } {
  return {
    src,
    alt,
    label: `${label} · ${temperatureF} °F`,
  };
}

export function getWeatherPrecipitationIcon(precipitation: string | null | undefined): WeatherIconAsset | null {
  if (!precipitation) {
    return null;
  }

  return precipitationIcons[precipitation] ?? null;
}

export function getWeatherWindIcon(windStrength: string | null | undefined): WeatherIconAsset | null {
  if (!windStrength) {
    return null;
  }

  return windIcons[windStrength] ?? null;
}

export function getWeatherTemperatureRiskIcon(
  temperatureF: number | null | undefined,
): (WeatherIconAsset & { label: string }) | null {
  if (temperatureF === null || temperatureF === undefined) {
    return null;
  }

  if (temperatureF <= -20) {
    return buildTemperatureRisk(tempExtremeColdIcon, "Riesgo extremo de frío", "Riesgo extremo de frío", temperatureF);
  }

  if (temperatureF <= 0) {
    return buildTemperatureRisk(tempSevereColdIcon, "Riesgo severo de frío", "Riesgo severo de frío", temperatureF);
  }

  if (temperatureF < 40) {
    return buildTemperatureRisk(tempColdWeatherIcon, "Riesgo de frío", "Riesgo de frío", temperatureF);
  }

  if (temperatureF > 140) {
    return buildTemperatureRisk(tempExtremeHeatIcon, "Riesgo extremo de calor", "Riesgo extremo de calor", temperatureF);
  }

  if (temperatureF > 110) {
    return buildTemperatureRisk(tempSevereHeatIcon, "Riesgo severo de calor", "Riesgo severo de calor", temperatureF);
  }

  if (temperatureF > 90) {
    return buildTemperatureRisk(tempHeatIcon, "Riesgo de calor", "Riesgo de calor", temperatureF);
  }

  return null;
}

export function getWeatherTemperatureTooltip(temperatureC: number | null | undefined, temperatureF: number | null | undefined): string {
  if (temperatureC === null || temperatureC === undefined || temperatureF === null || temperatureF === undefined) {
    return "—";
  }

  const riskIcon = getWeatherTemperatureRiskIcon(temperatureF);
  return riskIcon ? `${temperatureC} °C · ${riskIcon.label}` : `${temperatureC} °C · ${temperatureF} °F`;
}
