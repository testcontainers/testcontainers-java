package org.testcontainers.containers;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information about the LDAP Schema's ObjectClasses
 * <p>
 * Implementation Note
 * </p>
 * <p>
 * The parser current fails when the definition includes multiple names or multiple superior object classes.
 * The known examples are:
 * <ul>
 * <li>( 1.3.6.1.4.1.4203.1.4.1 NAME ( 'OpenLDAProotDSE' 'LDAProotDSE' ) DESC 'OpenLDAP Root DSE object' SUP top STRUCTURAL MAY cn )</li>
 * <li>( 0.9.2342.19200300.100.4.4 NAME ( 'pilotPerson' 'newPilotPerson' ) SUP person STRUCTURAL MAY ( userid $ textEncodedORAddress $ rfc822Mailbox $ favouriteDrink $ roomNumber $ userClass $ homeTelephoneNumber $ homePostalAddress $ secretary $ personalTitle $ preferredDeliveryMethod $ businessCategory $ janetMailbox $ otherMailbox $ mobileTelephoneNumber $ pagerTelephoneNumber $ organizationalStatus $ mailPreferenceOption $ personalSignature ) )</li>
 * <li>( 0.9.2342.19200300.100.4.20 NAME 'pilotOrganization' SUP ( organization $ organizationalUnit ) STRUCTURAL MAY buildingName )</li>
 * </ul>
 */

@Data
public class ObjectClassInformation {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectClassInformation.class);

    private static final String KEYWORD_DESCRIPTION = "DESC";
    private static final String KEYWORD_MAY = "MAY";
    private static final String KEYWORD_MUST = "MUST";
    private static final String KEYWORD_NAME = "NAME";
    private static final String KEYWORD_SUP = "SUP";

    public enum Type {
        ABSTRACT,
        STRUCTURAL,
        AUXILIARY
    };

    // this does not match ALL of the objectClass definitions!
    private static final Pattern PATTERN = Pattern.compile("^\\( " +
        "([0-9\\.]+) " +  // required
        KEYWORD_NAME + " '([^']+)'" + // required
        "( " + KEYWORD_DESCRIPTION + " '([^']+)')?" +
        "( " + KEYWORD_SUP + " ([^ ]+))?" +
        // " (" + String.join("|", Type.values()) + ")" +
        " (ABSTRACT|STRUCTURAL|AUXILIARY)" +
        "( " + KEYWORD_MUST + " (([^ (]+)|(\\( ([^)]+)\\))))?" +
        "( " + KEYWORD_MAY + " (([^ (]+)|(\\( ([^)]+)\\))))?" +
        "(.*)" +
        " \\)$");

    // remember that Spring#split uses a regex!
    private static final String DELIMITER = " \\$ ";

    private String oid;

    private String name;

    private String description;

    private String parent;

    private Type type;

    private List<String> mustValues = new ArrayList<>();

    private List<String> mayValues = new ArrayList<>();

    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    /**
     * Parse definition from ObjectClass attribute
     *
     * @param definition
     * @return objectClass information
     * @throws IllegalArgumentException unsupported definition
     */
    public static ObjectClassInformation parse(String definition) {
        final ObjectClassInformation info = new ObjectClassInformation();

        final Matcher m = PATTERN.matcher(definition);
        if (!m.matches()) {
            // LOG.info(PATTERN.pattern());
            throw new IllegalArgumentException("Unsupported definition: " + definition);
        }

        info.oid = m.group(1);
        info.name = m.group(2);
        info.description = m.group(4);
        info.parent = m.group(6);
        info.setType(m.group(7));

        // singleton
        if (StringUtils.isNotBlank(m.group(10))) {
            info.mustValues.add(m.group(10));
        }

        // list
        if (StringUtils.isNotBlank(m.group(12))) {
            for (String element : m.group(12).trim().split(DELIMITER)) {
                info.mustValues.add(element.trim());
            }
        }

        // singleton
        if (StringUtils.isNotBlank(m.group(15))) {
            info.mayValues.add(m.group(15));
        }

        // list
        if (StringUtils.isNotBlank(m.group(17))) {
            for (String element : m.group(17).trim().split(DELIMITER)) {
                info.mayValues.add(element.trim());
            }
        }

        return info;
    }

    /**
     * Regenerate definition
     * @return
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("( ");
        sb.append(oid);
        sb.append(" ");
        sb.append(KEYWORD_NAME);
        sb.append(" '");
        sb.append(name);
        sb.append("' ");
        if (StringUtils.isNotBlank(description)) {
            sb.append(KEYWORD_DESCRIPTION);
            sb.append(" '");
            sb.append(description);
            sb.append("' ");
        }
        if (StringUtils.isNotBlank(parent)) {
            sb.append(KEYWORD_SUP);
            sb.append(" ");
            sb.append(parent);
            sb.append(" ");
        }
        sb.append(type);
        sb.append(" ");

        if (!mustValues.isEmpty()) {
            sb.append(KEYWORD_MUST);
            sb.append(" ");
            if (mustValues.size() == 1) {
                sb.append(mustValues.get(0));
            } else {
                sb.append("( ");
                sb.append(String.join(" $ ", mustValues));
                sb.append(" )");
            }
            sb.append(" ");
        }

        if (!mayValues.isEmpty()) {
            sb.append(KEYWORD_MAY);
            sb.append(" ");
            if (mayValues.size() == 1) {
                sb.append(mayValues.get(0));
            } else {
                sb.append("( ");
                sb.append(String.join(" $ ", mayValues));
                sb.append(" )");
            }
            sb.append(" ");
        }

        sb.append(")");
        return sb.toString();
    }
}
