package org.testcontainers.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestContainersRunner extends BlockJUnit4ClassRunner {

    private Object testObject;

    public TestContainersRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        final Statement statement = super.classBlock(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final TestClass testClass = getTestClass();

                testClass.collectAnnotatedFieldValues(
                    null,
                    ClassContainer.class,
                    Startable.class,
                    (member, startable) -> {
                        if (member.isStatic()) {
                            startable.start();
                        }
                    }
                );
                statement.evaluate();

                testClass.collectAnnotatedFieldValues(
                    null,
                    ClassContainer.class,
                    Startable.class,
                    (member, startable) -> {
                        if (member.isStatic()) {
                            startable.stop();
                        }
                    }
                );

                testClass.collectAnnotatedFieldValues(
                    null,
                    ClassContainer.class,
                    Network.class,
                    (member, network) -> {
                        if (member.isStatic()) {
                            network.close();
                        }
                    }
                );
            }
        };
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        final Statement statement = super.methodBlock(method);

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<Throwable>();

                final TestClass testClass = getTestClass();

                testClass.collectAnnotatedFieldValues(
                    testObject,
                    Container.class,
                    Startable.class,
                    (member, startable) -> {
                        if (!member.isStatic()) {
                            startable.start();
                            if (startable instanceof TestLifecycleAware) {
                                final TestLifecycleAware lifecycleAware = (TestLifecycleAware) startable;
                                lifecycleAware.beforeTest(toDescription(describeChild(method)));
                            }
                        }
                    }
                );

                try {
                    statement.evaluate();
                    testClass.collectAnnotatedFieldValues(
                        testObject,
                        Container.class,
                        Startable.class,
                        (member, startable) -> {
                            if (!member.isStatic()) {
                                startable.stop();
                                if (startable instanceof TestLifecycleAware) {
                                    final TestLifecycleAware lifecycleAware = (TestLifecycleAware) startable;
                                    lifecycleAware.afterTest(toDescription(describeChild(method)), Optional.empty());
                                }
                            }
                        }
                    );
                } catch (Throwable e) {
                    errors.add(e);
                    testClass.collectAnnotatedFieldValues(
                        testObject,
                        Container.class,
                        Startable.class,
                        (member, startable) -> {
                            if (!member.isStatic()) {
                                startable.stop();
                                if (startable instanceof TestLifecycleAware) {
                                    final TestLifecycleAware lifecycleAware = (TestLifecycleAware) startable;
                                    lifecycleAware.afterTest(toDescription(describeChild(method)), Optional.of(e));
                                }
                            }
                        }
                    );
                }

                testClass.collectAnnotatedFieldValues(
                    testObject,
                    Container.class,
                    Network.class,
                    (member, network) -> {
                        if (!member.isStatic()) {
                            network.close();
                        }
                    }
                );
                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    @Override
    protected Object createTest() throws Exception {
        testObject = super.createTest();
        return testObject;
    }

    private TestDescription toDescription(Description description) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return description.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return description.getClassName() + "" + description.getMethodName();
            }
        };
    }
}
