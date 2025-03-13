# Llandaff Campus App - Claude Assist Guide

## Build/Run Commands
- Build app: `./gradlew build`
- Run app: `./gradlew installDebug`
- Clean build: `./gradlew clean`
- Run unit tests: `./gradlew test`
- Run single test: `./gradlew test --tests "com.example.llandaffcampusapp1.ExampleUnitTest.addition_isCorrect"`
- Run instrumented tests: `./gradlew connectedAndroidTest`

## Code Style Guidelines
- **Java Version**: Java 11
- **Package Structure**: `com.example.llandaffcampusapp1`
- **Naming**: CamelCase for classes (e.g., `MapFragment`), lowerCamelCase for methods/variables
- **Imports**: Group imports by package, sort alphabetically
- **Error Handling**: Use try/catch blocks with explicit exception handling; log errors with e.printStackTrace()
- **Documentation**: Use JavaDoc comments for classes and public methods
- **Map Overlays**: Follow existing polygon/polyline rendering patterns for map features
- **UI Components**: Use fragment-based architecture with Navigation component
- **ViewBinding**: Used for view access (avoid findViewById)