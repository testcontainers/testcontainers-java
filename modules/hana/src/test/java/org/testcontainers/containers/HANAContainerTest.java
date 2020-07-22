package org.testcontainers.containers;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HANAContainerTest {


    @Test	// Test if all required ports are available.
    public void containerStartsAndHANAPortsAreAvailable() {
    	HANAContainer container = new HANAContainer("store/saplabs/hanaexpress:2.00.040.00.20190729.1");
        container.start();
        assertThatHANAPortsAreAvailable(container);
    }

	@Test	// Test if the sap hana jdbc Driver is loaded sucessfully
    public void testDriverClassName(){
        // start container
    	HANAContainer container = new HANAContainer();
        container.start();
        
        String driverClassName = container.getDriverClassName();
        
        assert(driverClassName.equals("com.sap.db.jdbc.Driver"));
    }

	@Test
	public void testConstructor() {
		HANAContainer container = new HANAContainer();
		container.start();
		assertThatHANAPortsAreAvailable(container);
	}

	@Test
	public void testConstructorWithDifferentImageTag() {
		HANAContainer container = new HANAContainer("store/saplabs/hanaexpress:2.00.036.00.20190223.1");
		container.start();
		assertThatHANAPortsAreAvailable(container);
	}

	@Test
	public void testAcceptLicenseProgrammatically() {
		HANAContainer container = new HANAContainer("store/saplabs/hanaexpress:2.00.045.00.20200121.1").acceptLicense();	// this image is not inside the license acceptance file.
		container.start();
		assertThatHANAPortsAreAvailable(container);
	}
	
	@Test
	public void testBasicQueryOnSystemDB() {
		try {
			HANAContainer container = new HANAContainer();
			container.start();
			
			Connection conn = container.createConnection("?databaseName=SYSTEMDB");
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(container.getTestQueryString());
			
			rs.next();
            int resultSetInt = rs.getInt(1);

            assertEquals("A basic SELECT query on systemDB succeeds", 1, resultSetInt);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testBasicQueryOnTenantDB() {
		try {
			HANAContainer container = new HANAContainer();
			container.start();
			
			Connection conn = container.createConnection("?databaseName=HXE");
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(container.getTestQueryString());

			rs.next();
            int resultSetInt = rs.getInt(1);

            assertEquals("A basic SELECT query on tenantDB succeeds", 1, resultSetInt);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testQueryWithInitScript() {
		try {
			HANAContainer container = (HANAContainer) new HANAContainer().withInitScript("somepath/init_hana.sql");
			container.start();
			
			Connection connection = container.createConnection("?databaseName=HXE");
			
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM bar");
			
			rs.next();
			String resultSetString = rs.getString(1);
			
			assertEquals("A SELECT query on table from initScript succeeds", "hello world", resultSetString);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCustomPassword() {
		HANAContainer container = new HANAContainer().withPassword("RandomPassword1");		
		container.start();
		
		String pw = container.getPassword();
		
		assertEquals("Initializing the container with a custom password worked", "RandomPassword1", pw);
	}

	@Test
	public void testInvalidPassword() {
		
		String pw;
		
		try {
			HANAContainer container = new HANAContainer().withPassword("nocapitalshere");		
			container.start();
		
			pw = container.getPassword();
		} catch(IllegalArgumentException e) {
			pw = e.toString();
		}
		
		assertEquals("Checking password rejected:",
				"java.lang.IllegalArgumentException: Password must contain characters from the following three categories:\n - Latin uppercase letters (A through Z)\n - Latin lowercase letters (a through z)\n - Base 10 digits (0 through 9).\n", pw);
	}
	
	
	/**
	 * Helper method to check if the required HANA ports are available
	 * 
	 * @param container: HANAContainer
	 */
    private void assertThatHANAPortsAreAvailable(HANAContainer container) {
    	
    	Integer[] ports = new Integer[] {39013, 39017, 39041, 39042, 39043, 39044, 39045, 1128, 1129, 59013, 59014};
    	
    	List<Integer> unavailablePorts = new ArrayList<Integer>();
		
    	for (int i = 0; i < ports.length; i++) {
            try {
                Socket tmp = new Socket(container.getContainerIpAddress(), container.getMappedPort(ports[i]));
                tmp.close();
            } catch (IOException e) {
               
                unavailablePorts.add(ports[i]);
            }
    	}

    	if(unavailablePorts.isEmpty() == false) {
    		 throw new AssertionError("The required ports " + unavailablePorts.toString() + " are not available!");
    	}
	}

}
