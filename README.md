# Event Tracker Service

A Spring Boot microservice that tracks live events and publishes score updates to Kafka. The service provides REST endpoints for managing event statuses and automatically fetches score data from external APIs for live events.

## Features

- REST API for updating event statuses (live/not live)
- Automatic scheduled tasks for live events (every 10 seconds)
- External API integration for score fetching
- Kafka message publishing with retry logic
- Comprehensive error handling and logging
- Health checks and monitoring endpoints
- Mock external API for testing

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST Client   │────│  Event Tracker  │────│  External API   │
└─────────────────┘    │    Service      │    └─────────────────┘
                       └─────────────────┘
                               │
                               ▼
                       ┌─────────────────┐
                       │      Kafka      │
                       └─────────────────┘
```

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose (for infrastructure)

## Quick Start

### 1. Start Infrastructure

```bash
# Start Kafka and Zookeeper
docker-compose up -d zookeeper kafka kafka-ui

# Wait for services to be healthy
docker-compose ps
```

### 2. Build and Run Application

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/event-tracker-1.0.0.jar
```

### 3. Alternative: Run Everything with Docker

```bash
# Build and start everything
docker-compose up --build
```

## API Endpoints

### Update Event Status

```bash
# Set event to live
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-1", "live": true}'

# Set event to not live
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "event-1", "live": false}'
```

### Get Event Status

```bash
curl http://localhost:8080/events/event-1/status
```

### Get Active Event Count

```bash
curl http://localhost:8080/events/active-count
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Configuration

Key configuration properties in `application.yml`:

```yaml
app:
  external-api:
    url: http://localhost:8080/mock-api  # External API URL
  kafka:
    topic: score-updates                 # Kafka topic for score messages
  mock-api:
    enabled: true                        # Enable mock API for testing
```

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn test -Dtest=*IntegrationTest
```

### Test with Mock Data

The service includes a mock external API that returns random scores:

```bash
# Get mock score data
curl http://localhost:8080/mock-api/events/event-1/score
```

## Monitoring

### Kafka UI

Access Kafka UI at http://localhost:8081 to monitor:
- Topics and messages
- Consumer groups
- Broker status

### Application Metrics

```bash
# Health status
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
```

## Message Format

Score messages published to Kafka have this format:

```json
{
  "eventId": "event-1",
  "currentScore": "2:1",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Error Handling

The service implements comprehensive error handling:

- **Retry Logic**: 3 retries for external API calls and Kafka publishing
- **Circuit Breaker**: Automatic task cancellation for failed events
- **Graceful Degradation**: Continues processing other events if one fails
- **Detailed Logging**: All errors and state changes are logged

## Design Decisions

### 1. In-Memory Storage
- **Decision**: Use ConcurrentHashMap for event status storage
- **Rationale**: Simple, fast, and sufficient for prototype requirements
- **Trade-offs**: Data lost on restart, not suitable for production clustering

### 2. Spring TaskScheduler
- **Decision**: Use ThreadPoolTaskScheduler for periodic tasks
- **Rationale**: Native Spring integration, easy lifecycle management
- **Alternative**: Could use Quartz for more complex scheduling needs

### 3. Kafka Integration
- **Decision**: Use Spring Kafka with async publishing
- **Rationale**: Non-blocking operations, built-in retry and error handling
- **Configuration**: Producer configured with `acks=all` for reliability

### 4. External API Integration
- **Decision**: Use RestTemplate with @Retryable annotation
- **Rationale**: Simple, declarative retry logic
- **Enhancement**: Could use WebClient for better async performance

### 5. Error Handling Strategy
- **Decision**: Fail-fast for individual events, continue for others
- **Rationale**: Prevents cascading failures
- **Implementation**: Try-catch blocks around critical operations

## AI Usage Documentation

This solution was developed with AI assistance in the following areas:

### AI-Generated Components:
1. **Docker configuration** - Dockerfile and docker-compose.yml
2. **Documentation** - README structure and API documentation

### AI-Assisted Improvements:
1. **Error handling patterns** - Retry logic and exception handling strategies
2. **Configuration management** - Spring profiles and property organization
3. **Testing strategies** - Mock configurations and test data setup
4. **Logging implementation** - Structured logging with appropriate levels

## Production Considerations

For production deployment, consider:

1. **Persistence**: Replace in-memory storage with database (Redis/PostgreSQL)
2. **Monitoring**: Add metrics collection (Micrometer/Prometheus)
3. **Security**: Implement authentication and authorization
4. **Scaling**: Configure for horizontal scaling with sticky sessions
5. **Reliability**: Add circuit breakers and bulkheads
6. **Configuration**: Externalize configuration with Spring Cloud Config

## Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   ```bash
   # Check Kafka status
   docker-compose ps kafka
   
   # View Kafka logs
   docker-compose logs kafka
   ```

2. **External API Timeouts**
   ```bash
   # Check application logs
   docker-compose logs event-tracker
   
   # Test mock API directly
   curl http://localhost:8080/mock-api/events/test/score
   ```

3. **Memory Issues**
   ```bash
   # Increase JVM memory
   export JAVA_OPTS="-Xmx1g -Xms512m"
   java $JAVA_OPTS -jar target/event-tracker-1.0.0.jar
   ```

## Development

### Project Structure

```
src/
├── main/java/com/example/eventtracker/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── dto/            # Data transfer objects
│   ├── model/          # Domain models
│   └── service/        # Business logic
├── main/resources/
│   └── application.yml # Application configuration
└── test/java/          # Test classes
docker/                 # Docker configurations
scripts/               # Build and deployment scripts
```

### Building from Source

```bash
# Clone repository
git clone <repository-url>
cd event-tracker

# Build
mvn clean package

# Run tests
mvn test

# Run application
mvn spring-boot:run
```
