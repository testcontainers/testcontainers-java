import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Created by rnorth on 18/02/2016.
 */
@Path("/foo")
public class JerseyResource {

    @GET
    public String getMessage() {
        return "bar";
    }
}
