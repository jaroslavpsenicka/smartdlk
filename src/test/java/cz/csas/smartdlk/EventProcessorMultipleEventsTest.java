package cz.csas.smartdlk;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.repository.RuleEntityRepository;
import cz.csas.smartdlk.service.EventProcessor;
import cz.csas.smartdlk.service.RuleService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Transactional
@SpringBootTest
@RunWith(SpringRunner.class)
public class EventProcessorMultipleEventsTest {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private EventProcessor eventProcessor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void before() throws IOException {
        Rule rule = readResource("/def2.json");
        ruleService.deploy(rule);
    }

    @After
    public void after() {
        ruleEntityRepository.deleteAll();
        cacheManager.getCache("rules").clear();
    }

    @Test
    public void multipleEvents() throws IOException {
        GenericRecord event1 = new Record(getClass().getResourceAsStream("/schema.avs"));
        event1.put("caseType", "GDPR");
        event1.put("type", "CASE_UPDATED");
        event1.put("cidla", "SC001");
        event1.put("jsonData", "{\"attributes\":[" +
            "{\"name\": \"requestType\", \"value\": \"ABC\", \"mapping\": \"REQUEST_TYPE\"}, " +
            "{\"name\": \"resolutionType\", \"value\":\"DEF\", \"mapping\": null}" +
        "]}");

        eventProcessor.handle(event1);

        GenericRecord event2 = new Record(getClass().getResourceAsStream("/schema.avs"));
        event2.put("createdAt", new Date(0));
        event2.put("completedAt", new Date(1));
        event2.put("caseType", "GDPR");
        event2.put("type", "CASE_COMPLETED");
        event2.put("cidla", "SC001");
        event2.put("jsonData", "{}");

        eventProcessor.handle(event2);

        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from GDPR");
        assertEquals(1, list.size());
        assertNotNull(list.get(0).get("CREATEDAT"));
        assertNotNull(list.get(0).get("COMPLETEDAT"));
        assertEquals("ABC", list.get(0).get("REQUESTTYPE"));
        assertEquals("DEF", list.get(0).get("RESOLUTIONTYPE"));
    }

    private Rule readResource(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        return objectMapper.readValue(stream, Rule.class);
    }

    private static class Record extends SpecificRecordBase {

        private Schema schema;
        private ArrayList values;

        public Record(InputStream schemaStream) throws IOException {
            schema = Schema.parse(schemaStream);
            values = new ArrayList();
            schema.getFields().forEach(e -> values.add(""));
        }

        public Schema getSchema() {
            return schema;
        }

        public Object get(int i) {
            return values.get(i);
        }

        public void put(int i, Object o) {
            values.set(i, o);
        }
    }

}
