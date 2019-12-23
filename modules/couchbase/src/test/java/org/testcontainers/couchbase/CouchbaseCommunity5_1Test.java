package org.testcontainers.couchbase;

import java.util.Collections;

import org.junit.ClassRule;

import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import org.junit.Rule;

public class CouchbaseCommunity5_1Test extends BaseCouchbaseContainerTest {

    @Rule
    public CouchbaseContainer container = initCouchbaseContainer();

    private static CouchbaseContainer initCouchbaseContainer() {
        CouchbaseContainer couchbaseContainer = new CouchbaseContainer("couchbase/server:community-5.1.1");

        final UserSettings communityAdminUserSettings =
            UserSettings.build()
                .password(getDefaultBucketSettings().password())
                .roles(Collections.singletonList(new UserRole("admin")));

        couchbaseContainer.withNewBucket(getDefaultBucketSettings(), communityAdminUserSettings);
        return couchbaseContainer;
    }

    @Override
    public CouchbaseContainer getCouchbaseContainer() {
        return container;
    }
}
