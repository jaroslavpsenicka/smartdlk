package cz.csas.smartdlk.annotation;

import cz.csas.smartdlk.model.Rule;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DiscriminatorValid.Validator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscriminatorValid {

    String message() default "discriminator required for multiple events";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DiscriminatorValid, Rule> {

        public void initialize(DiscriminatorValid discriminator) {
        }

        public boolean isValid(Rule rule, ConstraintValidatorContext context) {
            boolean oneEvent = rule.getEvents() != null && rule.getEvents().size() == 1;
            boolean moreEvents = rule.getEvents() != null && rule.getEvents().size() > 1;
            boolean discriminator = StringUtils.isNotEmpty(rule.getDiscriminator());
            return oneEvent || ( moreEvents && discriminator );
        }

    }
}