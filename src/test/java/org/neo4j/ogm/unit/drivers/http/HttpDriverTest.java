package org.neo4j.ogm.unit.drivers.http;

import org.junit.Test;
import org.neo4j.ogm.cypher.query.DefaultGraphModelRequest;
import org.neo4j.ogm.driver.api.driver.Driver;
import org.neo4j.ogm.driver.api.response.Response;
import org.neo4j.ogm.driver.impl.model.GraphModel;
import org.neo4j.ogm.testutil.TestDriverFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author vince
 */
public class HttpDriverTest {

    private Driver driver = TestDriverFactory.driver("http");

    @Test
    public void shouldGetGraphModelResponse() {

        Response<GraphModel> response = driver.requestHandler().execute(new DefaultGraphModelRequest("CREATE p=(n:ITEM {name:'item 1'})-[r:LINK {weight:4}]->(m:ITEM {sizes: [1,5,11], colours: ['red', 'green', 'blue']}) RETURN p"));

        GraphModel model;

        while ((model = response.next()) != null) {
            assertNotNull(model);
            assertEquals(2, model.getNodes().size());
            assertEquals(1, model.getRelationships().size());
        }
        response.close();
    }


}
