package org.testcontainers.containers.traits;

import lombok.NonNull;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TraitsSupport<SELF extends Container<SELF>> extends SelfReference<SELF> {

    Comparator<Trait> TRAIT_COMPARATOR = (obj1, obj2) -> {
        if (obj1 instanceof Comparable && obj2 instanceof Comparable) {
            Comparable cmp1 = (Comparable)obj1;
            return cmp1.compareTo(obj2);
        }

        if (!(obj1 instanceof Comparable) && !(obj2 instanceof Comparable)) {
            return 0;
        }

        if (!(obj1 instanceof Comparable)) {
            return -1;
        }

        return 1;
    };

    List<Trait<SELF>> getTraits();

    default <T extends Trait<SELF>> Stream<T> getTraits(Class<T> traitClass) {
        return getTraits().stream().filter(traitClass::isInstance).map(traitClass::cast);
    }

    default <T extends Trait<SELF>> void replaceTraits(Class<T> traitClass, Stream<T> newValues) {
        List<Trait<SELF>> traits = self().getTraits();

        // Remove all other instances
        traits.removeIf(traitClass::isInstance);

        newValues.collect(Collectors.toCollection(() -> traits));
    }

    default <T extends Trait<SELF>> T computeTraitIfAbsent(Class<T> traitClass, Function<Class<T>, T> mappingFunction) {
        return getTrait(traitClass).orElseGet(() -> {
            T trait = mappingFunction.apply(traitClass);

            with(trait);

            return trait;
        });
    }

    default <T extends Trait> Optional<T> getTrait(Class<T> traitClass) {
        return getTraits().stream()
                .filter(trait -> traitClass.isAssignableFrom(trait.getClass()))
                .map(traitClass::cast)
                .findAny();
    }

    default SELF with(@NonNull Trait<SELF> trait) {
        getTraits().add(trait);

        return self();
    }
}
