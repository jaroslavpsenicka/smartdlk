package cz.csas.smartdlk.service;

import cz.csas.smartdlk.model.Rule;

import java.util.List;

public interface RuleService {

    List<Rule> getRules();
    void activate(String name);
    void deactivate(String name);

    List<String> prepareMigration(Rule rule) throws MigrationNotPossibleException;
    void deploy(Rule rule);
}
