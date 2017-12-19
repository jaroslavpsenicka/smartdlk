package cz.csas.smartdlk.service;

import cz.csas.smartdlk.model.Rule;

import java.util.List;

public interface RuleService {

    List<Rule> getRules();
    Rule getRule(String ruleName);
    Rule deploy(Rule rule);
    Rule activate(String name);
    Rule deactivate(String name);

    List<String> prepareMigration(Rule rule) throws MigrationNotPossibleException;
}
