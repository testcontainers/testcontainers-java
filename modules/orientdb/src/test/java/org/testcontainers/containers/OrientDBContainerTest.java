package org.testcontainers.containers;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author robfrank
 */
public class OrientDBContainerTest {


    @Test
    public void shouldReturnTheSameSession() {
        OrientDBContainer container = new OrientDBContainer();
        container.start();

        final ODatabaseSession session = container.getSession();
        final ODatabaseSession session2 = container.getSession();

        assertThat(session).isSameAs(session2);
    }

    @Test
    public void shouldInitializeWithCommands() {

        OrientDBContainer container = new OrientDBContainer();
        container.start();

        final ODatabaseSession session = container.getSession();

        session.command("CREATE CLASS Person EXTENDS V");
        session.command("INSERT INTO Person set name='john'");
        session.command("INSERT INTO Person set name='jane'");

        assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);

        container.stop();

    }

    @Test
    public void shouldInitializeDatabaseFromScript() {

        OrientDBContainer container = new OrientDBContainer()
            .withScriptPath("initscript.osql")
            .withDatabaseName("persons");

        container.start();

        assertThat(container.getDbUrl())
            .isEqualTo("remote:" + container.getContainerIpAddress() + ":" + container.getMappedPort(2424) + "/persons");

        final ODatabaseSession session = container.getSession();

        assertThat(session.query("SELECT FROM Person").stream()).hasSize(4);

        container.stop();

    }
}
