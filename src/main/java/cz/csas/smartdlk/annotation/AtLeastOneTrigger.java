package cz.csas.smartdlk.annotation;

import cz.csas.smartdlk.model.EventCondition;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.util.List;

@Documented
@Constraint(validatedBy = AtLeastOneTrigger.Validator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneTrigger {

    String message() default "at least one trigger is required";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AtLeastOneTrigger, List<EventCondition>> {

        public void initialize(AtLeastOneTrigger discriminator) {
        }

        public boolean isValid(List<EventCondition> conditions, ConstraintValidatorContext context) {
            return (conditions == null || conditions.size() <= 0) || conditions.stream().anyMatch(EventCondition::isTrigger);
        }

    }
}