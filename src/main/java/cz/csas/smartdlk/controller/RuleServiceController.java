package cz.csas.smartdlk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.csas.smartdlk.model.Rule;
import cz.csas.smartdlk.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
public class RuleServiceController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping(value = "/api/rules", method = RequestMethod.GET)
    public List<Rule> rules() {
        return ruleService.getRules();
    }

    @RequestMapping(value = "/api/rules", method = RequestMethod.POST)
    public Rule upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ruleService.deploy(objectMapper.readValue(file.getInputStream(), Rule.class));
    }

    @RequestMapping(value = "/api/rules/{ruleName}/activate", method = RequestMethod.POST)
    public Rule enableRule(@PathVariable String ruleName) {
        return ruleService.activate(ruleName);
    }

    @RequestMapping(value = "/api/rules/{ruleName}/deactivate", method = RequestMethod.POST)
    public Rule disableRule(@PathVariable String ruleName) {
        return ruleService.deactivate(ruleName);
    }

}
