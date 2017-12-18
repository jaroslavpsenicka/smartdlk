package cz.csas.smartdlk;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.service.RuleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class RuleServiceControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Rule rule;
    private MockMvc mockMvc;

    @Before
    public void before() throws Exception {
        jdbcTemplate.execute("drop table GDPR if exists");
        rule = readResource("/def1.json");
        ruleService.deploy(rule);
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void getRules() throws Exception {
        mockMvc.perform(get("/api/rules"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void activate() throws Exception {
        mockMvc.perform(post("/api/rules/GDPR/activate"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/rules"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].active", is(true)));
        mockMvc.perform(post("/api/rules/GDPR/deactivate"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/rules"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].active", is(false)));
    }

    private Rule readResource(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        return objectMapper.readValue(stream, Rule.class);
    }

}
