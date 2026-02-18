# Step 1: Use an official Java runtime as base image
FROM eclipse-temurin:17-jdk-alpine

# Step 2: Set working directory
WORKDIR /app

# Step 3: Copy the JAR file into the container
COPY target/WeatherApp-0.0.1-SNAPSHOT.jar app.jar

# Step 4: Expose the port your app runs on
EXPOSE 8080

# Step 5: Set environment variables (optional defaults)
ENV WEATHER_API_KEY=
ENV FRONTEND_URL=http://localhost:3000
ENV PORT=8080

# Step 6: Run the JAR
ENTRYPOINT ["java","-jar","app.jar"]
