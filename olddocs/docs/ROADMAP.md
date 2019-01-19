
Exact future features aren't set in stone, but here are the principles we should follow for this project:

* Testcontainers is intended to utilize containers to, primarily, make integration testing of systems easier, quicker and more reliable.
* It's designed primarily to help developers. Make it easy to do the right thing, with sensible defaults, a fairly limited surface area to discover, and helpful log/error messages.
* It's not intended to be the solution to every problem under the sun. Don't try and solve every problem with this one library; if it takes a lot of work to incorporate a feature then perhaps it's the job of another tool.
* In line with the above, we should aim for feature completeness soon.
* Similarly, it should leverage good things that already exist (like reliable public Docker images) and be composable with other tools.
* That said, don't lock us in to more third-party components than absolutely necessary, and only use well maintained dependencies.
* It must be possible for a developer to use _or build_ this library with just minimal standard tools (Java JDK, Maven), and a local Docker environment. Do not use any non-public dependencies in the core lib or standard modules.

