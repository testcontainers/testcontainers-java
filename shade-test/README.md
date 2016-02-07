# shade-test

This module contains what are effectively 'integration tests' for the shaded testcontainers core JAR.

These test modules should do things that will break without shading being used - i.e. depend upon incompatible versions
 of core's dependencies.