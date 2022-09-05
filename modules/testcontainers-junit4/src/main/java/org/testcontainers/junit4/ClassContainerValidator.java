package org.testcontainers.junit4;

import org.junit.runners.model.FrameworkField;
import org.junit.validator.AnnotationValidator;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;

import java.util.ArrayList;
import java.util.List;

public class ClassContainerValidator extends AnnotationValidator {

    @Override
    public List<Exception> validateAnnotatedField(FrameworkField field) {
        List<Exception> errors = new ArrayList<>();
        if (!field.isStatic()) {
            errors.add(new Exception("field " + field.getName() + " is not static"));
        }
        if (!field.isPublic()) {
            errors.add(new Exception("field " + field.getName() + " is not public"));
        }
        if (!Startable.class.isAssignableFrom(field.getType()) && !Network.class.isAssignableFrom(field.getType())) {
            errors.add(new Exception("field " + field.getName() + " is not assignable from Startable or Network"));
        }
        return errors;
    }
}
