# Vertica-CE Module

!!! note
This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

!!! note
We will need to get clarification on the license - the docker image provides an _Evaluation_ license which is a 'license to use Licensed Products for
internal evaluation and testing purposes only, and not for any development, production, distribution or
commercial purpose' and is only valid for a limited amount of time. It's common to see TestContainers used on both the developer's laptop and in CI/CD pipelines
so at first glance it seems like we can't use this license... but it seems likely that anyone who needs this module will already have an 'Express' license or better.
Given this I think it's likely that we can get a carve out but I can't guarantee it, or that their terms won't be too onerous for the project to accept.

[Vertica Analytics Database](https://www.vertica.com/) is a [data warehouse](https://en.wikipedia.org/wiki/Data_warehouse) implemented with a using a [columnar database](https://en.wikipedia.org/wiki/Column-oriented_database). See the [Vertica](https://en.wikipedia.org/wiki/Vertica) Wikipedia article for more details. Vertica also supports SQL access for legacy applications, to simplify initialization of the database, etc.

Data warehouses are usually cloud-native due to the amount of data they store but some vendors provide docker-based __Community Editions__ for use by developers and students who are primarily concerned in learning how to use the tool.

At this time this module is tested like all other relational databases. The contribution of data warehouse specific tests would be welcome.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:vertica-ce:{{latest_version}}"

// Vertica JDBC driver
testRuntimeOnly "com.vertica.jdbc:vertica-jdbc:24.1.0-0"
```
=== "Maven"
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>vertica-ce</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>

<!-- Vertica JDBC driver -->
<dependency>
    <groupId>com.vertica.jdbc</groupId>
    <artifactId>vertica-jdbc</artifactId>
    <version>24.1.0-0</version>
    }   <scope>test</scope>
</dependency>
```
}
!!! hint
Adding this Testcontainers library JAR will not automatically add the Vertica driver JAR to your project.
You should ensure that your project has the Vertica driver as a dependency.

## Licenses

I mentioned this in the note above - reading the licenses literally we can't use the docker image
since the module is likely to be used in CI/CD pipelines that feed into production. However I think we
can make a strong argument for a carve out since the only people using these CI/CD pipelines will already
need to have an 'Express' or better license - the purpose of this module is to facilitate software testing,
not development in isolation from all other resources. We won't know until we ask, and we probably shouldn't
ask until we have enough done to demonstrate the value of the module to their customers.

This may require a few iterations since we'll need a second (non-customer) carve out sufficiently broad for
any user to download and build the entire TestContainer repo.

From [Additional License Authorizations for Vertica software products](https://www.microfocus.com/media/documentation/additional-license-authorizations-for-vertica-software-products-documentation.pdf):

!!! block
Vertica Community Edition may be used for evaluation purposes only and the license for Vertica Community Edition is an
“Evaluation License” as such term is defined in the then current Micro Focus End User License Agreement (the “EULA”)
found at https://software.microfocus.com/en-us/legal/software-licensing. The Evaluation License for Vertica Community
Edition is governed by the terms of the EULA.

From [Micro Focus End User License Agreement](https://www.microfocus.com/media/documentation/micro_focus_end_user_license_agreement.pdf)

!!! block
. Evaluation Licenses. Except as specifically permitted in the applicable ALA, when Micro Focus and its
affiliates, respectively deliver and license the Licensed Products solely for evaluation, Customer
receives a non-transferable, non-sublicensable, non-exclusive license to use Licensed Products for
internal evaluation and testing purposes only, and not for any development, production, distribution or
commercial purpose (“Evaluation License”). The term of an Evaluation License will be 30 days
starting from the date Licensed Product is delivered (i.e., made available for download or physically
delivered) to Customer (“Evaluation Term”), unless Micro Focus authorizes a different period in
writing. The Licensed Product is provided “as is” and there are no warranties or obligations for Micro
Focus to provide support. The Evaluation License terminates at the end of the Evaluation Term, and
Customer is required to return, or, if Micro Focus so directs, delete and destroy, all copies of such
Licensed Product and provide Micro Focus with written confirmation of its compliance with this
provision within 30 days of the end of the Evaluation Term. The Evaluation License for any pre-release
or beta versions of Licensed Software (“Pre-Release Software”) shall be for a term of 90 days unless
Micro Focus authorizes a different period in writing. Customer agrees to promptly report to Micro Focus
all problems (including errors, failures, nonconforming results, and unexpected performances) and any
comments regarding the Pre-Release Software and to timely respond to all questionnaires submitted
by Micro Focus regarding the results of Customer’s testing of the Pre-Release Software. Micro Focus
may choose not to release a final version of the Pre-Release Software or, even if released, to alter
prices, features, specifications, capabilities, functions, release dates, general availability, or other
characteristics of the Pre-Release Software.
