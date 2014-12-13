/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.junit4;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.Pet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.springframework.test.transaction.TransactionTestUtils.*;

/**
 * Combined integration test for {@link AbstractJUnit4SpringContextTests} and
 * {@link AbstractTransactionalJUnit4SpringContextTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration
public class ConcreteTransactionalJUnit4SpringContextTests extends AbstractTransactionalJUnit4SpringContextTests
		implements BeanNameAware, InitializingBean {

	protected static final String BOB = "bob";
	protected static final String JANE = "jane";
	protected static final String SUE = "sue";
	protected static final String LUKE = "luke";
	protected static final String LEIA = "leia";
	protected static final String YODA = "yoda";

	private boolean beanInitialized = false;

	private String beanName = "replace me with [" + getClass().getName() + "]";

	private Employee employee;

	@Autowired
	private Pet pet;

	@Autowired(required = false)
	protected Long nonrequiredLong;

	@Resource
	protected String foo;

	protected String bar;


	protected static int clearPersonTable(final JdbcTemplate jdbcTemplate) {
		return JdbcTestUtils.deleteFromTables(jdbcTemplate, "person");
	}

	protected static void createPersonTable(final JdbcTemplate jdbcTemplate) {
		try {
			jdbcTemplate.update("CREATE TABLE person (name VARCHAR(20) NOT NULL, PRIMARY KEY(name))");
		}
		catch (DataAccessException dae) {
			/* ignore */
		}
	}

	protected static int countRowsInPersonTable(final JdbcTemplate jdbcTemplate) {
		return JdbcTestUtils.countRowsInTable(jdbcTemplate, "person");
	}

	protected static int addPerson(final JdbcTemplate jdbcTemplate, final String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	protected static int deletePerson(final JdbcTemplate jdbcTemplate, final String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	@Override
	@Resource
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Autowired
	protected final void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	@Resource
	protected final void setBar(final String bar) {
		this.bar = bar;
	}

	@Override
	public final void setBeanName(final String beanName) {
		this.beanName = beanName;
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		this.beanInitialized = true;
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyApplicationContext() {
		assertInTransaction(false);
		assertNotNull("The application context should have been set due to ApplicationContextAware semantics.",
				super.applicationContext);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyBeanInitialized() {
		assertInTransaction(false);
		assertTrue("This test bean should have been initialized due to InitializingBean semantics.",
				this.beanInitialized);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyBeanNameSet() {
		assertInTransaction(false);
		assertEquals("The bean name of this test instance should have been set to the fully qualified class name "
				+ "due to BeanNameAware semantics.", getClass().getName(), this.beanName);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyAnnotationAutowiredFields() {
		assertInTransaction(false);
		assertNull("The nonrequiredLong property should NOT have been autowired.", this.nonrequiredLong);
		assertNotNull("The pet field should have been autowired.", this.pet);
		assertEquals("Fido", this.pet.getName());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyAnnotationAutowiredMethods() {
		assertInTransaction(false);
		assertNotNull("The employee setter method should have been autowired.", this.employee);
		assertEquals("John Smith", this.employee.getName());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyResourceAnnotationWiredFields() {
		assertInTransaction(false);
		assertEquals("The foo field should have been wired via @Resource.", "Foo", this.foo);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public final void verifyResourceAnnotationWiredMethods() {
		assertInTransaction(false);
		assertEquals("The bar method should have been wired via @Resource.", "Bar", this.bar);
	}

	@BeforeTransaction
	public void beforeTransaction() {
		assertEquals("Verifying the number of rows in the person table before a transactional test method.", 1,
				countRowsInPersonTable(super.jdbcTemplate));
		assertEquals("Adding yoda", 1, addPerson(super.jdbcTemplate, YODA));
	}

	@Before
	public void setUp() throws Exception {
		assertEquals("Verifying the number of rows in the person table before a test method.",
				(inTransaction() ? 2 : 1), countRowsInPersonTable(super.jdbcTemplate));
	}

	@Test
	public void modifyTestDataWithinTransaction() {
		assertInTransaction(true);
		assertEquals("Adding jane", 1, addPerson(super.jdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(super.jdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table in modifyTestDataWithinTransaction().", 4,
				countRowsInPersonTable(super.jdbcTemplate));
	}

	@After
	public void tearDown() throws Exception {
		assertEquals("Verifying the number of rows in the person table after a test method.",
				(inTransaction() ? 4 : 1), countRowsInPersonTable(super.jdbcTemplate));
	}

	@AfterTransaction
	public void afterTransaction() {
		assertEquals("Deleting yoda", 1, deletePerson(super.jdbcTemplate, YODA));
		assertEquals("Verifying the number of rows in the person table after a transactional test method.", 1,
				countRowsInPersonTable(super.jdbcTemplate));
	}


	public static class DatabaseSetup {

		@Resource
		public void setDataSource(DataSource dataSource) {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			createPersonTable(jdbcTemplate);
			clearPersonTable(jdbcTemplate);
			addPerson(jdbcTemplate, BOB);
		}
	}

}
