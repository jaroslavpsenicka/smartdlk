package cz.csas.smartdlk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import cz.csas.smartdlk.model.*;
import cz.csas.smartdlk.repository.EventEntityRepository;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EventProcessor {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ScriptEngineManager scriptEngineManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventEntityRepository eventEntityRepository;

    public void handle(GenericRecord record) {
        ruleService.getRules().forEach(rule -> handle(record, rule));
    }

    @Transactional
    private void handle(GenericRecord record, Rule rule) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        record.getSchema().getFields().forEach(f -> engine.put(f.name(), record.get(f.name())));
        Optional<EventCondition> matchingCondition = rule.getEvents().stream()
            .filter(condition -> matches(condition, engine)).findFirst();
        if (matchingCondition.isPresent()) try {
            if (matchingCondition.get().isTrigger()) {
                fireProcessing(record, rule, matchingCondition.get(), engine);
            } else saveRecord(record, rule, engine, matchingCondition.get());
        } catch (Exception ex) {
            log.error("Error handling record: {}", record, ex);
        }
    }

    private boolean matches(EventCondition condition, ScriptEngine engine) {
        try {
            Object result = engine.eval(condition.getCondition());
            log.info("Expression {} evaluated to {}", condition.getCondition(), result);
            return "true".equalsIgnoreCase(result.toString());
        } catch (ScriptException ex) {
            log.error("Error evaluating expression: {}", condition.getCondition(), ex);
        }

        return false;
    }

    private void saveRecord(GenericRecord record, Rule rule, ScriptEngine engine, EventCondition matchingCondition)
        throws JsonProcessingException, ScriptException {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventName(matchingCondition.getName());
        eventEntity.setDiscriminator(createDiscriminator(rule, engine));
        eventEntity.setData(objectMapper.writeValueAsString(createJson(record)).getBytes());
        log.info("Saving record {}", eventEntity);
        eventEntityRepository.save(eventEntity);
    }

    private String createDiscriminator(Rule rule, ScriptEngine engine) throws ScriptException {
        return String.valueOf(engine.eval(rule.getDiscriminator()));
    }

    private void fireProcessing(GenericRecord record, Rule rule, EventCondition matchingCondition, ScriptEngine engine) throws IOException, ScriptException {
        JsonNode json = rule.getEvents().size() > 1 ?
            createMultiEventJson(record, matchingCondition, createDiscriminator(rule, engine)) : createJson(record);
        List<String> columnNames = getColumnNames(rule);
        List<Object> values = getValues(rule, objectMapper.writeValueAsString(json));
        log.info("Writing {} data {}", columnNames, values);
        jdbcTemplate.update("insert into " + rule.getName() + " (" + StringUtils.join(columnNames, ", ") +
            ") values (" + StringUtils.join(Collections.nCopies(columnNames.size(), "?"), ',') + ")",
            values.toArray());
    }

    private List<Object> getValues(Rule rule, String json) {
        return rule.getModel().stream()
            .map(m -> readValue(json, m))
            .collect(Collectors.toList());
    }

    private List<String> getColumnNames(Rule rule) {
        return rule.getModel().stream()
            .sorted(Comparator.comparing(Model::getName))
            .map(m -> m.getName())
            .collect(Collectors.toList());
    }

    private Object readValue(String node, Model m) {
        Object result = JsonPath.read(node, m.getMapping());
        if (result instanceof JSONArray) {
            JSONArray array = (JSONArray) result;
            if (array.size() == 0 && !m.isOptional()) throw new IllegalStateException("no value " + m);
            result = array.size() > 0 ? array.get(0) : null;
        }

        if (m.getType() == ModelType.date) {
            return new Date(result instanceof Integer ? (Integer) result : (Long) result);
        }

        return result;
    }

    private ObjectNode createJson(GenericRecord record) throws JsonProcessingException {
        ObjectNode node = objectMapper.createObjectNode();
        record.getSchema().getFields().forEach(f -> {
            Object element = createJsonField(f, record);
            if (element instanceof String) node.put(f.name(), (String)element);
            if (element instanceof Date) node.put(f.name(), ((Date)element).getTime());
            if (element instanceof Long) node.put(f.name(), (Long)element);
            if (element instanceof JsonNode) node.set(f.name(), (JsonNode)element);
        });

        return node;
    }

    private JsonNode createMultiEventJson(GenericRecord record, EventCondition matchingCondition, String discriminator) throws IOException {
        ObjectNode rootNode = createJson(record);
        ObjectNode eventsNode = objectMapper.createObjectNode();
        rootNode.set("events", eventsNode);
        JsonNode rootNodeClone = objectMapper.readValue(objectMapper.writeValueAsString(rootNode), JsonNode.class);
        eventsNode.set(matchingCondition.getName(), rootNodeClone);
        eventEntityRepository.findByDiscriminator(discriminator).forEach(e -> {
            try {
                eventsNode.set(e.getEventName(), objectMapper.readValue(e.getData(), JsonNode.class));
                eventEntityRepository.delete(e);
            } catch (IOException ex) {
                log.error("Error reading saved event {}", e, ex);
            }
        });

        return rootNode;
    }

    private Object createJsonField(Schema.Field f, GenericRecord monitorEvent) {
        String logicalType = f.getProp("logicalType");
        if (f.schema().getType() == Schema.Type.STRING && "json".equalsIgnoreCase(logicalType)) try {
            return objectMapper.readValue((String) monitorEvent.get(f.name()), JsonNode.class);
        } catch (Exception ex) {
            throw new RuntimeException("error processing json: " + monitorEvent.get(f.name()), ex);
        }

        return monitorEvent.get(f.name());
    }

}
