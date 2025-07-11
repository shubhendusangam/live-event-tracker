#!/bin/bash
# start.sh - Start the application with infrastructure

echo "Starting Event Tracker Service..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker first."
    exit 1
fi

# Start infrastructure
echo "Starting Kafka infrastructure..."
docker-compose up -d zookeeper kafka kafka-ui

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 30

# Check if Kafka is healthy
if docker-compose ps kafka | grep -q "Up (healthy)"; then
    echo "Kafka is ready!"
else
    echo "Kafka is not ready. Please check the logs:"
    echo "docker-compose logs kafka"
    exit 1
fi

# Build and run the application
echo "Building application..."
mvn clean package -DskipTests

echo "Starting application..."
java -jar target/event-tracker-1.0.0.jar

---

#!/bin/bash
# test-endpoints.sh - Test the REST endpoints

BASE_URL="http://localhost:8080"

echo "Testing Event Tracker API endpoints..."

# Test health check
echo "1. Testing health check..."
curl -s "$BASE_URL/actuator/health" | jq .

# Test setting event to live
echo "2. Setting event-1 to live..."
curl -X POST "$BASE_URL/events/status" \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-1", "live": true}' | jq .

# Test getting event status
echo "3. Getting event-1 status..."
curl -s "$BASE_URL/events/event-1/status" | jq .

# Test getting active count
echo "4. Getting active event count..."
curl -s "$BASE_URL/events/active-count"

# Test setting multiple events to live
echo "5. Setting multiple events to live..."
curl -X POST "$BASE_URL/events/status" \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-2", "live": true}' | jq .

curl -X POST "$BASE_URL/events/status" \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-3", "live": true}' | jq .

# Check active count again
echo "6. Getting updated active event count..."
curl -s "$BASE_URL/events/active-count"

# Test mock API
echo "7. Testing mock API..."
curl -s "$BASE_URL/mock-api/events/event-1/score" | jq .

# Set event to not live
echo "8. Setting event-1 to not live..."
curl -X POST "$BASE_URL/events/status" \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-1", "live": false}' | jq .

# Final active count
echo "9. Final active event count..."
curl -s "$BASE_URL/events/active-count"

echo "API testing completed!"

---

#!/bin/bash
# docker-start.sh - Start everything with Docker

echo "Starting Event Tracker with Docker..."

# Build and start all services
docker-compose up --build

---

#!/bin/bash
# clean.sh - Clean up resources

echo "Cleaning up Event Tracker resources..."

# Stop and remove containers
docker-compose down

# Remove dangling images
docker image prune -f

# Remove volumes (optional)
read -p "Do you want to remove Docker volumes? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker-compose down -v
fi

echo "Cleanup completed!"

---

#!/bin/bash
# kafka-consumer.sh - Start a Kafka consumer to see messages

echo "Starting Kafka consumer for score-updates topic..."

docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic score-updates \
  --from-beginning \
  --property print.key=true \
  --property key.separator=": "

---

#!/bin/bash
# run-tests.sh - Run all tests

echo "Running Event Tracker tests..."

# Run unit tests
echo "Running unit tests..."
mvn test -Dtest=*Test

# Run integration tests
echo "Running integration tests..."
mvn test -Dtest=*IntegrationTest

# Generate test report
echo "Test results available in target/surefire-reports/"

echo "All tests completed!"

---

# Make scripts executable
chmod +x start.sh test-endpoints.sh docker-start.sh clean.sh kafka-consumer.sh run-tests.sh