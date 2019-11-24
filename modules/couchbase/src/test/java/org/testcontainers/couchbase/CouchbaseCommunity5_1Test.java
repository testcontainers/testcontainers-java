package org.testcontainers.couchbase;

import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import org.junit.BeforeClass;

import java.util.Collections;

public class CouchbaseCommunity5_1Test extends BaseCouchbaseContainerTest {


    @BeforeClass
    public static void beforeClass() {
        final UserSettings communityAdminUserSettings =
            UserSettings.build()
                .password(getDefaultBucketSettings().password())
                .roles(Collections.singletonList(new UserRole("admin")));

        initializeContainerAndBucket(() ->
            new CouchbaseContainer("couchbase/server:community-5.1.1")
                .withNewBucket(getDefaultBucketSettings(), communityAdminUserSettings)
        );
    }
}
