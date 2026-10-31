import { WeatherHourlyStrip as WeatherHourlyStripView } from '@/modules/weather/components/WeatherHourlyStripView'
import { useSettingsStore } from '@/modules/settings/store/settingsStore'
import { useWeatherStore } from '@/modules/weather/store/weatherStore'

/** Store-bound container for the hourly forecast strip. */
export function WeatherHourlyStrip() {
  const hourly = useWeatherStore((s) => s.weather?.hourly)
  const temperatureUnit = useSettingsStore((s) => s.temperatureUnit)

  if (!hourly || hourly.length === 0) return null

  return <WeatherHourlyStripView hours={hourly} temperatureUnit={temperatureUnit} />
}
