package ac.simons.neo4j.demos.plugins;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class HelloWorld {

    @UserFunction("ac.simons.helloWorld")
    @Description("Simple Hello World")
    public String helloWorld(@Name("name") String name) {

        return "Hello, " + name;
    }
}
