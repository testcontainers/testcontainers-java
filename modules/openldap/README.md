# IdP - OpenLDAP Module

## Identity Providers (IdP)

Identity Providers provide externalized user authentication (AuthN) and
authorization (AuthZ), and are increasingly available as authentication
mechanisms used by the type of hosted services available as TestContainers.

### Providers

- Microsoft Active Directory
- IPA/[FreeIPA](https://freeipa.org/)
- Samba (limited)
- All cloud providers
- All OAuth providers such as Google, Facebook, GitHub, etc. (typically limited to AuthN)
- [Okta](https://okta.com/)
- [Authentik](https://goauthentik.io/)

Individual services are often available:

- Kerberos: [MIT KDC](https://web.mit.edu/kerberos/), [Heimdal](https://github.com/heimdal/heimdal), [Apache Kerby](https://directory.apache.org/kerby/)
- LDAP: [OpenLDAP](https://openldap.org/) ([bitnami/openldap](https://hub.docker.com/r/bitnami/openldap)), [389 Directory Service](https://www.port389.org/), [Apache Directory](https://directory.apache.org/)
- CA/PKI [DogTag](https://www.dogtagpki.org/)
- CA/PKI (ACME protocol): Let's Encrypt ([letsencrypt/pebble](https://hub.docker.com/r/letsencrypt/pebble))
- CA/PKI (EJBCA protocol): ([bitnami/ejbca](https://hub.docker.com/r/bitnami/ejbca)), ([keyfactor/ejbca-ce](https://hub.docker.com/r/keyfactor/ejbca-ce))

(Reminder: the 'bitnami' docker images are published by VMware.)

### Services

A good _de facto_ definition of an Enterprise IdP provider is
the services provided by Microsoft's Active Directory. It is not
limited to this - we'll often see additional support for OAuth,
J, and more.

The minimal services are

- DNS (for service discovery)
- Kerberos (for Authentication)
- LDAP (for storage of AuthN and AuthZ data)
- A Certificate Authority (for managing digital certificates used by servers)
- A Key Manager (for managing encryption keys)

In modern terms LDAP can be viewed as a column database with a few
unusual constraints. The biggest limiting factor is that you should
only use properly registered OIDs. This is not a huge burden - there's
a well-documented process to get a new OID root and you're free to
extend however you like - think of the requirement to register a domain
name and then having freedom to manage your subdomains however you like -
but this is step that's not required by any other database.

### Active Directory 

Active Directory makes extensive use of service discovery and requires
the LDAP server include an additional schema.

In addition Active Directory requires the use of DNS
[SRV records](https://en.wikipedia.org/wiki/SRV_record) in order to find the
required services. This is not difficult to do when you manage your own
DNS servers and it can solve a lot of problems. However it does require
a bit of prep work in both setting up the DNS server and modifying applications
to be able to use SRV records.

### Practical Note

It's now common to use a relational database as the backing store for both the
LDAP server and the Kerberos KDC. In these cases the latter two services are
still available for the applications that support (or require) them but many
applications will directly access the database.

### Java DNS lookup code using JNDI

This code retrieves and SRV records.

__Note: I've successfully used similar code in the past but haven't verified this specific implementation yet.__

```java
import javax.naming.Context;

public class ResourceLocator {
    // This assumes "DNS_SERVER" system property.
    private static final String DEFAULT_DNS_SERVER = "dns://1.1.1.1/";
    private static final String DnsContextFactoryName = "com.sun.jndi.ldap.LdapCtxFactory";
    
    enum Protocol {
        TCP,
        UDP
    }
    
    /**
     * Retrieve SRV records
     *
     * @param service service, e.g., "ldap", "imap", "postgres" 
     * @param protocol ("tcp", "udp")
     * @param domainName domain name to search
     * @return list of matching SRV records
     */
    List<SrvRecord> findSrvRecord(String service, Protocol protocol, String domainName) throws NamingException, IOException {
        final List<SrvRecord> srvRecords = new ArrayList<>();
        final String hostname = String.format("_%s._%s.%s.", service, protocol.name().toLowerCase(), domainName);

        // Find JNDI DirContext
        final Hashtable env = new Hashtable();
        env.put(Context.PROVIDER_URL, System.getProperty(DNS_SERVER, DEFAULT_DNS_SERVER));
        env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_CONTEXT_FACTORY_NAME);
        final DirContext ctx = new InitialDirContext(env);

        try {
            final NamingEnumeration<SearchResult> searchResultsEnumeration = ctx.search(hostname, new String[]{ "SRV" });
            while (searchResultsEnumertion.hasNext()) {
                // check - we may get this as string
                srvRecords.add(SrvRecord.parse(searchResultsEnumeration.next()));
            }
        } catch (NameNotFoundException e) {
            // leave srvRecords collection empty
        }
            
        return srvRecords;
    }
}

@Data
public class SrvRecord {
    private String service; // the symbolic name of the desired service.
    private String proto;   // the transport protocol of the desired service; this is usually either TCP or UDP.
    private String name;    // the domain name for which this record is valid, ending in a dot.
    private int ttl;       // standard DNS time to live field.
    private int priority;   // the priority of the target host, lower value means more preferred.
    private int weight;     // A relative weight for records with the same priority, higher value means higher chance of getting picked.
    private int port;       // the TCP or UDP port on which the service is to be found.
    private String target;  // the canonical hostname of the machine providing the service, ending in a dot. 
    
    public SrvRecord parse(SearchResult searchResult) {
        final SrvRecord record = new SrvRecord();    
        // format is '_service._proto.name. ttl IN SRV priority weight port target.'
        // extract attributes
        return record;
    }
}

```


## OpenLDAP Module

This module wraps the bitnami/openldap docker image.

## Tasks

- [x] Anonymous access
- [x] Admin access (simple authentication)
- [ ] Add users (simple authentication)
- [ ] Enable or Require TLS
  - [ ] Provide server cert
  - [ ] Provide third-party certs (/opt/bitnami/openldap/certs/)
- [ ] Add additional schemas
  - [ ] Active Directory schema
  - [ ] Kerberos
- [ ] Advanced authentication
- [ ] Run initialization scripts (/docker-entrypoint-initdb.d/)

## Server Functionality Tests

- [x] Anonymous access
- [x] Admin access (simple authentication)
- [x] List ObjectClasses ('proof of life', etc)
- [ ] Access using TLS
- [ ] Access using stronger authentication

## Additional Tests

(Hamcrest matchers...)

- [ ] User tasks
  - [ ] List
  - [ ] Add
  - [ ] Remove
- [ ] Group tasks
  - [ ] List
  - [ ] Add
  - [ ] Remove
  - [ ] Add user to group
  - [ ] Remove user from group
