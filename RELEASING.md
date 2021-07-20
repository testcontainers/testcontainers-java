# Release process

Testcontainers' release process is semi-automated through GitHub Actions. This describes the basic steps for a project member to perform a release.

## Steps

1. Ensure that the master branch is building and that tests are passing.
1. Create a new release on GitHub. **The tag name is used as the version**, so please keep the tag name plain (e.g. 1.2.3).
1. The release triggers a GitHub Action workflow, but it gets mostly build using results from the Gradle remote-cache. Therefore, this should be fairly fast.
1. Login to Sonatype to check the staging repository.
1. Get the staging url after GitHub Action workflow finished.
1. Manually test the release with the staging url as maven repository url (e.g. critical issues and features).
1. Run [TinSalver](https://github.com/bsideup/tinsalver) from GitHub using `npx` to sign artifact (see [TinsSalver README](https://github.com/bsideup/tinsalver/blob/main/README.md)).
1. Close the release in Sonatype. This will evaluate the release based on given Sontaype rules and afterwards automatically sync to Maven Central.
1. When available through Maven Central, poke [Richard North](https://github.com/rnorth) to announce the release on Twitter!

## Internal details

* The process is done with GitHub Actions, TinSalver and Sonatype.
* Sonatype will automatically promote the staging release to Maven Central.
* GPG key of signing developer needs to be uplodaed to the Ubuntu keyserver (or other server supported by Sonatype).
