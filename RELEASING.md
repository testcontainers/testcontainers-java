# Release process

Testcontainers' release process is automated as a Travis deployment. This describes the basic steps for a project member to perform a release.

## Steps

1. Ensure that the master branch is building and that tests are passing.
1. Ensure that the [`CHANGELOG`](CHANGELOG.md) file is up to date and includes all merged features.
1. Create a new release on GitHub. **The tag name is used as the version**, so please keep the tag name plain (e.g. 1.2.3).
1. Check that the Travis build passed.
1. Release of published artifacts to Bintray is fully automated.
1. After successful publication to Bintray, the artifacts must be synced to Maven Central.
1. When available through Maven Central, poke [Richard North](https://github.com/rnorth) to announce the release on Twitter!

## Internal details

* The process is done with Gradle and Bintray.
* Bintray will automatically promote the release to Maven Central.
* Travis secrets hold Bintray username/passwords that are used for publishing.
