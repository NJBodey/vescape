import { WeatherPill as WeatherPillView } from '@/modules/weather/components/WeatherPillView'
import { wholeTemperatureFromC } from '@/helpers/units'
import { useSettingsStore } from '@/modules/settings/store/settingsStore'
import { useWeatherStore } from '@/modules/weather/store/weatherStore'

interface WeatherPillProps {
  expanded?: boolean
  onPress: () => void
}

/** Store-bound container for the map weather pill. */
export function WeatherPill({ expanded, onPress }: WeatherPillProps) {
  const weather = useWeatherStore((s) => s.weather)
  const temperatureUnit = useSettingsStore((s) => s.temperatureUnit)

  if (!weather) return null

  return (
    <WeatherPillView
      icon={weather.icon}
      temperature={wholeTemperatureFromC(weather.temperatureC, temperatureUnit)}
      label={weather.label}
      precipProbability={weather.precipitationProbability}
      sunriseMinuteOfDay={weather.sunriseMinuteOfDay}
      sunsetMinuteOfDay={weather.sunsetMinuteOfDay}
      expanded={expanded}
      onPress={onPress}
    />
  )
}
