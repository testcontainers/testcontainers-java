# Roadmap

## Principles for the project

Exact future features aren't set in stone, but here are the principles we should follow for this project:

* Testcontainers is intended to utilize containers to, primarily, make integration testing of systems easier, quicker and more reliable.
* It's designed primarily to help developers. Make it easy to do the right thing, with sensible defaults, a fairly limited surface area to discover, and helpful log/error messages.
* It's not intended to be the solution to every problem under the sun. Don't try and solve every problem with this one library; if it takes a lot of work to incorporate a feature then perhaps it's the job of another tool.
* In line with the above, we should aim for feature completeness soon.
* Similarly, it should leverage good things that already exist (like reliable public Docker images) and be composable with other tools.
* That said, don't lock us in to more third-party components than absolutely necessary, and only use well maintained dependencies.
* It must be possible for a developer to use _or build_ this library with just minimal standard tools (Java JDK, Maven), and a local Docker environment. Do not use any non-public dependencies in the core lib or standard modules. (see the `proprietary-deps` maven profile for how we're dealing with the Oracle XE module that has a test-time dependency on the properitary Oracle JDBC driver JAR)

## Future goals/anti-goals

This list may cross over with the [issues list](https://github.com/testcontainers/testcontainers-java/issues) some times.

* **Better documentation and log messages**. We need this.
* **Database state snapshotting**. Some projects have complex test data that is time consuming to reload. This means that either testing takes a very long time (test data has to be reloaded often), or tests aren't sufficiently independent (test data is not reloaded and therefore tests aren't independent). Testcontainers should provide a way to snapshot DB state and quickly reload that state. Docker [native Checkpoint/Restore In Userspace](http://blog.kubernetes.io/2015/07/how-did-quake-demo-from-dockercon-work.html) looks like the best way to accomplish this.
* **Support docker images pulled from private registries**
* **Support integration testing in other languages/environments**. .NET and NodeJS seem like sensible options here.
* **Allow a JUnit @Rule to point to a local `Dockerfile` rather than an imagename**. Perhaps.
* **Don't enter the build tools space**
* **Don't become involved in live run-time environments**. This is a tool for testing. Some features could technically be misused in a live environment, but this isn't something we should want to support.