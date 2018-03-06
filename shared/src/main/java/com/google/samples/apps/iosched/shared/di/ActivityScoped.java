/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.shared.di;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * By default, if no scope annotation is present, the injector creates an instance , uses the
 * instance for one injection, and then forgets it. If a scope annotation is present, the
 * injector may retain the instance for possible reuse in a later injection.
 * (from: https://docs.oracle.com/javaee/6/api/javax/inject/Scope.html)
 *
 * Scopes TL;DR:
 * No scope = new instance created every time
 * [@Singleton] = only one instance
 * [@CustomScope] = instance reused depending on the component’s lifecycle
 *
 * In Dagger, an unscoped component cannot depend on a scoped component. As
 * AppComponent is a scoped component ({@code @Singleton}, we create a custom
 * scope to be used by all fragment components. Additionally, a component with a specific scope
 * cannot have a sub component with the same scope.
 *
 * The ActivityScoped scoping annotation specifies that the lifespan of a dependency be the same
 * as that of an Activity. This is used to annotate dependencies that behave like a singleton
 * within the lifespan of an Activity.
 *
 * [@Singleton] is used to specify that the lifespan of a dependency be the same as that of the
 * Application.
 */
@Documented
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityScoped {
}
