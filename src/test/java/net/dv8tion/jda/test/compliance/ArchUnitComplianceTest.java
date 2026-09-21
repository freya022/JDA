/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.test.compliance;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import net.dv8tion.jda.annotations.UnknownNullability;
import net.dv8tion.jda.api.managers.Manager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.IOBiConsumer;
import net.dv8tion.jda.api.utils.IOFunction;
import org.jetbrains.annotations.Contract;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

public class ArchUnitComplianceTest {
    @Test
    void testMethodsThatReturnRestActionHaveCorrectAnnotations() {
        methods()
                .that()
                .haveRawReturnType(assignableTo(RestAction.class))
                .and()
                .arePublic()
                .should()
                .beAnnotatedWith(CheckReturnValue.class)
                .andShould()
                .beAnnotatedWith(Nonnull.class)
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testMethodsThatReturnCompletableFutureHaveCorrectAnnotations() {
        methods()
                .that()
                .haveRawReturnType(assignableTo(CompletableFuture.class))
                .and()
                .arePublic()
                .should()
                .beAnnotatedWith(CheckReturnValue.class)
                .andShould()
                .beAnnotatedWith(Nonnull.class)
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testMethodsThatReturnObjectShouldHaveNullabilityAnnotations() {
        methods()
                .that()
                .haveRawReturnType(assignableTo(Object.class))
                .and()
                .arePublic()
                .and()
                .doNotHaveName("valueOf")
                .and()
                .doNotHaveName("toString")
                .should()
                .beAnnotatedWith(Nonnull.class)
                .orShould()
                .beAnnotatedWith(Nullable.class)
                .orShould()
                .beAnnotatedWith(Contract.class)
                .orShould()
                .beAnnotatedWith(UnknownNullability.class)
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testMethodsThatAcceptObjectShouldHaveNullabilityAnnotations() {
        methods()
                .that()
                .arePublic()
                .and()
                .doNotHaveName("equals")
                .and()
                .doNotHaveName("valueOf")
                .and()
                .doNotHaveName("accept")
                .and()
                .doNotHaveName("test")
                .and()
                .doNotHaveName("formatTo")
                .and()
                .areNotDeclaredIn(IOFunction.class)
                .and()
                .areNotDeclaredIn(IOBiConsumer.class)
                .should(haveNonPrimitiveParametersAnnotatedWithNullability())
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testMethodsThatReturnPrimitivesShouldNotHaveNullabilityAnnotations() {
        methods()
                .that()
                .haveRawReturnType(describe("primitive", JavaClass::isPrimitive))
                .and()
                .arePublic()
                .should()
                .notBeAnnotatedWith(Nonnull.class)
                .andShould()
                .notBeAnnotatedWith(Nullable.class)
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testRestActionClassesFollowNamePattern() {
        classes()
                .that()
                .areAssignableTo(RestAction.class)
                .and()
                .areNotAssignableTo(Manager.class)
                .and()
                .arePublic()
                .should()
                .haveSimpleNameEndingWith("Action")
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testManagerClassesFollowNamePattern() {
        classes()
                .that()
                .areAssignableTo(Manager.class)
                .and()
                .arePublic()
                .should()
                .haveSimpleNameEndingWith("Manager")
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testInternalClassesAreNotInApiPackage() {
        classes()
                .that()
                .arePublic()
                .and()
                .haveSimpleNameEndingWith("Impl")
                .should()
                .resideOutsideOfPackage("net.dv8tion.jda.api..")
                .allowEmptyShould(true)
                .check(SourceSets.getApiClasses());
    }

    @Test
    void testReturnedCollectionsHaveMutabilityAnnotation() {
        methods()
                .that()
                .arePublic()
                .or()
                .areProtected()
                .and(DescribedPredicate.alwaysTrue().as("return mutable types"))
                .and()
                // Overrides with different return/parameter types makes javac generate synthetic bridges,
                // ArchUnit picks them up as it reads the bytecode,
                // we can ignore those as they are inaccessible without reflection.
                .doNotHaveModifier(JavaModifier.SYNTHETIC)
                .should(haveUnmodifiableOrKotlinMutableAnnotation())
                .check(SourceSets.getApiClasses());
    }

    private ArchCondition<JavaMethod> haveNonPrimitiveParametersAnnotatedWithNullability() {
        return new ArchCondition<>("have non-primitive parameters annotated with @Nonnull or @Nullable") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameters().stream()
                        .filter(parameter -> !parameter.getRawType().isPrimitive())
                        .filter(parameter -> !parameter.isAnnotatedWith(Nonnull.class)
                                && !parameter.isAnnotatedWith(Nullable.class)
                                && !parameter.isAnnotatedWith(CheckForNull.class))
                        .forEach(parameter -> events.add(SimpleConditionEvent.violated(
                                method, parameter.getDescription() + " is not annotated with @Nonnull or @Nullable")));
            }
        };
    }

    private static class UnknownMutabilityReturnTypeWalker {
        private static final List<String> MUTABLE_TYPES = List.of(
                InternalNames.ITERABLE,
                InternalNames.ITERATOR,
                InternalNames.COLLECTION,
                InternalNames.LIST,
                InternalNames.SET,
                InternalNames.MAP,
                InternalNames.MAP_ENTRY);

        private final List<TypeAnnotation> typeAnnotations;

        private final List<List<Integer>> typeArgumentChains = new ArrayList<>();
        private final Deque<Integer> currentChain = new ArrayDeque<>();

        private UnknownMutabilityReturnTypeWalker(List<TypeAnnotation> typeAnnotations) {
            this.typeAnnotations = typeAnnotations;
        }

        static List<List<Integer>> walk(List<TypeAnnotation> typeAnnotations, Signature signature) {
            var walker = new UnknownMutabilityReturnTypeWalker(typeAnnotations);
            walker.walk(signature);
            return walker.typeArgumentChains;
        }

        private void walk(Signature signature) {
            if (signature instanceof Signature.ClassTypeSig classTypeSig) {
                if (isMutableType(classTypeSig) && !isCurrentTypeAnnotated(typeAnnotations)) {
                    typeArgumentChains.add(new ArrayList<>(currentChain));
                }

                // Recursion on type arguments (e.g. a list's element type)
                List<Signature.TypeArg> typeArgs = classTypeSig.typeArgs();
                for (int i = 0, typeArgsSize = typeArgs.size(); i < typeArgsSize; i++) {
                    var typeArg = typeArgs.get(i);
                    if (!(typeArg instanceof Signature.TypeArg.Bounded boundedTypeArg)) {
                        continue;
                    }

                    try {
                        currentChain.add(i);
                        walk(boundedTypeArg.boundType());
                    } finally {
                        currentChain.removeLast();
                    }
                }
            }
        }

        private static boolean isMutableType(Signature.ClassTypeSig classTypeSig) {
            return MUTABLE_TYPES.contains(classTypeSig.className());
        }

        private boolean isCurrentTypeAnnotated(List<TypeAnnotation> typeAnnotations) {
            for (var typeAnnotation : typeAnnotations) {
                if (isMutabilityAnnotation(typeAnnotation)) {
                    // There is at least one mutability annotation,
                    // but it needs to be applied on the expected (sub)signature

                    var pathComponents = typeAnnotation.targetPath();

                    // If not all path components are type arguments,
                    // the built path would be incompatible until the walker supports it
                    if (!pathComponents.stream().allMatch(UnknownMutabilityReturnTypeWalker::isTypeArgumentComponent)) {
                        continue;
                    }

                    List<Integer> typeArgumentPath = pathComponents.stream()
                            .map(TypeAnnotation.TypePathComponent::typeArgumentIndex)
                            .toList();

                    if (new ArrayList<>(currentChain).equals(typeArgumentPath)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private static boolean isMutabilityAnnotation(TypeAnnotation typeAnnotation) {
            var annotationName = typeAnnotation.annotation().className();
            return annotationName.equalsString(Descriptors.UNMODIFIABLE)
                    || annotationName.equalsString(Descriptors.UNMODIFIABLE_VIEW)
                    || annotationName.equalsString(Descriptors.MUTABLE);
        }

        private static boolean isTypeArgumentComponent(TypeAnnotation.TypePathComponent pathComponent) {
            return pathComponent.typePathKind() == TypeAnnotation.TypePathComponent.Kind.TYPE_ARGUMENT;
        }
    }

    private static ArchCondition<JavaMethod> haveUnmodifiableOrKotlinMutableAnnotation() {
        return new ArchCondition<>("have @Unmodifiable(View) or Kotlin's @Mutable annotation") {

            private final Map<String, ClassModel> classModelCache = new HashMap<>();

            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                var classModel = loadClassModel(method.getOwner());
                var methodModel = findMethodModel(classModel, method);

                // Collection types has type arguments so it always carries a Signature attribute
                var signature =
                        methodModel.findAttribute(Attributes.signature()).orElse(null);
                if (signature == null) {
                    return;
                }

                var typeArgumentChains = UnknownMutabilityReturnTypeWalker.walk(
                        getTypeAnnotations(methodModel),
                        // Always a ref type since there are type arguments and thus signature
                        signature.asMethodSignature().result());

                if (!typeArgumentChains.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            "Method is missing one or more @Unmodifiable(View) / @Mutable => %s %s.%s(%s) (%s:%s)"
                                    .formatted(
                                            method.getRawReturnType().getSimpleName(),
                                            method.getOwner().getSimpleName(),
                                            method.getName(),
                                            method.getParameterTypes().stream()
                                                    .map(Object::toString)
                                                    .collect(Collectors.joining(", ")),
                                            method.getSourceCodeLocation().getSourceFileName(),
                                            method.getSourceCodeLocation().getLineNumber())));
                }
            }

            private ClassModel loadClassModel(JavaClass javaClass) {
                return classModelCache.computeIfAbsent(javaClass.getFullName(), _ -> {
                    try {
                        var source = javaClass
                                .getSource()
                                .orElseThrow(() -> new AssertionError("No source for class " + javaClass));
                        return ClassFile.of().parse(Path.of(source.getUri()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Nonnull
            private static MethodModel findMethodModel(ClassModel classModel, JavaMethod method) {
                return classModel.methods().stream()
                        .filter(m -> m.methodName().equalsString(method.getName())
                                && m.methodType().equalsString(method.getDescriptor()))
                        .findAny()
                        .orElseThrow(() -> new AssertionError("Could not find matching MethodModel for " + method));
            }

            @Nonnull
            private static List<TypeAnnotation> getTypeAnnotations(MethodModel methodModel) {
                return methodModel
                        .findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
                        .map(RuntimeInvisibleTypeAnnotationsAttribute::annotations)
                        .orElse(Collections.emptyList());
            }
        };
    }
}
