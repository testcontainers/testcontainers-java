package org.testcontainers.containers;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrientDBContainerWithClassRuleTest {

    @ClassRule
    public static OrientDBContainer container = new OrientDBContainer();


    @Test
    public void shouldInitializeWithCommands() {

        final ODatabaseSession session = container.getSession();

        session.command("CREATE CLASS Person EXTENDS V");
        session.command("INSERT INTO Person set name='john'");
        session.command("INSERT INTO Person set name='jane'");

        assertThat(session.query("SELECT FROM Person").stream()).hasSize(2);

    }
}
