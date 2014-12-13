/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.aspectj;

import junit.framework.TestCase;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringConfiguredWithAutoProxyingTests extends TestCase {

	@Override
	protected void setUp() throws Exception {
		new ClassPathXmlApplicationContext("org/springframework/beans/factory/aspectj/springConfigured.xml");
	}

	public void testSpringConfiguredAndAutoProxyUsedTogether() {
		; // set up is sufficient to trigger failure if this is going to fail...
	}
}
