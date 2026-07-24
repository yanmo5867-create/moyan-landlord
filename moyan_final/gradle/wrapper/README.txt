Gradle Wrapper Jar
===================

The gradle-wrapper.jar file is automatically downloaded by GitHub Actions
during the build process. It does not need to be included in the repository.

The CI build uses `gradle/actions/setup-gradle@v3` which handles
downloading and caching Gradle distributions automatically.

For local builds, run: gradle wrapper --gradle-version 8.0
