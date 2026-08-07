#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -z "$JAVA_HOME" ]; then
  JAVACMD="java"
else
  JAVACMD="$JAVA_HOME/bin/java"
fi
exec "$JAVACMD" -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
