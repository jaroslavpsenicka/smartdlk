package cz.csas.smartdlk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.csas.smartdlk.model.Model;
import cz.csas.smartdlk.model.ModelType;
import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.model.RuleEntity;
import cz.csas.smartdlk.repository.RuleEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleServiceImpl implements RuleService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @Cacheable("rules")
    public List<Rule> getRules() {
        return ruleEntityRepository.findAll().stream()
            .map(e -> getRule(e))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Rule getRule(String ruleName) {
        return getRule(ruleEntityRepository.findByName(ruleName)
            .orElseThrow(() -> new IllegalArgumentException("not a rule: " + ruleName)));
    }

    @Transactional
    @CacheEvict(cacheNames = "rules", allEntries = true)
    public Rule deploy(Rule rule) {
        Assert.notNull(rule, "no rule given");
        try {
            RuleEntity ruleEntity = ruleEntityRepository.findByName(rule.getName()).orElse(new RuleEntity());
            ruleEntity.setName(rule.getName());
            ruleEntity.setRule(objectMapper.writeValueAsBytes(rule));
            ruleEntity.setActive(false);
            prepareMigration(rule).forEach(sql -> jdbcTemplate.execute(sql));
            ruleEntityRepository.save(ruleEntity);
            return rule;
        } catch (IOException ex) {
            log.error("Error writing rule {}", rule, ex);
            throw new IllegalStateException("error processing rule " + rule.getName(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<String> prepareMigration(Rule rule) throws MigrationNotPossibleException {
        if (tableExists(rule.getName())) {
            List<String> migration = new ArrayList<>();
            String query = "show columns from " + rule.getName();
            Map<String, Map> existing = jdbcTemplate.queryForList(query).stream()
                .collect(Collectors.toMap(c -> String.valueOf(c.get("FIELD")).toUpperCase(), c -> c));
            rule.getModel().forEach(m -> {
                if (existing.containsKey(m.getName().toUpperCase())) {
                    Map column = existing.remove(m.getName().toUpperCase());
                    if (!columnTypeMatches(m.getType(), String.valueOf(column.get("TYPE")))) {
                        throw new MigrationNotPossibleException("Migration of table " + rule.getName() +
                            " is not possible due to column " + m.getName() + " type mismatch: " + m.getType() +
                            " vs " + column.get("TYPE"));
                    }
                } else migration.add(addColumn(rule.getName(), createColumn(m)));
            });

            existing.values().forEach(c -> migration.add(removeColumn(rule.getName(), String.valueOf(c.get("FIELD")))));
            return migration;

        } else return createNewRuleTable(rule);
    }

    @Transactional
    @CacheEvict(cacheNames = "rules", allEntries = true)
    public Rule activate(String ruleName) {
        return setActive(ruleName, true);
    }

    @Transactional
    @CacheEvict(cacheNames = "rules", allEntries = true)
    public Rule deactivate(String ruleName) {
        return setActive(ruleName, false);
    }

    private Rule getRule(RuleEntity e) {
        try {
            Rule rule = objectMapper.readValue(e.getRule(), Rule.class);
            rule.setActive(e.isActive());
            return rule;
        } catch (IOException ex) {
            log.error("error reading json", ex);
            throw new IllegalStateException("error reading json", ex);
        }
    }

    private Rule setActive(String ruleName, boolean activeFlag) {
        RuleEntity ruleEntity = ruleEntityRepository.findByName(ruleName)
            .orElseThrow(() -> new IllegalArgumentException("no such rule: " + ruleName));
        ruleEntity.setActive(activeFlag);
        ruleEntityRepository.save(ruleEntity);
        return getRule(ruleEntity);
    }

    private boolean columnTypeMatches(ModelType type, String existingColumnType) {
        switch (type) {
            case string:
                return existingColumnType.toUpperCase().startsWith("VARCHAR");
            case date:
                return existingColumnType.toUpperCase().startsWith("DATE");
        }

        return false;
    }

    private boolean tableExists(String name) {
        return jdbcTemplate.queryForList("show tables").stream()
            .anyMatch(t -> name.equalsIgnoreCase(String.valueOf(t.get("TABLE_NAME"))));
    }

    private List<String> createNewRuleTable(Rule rule) {
        StringBuffer command = new StringBuffer();
        command.append("create table ");
        command.append(rule.getName());
        command.append(" (");
        rule.getModel().forEach(m -> {
            command.append(createColumn(m));
            command.append(",");
        });
        command.append(")");
        return Collections.singletonList(command.toString());
    }

    private String createColumn(Model model) {
        switch (model.getType()) {
            case string:
                return model.getName().toUpperCase() + " varchar(255)";
            case date:
                return model.getName().toUpperCase() + " date";
        }

        throw new IllegalArgumentException("illegal type: " + model.getType());
    }

    private String addColumn(String name, String column) {
        return "alter table " + name + " add column " + column;
    }

    private String removeColumn(String name, String column) {
        return "alter table " + name + " drop column " + column;
    }

}
