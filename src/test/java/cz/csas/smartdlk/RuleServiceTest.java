package cz.csas.smartdlk;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.model.RuleEntity;
import cz.csas.smartdlk.repository.RuleEntityRepository;
import cz.csas.smartdlk.service.MigrationNotPossibleException;
import cz.csas.smartdlk.service.RuleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@Transactional
@SpringBootTest
@RunWith(SpringRunner.class)
public class RuleServiceTest {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    private Rule rule;

    @Before
    public void before() throws IOException {
        rule = readResource("/def1.json");
        jdbcTemplate.execute("drop table GDPR if exists");
    }

    @Test
    public void deploy() throws IOException {
        ruleService.deploy(rule);
        Optional<RuleEntity> ruleEntity = ruleEntityRepository.findByName("GDPR");
        assertNotNull(ruleEntity.get().getId());
        assertNotNull(ruleEntity.get().getName());
        assertNotNull(ruleEntity.get().getRule());
    }

    @Test
    public void deployAndCreateTable() {
        ruleService.deploy(rule);
        Set<Object> tables = jdbcTemplate.queryForList("show tables").stream()
            .map(t -> t.get("TABLE_NAME"))
            .collect(Collectors.toSet());
        assertTrue(tables.contains("GDPR"));
        Set<Object> columns = jdbcTemplate.queryForList("show columns from GDPR").stream()
            .map(t -> t.get("FIELD"))
            .collect(Collectors.toSet());
        assertTrue(columns.contains("CREATEDAT"));
        assertTrue(columns.contains("COMPLETEDAT"));
        assertTrue(columns.contains("REQUESTTYPE"));
        assertTrue(columns.contains("RESOLUTIONTYPE"));
    }

    @Test
    public void deployAndAddAllColumns() {
        jdbcTemplate.execute("create table GDPR");
        ruleService.deploy(rule);
        Set<Object> tables = jdbcTemplate.queryForList("show tables").stream()
            .map(t -> t.get("TABLE_NAME"))
            .collect(Collectors.toSet());
        assertTrue(tables.contains("GDPR"));
        Set<Object> columns = jdbcTemplate.queryForList("show columns from GDPR").stream()
            .map(t -> t.get("FIELD"))
            .collect(Collectors.toSet());
        assertTrue(columns.contains("CREATEDAT"));
        assertTrue(columns.contains("COMPLETEDAT"));
        assertTrue(columns.contains("REQUESTTYPE"));
        assertTrue(columns.contains("RESOLUTIONTYPE"));
    }

    @Test
    public void deployAndAddColumn() {
        jdbcTemplate.execute("create table GDPR (createdAt date)");
        ruleService.deploy(rule);
        Set<Object> tables = jdbcTemplate.queryForList("show tables").stream()
            .map(t -> t.get("TABLE_NAME"))
            .collect(Collectors.toSet());
        assertTrue(tables.contains("GDPR"));
        Set<Object> columns = jdbcTemplate.queryForList("show columns from GDPR").stream()
            .map(t -> t.get("FIELD"))
            .collect(Collectors.toSet());
        assertTrue(columns.contains("CREATEDAT"));
        assertTrue(columns.contains("COMPLETEDAT"));
        assertTrue(columns.contains("REQUESTTYPE"));
        assertTrue(columns.contains("RESOLUTIONTYPE"));
    }

    @Test
    public void deployAndRemoveColumn() {
        jdbcTemplate.execute("create table GDPR (weird varchar(255))");
        ruleService.deploy(rule);
        Set<Object> tables = jdbcTemplate.queryForList("show tables").stream()
            .map(t -> t.get("TABLE_NAME"))
            .collect(Collectors.toSet());
        assertTrue(tables.contains("GDPR"));
        Set<Object> columns = jdbcTemplate.queryForList("show columns from GDPR").stream()
            .map(t -> t.get("FIELD"))
            .collect(Collectors.toSet());
        assertTrue(columns.contains("CREATEDAT"));
        assertTrue(columns.contains("COMPLETEDAT"));
        assertTrue(columns.contains("REQUESTTYPE"));
        assertTrue(columns.contains("RESOLUTIONTYPE"));
    }

    @Test
    public void deployAndCannotMigrate() {
        jdbcTemplate.execute("create table GDPR (createdAt varchar(255))");
        try {
            ruleService.deploy(rule);
            fail();
        } catch (MigrationNotPossibleException ex) {
            Set<Object> tables = jdbcTemplate.queryForList("show tables").stream()
                .map(t -> t.get("TABLE_NAME"))
                .collect(Collectors.toSet());
            assertTrue(tables.contains("GDPR"));
            Set<Object> columns = jdbcTemplate.queryForList("show columns from GDPR").stream()
                .map(t -> t.get("FIELD"))
                .collect(Collectors.toSet());
            assertTrue(columns.contains("CREATEDAT"));
            assertFalse(columns.contains("COMPLETEDAT"));
            assertFalse(columns.contains("REQUESTTYPE"));
            assertFalse(columns.contains("RESOLUTIONTYPE"));
        }
    }

    @Test
    public void listRules() {
        ruleService.deploy(rule);

        List<Rule> deployedRules = ruleService.getRules();
        assertEquals(1, deployedRules.size());
        Cache cache = cacheManager.getCache("rules");
        assertNotNull(cache.get(new SimpleKey()));
    }

    @Test
    public void activation() {
        ruleService.deploy(rule);
        ruleService.activate("GDPR");
        assertTrue(ruleService.getRules().stream()
            .filter(r -> "GDPR".equals(r.getName()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("no GDPR rule")).isActive());
        ruleService.deactivate("GDPR");
        assertFalse(ruleService.getRules().stream()
            .filter(r -> "GDPR".equals(r.getName()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("no GDPR rule")).isActive());
    }

    private Rule readResource(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        return objectMapper.readValue(stream, Rule.class);
    }

}
