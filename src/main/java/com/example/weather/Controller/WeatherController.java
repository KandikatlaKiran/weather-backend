package com.example.weather.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Value("${weather.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    
    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        try {
            if ((city == null || city.isEmpty()) && (lat == null || lon == null)) {
                return ResponseEntity.badRequest().body("City or coordinates required");
            }

            // --- Build API URLs ---
            String weatherUrl;
            String forecastUrl;

            if (city != null && !city.isEmpty()) {
                weatherUrl = String.format(
                        "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s",
                        city, apiKey);
                forecastUrl = String.format(
                        "https://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric&appid=%s",
                        city, apiKey);
            } else {
                weatherUrl = String.format(
                        "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric&appid=%s",
                        lat, lon, apiKey);
                forecastUrl = String.format(
                        "https://api.openweathermap.org/data/2.5/forecast?lat=%s&lon=%s&units=metric&appid=%s",
                        lat, lon, apiKey);
            }

            // --- Fetch data ---
            Map<String, Object> weatherData = restTemplate.getForObject(weatherUrl, Map.class);
            Map<String, Object> forecastResponse = restTemplate.getForObject(forecastUrl, Map.class);

            if (forecastResponse == null || !forecastResponse.containsKey("list")) {
                return ResponseEntity.status(500).body("Invalid forecast data");
            }

            List<Map<String, Object>> forecastList = (List<Map<String, Object>>) forecastResponse.get("list");

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.ENGLISH); // e.g., Mon, Tue

            // --- Simplify forecast (every 8th entry ≈ every 24 hours) ---
            List<Map<String, Object>> simplifiedForecast = new ArrayList<>();
            for (int i = 0; i < forecastList.size(); i += 8) {
                Map<String, Object> entry = forecastList.get(i);
                Map<String, Object> main = (Map<String, Object>) entry.get("main");
                List<Map<String, Object>> weatherArr = (List<Map<String, Object>>) entry.get("weather");
                Map<String, Object> weather = weatherArr.get(0);

                Map<String, Object> simplified = new HashMap<>();
                try {
                    Date date = inputFormat.parse(entry.get("dt_txt").toString());
                    simplified.put("day", outputFormat.format(date));
                } catch (ParseException e) {
                    simplified.put("day", "N/A");
                }

                simplified.put("temp", Math.round(Double.parseDouble(main.get("temp").toString())));
                simplified.put("condition", weather.get("main"));
                simplifiedForecast.add(simplified);
            }

            // --- Combine all data ---
            Map<String, Object> response = new HashMap<>();
            response.put("current", weatherData);
            response.put("forecast", simplifiedForecast);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching weather data");
        }
    }
}
