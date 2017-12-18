package cz.csas.smartdlk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import cz.csas.smartdlk.model.EventCondition;
import cz.csas.smartdlk.model.Model;
import cz.csas.smartdlk.model.ModelType;
import cz.csas.smartdlk.model.Rule;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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

    public void handle(GenericRecord record) {
        ruleService.getRules().forEach(rule -> handle(record, rule));
    }

    private void handle(GenericRecord record, Rule rule) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        record.getSchema().getFields().forEach(f -> engine.put(f.name(), record.get(f.name())));
        Optional<EventCondition> matchingCondition = rule.getEvents().stream()
            .filter(condition -> matches(condition, engine)).findFirst();
        if (matchingCondition.isPresent()) try {
            if (matchingCondition.get().getTrigger()) {
                fireProcessing(record, rule);
            } else saveRecord(record);
        } catch (Exception ex) {
            log.error("Error writing record: {}", record, ex);
        }
    }

    private boolean matches(EventCondition condition, ScriptEngine engine) {
        try {
            Object result = engine.eval(condition.getCondition());
            log.info("Expression: {} evaluated to {}", condition.getCondition(), result);
            return "true".equalsIgnoreCase(result.toString());
        } catch (ScriptException ex) {
            log.error("Error evaluating expression: {}", condition.getCondition(), ex);
        }

        return false;
    }

    private void saveRecord(GenericRecord record) {
        throw new NotImplementedException("no saving yet");
    }

    private void fireProcessing(GenericRecord monitorEvent, Rule rule) throws JsonProcessingException {
        String json = createJson(monitorEvent);
        List<String> columnNames = rule.getModel().stream()
            .sorted(Comparator.comparing(Model::getName))
            .map(m -> m.getName())
            .collect(Collectors.toList());
        List<Object> values = rule.getModel().stream()
            .map(m -> readValue(json, m))
            .collect(Collectors.toList());
        log.info("Writing {} data {}", columnNames, values);
        jdbcTemplate.update("insert into " + rule.getName() + " (" + StringUtils.join(columnNames, ", ") +
            ") values (" + StringUtils.join(Collections.nCopies(columnNames.size(), "?"), ',') + ")",
            values.toArray());
    }

    private Object readValue(String node, Model m) {
        Object value = JsonPath.read(node, m.getMapping());
        if (m.getType() == ModelType.date) {
            return new Date(value instanceof Integer ? (Integer) value : (Long) value);
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            return array.size() == 1 ? array.get(0) : array;
        }

        return value;
    }

    private String createJson(GenericRecord monitorEvent) throws JsonProcessingException {
        ObjectNode node = objectMapper.createObjectNode();
        monitorEvent.getSchema().getFields().forEach(f -> {
            Object element = createJsonField(f, monitorEvent);
            if (element instanceof String) node.put(f.name(), (String)element);
            if (element instanceof Date) node.put(f.name(), ((Date)element).getTime());
            if (element instanceof Long) node.put(f.name(), (Long)element);
            if (element instanceof JsonNode) node.set(f.name(), (JsonNode)element);
        });

        return objectMapper.writeValueAsString(node);
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
