package org.graphfoundation.ongdb.demos.plugins;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class HelloWorld {

    @UserFunction("gf.tc.helloWorld")
    @Description("Simple Hello World")
    public String helloWorld(@Name("name") String name) {

        return "Hello, " + name;
    }
}
