package com.graphql.java.examples.performance.checks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * The controller that exposes the GET and POST /graphql endpoints
 *
 * What is special about this controller is that the request methods return a {@link Callable} wrapping the actual
 * value maps. This is to activate the global timeout property, defined by `spring.mvc.async.request-timeout` in the
 * `application.properties` file.
 */
@RestController
public class GraphQLController {
    private final GraphQL graphql;
    private final ObjectMapper objectMapper;

    @Autowired
    public GraphQLController(GraphQL graphql, ObjectMapper objectMapper) {
        this.graphql = graphql;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(value = "/graphql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public Callable<Map<String, Object>> graphqlGET(@RequestParam("query") String query,
                                                    @RequestParam(value = "operationName", required = false) String operationName,
                                                    @RequestParam("variables") String variablesJson
    ) throws IOException {
        final Map<String, Object> variables = new LinkedHashMap<>();

        if (variablesJson != null) {
            variables.putAll(objectMapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {}));
        }

        return () -> executeGraphqlQuery(query, operationName, variables);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public Callable<Map<String, Object>> graphql(@RequestBody Map<String, Object> body) {

        final String query = (String) body.get("query");

        String operationName = (String) body.get("operationName");

        final Map<String, Object> variables = new LinkedHashMap<>();

        if (body.get("variables") != null) {
            variables.putAll((Map<String, Object>) body.get("variables"));
        }

        return () -> executeGraphqlQuery(query, operationName, variables);
    }

    private Map<String, Object> executeGraphqlQuery(String query, String operationName, Map<String, Object> variables) {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(operationName)
                .variables(variables)
                .build();
        return this.graphql.execute(executionInput).toSpecification();
    }


}
